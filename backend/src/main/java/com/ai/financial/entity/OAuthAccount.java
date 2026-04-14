package com.ai.financial.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "oauth_accounts")
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider; // e.g. "google"

    @Column(nullable = false, unique = true)
    private String providerId; // Google's sub ID
}
