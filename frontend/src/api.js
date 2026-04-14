import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

const api = axios.create({ baseURL: BASE_URL });

// Attach JWT automatically to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const authService = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  google:   (credential) => api.post('/auth/google', { credential }),
};

export const dashboardService = {
  simulate:    ()  => api.post('/v1/dashboard/simulate'),
  marketData:  ()  => api.get('/v1/dashboard/market-data'),
  predictions: ()  => api.get('/v1/dashboard/predictions'),
  risk:        ()  => api.get('/v1/dashboard/risk'),
};

export default api;
