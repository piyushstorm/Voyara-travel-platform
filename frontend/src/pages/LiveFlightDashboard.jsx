import { useState, useEffect, useMemo, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../context/AuthContext'
import { flightStatusApi } from '../api/phase3Api'
import { getUpcomingBookings } from '../api/bookingApi'
import { useWebSocket, useFlightTracking, requestNotificationPermission } from '../hooks/useWebSocket'

const STATUS_CONFIG = {
  ON_TIME: { label: 'ON TIME', color: 'bg-emerald-100 text-emerald-800 border-emerald-300', dot: 'bg-emerald-500' },
  SCHEDULED: { label: 'SCHEDULED', color: 'bg-blue-100 text-blue-800 border-blue-300', dot: 'bg-blue-500' },
  CHECK_IN_OPEN: { label: 'CHECK-IN OPEN', color: 'bg-cyan-100 text-cyan-800 border-cyan-300', dot: 'bg-cyan-500' },
  BOARDING: { label: 'BOARDING', color: 'bg-purple-100 text-purple-800 border-purple-300 animate-pulse', dot: 'bg-purple-500' },
  DEPARTED: { label: 'DEPARTED', color: 'bg-sky-100 text-sky-800 border-sky-300', dot: 'bg-sky-500' },
  IN_FLIGHT: { label: 'IN FLIGHT', color: 'bg-indigo-100 text-indigo-800 border-indigo-300', dot: 'bg-indigo-500' },
  APPROACHING: { label: 'APPROACHING', color: 'bg-teal-100 text-teal-800 border-teal-300', dot: 'bg-teal-500' },
  LANDED: { label: 'LANDED', color: 'bg-green-100 text-green-800 border-green-300', dot: 'bg-green-500' },
  ARRIVED: { label: 'ARRIVED', color: 'bg-green-100 text-green-800 border-green-300', dot: 'bg-green-500' },
  DELAYED: { label: 'DELAYED', color: 'bg-amber-100 text-amber-800 border-amber-300', dot: 'bg-amber-500' },
  CANCELLED: { label: 'CANCELLED', color: 'bg-red-100 text-red-800 border-red-300', dot: 'bg-red-500' },
}

function timeAgo(dateStr) {
  if (!dateStr) return 'Just now'
  const diffMs = new Date() - new Date(dateStr)
  const diffSec = Math.floor(diffMs / 1000)
  if (diffSec < 10) return 'Just now'
  if (diffSec < 60) return `${diffSec}s ago`
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}m ago`
  const diffHr = Math.floor(diffMin / 60)
  return `${diffHr}h ago`
}

function formatTime(dateStr) {
  if (!dateStr) return '--:--'
  try {
    return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
  } catch {
    return '--:--'
  }
}

export default function LiveFlightDashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { subscribe, connected } = useWebSocket()
  const { flightStatuses: liveWsUpdates, notifications, clearNotifications } = useFlightTracking(subscribe)

  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [selectedSimFlight, setSelectedSimFlight] = useState(null)
  const [lastRefreshedAt, setLastRefreshedAt] = useState(Date.now())

  useEffect(() => {
    requestNotificationPermission()
  }, [])

  // Debounce search query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 300)
    return () => clearTimeout(handler)
  }, [searchQuery])

  // Fetch tracked flights from backend (polling every 30s as fallback)
  const { data: serverTracked = [], isLoading: loadingTracked, refetch: refetchTracked } = useQuery({
    queryKey: ['tracked-flights'],
    queryFn: async () => {
      const res = await flightStatusApi.getMyTrackedFlights()
      setLastRefreshedAt(Date.now())
      return res.data || []
    },
    refetchInterval: 30000,
    enabled: !!user,
  })

  // Search flights query
  const { data: searchResults = [], isLoading: searchingFlights } = useQuery({
    queryKey: ['flight-search-status', debouncedQuery],
    queryFn: async () => {
      if (!debouncedQuery || debouncedQuery.trim().length < 2) return []
      const res = await flightStatusApi.searchFlights(debouncedQuery.trim())
      return res.data || []
    },
    enabled: !!user && debouncedQuery.trim().length >= 2,
  })

  // Load upcoming bookings to allow quick track
  const { data: bookingsData } = useQuery({
    queryKey: ['bookings', 'upcoming'],
    queryFn: () => getUpcomingBookings(0, 20),
    enabled: !!user,
  })

  const flightBookings = (bookingsData?.content || []).filter(b => b.bookingType === 'FLIGHT')

  // Merge server tracked flights with any live WebSocket updates
  const trackedFlights = useMemo(() => {
    return serverTracked.map(item => {
      const fid = item.flightId || item.id
      const wsUpdate = liveWsUpdates[fid]
      if (wsUpdate) {
        return { ...item, ...wsUpdate }
      }
      return item
    })
  }, [serverTracked, liveWsUpdates])

  const trackedFlightIds = useMemo(() => {
    return new Set(trackedFlights.map(s => s.flightId || s.id))
  }, [trackedFlights])

  const [trackingIdPending, setTrackingIdPending] = useState(null)
  const [untrackingIdPending, setUntrackingIdPending] = useState(null)
  const [actionError, setActionError] = useState(null)

  const trackMutation = useMutation({
    mutationFn: (flightId) => {
      setTrackingIdPending(flightId)
      setActionError(null)
      return flightStatusApi.trackFlight(flightId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tracked-flights'] })
    },
    onError: (err) => {
      const msg = err?.response?.data?.message || 'Failed to track flight. Please try again.'
      setActionError(msg)
    },
    onSettled: () => {
      setTrackingIdPending(null)
    }
  })

  const untrackMutation = useMutation({
    mutationFn: (flightId) => {
      setUntrackingIdPending(flightId)
      setActionError(null)
      return flightStatusApi.untrackFlight(flightId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tracked-flights'] })
    },
    onError: (err) => {
      const msg = err?.response?.data?.message || 'Failed to untrack flight. Please try again.'
      setActionError(msg)
    },
    onSettled: () => {
      setUntrackingIdPending(null)
    }
  })

  const simulateMutation = useMutation({
    mutationFn: ({ flightId, scenario }) => flightStatusApi.simulateStatus(flightId, scenario),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tracked-flights'] })
    },
  })

  const handleManualRefresh = () => {
    refetchTracked()
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-3">
            <span className="text-3xl">📡</span>
            <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">Live Flight Tracker</h1>
          </div>
          <p className="text-gray-500 text-sm mt-1">Real-time status updates, revised schedules, dynamic ETAs & notifications</p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-white border border-gray-200 px-3 py-1.5 rounded-full shadow-sm text-xs">
            <span className={`w-2.5 h-2.5 rounded-full ${connected ? 'bg-emerald-500 animate-pulse' : 'bg-amber-400'}`} />
            <span className="font-semibold text-gray-700">{connected ? 'Live STOMP' : 'Polling (30s)'}</span>
          </div>

          <button
            onClick={handleManualRefresh}
            className="flex items-center gap-1.5 bg-white hover:bg-gray-50 border border-gray-200 text-gray-700 px-3 py-1.5 rounded-full text-xs font-semibold shadow-sm transition"
            title="Refresh now"
          >
            <span>🔄</span> Refresh
          </button>
        </div>
      </div>

      {/* Flight Search & Quick Track Bar */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-200/80 mb-8">
        <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
          Track Any Flight (Multi-Flight Search)
        </label>
        <div className="relative">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by flight number (e.g. AI 245, 6E 102) or airport (DEL, BOM)..."
            className="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 pl-11 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary focus:bg-white transition"
          />
          <span className="absolute left-3.5 top-3.5 text-gray-400 text-base">🔍</span>
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3.5 top-3 text-gray-400 hover:text-gray-600 text-sm"
            >
              ✕
            </button>
          )}
        </div>

        {/* Search Results Dropdown */}
        {debouncedQuery.trim().length >= 2 && (
          <div className="mt-3 border-t border-gray-100 pt-3">
            {searchingFlights ? (
              <div className="text-center py-4 text-xs text-gray-500">Searching flights...</div>
            ) : searchResults.length === 0 ? (
              <div className="text-center py-4 text-xs text-gray-500">No flights found matching "{debouncedQuery}"</div>
            ) : (
              <div className="divide-y divide-gray-100 max-h-64 overflow-y-auto">
                {searchResults.map((flight) => {
                  const fid = flight.flightId || flight.id
                  const isTracked = trackedFlightIds.has(fid)
                  const cfg = STATUS_CONFIG[flight.status] || STATUS_CONFIG.ON_TIME
                  return (
                    <div key={fid} className="py-2.5 flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 font-bold text-xs flex items-center justify-center flex-shrink-0 border border-blue-100">
                          {flight.airlineCode || '✈️'}
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-gray-900 text-sm">{flight.flightNumber}</span>
                            <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${cfg.color}`}>
                              {flight.status?.replace('_', ' ')}
                            </span>
                          </div>
                          <p className="text-xs text-gray-500 truncate">
                            {flight.originCode} ({flight.originCity || 'Origin'}) → {flight.destinationCode} ({flight.destinationCity || 'Dest'}) · Dep: {formatTime(flight.estimatedDeparture || flight.scheduledDeparture)}
                          </p>
                        </div>
                      </div>

                      {isTracked ? (
                        <button
                          onClick={() => untrackMutation.mutate(fid)}
                          disabled={untrackingIdPending === fid}
                          className="text-xs bg-red-50 text-red-600 hover:bg-red-100 border border-red-200 px-3 py-1.5 rounded-lg font-medium transition whitespace-nowrap disabled:opacity-50"
                        >
                          {untrackingIdPending === fid ? 'Untracking...' : 'Untrack'}
                        </button>
                      ) : (
                        <button
                          onClick={() => trackMutation.mutate(fid)}
                          disabled={trackingIdPending === fid}
                          className="text-xs bg-primary hover:bg-primary-dark text-white px-3.5 py-1.5 rounded-lg font-semibold transition whitespace-nowrap shadow-sm shadow-primary/20 disabled:opacity-50"
                        >
                          {trackingIdPending === fid ? 'Tracking...' : '+ Track Flight'}
                        </button>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Action Error Banner */}
      {actionError && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl flex items-center justify-between text-sm text-red-700">
          <div className="flex items-center gap-2">
            <span>⚠️</span>
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="text-red-500 hover:text-red-700 font-bold ml-4">✕</button>
        </div>
      )}

      {/* Live Notifications Feed */}
      {notifications.length > 0 && (
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-200/80 mb-8">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-bold text-gray-900 text-sm flex items-center gap-2">
              <span>🔔</span> Real-Time Flight Notifications
              <span className="bg-primary/10 text-primary text-xs px-2 py-0.5 rounded-full font-semibold">{notifications.length}</span>
            </h2>
            <button onClick={clearNotifications} className="text-xs text-gray-500 hover:text-gray-700 font-medium">Clear Feed</button>
          </div>
          <div className="space-y-2 max-h-48 overflow-y-auto">
            {notifications.slice(0, 15).map((n) => (
              <div key={n.id} className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl text-sm border border-gray-100">
                <span className={`w-2.5 h-2.5 rounded-full mt-1.5 flex-shrink-0 ${
                  n.status === 'DELAYED' ? 'bg-amber-400' :
                  n.status === 'BOARDING' ? 'bg-purple-500 animate-ping' :
                  n.status === 'DEPARTED' ? 'bg-sky-400' : 'bg-emerald-400'
                }`} />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-gray-900">{n.flightNumber}</span>
                    <span className="text-xs text-gray-400">{timeAgo(n.timestamp)}</span>
                  </div>
                  <p className="text-gray-600 text-xs mt-0.5">{n.message}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tracked Flights Grid */}
      <div className="mb-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
            <span>✈️</span> Your Tracked Flights
            <span className="bg-gray-100 text-gray-600 text-xs px-2 py-0.5 rounded-full font-semibold">{trackedFlights.length}</span>
          </h2>
          <span className="text-xs text-gray-400">
            Last polled {timeAgo(lastRefreshedAt)}
          </span>
        </div>

        {loadingTracked ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[1, 2].map(i => <div key={i} className="skeleton h-48 w-full rounded-2xl" />)}
          </div>
        ) : trackedFlights.length === 0 ? (
          <div className="bg-white rounded-2xl p-10 text-center shadow-sm border border-gray-200/80">
            <div className="text-5xl mb-3">🛫</div>
            <h3 className="text-lg font-bold text-gray-900 mb-1">No flights being tracked</h3>
            <p className="text-gray-500 text-sm max-w-md mx-auto mb-4">
              Track your flights to receive live notifications for departure delays, gate updates, and dynamic arrival times.
            </p>
            <p className="text-xs text-primary font-semibold">Search for a flight above or track from your upcoming trips below.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {trackedFlights.map((status) => {
              const fid = status.flightId || status.id
              const cfg = STATUS_CONFIG[status.status] || STATUS_CONFIG.ON_TIME
              const isDelayed = status.status === 'DELAYED' || status.delayMinutes > 0
              const origin = status.originCode || status.departureAirportCode || 'ORIGIN'
              const dest = status.destinationCode || status.arrivalAirportCode || 'DEST'

              return (
                <div key={fid} className="bg-white rounded-2xl border border-gray-200/90 overflow-hidden shadow-sm hover:shadow-md transition">
                  {/* Card Header */}
                  <div className="p-5 border-b border-gray-100">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-black text-gray-900 text-lg tracking-tight">{status.flightNumber}</span>
                          <span className="text-xs text-gray-500">{status.airlineName || status.airlineCode}</span>
                        </div>
                        <div className="flex items-center gap-2 mt-1 text-sm font-semibold text-gray-700">
                          <span>{origin}</span>
                          <span className="text-gray-400 font-normal">({status.originCity || 'Origin'})</span>
                          <span className="text-gray-400">→</span>
                          <span>{dest}</span>
                          <span className="text-gray-400 font-normal">({status.destinationCity || 'Dest'})</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className={`text-xs font-bold px-3 py-1 rounded-full border ${cfg.color}`}>
                          {status.status?.replace('_', ' ')}
                        </span>
                        <button
                          onClick={() => untrackMutation.mutate(fid)}
                          disabled={untrackingIdPending === fid}
                          className="text-xs bg-gray-100 hover:bg-red-50 text-gray-500 hover:text-red-600 border border-gray-200 hover:border-red-200 px-2.5 py-1 rounded-lg font-medium transition disabled:opacity-50"
                          title="Stop tracking this flight"
                        >
                          {untrackingIdPending === fid ? 'Stopping...' : 'Stop Tracking'}
                        </button>
                      </div>
                    </div>

                    {/* Delay Context Banner */}
                    {isDelayed && (
                      <div className="mt-3 p-3 bg-amber-50/80 border border-amber-200 rounded-xl text-xs text-amber-900">
                        <div className="flex items-center gap-1.5 font-bold">
                          <span>⚠️</span>
                          <span>Delayed by {status.delayMinutes || 60} minutes</span>
                        </div>
                        {status.delayReason && (
                          <p className="mt-1 text-amber-700">Reason: {status.delayReason}</p>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Schedule & ETA Grid */}
                  <div className="p-5 bg-gray-50/50">
                    <div className="grid grid-cols-2 gap-4 text-xs">
                      {/* Departure */}
                      <div className="bg-white p-3 rounded-xl border border-gray-100">
                        <span className="text-gray-400 font-semibold uppercase tracking-wider block mb-1">Departure</span>
                        <div className="flex items-baseline gap-2">
                          <span className="text-base font-bold text-gray-900">
                            {formatTime(status.estimatedDeparture || status.scheduledDeparture)}
                          </span>
                          {status.estimatedDeparture && status.scheduledDeparture && status.estimatedDeparture !== status.scheduledDeparture && (
                            <span className="text-xs text-gray-400 line-through">
                              {formatTime(status.scheduledDeparture)}
                            </span>
                          )}
                        </div>
                        {status.actualDeparture && (
                          <span className="text-[11px] text-sky-600 font-medium block mt-1">
                            Departed: {formatTime(status.actualDeparture)}
                          </span>
                        )}
                      </div>

                      {/* Arrival / Dynamic ETA */}
                      <div className="bg-white p-3 rounded-xl border border-gray-100">
                        <span className="text-gray-400 font-semibold uppercase tracking-wider block mb-1">
                          {status.status === 'LANDED' ? 'Landed Time' : 'Estimated Arrival'}
                        </span>
                        <div className="flex items-baseline gap-2">
                          <span className={`text-base font-bold ${isDelayed ? 'text-amber-700 font-black' : 'text-gray-900'}`}>
                            {formatTime(status.estimatedArrival || status.scheduledArrival)}
                          </span>
                          {status.estimatedArrival && status.scheduledArrival && status.estimatedArrival !== status.scheduledArrival && (
                            <span className="text-xs text-gray-400 line-through">
                              {formatTime(status.scheduledArrival)}
                            </span>
                          )}
                        </div>
                        {status.actualArrival ? (
                          <span className="text-[11px] text-emerald-600 font-medium block mt-1">
                            Arrived: {formatTime(status.actualArrival)}
                          </span>
                        ) : (
                          <span className="text-[11px] text-gray-400 block mt-1">
                            Sched: {formatTime(status.scheduledArrival)}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Gate & Terminal Meta & Details Link */}
                    <div className="mt-3 flex items-center justify-between text-xs text-gray-500 pt-2 border-t border-gray-100">
                      <div className="flex items-center gap-3">
                        {status.gate && <span>Gate: <strong className="text-gray-800 font-semibold">{status.gate}</strong></span>}
                        {status.terminal && <span>Terminal: <strong className="text-gray-800 font-semibold">{status.terminal}</strong></span>}
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-[11px] text-gray-400">
                          Updated {timeAgo(status.updatedAt || status.lastUpdated)}
                        </span>
                        <Link
                          to={`/flights/${fid}`}
                          className="text-xs text-primary hover:text-primary-dark font-semibold hover:underline"
                        >
                          View Details →
                        </Link>
                      </div>
                    </div>

                    {/* Quick Simulation Trigger for testing */}
                    <div className="mt-3 pt-2 border-t border-gray-100 flex flex-wrap items-center gap-1.5">
                      <span className="text-[10px] text-gray-400 font-semibold mr-1">Simulate:</span>
                      {['ON_TIME', 'DELAYED', 'BOARDING', 'DEPARTED', 'IN_FLIGHT', 'LANDED'].map((sc) => (
                        <button
                          key={sc}
                          onClick={() => simulateMutation.mutate({ flightId: fid, scenario: sc })}
                          disabled={simulateMutation.isPending}
                          className="text-[10px] bg-white hover:bg-gray-100 text-gray-600 border border-gray-200 px-2 py-0.5 rounded transition disabled:opacity-50"
                        >
                          {sc.replace('_', ' ')}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Your Upcoming Flight Bookings — Quick Track */}
      {flightBookings.length > 0 && (
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-200/80 mb-8">
          <h2 className="font-bold text-gray-900 mb-4 text-base flex items-center gap-2">
            <span>🎫</span> Upcoming Trips with Flights
          </h2>
          <div className="space-y-3">
            {flightBookings.map((booking) => {
              const fid = booking.flightId
              const isTracked = trackedFlightIds.has(fid)
              return (
                <div key={booking.id} className="flex items-center justify-between p-3.5 bg-gray-50 rounded-xl border border-gray-100">
                  <div className="text-sm">
                    <span className="font-bold text-gray-900">{booking.flightNumber}</span>
                    <span className="text-gray-400 mx-2">·</span>
                    <span className="text-gray-700 font-medium">{booking.originCode} → {booking.destinationCode}</span>
                    <p className="text-xs text-gray-400 mt-0.5">Booking Ref: {booking.bookingReference}</p>
                  </div>
                  {isTracked ? (
                    <button
                      onClick={() => untrackMutation.mutate(fid)}
                      disabled={untrackingIdPending === fid}
                      className="text-xs bg-red-50 text-red-600 hover:bg-red-100 border border-red-200 px-3.5 py-1.5 rounded-lg font-medium transition disabled:opacity-50"
                    >
                      {untrackingIdPending === fid ? 'Untracking...' : 'Untrack'}
                    </button>
                  ) : (
                    <button
                      onClick={() => trackMutation.mutate(fid)}
                      disabled={trackingIdPending === fid}
                      className="text-xs bg-primary text-white hover:bg-primary-dark px-3.5 py-1.5 rounded-lg font-semibold transition disabled:opacity-50 shadow-sm shadow-primary/20"
                    >
                      {trackingIdPending === fid ? 'Tracking...' : '+ Track Flight'}
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
