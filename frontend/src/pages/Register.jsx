import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import GoogleSignInButton from '../components/auth/GoogleSignInButton';
import OtpInput from '../components/auth/OtpInput';
import { ShieldCheckIcon, SparklesIcon, CheckIcon, MapPinIcon } from '../components/common/Icons';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

function PasswordStrength({ password }) {
  if (!password) return null;
  const checks = [password.length >= 8, /[A-Z]/.test(password), /[0-9]/.test(password), /[^A-Za-z0-9]/.test(password)];
  const score = checks.filter(Boolean).length;
  const colors = ['bg-rose-500', 'bg-amber-500', 'bg-sky-500', 'bg-emerald-500'];

  return (
    <div className="mt-2">
      <div className="flex h-1.5 gap-1.5">
        {checks.map((passed, index) => (
          <div key={index} className={`flex-1 rounded-full transition-all duration-300 ${passed ? colors[Math.min(score - 1, 3)] : 'bg-slate-200'}`} />
        ))}
      </div>
      <p className="mt-1.5 text-xs text-slate-500 font-medium">
        Strength: <span className="font-bold text-slate-700">{score < 2 ? 'Weak' : score < 3 ? 'Fair' : score < 4 ? 'Good' : 'Strong'}</span> — 8+ chars, uppercase, number & symbol.
      </p>
    </div>
  );
}

