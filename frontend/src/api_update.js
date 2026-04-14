export const authService = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  google:   (credential) => api.post('/auth/google', { credential }),
};
