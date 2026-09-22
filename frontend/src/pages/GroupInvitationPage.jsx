import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { voyaraApi } from '../api/phase3Api';

export default function GroupInvitationPage() {
  const { token } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [invitation, setInvitation] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    const loadInvitation = async () => {
      try {
        const response = await voyaraApi.getInvitationByToken(token);
        setInvitation(response.data.data);
      } catch (err) {
        setError(err.response?.data?.message || 'This invitation is invalid or expired.');
      } finally {
        setLoading(false);
      }
    };

    if (token) loadInvitation();
  }, [token]);

  const handleInviteAction = async (action) => {
    if (!isAuthenticated) {
      navigate('/login', {
        replace: true,
        state: { from: { pathname: `/group-invitations/${token}` }, message: 'Please sign in to respond to this trip invite.' }
      });
      return;
    }

    try {
      setActionLoading(true);
      if (action === 'accept') {
        await voyaraApi.acceptInvitation(token);
      } else {
        await voyaraApi.declineInvitation(token);
      }
      navigate('/group-trips', { replace: true, state: { message: action === 'accept' ? 'Invitation accepted.' : 'Invitation declined.' } });
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to update this invitation right now.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return <div className="max-w-xl mx-auto px-4 py-20 text-center text-slate-500">Loading invitation...</div>;
  }

  if (error) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center">
        <div className="text-5xl mb-4">⚠️</div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Invitation unavailable</h1>
        <p className="text-slate-600 mb-6">{error}</p>
        <Link to="/" className="inline-flex rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white">Back to home</Link>
      </div>
    );
  }

  const trip = invitation?.groupTrip;
  const inviter = invitation?.inviterUser;
  const validForUser = !user || user.email?.toLowerCase() === invitation?.inviteeEmail?.toLowerCase();

  return (
    <div className="max-w-xl mx-auto px-4 py-10">
      <div className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-xl shadow-slate-200/50 sm:p-8">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-2xl">✈️</div>
          <h1 className="text-3xl font-black text-slate-900">Trip invitation</h1>
          <p className="mt-2 text-sm text-slate-500">{trip?.name || 'Group trip'}</p>
        </div>

        <div className="space-y-4 rounded-2xl bg-slate-50 p-4 text-sm text-slate-700">
          <div className="flex justify-between gap-3"><span>Invited by</span><strong>{inviter?.name || 'Your travel companion'}</strong></div>
          <div className="flex justify-between gap-3"><span>Trip</span><strong>{trip?.name}</strong></div>
          {trip?.startDate && <div className="flex justify-between gap-3"><span>Dates</span><strong>{trip.startDate} to {trip.endDate || trip.startDate}</strong></div>}
          {invitation?.message && <div className="rounded-xl border border-slate-200 bg-white p-3 text-slate-600">“{invitation.message}”</div>}
        </div>

        {!isAuthenticated && (
          <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Sign in to accept or decline this invitation.
          </div>
        )}

        {isAuthenticated && !validForUser && (
          <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            This invitation is for a different email address than the signed-in account.
          </div>
        )}

        <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <button
            type="button"
            disabled={actionLoading || !isAuthenticated || !validForUser}
            onClick={() => handleInviteAction('accept')}
            className="rounded-xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {actionLoading ? 'Processing…' : 'Accept'}
          </button>
          <button
            type="button"
            disabled={actionLoading || !isAuthenticated || !validForUser}
            onClick={() => handleInviteAction('decline')}
            className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
          >
            {actionLoading ? 'Processing…' : 'Decline'}
          </button>
        </div>

        <div className="mt-6 text-center">
          <Link to="/" className="text-sm font-medium text-slate-500 hover:text-slate-700">Return home</Link>
        </div>
      </div>
    </div>
  );
}