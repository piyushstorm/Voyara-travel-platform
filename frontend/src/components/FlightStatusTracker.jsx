import { useState, useEffect } from 'react';
import { flightStatusApi } from '../api/phase3Api';

const STATUS_COLORS = {
  ON_TIME: 'bg-emerald-100 text-emerald-700 border-emerald-200',
  SCHEDULED: 'bg-blue-100 text-blue-700 border-blue-200',
  CHECK_IN_OPEN: 'bg-cyan-100 text-cyan-700 border-cyan-200',
  DELAYED: 'bg-amber-100 text-amber-700 border-amber-200',
  BOARDING: 'bg-purple-100 text-purple-700 border-purple-200 animate-pulse',
  DEPARTED: 'bg-sky-100 text-sky-700 border-sky-200',
  IN_FLIGHT: 'bg-indigo-100 text-indigo-700 border-indigo-200',
  APPROACHING: 'bg-teal-100 text-teal-700 border-teal-200',
  LANDED: 'bg-green-100 text-green-700 border-green-200',
  ARRIVED: 'bg-green-100 text-green-700 border-green-200',
  CANCELLED: 'bg-red-100 text-red-700 border-red-200',
};

const STATUS_ICONS = {
  ON_TIME: '✓',
  SCHEDULED: '📅',
  CHECK_IN_OPEN: '🎫',
  DELAYED: '⏱',
  BOARDING: '✈',
  DEPARTED: '🛫',
  IN_FLIGHT: '✈️',
  APPROACHING: '🛬',
  LANDED: '🏁',
  ARRIVED: '🏁',
  CANCELLED: '✕',
};

export default function FlightStatusTracker({ flightId, flightNumber, origin, destination, subscribe }) {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Fetch initial status
    flightStatusApi.getStatus(flightId)
      .then((res) => setStatus(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));

    // Subscribe to live updates
    if (subscribe && flightId) {
      const unsub = subscribe(`/topic/flight-status/${flightId}`, (data) => {
        setStatus((prev) => ({ ...prev, ...data }));
      });
      return unsub;
    }
  }, [flightId, subscribe]);

  if (loading) {
    return (
      <div className="bg-white rounded-xl border p-4 animate-pulse">
        <div className="h-4 bg-gray-200 rounded w-1/3 mb-2"></div>
        <div className="h-3 bg-gray-100 rounded w-1/2"></div>
      </div>
    );
  }

  if (!status) {
    return (
      <div className="bg-white rounded-xl border p-4 text-gray-400 text-sm">
        No status information available for {flightNumber || 'this flight'}
      </div>
    );
  }

  const isDelayed = status.status === 'DELAYED';
  const timeChanged = status.estimatedDeparture !== status.scheduledDeparture;

  return (
    <div className={`bg-white rounded-xl border p-4 transition-all ${isDelayed ? 'border-amber-200' : ''}`}>
      <div className="flex items-center justify-between mb-3">
        <div>
          <h4 className="font-semibold text-gray-800">{status.flightNumber || flightNumber}</h4>
          <p className="text-sm text-gray-500">{origin || status.originCode} → {destination || status.destinationCode}</p>
        </div>
        <span className={`px-3 py-1 rounded-full text-xs font-medium border ${STATUS_COLORS[status.status] || STATUS_COLORS.ON_TIME}`}>
          {STATUS_ICONS[status.status] || '●'} {status.status?.replace('_', ' ')}
        </span>
      </div>

      {/* Timeline */}
      <div className="flex items-center justify-between text-sm mb-2">
        <div className="text-center">
          <p className="text-gray-400 text-xs">Scheduled</p>
          <p className="font-mono">
            {status.scheduledDeparture ? new Date(status.scheduledDeparture).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
          </p>
        </div>
        <div className="flex-1 mx-4 border-t-2 border-dashed border-gray-200 relative">
          {isDelayed && <div className="absolute inset-0 flex items-center justify-center">
            <span className="bg-white px-2 text-xs text-amber-600">delayed</span>
          </div>}
        </div>
        <div className="text-center">
          <p className="text-gray-400 text-xs">Scheduled</p>
          <p className="font-mono">
            {status.scheduledArrival ? new Date(status.scheduledArrival).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
          </p>
        </div>
      </div>

      {timeChanged && (
        <div className="flex items-center justify-between text-sm mt-2 p-2 bg-blue-50 rounded-lg">
          <div className="text-center">
            <p className="text-blue-500 text-xs">Estimated</p>
            <p className="font-mono text-blue-700 font-semibold">
              {status.estimatedDeparture ? new Date(status.estimatedDeparture).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
            </p>
          </div>
          <div className="flex-1 mx-4 border-t-2 border-blue-300"></div>
          <div className="text-center">
            <p className="text-blue-500 text-xs">Estimated</p>
            <p className="font-mono text-blue-700 font-semibold">
              {status.estimatedArrival ? new Date(status.estimatedArrival).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}
            </p>
          </div>
        </div>
      )}

      {status.delayReason && (
        <div className="mt-3 p-2 bg-amber-50 rounded-lg text-sm text-amber-700">
          ⚠ {status.delayReason}
        </div>
      )}

      {(status.gate || status.terminal) && (
        <div className="mt-2 flex gap-4 text-xs text-gray-500">
          {status.terminal && <span>Terminal: <strong>{status.terminal}</strong></span>}
          {status.gate && <span>Gate: <strong>{status.gate}</strong></span>}
        </div>
      )}
    </div>
  );
}
