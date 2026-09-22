import { useState, useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api from '../api/axios'

export default function VerifyEmail() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  
  const [status, setStatus] = useState('LOADING') // LOADING, SUCCESS, ERROR
  const [errorMsg, setErrorMsg] = useState('')
  const verifyAttempted = useRef(false)

  useEffect(() => {
    if (!token) {
      setStatus('ERROR')
      setErrorMsg('No verification token provided.')
      return
    }

    if (verifyAttempted.current) return
    verifyAttempted.current = true

    const verifyToken = async () => {
      try {
        await api.post(`/api/auth/verify-email?token=${token}`)
        setStatus('SUCCESS')
      } catch (err) {
        setStatus('ERROR')
        setErrorMsg(err.response?.data?.message || 'Verification failed. The link may be invalid or expired.')
      }
    }

    verifyToken()
  }, [token])

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
        {status === 'LOADING' && (
          <div>
            <div className="w-16 h-16 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mx-auto mb-6"></div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Verifying your email</h2>
            <p className="text-gray-500">Please wait a moment...</p>
          </div>
        )}

        {status === 'SUCCESS' && (
          <div>
            <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center mx-auto mb-6 text-3xl">
              ✓
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Email verified</h2>
            <p className="text-gray-600 mb-8">
              Your Voyara account is now active.
            </p>
            <Link to="/login" className="inline-block w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition">
              Continue to Login
            </Link>
          </div>
        )}

        {status === 'ERROR' && (
          <div>
            <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6 text-3xl">
              !
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Verification failed</h2>
            <p className="text-gray-600 mb-8">{errorMsg}</p>
            <Link to="/login" className="inline-block w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition">
              Return to Login
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}
