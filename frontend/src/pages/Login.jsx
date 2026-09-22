import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import GoogleSignInButton from '../components/auth/GoogleSignInButton';
import api from '../api/axios';
import OtpInput from '../components/auth/OtpInput';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

export default function Login() {
  const [method, setMethod] = useState('email');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [countryCode, setCountryCode] = useState('+91');
  const [phone, setPhone] = useState('');
  const [normalizedPhone, setNormalizedPhone] = useState('');
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [needsVerification, setNeedsVerification] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendMsg, setResendMsg] = useState('');
  const [googleConfigured, setGoogleConfigured] = useState(false);

  const { login, googleLogin, sendOtp, verifyOtpLogin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';
  const message = location.state?.message;

  useEffect(() => {
    setGoogleConfigured(!!GOOGLE_CLIENT_ID && GOOGLE_CLIENT_ID.length > 10);
  }, []);

  const handleEmailLogin = async (e) => {
    e.preventDefault();
    setError('');
    setNeedsVerification(false);
    setLoading(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      const msg = err.response?.data?.message;
      if (msg === 'EMAIL_NOT_VERIFIED') {
        setNeedsVerification(true);
        setError('Please verify your email before logging in.');
      } else if (msg?.includes('Bad credentials') || msg?.includes('Invalid email') || msg?.includes('Invalid credentials')) {
        setError('Invalid email or password.');
      } else {
        setError(msg || 'Invalid email or password.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setError('');
    const raw = phone.trim().replace(/[\s\-\(\)\.]/g, '');
    if (!raw) {
      setError('Please enter your mobile number.');
      return;
    }
    let formatted = raw;
    if (countryCode === '+91') {
      if (formatted.startsWith('0') && formatted.length === 11) formatted = formatted.substring(1);
      if (formatted.startsWith('+91')) formatted = formatted.substring(3);
      if (formatted.startsWith('91') && formatted.length === 12) formatted = formatted.substring(2);
      if (formatted.length !== 10 || !/^\d{10}$/.test(formatted)) {
        setError('Enter a valid 10-digit Indian mobile number.');
        return;
      }
      formatted = '+91' + formatted;
    } else {
      if (!formatted.startsWith('+')) formatted = countryCode + formatted;
    }

    setNormalizedPhone(formatted);
    setLoading(true);
    try {
      await sendOtp(formatted);
      setOtpSent(true);
      setMethod('otp');
      startResendTimer();
    } catch (err) {
      const msg = err.response?.data?.message;
      if (msg?.includes('temporarily unavailable')) {
        setError('OTP service is temporarily unavailable. Please try again.');
      } else if (msg?.includes('Too many') || msg?.includes('wait')) {
        setError(msg);
      } else {
        setError(msg || 'Failed to send OTP. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (otp.length !== 6) return;
    setError('');
    setLoading(true);
    try {
      await verifyOtpLogin(normalizedPhone || phone, otp);
      navigate(from, { replace: true });
    } catch (err) {
      const msg = err.response?.data?.message;
      if (msg?.includes('expired')) {
        setError('This verification code has expired. Please request a new one.');
      } else if (msg?.includes('Invalid') || msg?.includes('Incorrect')) {
        setError('Invalid verification code.');
      } else if (msg?.includes('Too many failed') || msg?.includes('attempts')) {
        setError('Too many attempts. Please wait before requesting another OTP.');
      } else {
        setError(msg || 'Verification failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    setError('');
    setLoading(true);
    try {
      await googleLogin(credentialResponse.credential);
      navigate(from, { replace: true });
    } catch (err) {
      const msg = err.response?.data?.message;
      if (msg?.includes('ACCOUNT_LINKING_REQUIRED')) {
        setError('An account with this email already exists. Please sign in with email and password, then link Google from your profile.');
      } else {
        setError(msg || 'Google sign-in is temporarily unavailable.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = (err) => {
    console.error('Google auth error:', err?.message || err);
    setError('Google sign-in is temporarily unavailable.');
  };

  const startResendTimer = () => {
    setResendTimer(60);
    const iv = setInterval(() => {
      setResendTimer((previous) => {
        if (previous <= 1) {
          clearInterval(iv);
          return 0;
        }
        return previous - 1;
      });
    }, 1000);
  };

  const handleResendOtp = async () => {
    if (resendTimer > 0) return;
    setLoading(true);
    try {
      await sendOtp(phone);
      startResendTimer();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to resend OTP.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    setResendMsg('');
    try {
      await api.post('/api/auth/resend-verification', { email });
      setResendMsg('Verification email resent.');
    } catch {
      setResendMsg('Failed to resend.');
    } finally {
      setResending(false);
    }
  };

  if (method === 'otp' && otpSent) {
    return (
      <div className="flex min-h-[80vh] items-center justify-center px-4 py-10">
        <div className="w-full max-w-md rounded-[28px] border border-slate-200 bg-white p-6 shadow-xl shadow-slate-200/40 sm:p-8">
          <div className="mb-8 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-2xl">📱</div>
            <h2 className="text-3xl font-black text-slate-900">Verify your phone</h2>
            <p className="mt-2 text-sm text-slate-500">
              Enter the 6-digit code sent to <span className="font-semibold text-slate-800">{phone}</span>
            </p>
          </div>

          {error && <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

          <div className="mb-6"><OtpInput length={6} value={otp} onChange={setOtp} disabled={loading} error={!!error} /></div>

          <button onClick={handleVerifyOtp} disabled={loading || otp.length !== 6} className="travel-button-primary mb-4 h-[52px] w-full text-sm">
            {loading ? 'Verifying...' : 'Verify & continue'}
          </button>

          <div className="space-y-2 text-center">
            {resendTimer > 0 ? (
              <p className="text-sm text-slate-400">Resend code in {resendTimer}s</p>
            ) : (
              <button onClick={handleResendOtp} disabled={loading} className="text-sm font-semibold text-primary hover:underline">Resend code</button>
            )}
            <button onClick={() => { setMethod('phone'); setOtpSent(false); setOtp(''); setError(''); }} className="block w-full text-sm text-slate-500 hover:text-slate-700">
              Change number
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="section-shell py-8 sm:py-12 max-w-5xl mx-auto">
      <div className="grid lg:grid-cols-12 gap-8 items-center">
        {/* Left Editorial Visual (Desktop) */}
        <div className="hidden lg:flex lg:col-span-6 flex-col justify-between h-[600px] rounded-[32px] bg-gradient-to-br from-[#071326] via-[#0b2654] to-primary p-10 text-white relative overflow-hidden shadow-2xl">
          <div className="absolute top-0 right-0 w-80 h-80 rounded-full bg-cyan-400/10 blur-3xl pointer-events-none" />
          
          <div>
            <div className="flex items-center gap-2.5 mb-8">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/20 text-white font-black text-xl backdrop-blur-md">
                V
              </div>
              <span className="text-2xl font-black tracking-tight">Voyara</span>
            </div>

            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3.5 py-1 text-xs font-bold text-sky-200 border border-white/15 mb-4">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Verified Global Travel Platform
            </span>

            <h2 className="text-3xl font-black leading-tight text-white">
              Every great journey begins with confidence.
            </h2>
            <p className="mt-3 text-sm text-sky-200/80 leading-relaxed max-w-sm">
              Access your booked flights, hotels, Guardian alerts, and exclusive Voyara Rewards in one place.
            </p>
          </div>

          <div className="border-t border-white/15 pt-6 text-xs text-sky-100/70 space-y-2">
            <p className="font-medium">"Voyara Guardian caught my terminal transfer delay before the airline even announced it."</p>
            <p className="text-[11px] font-bold text-white">— Verified Voyager, Mumbai</p>
          </div>
        </div>

        {/* Right Form Card */}
        <div className="lg:col-span-6 w-full max-w-md mx-auto">
          <div className="voyara-card p-6 sm:p-9 shadow-xl">
            <div className="mb-6 text-center lg:text-left">
              <h2 className="text-2xl sm:text-3xl font-black text-slate-900">Welcome back</h2>
              <p className="mt-1 text-xs sm:text-sm text-slate-500">Sign in to manage your bookings and rewards.</p>
            </div>

            {message && (
              <div className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-semibold text-emerald-700">
                {message}
              </div>
            )}
            {error && (
              <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs font-semibold text-red-700">
                <p>{error}</p>
                {needsVerification && (
                  <button type="button" onClick={handleResend} disabled={resending} className="mt-1.5 font-bold underline text-red-800 block">
                    {resending ? 'Sending...' : 'Resend verification email'}
                  </button>
                )}
                {resendMsg && <p className="mt-1.5 text-red-800">{resendMsg}</p>}
              </div>
            )}

            {googleConfigured && (
              <div className="mb-5 flex justify-center">
                <GoogleSignInButton onSuccess={handleGoogleSuccess} onError={handleGoogleError} text="continue_with" />
              </div>
            )}

            <div className="relative mb-5">
              <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-200" /></div>
              <div className="relative flex justify-center"><span className="bg-white px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">Or Continue With</span></div>
            </div>

            {method === 'email' && (
              <button
                type="button"
                onClick={() => setMethod('phone')}
                className="travel-button-secondary mb-4 h-11 w-full text-xs font-bold"
              >
                📱 Mobile OTP Sign In
              </button>
            )}

            {method === 'phone' && (
              <form onSubmit={handleSendOtp} className="mb-4 space-y-3">
                <div>
                  <label className="mb-1 block text-xs font-bold text-slate-700 uppercase tracking-wider">Mobile Number</label>
                  <div className="flex gap-2">
                    <select
                      value={countryCode}
                      onChange={(e) => setCountryCode(e.target.value)}
                      className="h-11 w-24 rounded-xl border border-slate-200 bg-slate-50 px-2 text-xs font-semibold text-slate-700 outline-none focus:border-primary"
                    >
                      <option value="+91">🇮🇳 +91</option>
                      <option value="+1">🇺🇸 +1</option>
                      <option value="+44">🇬🇧 +44</option>
                    </select>
                    <input
                      type="tel"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      placeholder="98765 43210"
                      autoFocus
                      className="voyara-input h-11 flex-1 text-xs font-semibold"
                    />
                  </div>
                </div>
                <button type="submit" disabled={loading} className="travel-button-primary h-11 w-full text-xs font-bold">
                  {loading ? 'Sending OTP...' : 'Send Verification OTP'}
                </button>
                <button type="button" onClick={() => setMethod('email')} className="w-full text-xs font-bold text-slate-500 hover:text-primary pt-1">
                  Use email and password instead
                </button>
              </form>
            )}

            {method === 'email' && (
              <form onSubmit={handleEmailLogin} className="space-y-3.5">
                <div>
                  <label className="mb-1 block text-xs font-bold text-slate-700 uppercase tracking-wider">Email Address</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoFocus
                    className="voyara-input h-11 text-xs font-medium"
                    placeholder="name@example.com"
                  />
                </div>

                <div>
                  <div className="mb-1 flex items-center justify-between gap-3">
                    <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">Password</label>
                    <Link to="/forgot-password" className="text-xs font-bold text-primary hover:underline">Forgot password?</Link>
                  </div>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      className="voyara-input h-11 pr-11 text-xs font-medium"
                      placeholder="Enter your password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((p) => !p)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs font-bold hover:text-slate-600"
                    >
                      {showPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </div>

                <button type="submit" disabled={loading} className="travel-button-primary h-11 w-full text-xs font-bold">
                  {loading ? 'Signing in...' : 'Sign in to Voyara'}
                </button>
              </form>
            )}

            <p className="mt-6 text-center text-xs text-slate-500 font-medium">
              New to Voyara? <Link to="/register" className="font-bold text-primary hover:underline">Create account</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
