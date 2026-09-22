import { useState, useEffect } from 'react'
import GoogleSignInButton from './GoogleSignInButton'
import { useAuth } from '../../context/AuthContext'
import OtpInput from './OtpInput'

const PROVIDER_CONFIG = {
  LOCAL: { icon: '✉️', label: 'Email', color: 'bg-blue-100 text-blue-700' },
  GOOGLE: { icon: '🔵', label: 'Google', color: 'bg-red-100 text-red-700' },
  PHONE: { icon: '📱', label: 'Mobile', color: 'bg-green-100 text-green-700' },
}

export default function LinkedAccounts({ showToast }) {
  const { getIdentities, linkGoogle: linkGoogleIdentity, sendLinkingOtp, linkPhone: linkPhoneIdentity, unlinkIdentity } = useAuth()
  const [identities, setIdentities] = useState([])
  const [loading, setLoading] = useState(true)

  // Phone linking state
  const [showPhoneLink, setShowPhoneLink] = useState(false)
  const [linkPhone, setLinkPhone] = useState('')
  const [linkOtp, setLinkOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [linkLoading, setLinkLoading] = useState(false)
  const [resendTimer, setResendTimer] = useState(0)
  const [error, setError] = useState('')

  // Google linking state
  const [linkingGoogle, setLinkingGoogle] = useState(false)

  const fetchIdentities = async () => {
    try {
      const data = await getIdentities()
      setIdentities(data || [])
    } catch { /* ignore */ }
    finally { setLoading(false) }
  }

  useEffect(() => { fetchIdentities() }, [])

  const hasProvider = (provider) => identities.some(i => i.provider === provider)

  // Google link
  const handleGoogleLink = async (credentialResponse) => {
    setLinkingGoogle(true)
    setError('')
    try {
      await linkGoogleIdentity(credentialResponse.credential)
      showToast('Google account linked successfully')
      fetchIdentities()
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to link Google account', 'error')
    } finally { setLinkingGoogle(false) }
  }

  // Phone link - send OTP
  const handleSendLinkOtp = async () => {
    if (!linkPhone.trim()) return
    setLinkLoading(true)
    setError('')
    try {
      await sendLinkingOtp(linkPhone)
      setOtpSent(true)
      setResendTimer(60)
      const iv = setInterval(() => setResendTimer(p => { if (p <= 1) { clearInterval(iv); return 0 } return p - 1 }), 1000)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP')
    } finally { setLinkLoading(false) }
  }

  // Phone link - verify OTP
  const handleVerifyLinkOtp = async () => {
    if (linkOtp.length !== 6) return
    setLinkLoading(true)
    setError('')
    try {
      await linkPhoneIdentity(linkPhone, linkOtp)
      showToast('Phone number linked successfully')
      setShowPhoneLink(false)
      setOtpSent(false)
      setLinkPhone('')
      setLinkOtp('')
      fetchIdentities()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to link phone')
    } finally { setLinkLoading(false) }
  }

  // Unlink
  const handleUnlink = async (provider) => {
    if (!window.confirm(`Remove ${PROVIDER_CONFIG[provider]?.label || provider} from your account?`)) return
    try {
      await unlinkIdentity(provider)
      showToast(`${PROVIDER_CONFIG[provider]?.label || provider} removed`)
      fetchIdentities()
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to remove', 'error')
    }
  }

  if (loading) {
    return (
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <div className="animate-pulse space-y-3">
          <div className="h-5 bg-gray-200 rounded w-40" />
          <div className="h-16 bg-gray-100 rounded-xl" />
          <div className="h-16 bg-gray-100 rounded-xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Linked Accounts */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="font-bold text-gray-900 mb-1">Login Methods</h2>
        <p className="text-sm text-gray-500 mb-5">Manage how you sign in to your Voyara account</p>

        <div className="space-y-3">
          {identities.map(identity => {
            const config = PROVIDER_CONFIG[identity.provider] || { icon: '🔑', label: identity.provider, color: 'bg-gray-100 text-gray-700' }
            return (
              <div key={identity.id} className="flex items-center justify-between py-3 px-4 bg-gray-50 rounded-xl">
                <div className="flex items-center gap-3">
                  <span className="text-xl">{config.icon}</span>
                  <div>
                    <p className="text-sm font-semibold text-gray-900">{config.label}</p>
                    <p className="text-xs text-gray-500">
                      {identity.email || identity.displayPhone || identity.displayName || 'Connected'}
                      {identity.verified && <span className="ml-2 text-green-600">✓ Verified</span>}
                    </p>
                  </div>
                </div>
                {identity.provider !== 'LOCAL' && (
                  <button onClick={() => handleUnlink(identity.provider)}
                    className="text-xs text-red-500 hover:text-red-700 font-medium px-2 py-1 rounded-lg hover:bg-red-50 transition">
                    Remove
                  </button>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Add Authentication Methods */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h3 className="font-bold text-gray-900 mb-4">Add Authentication Method</h3>

        {/* Connect Google */}
        {!hasProvider('GOOGLE') && (
          <div className="mb-4">
            <p className="text-sm text-gray-500 mb-3">Link your Google account for one-click sign-in</p>
            <GoogleSignInButton
              onSuccess={handleGoogleLink}
              onError={() => showToast('Google linking failed', 'error')}
              text="continue_with"
            />
          </div>
        )}

        {/* Add Phone */}
        {!hasProvider('PHONE') && !showPhoneLink && (
          <div>
            <p className="text-sm text-gray-500 mb-3">Link your mobile number for OTP sign-in</p>
            <button onClick={() => setShowPhoneLink(true)}
              className="flex items-center gap-2 border border-gray-200 rounded-xl px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition">
              <span>📱</span> Add Mobile Number
            </button>
          </div>
        )}

        {/* Phone Linking Form */}
        {showPhoneLink && (
          <div className="mt-3 space-y-3">
            {error && <div className="bg-red-50 text-red-700 px-4 py-3 rounded-xl text-sm border border-red-200">{error}</div>}

            {!otpSent ? (
              <>
                <div className="flex gap-2">
                  <select className="px-3 py-3 border border-gray-300 rounded-xl text-sm bg-gray-50 focus:ring-2 focus:ring-blue-500 outline-none w-24">
                    <option>+91 🇮🇳</option>
                  </select>
                  <input type="tel" value={linkPhone} onChange={e => setLinkPhone(e.target.value)}
                    placeholder="98765 43210" autoFocus
                    className="flex-1 px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none" />
                </div>
                <div className="flex gap-2">
                  <button onClick={handleSendLinkOtp} disabled={linkLoading}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
                    {linkLoading ? 'Sending...' : 'Send OTP'}
                  </button>
                  <button onClick={() => { setShowPhoneLink(false); setOtpSent(false); setError('') }}
                    className="px-4 py-2.5 border border-gray-300 rounded-xl text-sm font-medium text-gray-700 hover:bg-gray-50 transition">
                    Cancel
                  </button>
                </div>
              </>
            ) : (
              <>
                <p className="text-sm text-gray-500">Enter the 6-digit code sent to {linkPhone}</p>
                <OtpInput length={6} value={linkOtp} onChange={setLinkOtp} disabled={linkLoading} />
                <div className="flex gap-2">
                  <button onClick={handleVerifyLinkOtp} disabled={linkLoading || linkOtp.length !== 6}
                    className="bg-blue-600 text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
                    {linkLoading ? 'Verifying...' : 'Verify & Link'}
                  </button>
                  {resendTimer > 0
                    ? <span className="text-sm text-gray-400 py-2.5">Resend in {resendTimer}s</span>
                    : <button onClick={handleSendLinkOtp} className="text-sm text-blue-600 font-medium hover:underline py-2.5">Resend</button>
                  }
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
