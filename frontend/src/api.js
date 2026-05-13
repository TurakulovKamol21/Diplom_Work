import axios from 'axios';

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : '/api');

const api = axios.create({ baseURL: BASE_URL });

function getStoredToken() {
  try {
    return window.localStorage.getItem('token');
  } catch (error) {
    console.warn('Unable to read auth token from localStorage', error);
    return null;
  }
}

function clearStoredAuth() {
  try {
    window.localStorage.removeItem('token');
    window.localStorage.removeItem('username');
  } catch (error) {
    console.warn('Unable to clear auth state from localStorage', error);
  }
}

function isJwtLike(token) {
  return typeof token === 'string' && token.split('.').length === 3;
}

function isAuthRequest(url = '') {
  return typeof url === 'string' && url.startsWith('/auth/');
}

// Attach JWT automatically to every request
api.interceptors.request.use((config) => {
  if (isAuthRequest(config.url)) {
    return config;
  }

  const token = getStoredToken();
  if (!token) {
    return config;
  }

  if (!isJwtLike(token)) {
    clearStoredAuth();
    return config;
  }

  config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const requestUrl = error?.config?.url;

    if ((status === 401 || status === 403) && !isAuthRequest(requestUrl)) {
      clearStoredAuth();

      if (typeof window !== 'undefined' && window.location.pathname !== '/') {
        window.location.assign('/');
      }
    }

    return Promise.reject(error);
  }
);

export const authService = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  google:   (credential) => api.post('/auth/google', { credential }),
};

export const dashboardService = {
  simulate:         () => api.post('/v1/dashboard/simulate'),
  marketData:       () => api.get('/v1/dashboard/market-data'),
  predictions:      () => api.get('/v1/dashboard/predictions'),
  risk:             () => api.get('/v1/dashboard/risk'),
  scenarioAnalysis: () => api.get('/v1/dashboard/scenario-analysis'),
  backtest:         () => api.get('/v1/dashboard/backtest'),
};

export default api;
