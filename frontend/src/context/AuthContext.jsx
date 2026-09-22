import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import api from '../api/axios'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const loadUser = useCallback(() => {
    const token = localStorage.getItem('accessToken')
    const userData = localStorage.getItem('user')
    if (token && userData) {
      try {
        setUser(JSON.parse(userData))
      } catch {
        localStorage.clear()
      }
    } else {
      setUser(null)
    }
    setLoading(false)
  }, [])

  useEffect(() => {
    loadUser()
    const handleAuthUpdate = () => loadUser()
    window.addEventListener('auth_updated', handleAuthUpdate)
    return () => window.removeEventListener('auth_updated', handleAuthUpdate)
  }, [loadUser])

  // Helper: store tokens and user from login response
  const handleAuthResponse = (data) => {
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    const userInfo = { email: data.email, name: data.name, role: data.role }
    localStorage.setItem('user', JSON.stringify(userInfo))
    setUser(userInfo)
    return data
  }

  // Email/password login
  const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password })
    return handleAuthResponse(response.data.data)
  }

  // Email/password register
  const register = async (name, email, password) => {
    await api.post('/auth/register', { name, email, password })
  }

  // Google login
  const googleLogin = async (idToken) => {
    const response = await api.post('/auth/google', { idToken })
    return handleAuthResponse(response.data.data)
  }

  // Phone: send OTP
  const sendOtp = async (phoneNumber) => {
    await api.post('/auth/phone/send-otp', { phoneNumber })
  }

  // Phone: verify OTP and login
  const verifyOtpLogin = async (phoneNumber, otp) => {
    const response = await api.post('/auth/phone/verify-otp', { phoneNumber, otp })
    return handleAuthResponse(response.data.data)
  }

  // Get linked identities
  const getIdentities = async () => {
    const response = await api.get('/auth/identities')
    return response.data.data
  }

  // Link Google
  const linkGoogle = async (idToken) => {
    await api.post('/auth/identities/google', { idToken })
  }

  // Link phone
  const linkPhone = async (phoneNumber, otp) => {
    await api.post('/auth/identities/phone', { phoneNumber, otp })
  }

  // Send linking OTP
  const sendLinkingOtp = async (phoneNumber) => {
    await api.post('/auth/identities/phone/send-otp', { phoneNumber })
  }

  // Unlink identity
  const unlinkIdentity = async (provider) => {
    await api.delete(`/auth/identities/${provider}`)
  }

  // Logout
  const logout = async () => {
    const refreshToken = localStorage.getItem('refreshToken')
    try {
      await api.post('/auth/logout', { refreshToken })
    } catch {
      // Ignore logout errors
    }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
  }

  const value = {
    user,
    loading,
    login,
    register,
    googleLogin,
    sendOtp,
    verifyOtpLogin,
    getIdentities,
    linkGoogle,
    linkPhone,
    sendLinkingOtp,
    unlinkIdentity,
    logout,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