export default function Register() {
  const [method, setMethod] = useState('email');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [phone, setPhone] = useState('');
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [googleConfigured, setGoogleConfigured] = useState(false);

  const { register, googleLogin, sendOtp, verifyOtpLogin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    setGoogleConfigured(!!GOOGLE_CLIENT_ID && GOOGLE_CLIENT_ID.length > 10);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (name.trim().length < 2) {
      setError('Name must be at least 2 characters.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await register(name.trim(), email, password);
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    setError('');
    setLoading(true);
    try {
      await googleLogin(credentialResponse.credential);
      navigate('/', { replace: true });
    } catch (err) {
      const msg = err.response?.data?.message;
      setError(msg?.includes('ACCOUNT_LINKING_REQUIRED') ? 'An account with this email already exists. Please sign in.' : (msg || 'Google sign-up failed.'));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = (err) => {
    console.error('Google auth error:', err);
    setError('Google sign-in failed.');
  };

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setError('');
    if (!phone.trim()) {
      setError('Please enter your phone number.');
      return;
    }

    setLoading(true);
    try {
      await sendOtp(phone);
      setOtpSent(true);
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
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (otp.length !== 6) return;
    setError('');
    setLoading(true);
    try {
      await verifyOtpLogin(phone, otp);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed.');
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="flex min-h-[85vh] items-center justify-center px-4 py-12">
        <div className="voyara-card w-full max-w-md p-8 text-center animate-fade-in">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-100 shadow-sm">
            <CheckIcon className="h-8 w-8" />
          </div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Account Created</h2>
          <p className="mt-3 text-sm leading-relaxed text-slate-600">
            We’ve sent a verification link to <span className="font-semibold text-slate-800">{email}</span>. Please verify your email before booking your next adventure.
          </p>
          <Link to="/login" className="travel-button-primary mt-6 h-12 w-full text-sm font-bold justify-center">
            Sign In to Voyara
          </Link>
        </div>
      </div>
    );
  }

  if (method === 'phone' && otpSent) {
    return (
      <div className="flex min-h-[85vh] items-center justify-center px-4 py-12">
        <div className="voyara-card w-full max-w-md p-8 shadow-xl animate-fade-in">
          <div className="mb-6 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-blue-600 border border-blue-100 shadow-sm">
              <SparklesIcon className="h-7 w-7" />
            </div>
            <h2 className="text-2xl font-bold tracking-tight text-slate-900">Verify Phone Number</h2>
            <p className="mt-2 text-xs text-slate-500">
              Enter the 6-digit code sent to <span className="font-semibold text-slate-800">{phone}</span>
            </p>
          </div>

          {error && (
            <div className="mb-4 rounded-xl border border-red-200 bg-red-50/80 px-4 py-3 text-xs text-red-700 font-medium">
              {error}
            </div>
          )}

          <div className="mb-6">
            <OtpInput length={6} value={otp} onChange={setOtp} disabled={loading} error={!!error} />
          </div>

          <button
            onClick={handleVerifyOtp}
            disabled={loading || otp.length !== 6}
            className="travel-button-primary mb-4 h-11 w-full text-xs font-bold justify-center"
          >
            {loading ? 'Verifying...' : 'Verify & Create Account'}
          </button>

          <div className="space-y-2 text-center text-xs">
            {resendTimer > 0 ? (
              <p className="text-slate-400">Resend code in {resendTimer}s</p>
            ) : (
              <button onClick={handleSendOtp} disabled={loading} className="font-semibold text-primary hover:underline">
                Resend verification code
              </button>
            )}
            <div>
              <button
                onClick={() => { setMethod('phone'); setOtpSent(false); setOtp(''); setError(''); }}
                className="text-slate-500 hover:text-slate-700 underline"
              >
                Change phone number
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-5xl rounded-3xl border border-slate-200/80 bg-white shadow-2xl shadow-slate-900/5 overflow-hidden grid grid-cols-1 lg:grid-cols-12 min-h-[640px]">
        {/* Editorial Brand / Visual Panel */}
        <div className="hidden lg:flex lg:col-span-5 bg-gradient-to-br from-slate-950 via-slate-900 to-sky-950 p-10 flex-col justify-between text-white relative overflow-hidden">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(14,165,233,0.18),transparent_50%)]" />
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_bottom_left,rgba(99,102,241,0.15),transparent_50%)]" />

          <div className="relative z-10">
            <div className="flex items-center gap-2.5 mb-8">
              <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-sky-400 to-indigo-500 flex items-center justify-center text-white font-black text-sm tracking-tight shadow-md">
                VY
              </div>
              <span className="text-xl font-bold tracking-tight text-white">Voyara</span>
            </div>

            <div className="space-y-3">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-[11px] font-semibold tracking-wide text-sky-200 uppercase backdrop-blur-md border border-white/10">
                <SparklesIcon className="w-3.5 h-3.5 text-sky-400" />
                Next-Gen Travel Membership
              </span>
              <h1 className="text-3xl font-extrabold tracking-tight leading-tight">
                Your journey starts with intelligent clarity.
              </h1>
              <p className="text-xs text-slate-300/90 leading-relaxed">
                Join over 250,000 travelers using Voyara for real-time connection risk analysis, autonomous group expense splitting, and verified fare guarantees.
              </p>
            </div>
          </div>

          {/* Value props list */}
          <div className="relative z-10 space-y-4 my-6">
            <div className="flex items-start gap-3 rounded-2xl bg-white/[0.06] border border-white/10 p-3.5 backdrop-blur-md">
              <div className="mt-0.5 rounded-lg bg-sky-500/20 p-1.5 text-sky-400">
                <ShieldCheckIcon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-xs font-semibold text-white">Travel Guardian Protection</p>
                <p className="text-[11px] text-slate-300">Live flight telemetry and connection buffers calculated before you book.</p>
              </div>
            </div>

            <div className="flex items-start gap-3 rounded-2xl bg-white/[0.06] border border-white/10 p-3.5 backdrop-blur-md">
              <div className="mt-0.5 rounded-lg bg-emerald-500/20 p-1.5 text-emerald-400">
                <CheckIcon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-xs font-semibold text-white">Instant Tier Rewards</p>
                <p className="text-[11px] text-slate-300">Earn 1.5x Voyara Miles on every confirmed flight and boutique stay.</p>
              </div>
            </div>
          </div>

          <div className="relative z-10 pt-4 border-t border-white/10 flex items-center justify-between text-[11px] text-slate-400">
            <span>© 2026 Voyara Technologies</span>
            <span className="flex items-center gap-1">
              <MapPinIcon className="h-3 w-3 text-sky-400" /> Global Travel Tech
            </span>
          </div>
        </div>

        {/* Right Registration Form */}
        <div className="lg:col-span-7 p-6 sm:p-10 flex flex-col justify-center bg-white">
          <div className="w-full max-w-md mx-auto">
            <div className="mb-6">
              <h2 className="text-2xl font-bold tracking-tight text-slate-900">Create Account</h2>
              <p className="text-xs text-slate-500 mt-1">
                Enter your details to unlock personal itineraries and member rates.
              </p>
            </div>

            {error && (
              <div className="mb-5 rounded-xl border border-red-200 bg-red-50/80 px-4 py-3 text-xs text-red-700 font-medium animate-fade-in">
                {error}
              </div>
            )}

            {googleConfigured && (
              <div className="mb-5 flex justify-center">
                <GoogleSignInButton
                  onSuccess={handleGoogleSuccess}
                  onError={handleGoogleError}
                  text="signup_with"
                />
              </div>
            )}

            {!googleConfigured && (
              <div className="mb-5 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-center text-xs text-slate-400">
                Google sign-up is disabled in this environment.
              </div>
            )}

            <div className="relative mb-5">
              <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-200" /></div>
              <div className="relative flex justify-center">
                <span className="bg-white px-3 text-[10px] font-bold tracking-wider uppercase text-slate-400">
                  Or register with
                </span>
              </div>
            </div>

            {method === 'email' && (
              <button
                type="button"
                onClick={() => setMethod('phone')}
                className="travel-button-secondary mb-4 h-10 w-full text-xs font-semibold justify-center"
              >
                Sign up with mobile number instead
              </button>
            )}

            {method === 'phone' && (
              <form onSubmit={handleSendOtp} className="mb-4 space-y-3">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-slate-700">Phone number</label>
                  <div className="flex gap-2">
                    <select className="h-10 w-24 rounded-xl border border-slate-200 bg-slate-50 px-2 text-xs text-slate-700 outline-none focus:border-primary">
                      <option>+91</option>
                      <option>+1</option>
                      <option>+44</option>
                    </select>
                    <input
                      type="tel"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      placeholder="98765 43210"
                      autoFocus
                      className="voyara-input h-10 flex-1 text-xs"
                    />
                  </div>
                </div>
                <button type="submit" disabled={loading} className="travel-button-primary h-11 w-full text-xs font-bold justify-center">
                  {loading ? 'Sending...' : 'Send verification code'}
                </button>
                <button
                  type="button"
                  onClick={() => setMethod('email')}
                  className="w-full text-xs text-slate-500 hover:text-slate-800 font-medium py-1"
                >
                  Use email instead
                </button>
              </form>
            )}

            {method === 'email' && (
              <form onSubmit={handleSubmit} className="space-y-3.5">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-slate-700">Full name</label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    autoFocus
                    className="voyara-input h-10 text-xs"
                    placeholder="e.g. Maya Chen"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-slate-700">Email address</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className="voyara-input h-10 text-xs"
                    placeholder="maya@example.com"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-slate-700">Password</label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      minLength={8}
                      className="voyara-input h-10 text-xs pr-10"
                      placeholder="Minimum 8 characters"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 font-medium"
                    >
                      {showPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                  <PasswordStrength password={password} />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-slate-700">Confirm password</label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    className={`voyara-input h-10 text-xs ${confirmPassword && confirmPassword !== password ? 'border-red-400 focus:border-red-400' : ''}`}
                    placeholder="Re-enter your password"
                  />
                  {confirmPassword && confirmPassword !== password && (
                    <p className="mt-1 text-[11px] text-red-500 font-medium">Passwords do not match.</p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="travel-button-primary h-11 w-full text-xs font-bold justify-center mt-2"
                >
                  {loading ? 'Creating account...' : 'Create Voyara Account'}
                </button>
              </form>
            )}

            <p className="mt-6 text-center text-xs text-slate-500 font-medium">
              Already have an account?{' '}
              <Link to="/login" className="font-bold text-primary hover:underline">
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
