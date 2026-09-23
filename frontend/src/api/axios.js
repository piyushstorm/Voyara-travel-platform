import axios from 'axios'

const rawBase = import.meta.env.VITE_API_BASE_URL || ''
const API_BASE_URL = rawBase.replace(/\/+$/, '')

const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: normalize URL and attach access token
api.interceptors.request.use(
  (config) => {
    // Strip redundant leading /api if passed in request URL to prevent /api/api/...
    if (config.url && config.url.startsWith('/api/')) {
      config.url = config.url.substring(4)
    }
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: auto-refresh on 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    const status = error.response?.status
    if ((status === 401 || status === 403) && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')

      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, { refreshToken })
          const { accessToken, refreshToken: newRefreshToken, role, name, email } = response.data.data
          localStorage.setItem('accessToken', accessToken)
          localStorage.setItem('refreshToken', newRefreshToken)
          
          if (role) {
            const userData = JSON.parse(localStorage.getItem('user') || '{}');
            userData.role = role;
            if (name) userData.name = name;
            if (email) userData.email = email;
            localStorage.setItem('user', JSON.stringify(userData));
            window.dispatchEvent(new Event('auth_updated'));
          }

          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return api(originalRequest)
        } catch {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('user')
          window.dispatchEvent(new Event('auth_updated'))
          if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login'
          }
        }
      } else {
        // Unauthenticated access on protected route
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.dispatchEvent(new Event('auth_updated'))
      }
    }

    return Promise.reject(error)
  }
)

export default api
