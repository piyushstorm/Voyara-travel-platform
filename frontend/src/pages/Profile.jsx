import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { notificationsApi } from '../api/phase3Api';
import { getTravelPreferences, updateTravelPreferences } from '../api/selectionApi';
import LinkedAccounts from '../components/auth/LinkedAccounts';

const TITLES = ['Mr', 'Mrs', 'Ms', 'Dr'];
const GENDERS = ['MALE', 'FEMALE', 'OTHER'];

function Toast({ message, type, onClose }) {
  useEffect(() => { const t = setTimeout(onClose, 3500); return () => clearTimeout(t); }, [onClose]);
  const colors = { success: 'bg-green-50 text-green-700 border-green-200', error: 'bg-red-50 text-red-700 border-red-200', info: 'bg-blue-50 text-blue-700 border-blue-200' };
  return (
    <div className={`fixed top-20 right-4 z-[100] px-4 py-3 rounded-xl border shadow-lg text-sm font-medium transition-all animate-[slideIn_0.2s_ease-out] ${colors[type] || colors.info}`}>
      {message}
    </div>
  );
}

function ConfirmModal({ title, message, onConfirm, onCancel, loading }) {
  return (
    <div className="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center p-4" onClick={onCancel}>
      <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-sm w-full" onClick={e => e.stopPropagation()}>
        <h3 className="text-lg font-bold text-gray-900 mb-2">{title}</h3>
        <p className="text-sm text-gray-500 mb-6">{message}</p>
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel} className="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-200 rounded-xl hover:bg-gray-50 transition">Cancel</button>
          <button onClick={onConfirm} disabled={loading} className="px-4 py-2 text-sm font-semibold text-white bg-red-600 rounded-xl hover:bg-red-700 disabled:opacity-50 transition">
            {loading ? 'Removing...' : 'Remove'}
          </button>
        </div>
      </div>
    </div>
  );
}

