import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError } from '../types/analysis';

const BASE_URL = process.env.REACT_APP_API_BASE_URL || '/api';

// Debug: Log the BASE_URL in console (remove after verifying)
console.log('[httpClient] BASE_URL configured as:', BASE_URL);

/**
 * Configured Axios instance for all API calls.
 *
 * Interceptors handle:
 * - Injecting the X-Offline-Mode header from sessionStorage
 * - Transforming error responses into typed ApiError objects
 * - Logging requests in development mode
 */
export const httpClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// ── Request Interceptor ───────────────────────────────────────────────────────

httpClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Inject offline mode header if active
    const isOffline = sessionStorage.getItem('offlineMode') === 'true';
    if (isOffline) {
      config.headers['X-Offline-Mode'] = 'true';
    }

    if (process.env.NODE_ENV === 'development') {
      console.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`, {
        params: config.params,
        offline: isOffline,
      });
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response Interceptor ──────────────────────────────────────────────────────

httpClient.interceptors.response.use(
  (response) => {
    if (process.env.NODE_ENV === 'development') {
      console.debug(`[API] Response ${response.status}:`, response.data?.source);
    }
    return response;
  },
  (error: AxiosError) => {
    const apiError: ApiError = {
      code: 'UNKNOWN_ERROR',
      message: 'An unexpected error occurred',
      timestamp: new Date().toISOString(),
      status: error.response?.status || 0,
    };

    if (error.response?.data) {
      const data = error.response.data as Partial<ApiError>;
      apiError.code = data.code || apiError.code;
      apiError.message = data.message || apiError.message;
      apiError.timestamp = data.timestamp || apiError.timestamp;
    } else if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      apiError.code = 'TIMEOUT';
      apiError.message = 'Request timed out. The API may be slow or unavailable.';
    } else if (!error.response) {
      apiError.code = 'NETWORK_ERROR';
      apiError.message = 'Cannot connect to the API. Check your network connection.';
    }

    return Promise.reject(apiError);
  }
);

export default httpClient;
