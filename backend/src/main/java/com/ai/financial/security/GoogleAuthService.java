package com.ai.financial.security;

import com.ai.financial.entity.OAuthAccount;
import com.ai.financial.entity.Role;
import com.ai.financial.entity.User;
import com.ai.financial.repository.OAuthAccountRepository;
import com.ai.financial.repository.RoleRepository;
import com.ai.financial.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public GoogleAuthService(UserRepository userRepository,
                             OAuthAccountRepository oauthAccountRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String authenticateWithGoogle(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Invalid ID token.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String subject = payload.getSubject(); // Google User ID
        String email = payload.getEmail();
        
        // Use part of email as username, ensuring it's unique
        String username = email.split("@")[0];

        Optional<OAuthAccount> oauthAccountOpt = oauthAccountRepository.findByProviderAndProviderId("google", subject);

        User user;
        if (oauthAccountOpt.isPresent()) {
            user = oauthAccountOpt.get().getUser();
        } else {
            // Check if user with this email already exists
            Optional<User> existingUser = userRepository.findByUsername(username);
            if (existingUser.isPresent()) {
                // If simple username exists, append a random string
                username = username + "_" + UUID.randomUUID().toString().substring(0, 5);
            }

            user = new User();
            user.setUsername(username);
            user.setEmail(email);
            // Dummy password for oauth users
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            
            // Assign default USER role
            Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
                Role r = new Role("ROLE_USER");
                return roleRepository.save(r);
            });
            user.getRoles().add(userRole);
            
            user = userRepository.save(user);

            OAuthAccount oauthAccount = new OAuthAccount();
            oauthAccount.setUser(user);
            oauthAccount.setProvider("google");
            oauthAccount.setProviderId(subject);
            oauthAccountRepository.save(oauthAccount);
        }

        // Return our own system's JWT
        return jwtUtil.generateToken(user.getUsername());
    }
}
