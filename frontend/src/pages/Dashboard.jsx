import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { getMyBookings } from '../api/bookingApi';
import { refundApi, flightStatusApi, voyaraApi } from '../api/phase3Api';
import CancellationFlow from '../components/CancellationFlow';
import RefundStatusTracker from '../components/RefundStatusTracker';
import RecommendationsSection from '../components/RecommendationsSection';
import {
  FlightIcon,
  HotelIcon,
  LuggageIcon,
  ShieldCheckIcon,
  ClockIcon,
  CheckIcon,
  CompassIcon,
  AlertTriangleIcon,
} from '../components/common/Icons';

const STATUS_STYLES = {
  CONFIRMED: 'border border-emerald-200 bg-emerald-50 text-emerald-700',
  PENDING: 'border border-amber-200 bg-amber-50 text-amber-700',
  CANCELLED: 'border border-red-200 bg-red-50 text-red-700',
  COMPLETED: 'border border-blue-200 bg-blue-50 text-blue-700',
  FAILED: 'border border-slate-200 bg-slate-100 text-slate-600',
};

function SkeletonCard() {
  return (
    <div className="skeleton rounded-2xl border border-slate-200 bg-white p-5 h-36" />
  );
}

function NextTripCard({ booking }) {
  if (!booking) return null;
  const isFlight = booking.bookingType === 'FLIGHT';
  const travelDate = booking.departureTime || booking.checkInDate;
  const dateObj = travelDate ? new Date(travelDate) : null;

  return (
    <div className="mb-6 overflow-hidden rounded-[26px] bg-gradient-to-br from-[#071326] via-[#0b2550] to-primary p-6 sm:p-7 text-white shadow-xl shadow-blue-900/15">
      <div className="relative">
        <div className="flex items-center gap-2 mb-3">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-[10px] font-bold uppercase tracking-[0.2em] text-sky-200 border border-white/15">
            <LuggageIcon className="w-3.5 h-3.5" />
            Upcoming Journey
          </span>
        </div>

        {isFlight ? (
          <div className="mb-4 flex items-center gap-3.5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15 text-white backdrop-blur-sm shadow-inner">
              <FlightIcon className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl sm:text-3xl font-black">{booking.originCode} → {booking.destinationCode}</div>
              <div className="text-xs sm:text-sm text-sky-200 font-medium">{booking.airlineName} · {booking.flightNumber}</div>
            </div>
          </div>
        ) : (
          <div className="mb-4 flex items-center gap-3.5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15 text-white backdrop-blur-sm shadow-inner">
              <HotelIcon className="w-6 h-6" />
            </div>
            <div>
              <div className="text-2xl sm:text-3xl font-black">{booking.hotelName}</div>
              <div className="text-xs sm:text-sm text-sky-200 font-medium">{booking.roomName || booking.roomType}</div>
            </div>
          </div>
        )}

        <div className="mb-5 flex flex-wrap items-center gap-5 text-xs sm:text-sm text-sky-200">
          {dateObj && (
            <div>
              <div className="text-[10px] uppercase font-bold tracking-wider text-sky-300/70">Date</div>
              <div className="font-bold text-white">{dateObj.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })}</div>
            </div>
          )}
          {isFlight && booking.departureTime && (
            <div>
              <div className="text-[10px] uppercase font-bold tracking-wider text-sky-300/70">Departure</div>
              <div className="font-bold text-white">{new Date(booking.departureTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}</div>
            </div>
          )}
          {!isFlight && booking.checkInDate && (
            <div>
              <div className="text-[10px] uppercase font-bold tracking-wider text-sky-300/70">Check-in</div>
              <div className="font-bold text-white">{new Date(booking.checkInDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</div>
            </div>
          )}
          <div>
            <div className="text-[10px] uppercase font-bold tracking-wider text-sky-300/70">Amount</div>
            <div className="text-base sm:text-lg font-black text-white">₹{booking.totalAmount?.toLocaleString()}</div>
          </div>
        </div>

        <div className="flex flex-wrap gap-2.5">
          <Link to={`/dashboard/trip/${booking.bookingReference}`} className="travel-button-primary px-5 py-2.5 text-xs font-bold">
            View Trip Details
          </Link>
          {isFlight && (
            <Link to="/live-tracker" className="rounded-xl border border-white/20 bg-white/10 px-4 py-2 text-xs font-bold text-white transition hover:bg-white/15 flex items-center gap-1.5">
              <CompassIcon className="w-3.5 h-3.5" />
              Live Tracker
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}

function FlightTripCard({ booking, onCancel, onTrackRefund }) {
  const depTime = booking.departureTime ? new Date(booking.departureTime) : null;
  const arrTime = booking.arrivalTime ? new Date(booking.arrivalTime) : null;
  const dur = booking.durationMinutes || 0;

  return (
    <div className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm transition hover:shadow-md">
      <div className="p-5">
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-sm font-black text-primary">{booking.airlineName?.substring(0, 2).toUpperCase() || 'FL'}</div>
            <div className="min-w-0">
              <div className="truncate text-sm font-bold text-slate-900">{booking.airlineName} · {booking.flightNumber}</div>
              <div className="text-xs text-slate-500">{booking.cabinClass?.replace('_', ' ') || 'Economy'}</div>
            </div>
          </div>
          <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${STATUS_STYLES[booking.status] || STATUS_STYLES.FAILED}`}>
            {booking.status}
          </span>
        </div>

        <div className="mb-4 flex items-center justify-between gap-3">
          <div className="text-center">
            <div className="text-xl font-black text-slate-900">{booking.originCode}</div>
            {depTime && <div className="text-xs text-slate-500">{depTime.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}</div>}
          </div>

          <div className="flex-1 px-2">
            <div className="text-center text-[10px] font-semibold uppercase tracking-[0.16em] text-slate-400">{Math.floor(dur / 60)}h {dur % 60}m</div>
            <div className="mt-1 flex items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full border-2 border-primary" />
              <span className="h-px flex-1 bg-slate-300" />
              <span className="h-2.5 w-2.5 rounded-full bg-primary" />
            </div>
            <div className="mt-1 text-center text-[10px] text-slate-400">{booking.stops === 0 ? 'Non-stop' : `${booking.stops} stop${booking.stops > 1 ? 's' : ''}`}</div>
          </div>

          <div className="text-center">
            <div className="text-xl font-black text-slate-900">{booking.destinationCode}</div>
            {arrTime && <div className="text-xs text-slate-500">{arrTime.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}</div>}
          </div>
        </div>

        <div className="mb-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500">
          <span>{depTime ? depTime.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : '—'}</span>
          <span>{booking.passengerCount} traveller{booking.passengerCount !== 1 ? 's' : ''}</span>
          {booking.seatNumbers && <span>Seat {booking.seatNumbers}</span>}
        </div>

        <div className="flex items-center justify-between gap-3">
          <div>
            <div className="text-[10px] uppercase tracking-[0.16em] text-slate-400">Total</div>
            <div className="text-lg font-black text-slate-900">₹{booking.totalAmount?.toLocaleString()}</div>
          </div>

          <div className="flex flex-wrap justify-end gap-2">
            <Link to={`/dashboard/trip/${booking.bookingReference}`} className="rounded-xl bg-slate-100 px-3 py-2 text-[11px] font-bold text-slate-700 hover:bg-slate-200">View details</Link>
            {booking.status === 'CONFIRMED' && (
              <>
                <Link to="/live-tracker" className="rounded-xl bg-blue-50 px-3 py-2 text-[11px] font-bold text-primary hover:bg-blue-100">📡 Track</Link>
                <button onClick={() => onCancel(booking)} className="rounded-xl bg-red-50 px-3 py-2 text-[11px] font-bold text-red-600 hover:bg-red-100">Cancel</button>
              </>
            )}
            {booking.status === 'CANCELLED' && (
              <button onClick={() => onTrackRefund(booking)} className="rounded-xl bg-indigo-50 px-3 py-2 text-[11px] font-bold text-indigo-700 hover:bg-indigo-100">💳 Track Refund</button>
            )}
          </div>
        </div>
        <div className="mt-2 text-[10px] text-slate-400">Ref: {booking.bookingReference}</div>
      </div>
    </div>
  );
}

function HotelTripCard({ booking, onCancel, onTrackRefund }) {
  const checkIn = booking.checkInDate ? new Date(booking.checkInDate) : null;
  const checkOut = booking.checkOutDate ? new Date(booking.checkOutDate) : null;

  return (
    <div className="overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm transition hover:shadow-md">
      <div className="p-5">
        <div className="mb-3 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-xl">🏨</div>
            <div className="min-w-0">
              <div className="truncate text-sm font-bold text-slate-900">{booking.hotelName}</div>
              <div className="text-xs text-slate-500">{booking.roomName || booking.roomType || 'Room'}</div>
            </div>
          </div>
          <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${STATUS_STYLES[booking.status] || STATUS_STYLES.FAILED}`}>{booking.status}</span>
        </div>

        <div className="mb-4 flex items-center gap-4 border-t border-slate-100 pt-3 text-sm text-slate-700">
          {checkIn && (
            <div>
              <div className="text-[10px] uppercase tracking-[0.16em] text-slate-400">Check-in</div>
              <div className="font-semibold">{checkIn.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</div>
            </div>
          )}
          {checkOut && (
            <div>
              <div className="text-[10px] uppercase tracking-[0.16em] text-slate-400">Check-out</div>
              <div className="font-semibold">{checkOut.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</div>
            </div>
          )}
          <div>
            <div className="text-[10px] uppercase tracking-[0.16em] text-slate-400">Nights</div>
            <div className="font-semibold">{booking.numberOfNights || 1}</div>
          </div>
        </div>

        <div className="flex items-center justify-between gap-3">
          <div>
            <div className="text-[10px] uppercase tracking-[0.16em] text-slate-400">Total</div>
            <div className="text-lg font-black text-slate-900">₹{booking.totalAmount?.toLocaleString()}</div>
          </div>

          <div className="flex flex-wrap justify-end gap-2">
            <Link to={`/dashboard/trip/${booking.bookingReference}`} className="rounded-xl bg-slate-100 px-3 py-2 text-[11px] font-bold text-slate-700 hover:bg-slate-200">View details</Link>
            {booking.status === 'CONFIRMED' && <button onClick={() => onCancel(booking)} className="rounded-xl bg-red-50 px-3 py-2 text-[11px] font-bold text-red-600 hover:bg-red-100">Cancel</button>}
            {booking.status === 'CANCELLED' && (
              <button onClick={() => onTrackRefund(booking)} className="rounded-xl bg-indigo-50 px-3 py-2 text-[11px] font-bold text-indigo-700 hover:bg-indigo-100">💳 Track Refund</button>
            )}
          </div>
        </div>
        <div className="mt-2 text-[10px] text-slate-400">Ref: {booking.bookingReference}</div>
      </div>
    </div>
  );
}

function EmptyState({ icon, title, description, links }) {
  return (
    <div className="rounded-[28px] border border-slate-200 bg-white p-8 text-center shadow-sm sm:p-12">
      <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-3xl">{icon}</div>
      <h3 className="text-xl font-black text-slate-900">{title}</h3>
      <p className="mx-auto mt-2 max-w-md text-sm text-slate-500">{description}</p>
      {links && (
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          {links.map((link) => (
            <Link key={link.to} to={link.to} className="travel-button-primary px-5 py-2.5 text-sm">{link.label}</Link>
          ))}
        </div>
      )}
    </div>
  );
}

function TrackedFlightDashboardCard({ flight, onUntrack }) {
  const depTime = flight.estimatedDeparture || flight.scheduledDeparture;
  const arrTime = flight.estimatedArrival || flight.scheduledArrival;
  const isDelayed = flight.status === 'DELAYED';
  const isBoarding = flight.status === 'BOARDING';

  const badgeClass =
    isDelayed ? 'bg-amber-100 text-amber-800 border-amber-300' :
    isBoarding ? 'bg-blue-100 text-blue-800 border-blue-300 animate-pulse' :
    flight.status === 'IN_FLIGHT' || flight.status === 'DEPARTED' ? 'bg-indigo-100 text-indigo-800 border-indigo-300' :
    flight.status === 'LANDED' ? 'bg-emerald-100 text-emerald-800 border-emerald-300' :
    flight.status === 'CANCELLED' ? 'bg-red-100 text-red-800 border-red-300' :
    'bg-emerald-50 text-emerald-700 border-emerald-200';

  return (
    <div className="rounded-[22px] border border-slate-200 bg-white p-4 shadow-sm hover:shadow-md transition">
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-sm font-black text-primary">
            ✈️
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-black text-slate-900">{flight.airlineName}</span>
              <span className="text-xs font-mono font-bold text-primary">{flight.flightNumber}</span>
            </div>
            <div className="text-xs text-slate-500 font-semibold">
              {flight.departureAirportCode || flight.departureAirport || 'DEP'} → {flight.arrivalAirportCode || flight.arrivalAirport || 'ARR'}
            </div>
          </div>
        </div>
        <div className="flex flex-col items-end gap-1">
          <span className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold ${badgeClass}`}>
            {flight.status?.replace('_', ' ')}
          </span>
          {isDelayed && flight.delayMinutes > 0 && (
            <span className="text-[10px] font-bold text-amber-600">
              +{flight.delayMinutes}m delay
            </span>
          )}
        </div>
      </div>

      {isDelayed && flight.delayReason && (
        <div className="mb-3 rounded-lg bg-amber-50 px-2.5 py-1.5 text-xs text-amber-800 flex items-center gap-1.5 border border-amber-100">
          <span>⚠️</span>
          <span className="font-medium">{flight.delayReason}</span>
        </div>
      )}

      <div className="grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-2.5 text-xs">
        <div>
          <p className="text-[10px] uppercase font-bold text-slate-400">Departure</p>
          <p className="font-mono font-bold text-slate-800">
            {depTime ? new Date(depTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
          </p>
          {flight.estimatedDeparture && flight.estimatedDeparture !== flight.scheduledDeparture && (
            <p className="text-[9px] text-slate-400 line-through">
              Sched: {new Date(flight.scheduledDeparture).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </p>
          )}
        </div>
        <div>
          <p className="text-[10px] uppercase font-bold text-slate-400">Estimated Arrival (ETA)</p>
          <p className="font-mono font-bold text-primary">
            {arrTime ? new Date(arrTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
          </p>
          {flight.estimatedArrival && flight.estimatedArrival !== flight.scheduledArrival && (
            <p className="text-[9px] text-slate-400 line-through">
              Sched: {new Date(flight.scheduledArrival).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </p>
          )}
        </div>
      </div>

      <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
        <div className="flex items-center gap-2 text-[11px]">
          {flight.terminal && <span>T{flight.terminal}</span>}
          {flight.gate && <span>Gate {flight.gate}</span>}
          {flight.lastUpdated && (
            <span className="text-slate-400">
              • {new Date(flight.lastUpdated).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/live-tracker"
            className="text-[11px] font-bold text-primary hover:underline"
          >
            Live Radar →
          </Link>
          {onUntrack && (
            <button
              type="button"
              onClick={() => onUntrack(flight.flightId || flight.id)}
              className="text-[10px] font-semibold text-slate-400 hover:text-red-500 transition"
            >
              Untrack
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [sort, setSort] = useState('newest');
  const [search, setSearch] = useState('');
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [trackingRefundBooking, setTrackingRefundBooking] = useState(null);

  const { data: allData, isLoading } = useQuery({
    queryKey: ['bookings', 'all'],
    queryFn: () => getMyBookings(0, 100),
  });

  const { data: refundsData } = useQuery({
    queryKey: ['refunds'],
    queryFn: () => refundApi.getMyRefunds(),
  });

  const { data: trackedFlightsData } = useQuery({
    queryKey: ['dashboard-tracked-flights'],
    queryFn: () => flightStatusApi.getMyTrackedFlights(),
    refetchInterval: 30000,
  });

  const untrackFlightMutation = useMutation({
    mutationFn: (flightId) => flightStatusApi.untrackFlight(flightId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard-tracked-flights'] });
      queryClient.invalidateQueries({ queryKey: ['tracked-flights'] });
    },
  });

  const allBookings = allData?.content || [];
  const refunds = refundsData?.data || [];
  const trackedFlights = trackedFlightsData?.data || [];

  const now = new Date();
  const upcoming = allBookings.filter((booking) => {
    const travel = booking.departureTime || booking.checkInDate;
    return booking.status === 'CONFIRMED' || booking.status === 'PENDING' || (travel && new Date(travel) >= now);
  });
  const completed = allBookings.filter((booking) => booking.status === 'COMPLETED');
  const cancelled = allBookings.filter((booking) => booking.status === 'CANCELLED' || booking.status === 'FAILED');
  const nextTrip = upcoming[0] || null;

  // ═══ Travel Guardian Alerts ═══
  const { data: guardianData } = useQuery({
    queryKey: ['guardian-alerts'],
    queryFn: () => voyaraApi.getGuardianAlerts(),
    refetchInterval: 60000,
  });
  const guardianAlerts = guardianData?.data?.data || [];
  const activeAlerts = guardianAlerts.filter(a => !a.dismissed);
  const criticalAlerts = activeAlerts.filter(a => a.severity === 'CRITICAL' || a.severity === 'HIGH');

  // ═══ Next Trip Readiness ═══
  const { data: readinessData } = useQuery({
    queryKey: ['readiness', nextTrip?.id],
    queryFn: () => voyaraApi.getReadiness(nextTrip.id),
    enabled: !!nextTrip?.id,
  });
  const readiness = readinessData?.data?.data || null;

  const filteredBookings = useMemo(() => {
    let list = [...allBookings];

    if (filter === 'upcoming') list = upcoming;
    else if (filter === 'completed') list = completed;
    else if (filter === 'cancelled') list = cancelled;

    if (typeFilter !== 'all') list = list.filter((booking) => booking.bookingType === typeFilter.toUpperCase());

    if (search.trim()) {
      const query = search.toLowerCase();
      list = list.filter((booking) =>
        booking.bookingReference?.toLowerCase().includes(query) ||
        booking.airlineName?.toLowerCase().includes(query) ||
        booking.flightNumber?.toLowerCase().includes(query) ||
        booking.hotelName?.toLowerCase().includes(query) ||
        booking.originCode?.toLowerCase().includes(query) ||
        booking.destinationCode?.toLowerCase().includes(query)
      );
    }

    list.sort((a, b) => {
      const dateA = new Date(a.createdAt || 0);
      const dateB = new Date(b.createdAt || 0);
      const travelA = new Date(a.departureTime || a.checkInDate || 0);
      const travelB = new Date(b.departureTime || b.checkInDate || 0);

      switch (sort) {
        case 'oldest': return dateA - dateB;
        case 'travel': return travelA - travelB;
        case 'price-high': return (b.totalAmount || 0) - (a.totalAmount || 0);
        case 'price-low': return (a.totalAmount || 0) - (b.totalAmount || 0);
        default: return dateB - dateA;
      }
    });

    return list;
  }, [allBookings, filter, typeFilter, search, sort, upcoming, completed, cancelled]);

  const handleCancelComplete = () => {
    setCancellingBooking(null);
    queryClient.invalidateQueries({ queryKey: ['bookings'] });
  };

  return (
    <div className="section-shell py-6 sm:py-8">
      <div className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">Dashboard</p>
          <h1 className="mt-2 text-3xl font-black text-slate-900">My trips</h1>
        </div>
        <div className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-600 shadow-sm">
          Welcome back, {user?.name || 'traveller'}
        </div>
      </div>

      <div className="mb-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {[
          { key: 'all', label: 'Total', count: allBookings.length, icon: '🧳', className: 'bg-slate-100 text-slate-700' },
          { key: 'upcoming', label: 'Upcoming', count: upcoming.length, icon: '✈️', className: 'bg-blue-50 text-primary' },
          { key: 'completed', label: 'Completed', count: completed.length, icon: '✅', className: 'bg-emerald-50 text-emerald-700' },
          { key: 'cancelled', label: 'Cancelled', count: cancelled.length, icon: '❌', className: 'bg-red-50 text-red-600' },
        ].map((item) => (
          <button key={item.key} type="button" onClick={() => setFilter(item.key)} className={`rounded-[22px] border p-4 text-left shadow-sm transition hover:-translate-y-0.5 ${filter === item.key ? 'border-primary/40 bg-white shadow-md' : 'border-slate-200 bg-white'} `}>
            <div className="mb-2 flex items-center justify-between">
              <span className={`flex h-10 w-10 items-center justify-center rounded-xl ${item.className}`}>{item.icon}</span>
              <span className="text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400">{item.label}</span>
            </div>
            <div className="text-3xl font-black text-slate-900">{item.count}</div>
          </button>
        ))}
      </div>

      {(filter === 'all' || filter === 'upcoming') && <NextTripCard booking={nextTrip} />}

      {/* Live Tracked Flights Section */}
      {trackedFlights.length > 0 ? (
        <div className="mb-6 rounded-[28px] border border-blue-100 bg-white p-6 shadow-sm">
          <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
            <div className="flex items-center gap-2.5">
              <span className="relative flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
              </span>
              <h2 className="text-xl font-black text-slate-900">Live Tracked Flights</h2>
              <span className="rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-bold text-primary">
                {trackedFlights.length} Active
              </span>
            </div>
            <Link to="/live-tracker" className="text-xs font-bold text-primary hover:underline flex items-center gap-1">
              Open Full Flight Radar →
            </Link>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {trackedFlights.map((flight) => (
              <TrackedFlightDashboardCard
                key={flight.id || flight.flightId}
                flight={flight}
                onUntrack={(fId) => untrackFlightMutation.mutate(fId)}
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="mb-6 flex flex-col sm:flex-row items-center justify-between gap-4 rounded-[24px] border border-blue-100 bg-gradient-to-r from-blue-50/80 to-indigo-50/50 p-5 shadow-sm">
          <div className="flex items-center gap-3.5">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white text-2xl shadow-sm border border-blue-100">
              📡
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Live Flight Status & Radar</h3>
              <p className="text-xs text-slate-500">Track multiple flights simultaneously with dynamic ETAs, delay alerts & gate changes.</p>
            </div>
          </div>
          <Link
            to="/live-tracker"
            className="travel-button-primary px-4 py-2.5 text-xs whitespace-nowrap shrink-0"
          >
            Track a Flight →
          </Link>
        </div>
      )}

      <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center">
        <div className="relative flex-1">
          <svg className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input value={search} onChange={(e) => setSearch(e.target.value)} type="text" placeholder="Search by reference, airline, route..." className="h-[48px] w-full rounded-xl border border-slate-200 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
        </div>

        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="h-[48px] rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-700 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10">
          <option value="all">All types</option>
          <option value="flights">Flights</option>
          <option value="hotels">Hotels</option>
        </select>

        <select value={sort} onChange={(e) => setSort(e.target.value)} className="h-[48px] rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-700 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10">
          <option value="newest">Newest first</option>
          <option value="oldest">Oldest first</option>
          <option value="travel">Travel date</option>
          <option value="price-high">Price: high to low</option>
          <option value="price-low">Price: low to high</option>
        </select>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((item) => <SkeletonCard key={item} />)}
        </div>
      ) : filteredBookings.length === 0 ? (
        <EmptyState
          icon={filter === 'completed' ? '✅' : filter === 'cancelled' ? '🚫' : '✈️'}
          title={filter === 'completed' ? 'No completed trips yet' : filter === 'cancelled' ? 'No cancelled trips' : search ? 'No trips match your search' : 'No trips yet'}
          description={filter === 'completed' ? 'Your completed trips will appear here after they are marked done.' : filter === 'cancelled' ? 'You have not cancelled any bookings yet.' : search ? `No matches for “${search}”. Try a different search.` : 'Start planning your next trip by exploring flights, stays, and holiday options.'}
          links={!search && filter === 'all' ? [{ to: '/', label: 'Search flights' }, { to: '/hotels', label: 'Explore hotels' }, { to: '/holidays', label: 'Browse holidays' }] : undefined}
        />
      ) : (
        <div className="space-y-4">
          <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{filteredBookings.length} trip{filteredBookings.length !== 1 ? 's' : ''} found</div>
          {filteredBookings.map((booking) => (
            booking.bookingType === 'FLIGHT' ? <FlightTripCard key={booking.id} booking={booking} onCancel={setCancellingBooking} onTrackRefund={setTrackingRefundBooking} /> : <HotelTripCard key={booking.id} booking={booking} onCancel={setCancellingBooking} onTrackRefund={setTrackingRefundBooking} />
          ))}
        </div>
      )}

      {/* ═══ Travel Intelligence Section ═══ */}
      {(criticalAlerts.length > 0 || readiness) && (
        <div className="mb-6 grid gap-4 sm:grid-cols-2">
          {/* Guardian Alert Summary */}
          {criticalAlerts.length > 0 && (
            <div className="rounded-[24px] border border-red-200/80 bg-gradient-to-br from-red-50 to-orange-50 p-5 shadow-sm">
              <div className="mb-3 flex items-center gap-2.5">
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-red-100 text-red-600">
                  <ShieldCheckIcon className="w-5 h-5" />
                </span>
                <div>
                  <h3 className="text-sm font-bold text-red-900">Travel Guardian Alerts</h3>
                  <p className="text-[10px] text-red-600 font-semibold">{criticalAlerts.length} alert{criticalAlerts.length !== 1 ? 's' : ''} require attention</p>
                </div>
              </div>
              <div className="space-y-2 max-h-32 overflow-y-auto">
                {criticalAlerts.slice(0, 3).map((alert) => (
                  <div key={alert.id} className={`rounded-xl px-3 py-2 text-xs border ${
                    alert.severity === 'CRITICAL' ? 'bg-red-50 border-red-200 text-red-800' : 'bg-orange-50 border-orange-200 text-orange-800'
                  }`}>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[10px] font-black px-1.5 py-0.5 rounded bg-white/60">{alert.severity}</span>
                      <span className="font-semibold truncate">{alert.title}</span>
                    </div>
                  </div>
                ))}
              </div>
              {criticalAlerts.length > 3 && (
                <p className="mt-2 text-[10px] text-red-500 font-semibold">+{criticalAlerts.length - 3} more alerts</p>
              )}
            </div>
          )}

          {/* Next Trip Readiness */}
          {readiness && nextTrip && (
            <div className="rounded-[24px] border border-emerald-200/80 bg-gradient-to-br from-emerald-50 to-teal-50 p-5 shadow-sm">
              <div className="mb-3 flex items-center gap-2.5">
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
                  <CheckIcon className="w-5 h-5" />
                </span>
                <div>
                  <h3 className="text-sm font-bold text-emerald-900">Pre-Flight Readiness</h3>
                  <p className="text-[10px] text-emerald-600 font-semibold">Next trip: {nextTrip.bookingReference}</p>
                </div>
              </div>
              <div className="mb-2 flex items-center gap-3">
                <div className="flex-1 h-3 bg-emerald-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-700 ${
                      readiness.score >= 80 ? 'bg-emerald-500' : readiness.score >= 50 ? 'bg-amber-500' : 'bg-red-500'
                    }`}
                    style={{ width: `${readiness.score}%` }}
                  />
                </div>
                <span className={`text-lg font-black ${
                  readiness.score >= 80 ? 'text-emerald-700' : readiness.score >= 50 ? 'text-amber-700' : 'text-red-700'
                }`}>{readiness.score}%</span>
              </div>
              <Link
                to={`/dashboard/trip/${nextTrip.bookingReference}`}
                className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 hover:text-emerald-800 transition"
              >
                View full readiness checklist →
              </Link>
            </div>
          )}
        </div>
      )}

      {/* Personalized Recommendation Engine Section */}
      <div className="mt-12 pt-8 border-t border-slate-200">
        <RecommendationsSection
          title="Recommended For Your Next Trip"
          subtitle="Tailored stays, flights, and holiday escapes matched with your travel history and preferences"
        />
      </div>

      {cancellingBooking && <CancellationFlow booking={cancellingBooking} onCancel={handleCancelComplete} onClose={() => setCancellingBooking(null)} />}
      {trackingRefundBooking && (
        <RefundStatusTracker
          refundId={trackingRefundBooking.refundId || `REF-${trackingRefundBooking.bookingReference?.replace('TP-', '')}`}
          initialData={{
            refundId: trackingRefundBooking.refundId,
            refundAmount: trackingRefundBooking.refundAmount,
            originalAmount: trackingRefundBooking.totalAmount,
            status: 'PENDING',
            cancellationReason: trackingRefundBooking.cancellationReason,
          }}
          onClose={() => setTrackingRefundBooking(null)}
        />
      )}
    </div>
  );
}
