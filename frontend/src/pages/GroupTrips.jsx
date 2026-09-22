import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { voyaraApi } from '../api/phase3Api';
import { getTodayDate } from '../utils/dateUtils';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function GroupTrips() {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(null);
  const [newTrip, setNewTrip] = useState({ name: '', description: '', startDate: '', endDate: '' });
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteError, setInviteError] = useState('');
  const [toast, setToast] = useState(null);

  const today = getTodayDate();

  const showToast = (type, message) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };

  const { data: tripsData, isLoading } = useQuery({
    queryKey: ['groupTrips'],
    queryFn: () => voyaraApi.getGroupTrips().then(r => r.data),
  });

  const createMutation = useMutation({
    mutationFn: (data) => voyaraApi.createGroupTrip(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groupTrips'] });
      setShowCreateModal(false);
      setNewTrip({ name: '', description: '', startDate: '', endDate: '' });
      showToast('success', 'Trip created!');
    },
    onError: (err) => showToast('error', err.response?.data?.message || 'Failed to create trip'),
  });

  const inviteMutation = useMutation({
    mutationFn: ({ tripId, email }) => voyaraApi.inviteCompanion(tripId, { email, message: 'Join my trip!' }),
    onSuccess: () => {
      setShowInviteModal(null);
      setInviteEmail('');
      setInviteError('');
      showToast('success', 'Invitation sent successfully!');
    },
    onError: (err) => {
      const errMsg = err.response?.data?.message || 'Unable to send invitation email. Please try again.';
      setInviteError(errMsg);
      showToast('error', errMsg);
    },
  });

  const handleCreateTrip = () => {
    if (!newTrip.name.trim()) {
      showToast('error', 'Trip name is required');
      return;
    }
    if (newTrip.startDate && newTrip.startDate < today) {
      showToast('error', 'Trip dates cannot be in the past.');
      return;
    }
    if (newTrip.endDate && newTrip.endDate < today) {
      showToast('error', 'Trip dates cannot be in the past.');
      return;
    }
    if (newTrip.startDate && newTrip.endDate && newTrip.endDate < newTrip.startDate) {
      showToast('error', 'End date must be on or after the start date.');
      return;
    }
    createMutation.mutate(newTrip);
  };

  const handleSendInvite = (tripId) => {
    const trimmed = inviteEmail.trim();
    if (!trimmed) {
      setInviteError('Enter an email address.');
      return;
    }
    if (!EMAIL_REGEX.test(trimmed)) {
      setInviteError('Enter a valid email address.');
      return;
    }
    setInviteError('');
    inviteMutation.mutate({ tripId, email: trimmed.toLowerCase() });
  };

  const trips = tripsData?.data || [];

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 sm:py-8">
      {toast && (
        <div className={`fixed top-20 right-4 z-[100] px-4 py-3 rounded-xl border shadow-lg text-sm font-medium ${
          toast.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'
        }`}>
          {toast.message}
        </div>
      )}

      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Group Trips</h1>
          <p className="text-sm text-gray-500 mt-1">Plan and share trips with travel companions</p>
        </div>
        <button onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 transition">
          + New Trip
        </button>
      </div>

      {isLoading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2].map(i => <div key={i} className="h-24 bg-gray-100 rounded-2xl" />)}
        </div>
      ) : trips.length === 0 ? (
        <div className="text-center py-16">
          <div className="text-5xl mb-4">👥</div>
          <h2 className="text-lg font-semibold text-gray-900 mb-2">No group trips yet</h2>
          <p className="text-sm text-gray-500 mb-6">Create a trip and invite your travel companions</p>
          <button onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 text-white px-6 py-3 rounded-xl text-sm font-semibold hover:bg-blue-700 transition">
            Create Your First Trip
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {trips.map(trip => (
            <div key={trip.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 hover:shadow-md transition">
              <div className="flex items-start justify-between">
                <div>
                  <Link to={`/group-trips/${trip.id}`} className="text-lg font-bold text-gray-900 hover:text-blue-600 transition">
                    {trip.name}
                  </Link>
                  {trip.description && <p className="text-sm text-gray-500 mt-1">{trip.description}</p>}
                  <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                    {trip.startDate && <span>📅 {trip.startDate}</span>}
                    {trip.endDate && <span>→ {trip.endDate}</span>}
                  </div>
                </div>
                <button onClick={() => setShowInviteModal(trip.id)}
                  className="text-xs bg-gray-100 text-gray-600 px-3 py-1.5 rounded-lg font-medium hover:bg-gray-200 transition">
                  + Invite
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={() => setShowCreateModal(false)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-md w-full" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-bold text-gray-900 mb-4">Create Group Trip</h3>
            <div className="space-y-3">
              <input type="text" placeholder="Trip name (e.g., Goa Trip)" value={newTrip.name}
                onChange={e => setNewTrip(p => ({ ...p, name: e.target.value }))}
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
              <textarea placeholder="Description (optional)" value={newTrip.description}
                onChange={e => setNewTrip(p => ({ ...p, description: e.target.value }))}
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none h-20 resize-none" />
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-gray-500 mb-1">Start Date</label>
                  <input
                    type="date"
                    value={newTrip.startDate}
                    min={today}
                    onChange={e => {
                      const val = e.target.value;
                      if (val && val < today) {
                        showToast('error', 'Trip dates cannot be in the past.');
                        return;
                      }
                      setNewTrip(p => ({
                        ...p,
                        startDate: val,
                        // Auto-clear endDate if it's now before the new startDate
                        endDate: p.endDate && p.endDate < val ? '' : p.endDate,
                      }));
                    }}
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">End Date</label>
                  <input
                    type="date"
                    value={newTrip.endDate}
                    min={newTrip.startDate || today}
                    onChange={e => {
                      const val = e.target.value;
                      const minAllowed = newTrip.startDate || today;
                      if (val && val < minAllowed) {
                        showToast('error', newTrip.startDate ? 'End date must be on or after the start date.' : 'Trip dates cannot be in the past.');
                        return;
                      }
                      setNewTrip(p => ({ ...p, endDate: val }));
                    }}
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
              </div>
            </div>
            <div className="flex gap-3 mt-5">
              <button onClick={() => setShowCreateModal(false)}
                className="flex-1 px-4 py-3 border border-gray-200 rounded-xl text-sm font-semibold text-gray-700 hover:bg-gray-50 transition">
                Cancel
              </button>
              <button
                onClick={handleCreateTrip}
                disabled={!newTrip.name || createMutation.isPending}
                className="flex-1 px-4 py-3 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
                {createMutation.isPending ? 'Creating...' : 'Create Trip'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Invite Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={() => { setShowInviteModal(null); setInviteError(''); }}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-md w-full" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-bold text-gray-900 mb-1">Invite Companion</h3>
            <p className="text-xs text-gray-500 mb-4">An invitation email will be sent to this address.</p>

            {inviteError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium">
                {inviteError}
              </div>
            )}

            <input
              type="email"
              placeholder="Email address"
              value={inviteEmail}
              disabled={inviteMutation.isPending}
              onChange={e => {
                setInviteEmail(e.target.value);
                if (inviteError) setInviteError('');
              }}
              onKeyDown={e => {
                if (e.key === 'Enter' && inviteEmail && !inviteMutation.isPending) {
                  handleSendInvite(showInviteModal);
                }
              }}
              className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none disabled:bg-gray-50" />
            <div className="flex gap-3 mt-5">
              <button
                onClick={() => { setShowInviteModal(null); setInviteError(''); }}
                disabled={inviteMutation.isPending}
                className="flex-1 px-4 py-3 border border-gray-200 rounded-xl text-sm font-semibold text-gray-700 hover:bg-gray-50 transition disabled:opacity-50">
                Cancel
              </button>
              <button
                onClick={() => handleSendInvite(showInviteModal)}
                disabled={!inviteEmail.trim() || inviteMutation.isPending}
                className="flex-1 px-4 py-3 bg-blue-600 text-white rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition flex items-center justify-center gap-2">
                {inviteMutation.isPending ? (
                  <>
                    <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                    </svg>
                    <span>Sending...</span>
                  </>
                ) : inviteError ? (
                  'Unable to send — Try Again'
                ) : (
                  'Send Invite'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