function TravellerForm({ traveller, onSave, onCancel, loading }) {
  const [form, setForm] = useState(traveller || {
    title: 'Mr', firstName: '', middleName: '', lastName: '', gender: 'MALE',
    dateOfBirth: '', nationality: 'Indian', passportNumber: '', passportExpiry: '', passportCountry: '',
  });
  const update = (k, v) => setForm(prev => ({ ...prev, [k]: v }));
  const [errors, setErrors] = useState({});

  const validate = () => {
    const e = {};
    if (!form.firstName?.trim()) e.firstName = 'Required';
    if (!form.lastName?.trim()) e.lastName = 'Required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = () => { if (validate()) onSave(form); };

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-5 sm:p-6 shadow-sm">
      <h3 className="font-bold text-gray-900 mb-4">{traveller ? 'Edit Traveller' : 'Add Traveller'}</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Title *</label>
          <select value={form.title} onChange={e => update('title', e.target.value)} className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none">
            {TITLES.map(t => <option key={t}>{t}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">First Name *</label>
          <input type="text" value={form.firstName} onChange={e => update('firstName', e.target.value)} placeholder="First name"
            className={`w-full px-3 py-2.5 border rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none ${errors.firstName ? 'border-red-300' : 'border-gray-200'}`} />
          {errors.firstName && <p className="text-xs text-red-500 mt-1">{errors.firstName}</p>}
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Middle Name</label>
          <input type="text" value={form.middleName || ''} onChange={e => update('middleName', e.target.value)} placeholder="Optional"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Last Name *</label>
          <input type="text" value={form.lastName} onChange={e => update('lastName', e.target.value)} placeholder="Last name"
            className={`w-full px-3 py-2.5 border rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none ${errors.lastName ? 'border-red-300' : 'border-gray-200'}`} />
          {errors.lastName && <p className="text-xs text-red-500 mt-1">{errors.lastName}</p>}
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Gender</label>
          <select value={form.gender || ''} onChange={e => update('gender', e.target.value)} className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none">
            <option value="">Select</option>
            {GENDERS.map(g => <option key={g}>{g}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Date of Birth</label>
          <input type="date" value={form.dateOfBirth || ''} onChange={e => update('dateOfBirth', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Nationality</label>
          <input type="text" value={form.nationality || ''} onChange={e => update('nationality', e.target.value)} placeholder="Indian"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Passport Number</label>
          <input type="text" value={form.passportNumber || ''} onChange={e => update('passportNumber', e.target.value)} placeholder="A1234567"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Passport Expiry</label>
          <input type="date" value={form.passportExpiry || ''} onChange={e => update('passportExpiry', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
        </div>
      </div>
      <div className="flex gap-3 mt-5">
        <button onClick={handleSubmit} disabled={loading}
          className="bg-primary text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
          {loading ? 'Saving...' : 'Save Traveller'}
        </button>
        <button onClick={onCancel} className="px-5 py-2.5 border border-gray-300 rounded-xl text-sm font-medium text-gray-700 hover:bg-gray-50 transition">Cancel</button>
      </div>
    </div>
  );
}

export default function Profile() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [sp, setSp] = useSearchParams();
  const tab = sp.get('tab') || 'profile';

  const [profile, setProfile] = useState(null);
  const [travellers, setTravellers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Profile editing
  const [editingProfile, setEditingProfile] = useState(false);
  const [nameValue, setNameValue] = useState('');
  const [phoneValue, setPhoneValue] = useState('');
  const [profileSaving, setProfileSaving] = useState(false);

  // Password
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [pwLoading, setPwLoading] = useState(false);

  // Travellers
  const [showTravellerForm, setShowTravellerForm] = useState(false);
  const [editingTraveller, setEditingTraveller] = useState(null);
  const [tLoading, setTLoading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Toast
  const [toast, setToast] = useState(null);
  const showToast = (message, type = 'success') => setToast({ message, type });

  // Preferences
  const [prefs, setPrefs] = useState({
    preferredCabin: 'ECONOMY', currency: 'INR', language: 'English',
  });
  const [notifPrefs, setNotifPrefs] = useState(null);
  const [notifPrefsLoading, setNotifPrefsLoading] = useState(true);

  const [travelPrefs, setTravelPrefs] = useState({
    preferredSeatPosition: 'WINDOW',
    preferredSeatType: 'EXTRA_LEGROOM',
    preferredRoomType: 'DELUXE',
    preferredBedType: 'KING',
    preferredRoomFeatures: ['CITY_VIEW', 'BALCONY'],
  });
  const [travelPrefsSaving, setTravelPrefsSaving] = useState(false);

  const setTab = useCallback((t) => setSp({ tab: t }, { replace: true }), [setSp]);

  useEffect(() => {
    Promise.all([
      api.get('/user/profile').then(r => {
        setProfile(r.data.data);
        setNameValue(r.data.data.name);
        setPhoneValue(r.data.data.phone || '');
      }),
      api.get('/user/travellers').then(r => setTravellers(r.data.data || [])),
      notificationsApi.getPreferences().then(r => {
        setNotifPrefs(r.data);
        setNotifPrefsLoading(false);
      }),
      getTravelPreferences().then(tp => {
        if (tp) setTravelPrefs(tp);
      }),
    ]).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const handleSaveTravelPrefs = async () => {
    setTravelPrefsSaving(true);
    try {
      const updated = await updateTravelPreferences(travelPrefs);
      setTravelPrefs(updated);
      showToast('Travel personalization saved!');
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to save travel preferences', 'error');
    } finally {
      setTravelPrefsSaving(false);
    }
  };

  // Profile
  const handleProfileSave = async () => {
    if (!nameValue.trim() || nameValue.trim().length < 2) { showToast('Name must be at least 2 characters', 'error'); return; }
    setProfileSaving(true);
    try {
      await api.put('/user/profile', { name: nameValue.trim() });
      setProfile(p => ({ ...p, name: nameValue.trim() }));
      setEditingProfile(false);
      // Update AuthContext user name
      const userData = JSON.parse(localStorage.getItem('user') || '{}');
      userData.name = nameValue.trim();
      localStorage.setItem('user', JSON.stringify(userData));
      window.location.reload(); // Refresh to update navbar
    } catch (err) { showToast(err.response?.data?.message || 'Failed to update', 'error'); }
    finally { setProfileSaving(false); }
  };

  // Password
  const passwordStrength = (pw) => {
    if (!pw) return 0;
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return score;
  };
  const strengthLabel = ['Too short', 'Weak', 'Fair', 'Good', 'Strong'];
  const strengthColor = ['bg-gray-200', 'bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-green-500'];
  const pwScore = passwordStrength(newPw);

  const handlePasswordChange = async () => {
    if (newPw.length < 8) { showToast('Password must be at least 8 characters', 'error'); return; }
    if (newPw !== confirmPw) { showToast('Passwords do not match', 'error'); return; }
    setPwLoading(true);
    try {
      await api.put('/user/password', { currentPassword: currentPw, newPassword: newPw });
      showToast('Password changed successfully');
      setCurrentPw(''); setNewPw(''); setConfirmPw('');
    } catch (err) { showToast(err.response?.data?.message || 'Failed to change password', 'error'); }
    finally { setPwLoading(false); }
  };

  // Travellers
  const handleTravellerSave = async (form) => {
    setTLoading(true);
    try {
      if (editingTraveller) {
        await api.put(`/user/travellers/${editingTraveller.id}`, form);
        showToast('Traveller updated');
      } else {
        await api.post('/user/travellers', form);
        showToast('Traveller added');
      }
      const res = await api.get('/user/travellers');
      setTravellers(res.data.data || []);
      setShowTravellerForm(false);
      setEditingTraveller(null);
    } catch (err) { showToast(err.response?.data?.message || 'Failed to save traveller', 'error'); }
    finally { setTLoading(false); }
  };

  const handleTravellerDelete = async () => {
    if (!deleteTarget) return;
    setDeleteLoading(true);
    try {
      await api.delete(`/user/travellers/${deleteTarget.id}`);
      setTravellers(prev => prev.filter(t => t.id !== deleteTarget.id));
      showToast('Traveller removed');
    } catch (err) { showToast('Failed to remove traveller', 'error'); }
    finally { setDeleteLoading(false); setDeleteTarget(null); }
  };

  const handleLogout = async () => { await logout(); navigate('/'); };

  const maskPassport = (num) => !num ? '—' : '••••' + num.slice(-4);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="space-y-4 animate-pulse">
          <div className="h-36 bg-gray-200 rounded-2xl" />
          <div className="h-12 bg-gray-200 rounded-xl" />
          <div className="h-64 bg-gray-200 rounded-2xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 sm:py-8">
      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
      {deleteTarget && <ConfirmModal title="Remove Traveller" message={`Remove ${deleteTarget.title} ${deleteTarget.firstName} ${deleteTarget.lastName} from saved travellers?`} onConfirm={handleTravellerDelete} onCancel={() => setDeleteTarget(null)} loading={deleteLoading} />}

      {/* Profile header */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <div className="w-16 h-16 rounded-full bg-gradient-to-br from-primary to-blue-600 flex items-center justify-center text-white text-2xl font-bold shadow-lg shrink-0">
            {profile?.name?.charAt(0)?.toUpperCase() || 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="text-xl font-bold text-gray-900 truncate">{profile?.name}</h1>
            <p className="text-sm text-gray-500 truncate">{profile?.email}</p>
            <div className="flex items-center gap-2 mt-1.5 flex-wrap">
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${isAdmin ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'}`}>
                {isAdmin ? 'Administrator' : 'Traveler'}
              </span>
              <span className="text-xs text-gray-400">·</span>
              <span className="text-xs text-gray-400">
                Member since {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' }) : ''}
              </span>
              <span className="flex items-center gap-1 text-xs text-green-600">
                <span className="w-1.5 h-1.5 bg-green-500 rounded-full" /> Active
              </span>
            </div>
          </div>
          {isAdmin && (
            <Link to="/admin" className="bg-purple-600 text-white px-4 py-2 rounded-xl text-sm font-semibold hover:bg-purple-700 transition-colors whitespace-nowrap">
              Open Admin Panel →
            </Link>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 bg-gray-100 rounded-xl p-1 overflow-x-auto no-scrollbar">
        {[
          { key: 'profile', label: 'Profile', icon: '👤' },
          { key: 'security', label: 'Security', icon: '🔒' },
          { key: 'travellers', label: 'Travellers', icon: '❤️', count: travellers.length },
          { key: 'rewards', label: 'Voyara Rewards', icon: '⭐' },
          { key: 'preferences', label: 'Preferences', icon: '⚙️' },
        ].map(t => (
          <button key={t.key} onClick={() => { if (t.key === 'rewards') { navigate('/rewards'); } else { setTab(t.key); } }}
            className={`flex items-center gap-1.5 px-4 py-2.5 rounded-lg text-sm font-semibold transition-all whitespace-nowrap ${
              tab === t.key ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}>
            <span className="text-base">{t.icon}</span>
            {t.label}
            {t.count !== undefined && <span className="text-[10px] bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded-full font-bold">{t.count}</span>}
          </button>
        ))}
      </div>

      {/* Profile Tab */}
      {tab === 'profile' && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="font-bold text-gray-900">Personal Information</h2>
            {!editingProfile && (
              <button onClick={() => setEditingProfile(true)} className="text-primary text-sm font-medium hover:underline">Edit Profile</button>
            )}
          </div>

          {editingProfile ? (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                <input type="text" value={nameValue} onChange={e => setNameValue(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                <input type="tel" value={phoneValue} onChange={e => setPhoneValue(e.target.value)} placeholder="+91 98765 43210"
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" />
                <p className="text-xs text-gray-400 mt-1">Phone storage requires backend support</p>
              </div>
              <div className="flex gap-3">
                <button onClick={handleProfileSave} disabled={profileSaving}
                  className="bg-primary text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
                  {profileSaving ? 'Saving...' : 'Save Changes'}
                </button>
                <button onClick={() => { setEditingProfile(false); setNameValue(profile?.name || ''); setPhoneValue(profile?.phone || ''); }}
                  className="px-6 py-2.5 border border-gray-300 rounded-xl text-sm font-medium text-gray-700 hover:bg-gray-50 transition">Cancel</button>
              </div>
            </div>
          ) : (
            <div className="space-y-0">
              {[
                { label: 'Full Name', value: profile?.name },
                { label: 'Email', value: profile?.email, note: 'Contact support to change email' },
                { label: 'Phone', value: profile?.phone || 'Not provided' },
                { label: 'Account Role', value: isAdmin ? 'Administrator' : 'Traveler' },
                { label: 'Member Since', value: profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' }) : 'N/A' },
                { label: 'Account Status', value: 'Active', badge: 'bg-green-100 text-green-700' },
              ].map((item, i) => (
                <div key={i} className="flex items-start justify-between py-3.5 border-b border-gray-100 last:border-0">
                  <div className="min-w-0">
                    <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold">{item.label}</p>
                    <p className="text-gray-900 font-medium text-sm mt-0.5">{item.value}</p>
                    {item.note && <p className="text-xs text-gray-400 mt-0.5">{item.note}</p>}
                  </div>
                  {item.badge && <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${item.badge}`}>{item.value}</span>}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Security Tab */}
      {tab === 'security' && (
        <div className="space-y-6">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <h2 className="font-bold text-gray-900 mb-5">Change Password</h2>
            <div className="space-y-4 max-w-md">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
                <input type="password" value={currentPw} onChange={e => setCurrentPw(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" placeholder="Enter current password" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <div className="relative">
                  <input type={showPw ? 'text' : 'password'} value={newPw} onChange={e => setNewPw(e.target.value)}
                    className="w-full px-4 py-3 pr-12 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none" placeholder="At least 8 characters" />
                  <button type="button" onClick={() => setShowPw(!showPw)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-1">
                    {showPw ? (
                      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" /></svg>
                    ) : (
                      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                    )}
                  </button>
                </div>
                {newPw.length > 0 && (
                  <div className="mt-2">
                    <div className="flex gap-1 h-1.5">
                      {[1,2,3,4].map(i => (
                        <div key={i} className={`flex-1 rounded-full transition-colors ${pwScore >= i ? strengthColor[pwScore] : 'bg-gray-200'}`} />
                      ))}
                    </div>
                    <p className="text-xs text-gray-400 mt-1">{strengthLabel[pwScore]}</p>
                  </div>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
                <input type={showPw ? 'text' : 'password'} value={confirmPw} onChange={e => setConfirmPw(e.target.value)}
                  className={`w-full px-4 py-3 border rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none ${confirmPw && confirmPw !== newPw ? 'border-red-300' : 'border-gray-300'}`} placeholder="Re-enter new password" />
                {confirmPw && confirmPw !== newPw && <p className="text-xs text-red-500 mt-1">Passwords do not match</p>}
              </div>
              <button onClick={handlePasswordChange} disabled={pwLoading || !currentPw || !newPw || !confirmPw}
                className="bg-primary text-white px-6 py-3 rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
                {pwLoading ? 'Changing...' : 'Change Password'}
              </button>
            </div>
          </div>

          <LinkedAccounts showToast={showToast} />

          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <h3 className="font-bold text-gray-900 mb-2">Forgot your password?</h3>
            <p className="text-sm text-gray-500 mb-4">We can send you a password reset link to your email address.</p>
            <Link to="/forgot-password" className="inline-block text-primary text-sm font-semibold hover:underline">Send reset link →</Link>
          </div>
        </div>
      )}

      {/* Saved Travellers Tab */}
      {tab === 'travellers' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-bold text-gray-900">Saved Travellers</h2>
              <p className="text-xs text-gray-500 mt-0.5">Speed up your booking by saving passenger details (max 10)</p>
            </div>
            {!showTravellerForm && travellers.length < 10 && (
              <button onClick={() => { setShowTravellerForm(true); setEditingTraveller(null); }}
                className="bg-primary text-white px-4 py-2 rounded-xl text-sm font-semibold hover:bg-blue-700 transition">
                + Add
              </button>
            )}
          </div>

          {showTravellerForm && (
            <TravellerForm traveller={editingTraveller} loading={tLoading} onSave={handleTravellerSave}
              onCancel={() => { setShowTravellerForm(false); setEditingTraveller(null); }} />
          )}

          {!showTravellerForm && travellers.length === 0 && (
            <div className="bg-white rounded-2xl border border-gray-100 p-10 text-center">
              <div className="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3">
                <span className="text-2xl">👤</span>
              </div>
              <h3 className="font-bold text-gray-900 mb-1">No saved travellers yet</h3>
              <p className="text-sm text-gray-500 mb-4 max-w-sm mx-auto">Save traveller details once, and select them during booking to save time.</p>
              <button onClick={() => setShowTravellerForm(true)}
                className="bg-primary text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition">Add your first traveller</button>
            </div>
          )}

          {!showTravellerForm && travellers.map(t => (
            <div key={t.id} className="bg-white rounded-2xl border border-gray-100 p-5 shadow-sm hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-11 h-11 rounded-full bg-primary-light flex items-center justify-center text-primary font-bold text-sm shrink-0">
                    {t.firstName?.charAt(0)}{t.lastName?.charAt(0)}
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-900 text-sm">{t.title} {t.firstName} {t.middleName || ''} {t.lastName}</p>
                    <div className="flex items-center gap-2 text-xs text-gray-500 mt-0.5 flex-wrap">
                      {t.gender && <span>{t.gender.charAt(0) + t.gender.slice(1).toLowerCase()}</span>}
                      {t.gender && t.nationality && <span>·</span>}
                      {t.nationality && <span>{t.nationality}</span>}
                    </div>
                    {t.dateOfBirth && <p className="text-xs text-gray-400 mt-0.5">DOB: {new Date(t.dateOfBirth).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</p>}
                    {t.passportNumber && <p className="text-xs text-gray-400 mt-0.5">Passport: {maskPassport(t.passportNumber)}</p>}
                  </div>
                </div>
                <div className="flex gap-1 shrink-0">
                  <button onClick={() => { setEditingTraveller(t); setShowTravellerForm(true); }}
                    className="text-primary text-xs font-medium hover:underline px-2 py-1 rounded-lg hover:bg-blue-50 transition">Edit</button>
                  <button onClick={() => setDeleteTarget(t)}
                    className="text-red-500 text-xs font-medium hover:underline px-2 py-1 rounded-lg hover:bg-red-50 transition">Remove</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Preferences Tab */}
      {tab === 'preferences' && (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
          <h2 className="font-bold text-gray-900 mb-5">Travel Preferences</h2>
          <div className="space-y-5 max-w-lg">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Preferred Cabin Class</label>
              <select value={prefs.preferredCabin} onChange={e => setPrefs(p => ({ ...p, preferredCabin: e.target.value }))}
                className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none">
                <option value="ECONOMY">Economy</option>
                <option value="PREMIUM_ECONOMY">Premium Economy</option>
                <option value="BUSINESS">Business</option>
                <option value="FIRST">First Class</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Currency</label>
              <select value={prefs.currency} onChange={e => setPrefs(p => ({ ...p, currency: e.target.value }))}
                className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none">
                <option value="INR">₹ INR (Indian Rupee)</option>
                <option value="USD">$ USD (US Dollar)</option>
                <option value="EUR">€ EUR (Euro)</option>
                <option value="GBP">£ GBP (British Pound)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Language</label>
              <select value={prefs.language} onChange={e => setPrefs(p => ({ ...p, language: e.target.value }))}
                className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none">
                <option>English</option>
                <option>Hindi</option>
              </select>
            </div>

            {/* Seat & Room Selection Personalization */}
            <div className="border-t border-gray-100 pt-5">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <h3 className="font-bold text-gray-900 flex items-center gap-2">
                    <span>💺🏨</span> Seat & Room Personalization
                  </h3>
                  <p className="text-xs text-gray-500">
                    Your preferences will be automatically highlighted on flight seat maps and hotel room grids.
                  </p>
                </div>
              </div>

              <div className="space-y-4 bg-gray-50/70 p-4 rounded-2xl border border-gray-100">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Preferred Flight Seat</label>
                    <select
                      value={travelPrefs.preferredSeatPosition}
                      onChange={e => setTravelPrefs(p => ({ ...p, preferredSeatPosition: e.target.value }))}
                      className="w-full px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      <option value="WINDOW">🪟 Window Seat</option>
                      <option value="AISLE">🚶 Aisle Seat</option>
                      <option value="MIDDLE">👥 Middle Seat</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Seat Category</label>
                    <select
                      value={travelPrefs.preferredSeatType}
                      onChange={e => setTravelPrefs(p => ({ ...p, preferredSeatType: e.target.value }))}
                      className="w-full px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      <option value="EXTRA_LEGROOM">Extra Legroom</option>
                      <option value="EXIT_ROW">Exit Row</option>
                      <option value="PREMIUM">Premium Front Row</option>
                      <option value="STANDARD">Standard</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Preferred Hotel Room</label>
                    <select
                      value={travelPrefs.preferredRoomType}
                      onChange={e => setTravelPrefs(p => ({ ...p, preferredRoomType: e.target.value }))}
                      className="w-full px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      <option value="STANDARD">Standard Room</option>
                      <option value="DELUXE">Deluxe Room</option>
                      <option value="SUITE">Executive Suite</option>
                      <option value="PRESIDENTIAL">Presidential Suite</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Bed Configuration</label>
                    <select
                      value={travelPrefs.preferredBedType}
                      onChange={e => setTravelPrefs(p => ({ ...p, preferredBedType: e.target.value }))}
                      className="w-full px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      <option value="KING">King Bed</option>
                      <option value="QUEEN">Queen Bed</option>
                      <option value="TWIN">Twin Beds</option>
                    </select>
                  </div>
                </div>

                <div className="flex justify-end pt-2">
                  <button
                    type="button"
                    onClick={handleSaveTravelPrefs}
                    disabled={travelPrefsSaving}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold transition shadow-xs disabled:opacity-50"
                  >
                    {travelPrefsSaving ? 'Saving...' : 'Save Seat & Room Preferences'}
                  </button>
                </div>
              </div>
            </div>

            <div className="border-t border-gray-100 pt-5">
              <h3 className="font-semibold text-gray-900 mb-3">Notification Preferences</h3>
              {notifPrefsLoading ? (
                <div className="space-y-3">
                  {[1, 2, 3, 4, 5].map(i => <div key={i} className="h-14 bg-gray-100 rounded-lg animate-pulse" />)}
                </div>
              ) : notifPrefs ? (
                <div className="space-y-1">
                  <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2">Email Notifications</p>
                  {[
                    { key: 'emailBookingUpdates', label: 'Booking updates', desc: 'Confirmation, cancellation, and status changes' },
                    { key: 'emailPaymentUpdates', label: 'Payment updates', desc: 'Payment confirmation and receipts' },
                    { key: 'emailCancellationRefund', label: 'Cancellation & Refunds', desc: 'Cancellation and refund processing updates' },
                    { key: 'emailFlightUpdates', label: 'Flight updates', desc: 'Flight status changes and delays' },
                    { key: 'emailPriceAlerts', label: 'Price alerts', desc: 'Price drop notifications' },
                    { key: 'emailMarketing', label: 'Marketing', desc: 'Deals, offers, and promotions' },
                  ].map(item => (
                    <div key={item.key} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0">
                      <div>
                        <p className="text-sm font-medium text-gray-900">{item.label}</p>
                        <p className="text-xs text-gray-500">{item.desc}</p>
                      </div>
                      <button
                        onClick={() => {
                          const updated = { ...notifPrefs, [item.key]: !notifPrefs[item.key] };
                          setNotifPrefs(updated);
                          notificationsApi.updatePreferences(updated).catch(() => setNotifPrefs(notifPrefs));
                        }}
                        className={`w-11 h-6 rounded-full transition-colors relative ${notifPrefs[item.key] ? 'bg-primary' : 'bg-gray-300'}`}
                        role="switch"
                        aria-checked={notifPrefs[item.key]}
                      >
                        <span className="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
                          style={{ left: notifPrefs[item.key] ? '22px' : '2px' }} />
                      </button>
                    </div>
                  ))}

                  <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mt-4 mb-2">In-App Notifications</p>
                  {[
                    { key: 'inAppBookingUpdates', label: 'Booking updates', desc: 'Booking confirmation and status' },
                    { key: 'inAppPaymentUpdates', label: 'Payment updates', desc: 'Payment confirmations' },
                    { key: 'inAppFlightUpdates', label: 'Flight updates', desc: 'Flight status changes' },
                    { key: 'inAppPriceAlerts', label: 'Price alerts', desc: 'Price drop alerts' },
                  ].map(item => (
                    <div key={item.key} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0">
                      <div>
                        <p className="text-sm font-medium text-gray-900">{item.label}</p>
                        <p className="text-xs text-gray-500">{item.desc}</p>
                      </div>
                      <button
                        onClick={() => {
                          const updated = { ...notifPrefs, [item.key]: !notifPrefs[item.key] };
                          setNotifPrefs(updated);
                          notificationsApi.updatePreferences(updated).catch(() => setNotifPrefs(notifPrefs));
                        }}
                        className={`w-11 h-6 rounded-full transition-colors relative ${notifPrefs[item.key] ? 'bg-primary' : 'bg-gray-300'}`}
                        role="switch"
                        aria-checked={notifPrefs[item.key]}
                      >
                        <span className="absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform"
                          style={{ left: notifPrefs[item.key] ? '22px' : '2px' }} />
                      </button>
                    </div>
                  ))}
                  <p className="text-[11px] text-gray-400 mt-3">Security notifications (password reset, account alerts) are always enabled and cannot be disabled.</p>
                </div>
              ) : (
                <p className="text-sm text-gray-500">Could not load notification preferences.</p>
              )}
            </div>

            <p className="text-xs text-gray-400 italic">These preferences are stored locally and will sync to your account when backend support is available.</p>
          </div>
        </div>
      )}
    </div>
  );
}
