import axios from 'axios';

const BASE_URL =
  process.env.REACT_APP_API_BASE_URL ||
  (process.env.NODE_ENV === 'production'
    ? 'https://github-analyzer-api-niksoni.azurewebsites.net/api'
    : '/api');

const httpClient = axios.create({
  baseURL: BASE_URL,
  timeout: 120000, // 2 minutes — accounts for Azure cold start + parallel GitHub API calls
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

httpClient.interceptors.request.use((config) => {
  const isOffline = sessionStorage.getItem('offlineMode') === 'true';
  if (isOffline) {
    config.headers['X-Offline-Mode'] = 'true';
  }
  if (process.env.NODE_ENV === 'development') {
    console.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`, config.params);
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError = {
      code: 'UNKNOWN_ERROR',
      message: 'An unexpected error occurred',
      timestamp: new Date().toISOString(),
      status: error.response?.status || 0,
    };

    if (error.response?.data) {
      const data = error.response.data;
      apiError.code = data.code || apiError.code;
      apiError.message = data.message || apiError.message;
      apiError.timestamp = data.timestamp || apiError.timestamp;
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      apiError.code = 'TIMEOUT';
      apiError.message = 'Request timed out. The server may be starting up — please try again in a moment.';
    } else if (!error.response) {
      apiError.code = 'NETWORK_ERROR';
      apiError.message = 'Cannot connect to the API. Check your network connection.';
    }

    return Promise.reject(apiError);
  }
);

export default httpClient;
