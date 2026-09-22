import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getBookingByReference } from '../api/bookingApi';
import { voyaraApi } from '../api/phase3Api';
import ConnectionRiskMeter from '../components/intelligence/ConnectionRiskMeter';
import TripReadinessCard from '../components/intelligence/TripReadinessCard';
import SmartTimeline from '../components/intelligence/SmartTimeline';
import CancellationFlow from '../components/CancellationFlow';
import RefundStatusTracker from '../components/RefundStatusTracker';
import {
  FlightIcon,
  HotelIcon,
  ShieldCheckIcon,
  ClockIcon,
  MapPinIcon,
  LuggageIcon,
  AlertTriangleIcon,
  CheckIcon,
  ArrowRightIcon,
} from '../components/common/Icons';

const STATUS_COLORS = {
  CONFIRMED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
  COMPLETED: 'bg-blue-50 text-blue-700 border-blue-200',
  FAILED: 'bg-slate-100 text-slate-700 border-slate-200',
};

export default function TripOverview() {
  const { reference } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('overview');
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showRefundModal, setShowRefundModal] = useState(false);

  const { data: booking, isLoading: bookingLoading } = useQuery({
    queryKey: ['booking', reference],
    queryFn: () => getBookingByReference(reference),
    enabled: !!reference,
  });

  const { data: readiness, refetch: refetchReadiness } = useQuery({
    queryKey: ['readiness', booking?.id],
    queryFn: () => voyaraApi.getReadiness(booking.id).then((r) => r.data.data),
    enabled: !!booking?.id,
  });

  const { data: connectionRisk } = useQuery({
    queryKey: ['connectionRisk', booking?.id],
    queryFn: () => voyaraApi.getConnectionRisk(booking.id).then((r) => r.data.data),
    enabled: !!booking?.id && booking?.bookingType === 'FLIGHT',
  });

  const { data: guardianData } = useQuery({
    queryKey: ['guardian'],
    queryFn: () => voyaraApi.getGuardianAlerts().then((r) => r.data),
    enabled: !!booking,
  });

  const { data: timeline } = useQuery({
    queryKey: ['timeline', booking?.id],
    queryFn: () => voyaraApi.getTimeline(booking.id).then((r) => r.data.data),
    enabled: !!booking?.id,
  });

  if (bookingLoading) {
    return (
      <div className="section-shell py-10 max-w-4xl mx-auto space-y-4">
        <div className="skeleton h-8 w-48 rounded-xl" />
        <div className="skeleton h-44 w-full rounded-2xl" />
        <div className="grid sm:grid-cols-2 gap-4">
          <div className="skeleton h-56 rounded-2xl" />
          <div className="skeleton h-56 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="section-shell py-20 text-center max-w-md mx-auto">
        <div className="w-16 h-16 rounded-2xl bg-blue-50 text-primary flex items-center justify-center mx-auto mb-4">
          <LuggageIcon className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-slate-900 mb-2">Booking not found</h2>
        <p className="text-sm text-slate-500 mb-6">
          We couldn't retrieve booking reference <span className="font-mono font-bold text-slate-800">{reference}</span>.
        </p>
        <Link to="/dashboard" className="travel-button-primary px-6 py-2.5 text-xs">
          Return to My Trips
        </Link>
      </div>
    );
  }

  const isFlight = booking.bookingType === 'FLIGHT';
  const guardianAlerts = guardianData?.data?.filter((a) => !a.dismissed) || [];
  const unreadAlerts = guardianData?.unreadCount || 0;

  return (
    <div className="section-shell py-8 max-w-5xl mx-auto">
      {/* Back button */}
      <div className="mb-5 flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate('/dashboard')}
          className="inline-flex items-center gap-2 text-xs font-bold text-slate-600 hover:text-primary transition"
        >
          ← Back to My Trips
        </button>

        <span className="text-xs font-mono font-bold text-slate-400">
          Ref: {booking.bookingReference}
        </span>
      </div>

      {/* Hero Trip Card */}
      <div className="overflow-hidden rounded-[26px] bg-gradient-to-br from-[#071326] via-[#0b244d] to-primary p-6 sm:p-8 text-white shadow-xl shadow-blue-900/15 mb-6 relative">
        <div className="relative z-10 flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-xs font-bold tracking-wider text-sky-200 border border-white/15 mb-3">
              {isFlight ? <FlightIcon className="w-3.5 h-3.5" /> : <HotelIcon className="w-3.5 h-3.5" />}
              {booking.bookingType} RESERVATION
            </span>

            <h1 className="text-2xl sm:text-3xl font-black text-white">
              {isFlight ? `${booking.originCode} → ${booking.destinationCode}` : booking.hotelName}
            </h1>

            <p className="text-xs sm:text-sm text-sky-200/85 mt-1 font-medium">
              {isFlight
                ? `${booking.airlineName} · ${booking.flightNumber} · ${booking.cabinClass?.replace('_', ' ') || 'Economy'}`
                : `${booking.roomName || booking.roomType || 'Standard Room'}`}
            </p>
          </div>

          <div className="flex flex-col items-end gap-2">
            <span
              className={`px-3 py-1 rounded-full text-xs font-bold border backdrop-blur-sm ${
                STATUS_COLORS[booking.status] || STATUS_COLORS.PENDING
              }`}
            >
              {booking.status}
            </span>

            <div className="text-right">
              <span className="text-[10px] uppercase font-bold text-sky-200/70 block">Total Fare</span>
              <span className="text-xl font-black text-white">₹{booking.totalAmount?.toLocaleString()}</span>
            </div>
          </div>
        </div>

        {/* Quick flight schedule strip */}
        {isFlight && booking.departureTime && (
          <div className="relative z-10 mt-6 pt-5 border-t border-white/10 grid grid-cols-3 gap-2 text-center text-xs">
            <div>
              <span className="text-[10px] uppercase font-bold text-sky-200/70 block">Departure</span>
              <span className="text-sm font-bold text-white">
                {new Date(booking.departureTime).toLocaleTimeString('en-IN', {
                  hour: '2-digit',
                  minute: '2-digit',
                  hour12: false,
                })}
              </span>
              <span className="text-[11px] text-sky-200 block">{booking.originCode}</span>
            </div>

            <div className="flex flex-col items-center justify-center">
              <span className="text-[10px] text-sky-200/70 font-semibold">
                {booking.durationMinutes ? `${Math.floor(booking.durationMinutes / 60)}h ${booking.durationMinutes % 60}m` : 'Direct'}
              </span>
              <div className="w-full max-w-[100px] h-px bg-sky-400/40 my-1" />
              <span className="text-[10px] text-emerald-300 font-bold">
                {booking.stops === 0 ? 'Non-stop' : `${booking.stops} stop`}
              </span>
            </div>

            <div>
              <span className="text-[10px] uppercase font-bold text-sky-200/70 block">Arrival</span>
              <span className="text-sm font-bold text-white">
                {booking.arrivalTime
                  ? new Date(booking.arrivalTime).toLocaleTimeString('en-IN', {
                      hour: '2-digit',
                      minute: '2-digit',
                      hour12: false,
                    })
                  : '—'}
              </span>
              <span className="text-[11px] text-sky-200 block">{booking.destinationCode}</span>
            </div>
          </div>
        )}
      </div>

      {/* Navigation Tabs */}
      <div className="flex gap-1 rounded-2xl bg-slate-100 p-1.5 mb-6 overflow-x-auto no-scrollbar border border-slate-200/80">
        {[
          { key: 'overview', label: 'Intelligence & Readiness' },
          { key: 'timeline', label: 'Smart Timeline' },
          { key: 'guardian', label: `Guardian Alerts ${unreadAlerts > 0 ? `(${unreadAlerts})` : ''}` },
          { key: 'details', label: 'Booking & Fare Details' },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex-1 min-w-[140px] px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === tab.key
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab: Overview (Intelligence & Readiness) */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Connection Risk Meter (if flight with connections or data available) */}
          {connectionRisk && <ConnectionRiskMeter riskData={connectionRisk} />}

          {/* Trip Readiness Card */}
          {readiness && (
            <TripReadinessCard
              bookingId={booking.id}
              readiness={readiness}
              onRecalculate={refetchReadiness}
            />
          )}

          {/* Quick Actions Row */}
          <div className="flex flex-wrap gap-3 pt-2">
            {isFlight && (
              <Link
                to="/live-tracker"
                className="travel-button-primary px-5 py-2.5 text-xs font-bold"
              >
                📡 Open Live Flight Tracker
              </Link>
            )}

            {booking.status === 'CONFIRMED' && (
              <button
                type="button"
                onClick={() => setShowCancelModal(true)}
                className="px-5 py-2.5 rounded-xl border border-red-200 bg-red-50 text-xs font-bold text-red-700 hover:bg-red-100 transition"
              >
                Cancel Booking & Request Refund
              </button>
            )}

            {booking.status === 'CANCELLED' && (
              <button
                type="button"
                onClick={() => setShowRefundModal(true)}
                className="px-5 py-2.5 rounded-xl border border-slate-200 bg-white text-xs font-bold text-slate-700 hover:bg-slate-50 transition"
              >
                View Refund Tracker
              </button>
            )}
          </div>
        </div>
      )}

      {/* Tab: Smart Timeline */}
      {activeTab === 'timeline' && (
        <SmartTimeline events={timeline || []} />
      )}

      {/* Tab: Guardian Alerts */}
      {activeTab === 'guardian' && (
        <div className="voyara-card p-5 sm:p-6 space-y-3">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <h4 className="text-sm font-bold text-slate-900">Guardian Travel Alerts</h4>
            <span className="text-xs text-slate-500 font-semibold">{guardianAlerts.length} Active Alerts</span>
          </div>

          {guardianAlerts.length === 0 ? (
            <div className="py-10 text-center">
              <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto mb-3">
                <CheckIcon className="w-6 h-6" />
              </div>
              <h5 className="text-sm font-bold text-slate-900">No Disruption Detected</h5>
              <p className="text-xs text-slate-500 mt-1">Voyara Guardian is actively tracking flight corridors and weather conditions.</p>
            </div>
          ) : (
            guardianAlerts.map((alert) => (
              <div
                key={alert.id}
                className="rounded-2xl border border-amber-200 bg-amber-50/50 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-amber-700 px-2 py-0.5 rounded-md bg-amber-100">
                    {alert.severity || 'ALERT'}
                  </span>
                  <h5 className="text-sm font-bold text-slate-900 mt-1">{alert.title}</h5>
                  <p className="text-xs text-slate-600 mt-0.5 leading-relaxed">{alert.message}</p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {alert.actionRoute && (
                    <Link
                      to={alert.actionRoute}
                      className="px-3 py-1.5 rounded-lg bg-white border border-amber-300 text-xs font-bold text-amber-800 hover:bg-amber-50 shadow-sm transition"
                    >
                      {alert.actionLabel || 'Resolve'}
                    </Link>
                  )}
                  <button
                    onClick={() => voyaraApi.dismissAlert(alert.id).then(() => queryClient.invalidateQueries({ queryKey: ['guardian'] }))}
                    className="px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-500 hover:text-slate-800"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Tab: Booking & Fare Details */}
      {activeTab === 'details' && (
        <div className="grid sm:grid-cols-2 gap-5">
          <div className="voyara-card p-5 sm:p-6 space-y-3">
            <h4 className="text-sm font-bold text-slate-900 pb-2 border-b border-slate-100">Passenger & Route Info</h4>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Service</span>
                <span className="font-bold text-slate-800">{booking.bookingType}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Travelers</span>
                <span className="font-bold text-slate-800">{booking.passengerCount} Guest(s)</span>
              </div>
              {booking.seatNumbers && (
                <div className="flex justify-between py-1 border-b border-slate-50">
                  <span className="text-slate-500">Selected Seats</span>
                  <span className="font-bold text-primary">{booking.seatNumbers}</span>
                </div>
              )}
              {booking.cabinClass && (
                <div className="flex justify-between py-1 border-b border-slate-50">
                  <span className="text-slate-500">Cabin Class</span>
                  <span className="font-bold text-slate-800">{booking.cabinClass.replace('_', ' ')}</span>
                </div>
              )}
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Booking Reference</span>
                <span className="font-mono font-bold text-slate-900">{booking.bookingReference}</span>
              </div>
            </div>
          </div>

          <div className="voyara-card p-5 sm:p-6 space-y-3">
            <h4 className="text-sm font-bold text-slate-900 pb-2 border-b border-slate-100">Payment Breakdown</h4>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Payment Status</span>
                <span className="font-bold text-emerald-600">Paid (Razorpay)</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">Taxes & Surcharges</span>
                <span className="font-medium text-slate-800">Included</span>
              </div>
              <div className="flex justify-between py-2 text-sm font-black text-slate-900 border-t border-slate-100">
                <span>Total Amount</span>
                <span className="text-primary">₹{booking.totalAmount?.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Embedded Cancellation Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-lg bg-white rounded-3xl p-6 shadow-2xl">
            <CancellationFlow
              booking={booking}
              onClose={() => {
                setShowCancelModal(false);
                queryClient.invalidateQueries({ queryKey: ['booking', reference] });
              }}
            />
          </div>
        </div>
      )}

      {/* Embedded Refund Status Modal */}
      {showRefundModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-xl bg-white rounded-3xl p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-base font-bold text-slate-900">Refund Tracker</h4>
              <button
                onClick={() => setShowRefundModal(false)}
                className="text-slate-400 hover:text-slate-600 text-lg"
              >
                ✕
              </button>
            </div>
            <RefundStatusTracker bookingId={booking.id} />
          </div>
        </div>
      )}
    </div>
  );
}
