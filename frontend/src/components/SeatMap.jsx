import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { getSeatMap, holdSeat, releaseSeatHold } from '../api/selectionApi';
import { useWebSocket } from '../hooks/useWebSocket';
import TravelPreferencesModal from './TravelPreferencesModal';

const STATUS_COLORS = {
  AVAILABLE: '#10b981',   // Emerald
  HELD: '#f59e0b',        // Amber
  BOOKED: '#94a3b8',      // Slate / muted
  SELECTED: '#2563eb',    // Primary Blue
};

export default function SeatMap({
  flightId,
  cabinClass = 'ECONOMY',
  onSeatSelected,
  maxSeats = 9,
  baseFare = 0,
  onContinue,
}) {
  const [seatMap, setSeatMap] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [holdTimeLeft, setHoldTimeLeft] = useState({});
  const [loading, setLoading] = useState(true);
  const [holdingSeatId, setHoldingSeatId] = useState(null);
  const [error, setError] = useState(null);
  const [infoMessage, setInfoMessage] = useState(null);
  const [showPreferencesModal, setShowPreferencesModal] = useState(false);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { subscribe } = useWebSocket();

  // Refs to always access latest props & state inside callbacks and intervals without stale closures or dependency loops
  const selectedSeatsRef = useRef(selectedSeats);
  useEffect(() => {
    selectedSeatsRef.current = selectedSeats;
  }, [selectedSeats]);

  const onSeatSelectedRef = useRef(onSeatSelected);
  useEffect(() => {
    onSeatSelectedRef.current = onSeatSelected;
  }, [onSeatSelected]);

  const onContinueRef = useRef(onContinue);
  useEffect(() => {
    onContinueRef.current = onContinue;
  }, [onContinue]);

  const holdingSeatIdRef = useRef(null);

  // Load seat map from backend API with authoritative hold reconciliation
  const fetchSeatMap = useCallback(
    async (isSilent = false) => {
      if (!flightId) return;
      if (!isSilent) setLoading(true);
      try {
        const data = await getSeatMap(flightId, cabinClass);
        if (!data || !data.seats) {
          if (!isSilent) setLoading(false);
          return;
        }

        const myActiveIds = (data.myActiveHoldSeatIds || []).map(String);
        const currentSelected = selectedSeatsRef.current || [];

        // Check if any previously selected seats are no longer active on the backend
        let seatsExpiredOrLost = false;
        const stillValidSelected = [];

        for (const seat of currentSelected) {
          const backendSeat = data.seats.find((s) => String(s.id) === String(seat.id));
          const isStillHeldByMe = myActiveIds.includes(String(seat.id)) || (backendSeat && backendSeat.status === 'SELECTED');

          if (isStillHeldByMe) {
            stillValidSelected.push({
              ...seat,
              status: 'SELECTED',
              ...(backendSeat ? {
                premiumSurcharge: backendSeat.premiumSurcharge != null ? Number(backendSeat.premiumSurcharge) : Number(seat.premiumSurcharge || 0),
                price: backendSeat.price != null ? Number(backendSeat.price) : Number(seat.price || 0),
                seatType: backendSeat.seatType || seat.seatType,
              } : {}),
            });
          } else {
            seatsExpiredOrLost = true;
          }
        }

        // Check if backend has active holds for this user that aren't yet in local state (e.g. fresh page load)
        for (const backendSeat of data.seats) {
          if (myActiveIds.includes(String(backendSeat.id)) || backendSeat.status === 'SELECTED') {
            const alreadyIn = stillValidSelected.some((s) => String(s.id) === String(backendSeat.id));
            if (!alreadyIn) {
              const rawRow = backendSeat.row ?? backendSeat.rowNumber;
              const row = rawRow != null ? Number(rawRow) : backendSeat.seatNumber ? parseInt(backendSeat.seatNumber, 10) : null;
              const rawCol = backendSeat.column ?? backendSeat.columnLetter;
              const col = rawCol != null ? String(rawCol).toUpperCase() : backendSeat.seatNumber ? backendSeat.seatNumber.replace(/^[0-9]+/, '').toUpperCase() : '';
              const seatNumber = backendSeat.seatNumber || (row && col ? `${row}${col}` : `S-${backendSeat.id}`);

              stillValidSelected.push({
                ...backendSeat,
                row,
                column: col,
                seatNumber,
                status: 'SELECTED',
                price: Number(backendSeat.price || 0),
                premiumSurcharge: Number(backendSeat.premiumSurcharge || 0),
                totalPrice: Number(backendSeat.totalPrice || Number(backendSeat.price || 0) + Number(backendSeat.premiumSurcharge || 0)),
                expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
              });
            }
          }
        }

        if (seatsExpiredOrLost) {
          setHoldTimeLeft((prev) => {
            const next = {};
            for (const s of stillValidSelected) {
              if (prev[s.id]) next[s.id] = prev[s.id];
            }
            return next;
          });
          setInfoMessage('Hold on seat has expired. Seat availability has been refreshed.');
        }

        // Only update selectedSeats if the list actually changed to prevent render loops
        const isDifferent =
          stillValidSelected.length !== currentSelected.length ||
          stillValidSelected.some((s, i) => String(s.id) !== String(currentSelected[i]?.id));

        if (isDifferent) {
          selectedSeatsRef.current = stillValidSelected;
          setSelectedSeats(stillValidSelected);
          if (onSeatSelectedRef.current) {
            onSeatSelectedRef.current(stillValidSelected);
          }
        }

        // Ensure all seats currently held by current user are marked as SELECTED in seatMap
        const normalizedBackendSeats = data.seats.map((s) => {
          const isMine = stillValidSelected.some((sel) => String(sel.id) === String(s.id));
          if (isMine) {
            return { ...s, status: 'SELECTED' };
          }
          return s;
        });

        setSeatMap({
          ...data,
          seats: normalizedBackendSeats,
        });

        if (!isSilent) setError(null);
      } catch (err) {
        console.error('Seat map error:', err);
        if (!isSilent) {
          setError(err.response?.data?.message || 'Unable to load seat map. Please try again.');
        }
      } finally {
        if (!isSilent) setLoading(false);
      }
    },
    [flightId, cabinClass]
  );

  useEffect(() => {
    fetchSeatMap();
  }, [fetchSeatMap]);

  // Controlled background polling every 12 seconds when the document is visible
  useEffect(() => {
    if (!flightId) return;
    const interval = setInterval(() => {
      if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
        fetchSeatMap(true);
      }
    }, 12000);
    return () => clearInterval(interval);
  }, [flightId, fetchSeatMap]);

  // Real-time WebSocket updates with current-user hold distinction
  useEffect(() => {
    if (!flightId) return;
    const topic = `/topic/seats/${flightId}`;
    const unsubscribe = subscribe(topic, (update) => {
      if (!update || !update.seatId) return;

      const isMine =
        selectedSeatsRef.current.some((s) => String(s.id) === String(update.seatId)) ||
        String(holdingSeatIdRef.current) === String(update.seatId);

      let effectiveStatus = update.status;
      if (update.status === 'HELD') {
        // If seat belongs to current user, preserve SELECTED; otherwise it is HELD by another user
        effectiveStatus = isMine ? 'SELECTED' : 'HELD';
      } else if (update.status === 'AVAILABLE' && isMine) {
        // Hold was released or expired on backend for current user
        const retained = selectedSeatsRef.current.filter(
          (s) => String(s.id) !== String(update.seatId)
        );
        selectedSeatsRef.current = retained;
        setSelectedSeats(retained);
        if (onSeatSelectedRef.current) onSeatSelectedRef.current(retained);
        setHoldTimeLeft((prev) => {
          const next = { ...prev };
          delete next[update.seatId];
          return next;
        });
        setInfoMessage('Hold on seat has expired. Seat availability has been refreshed.');
        effectiveStatus = 'AVAILABLE';
      }

      setSeatMap((prev) => {
        if (!prev || !prev.seats) return prev;
        const updatedSeats = prev.seats.map((seat) => {
          if (String(seat.id) === String(update.seatId)) {
            return {
              ...seat,
              status: effectiveStatus,
              heldUntil: effectiveStatus === 'SELECTED' ? seat.heldUntil : (update.expiresAt || null),
            };
          }
          return seat;
        });

        const summary = {
          total: updatedSeats.length,
          available: updatedSeats.filter((s) => s.status === 'AVAILABLE').length,
          held: updatedSeats.filter((s) => s.status === 'HELD').length,
          booked: updatedSeats.filter((s) => s.status === 'BOOKED' || s.status === 'OCCUPIED').length,
        };

        return { ...prev, seats: updatedSeats, summary };
      });
    });

    return () => {
      if (typeof unsubscribe === 'function') unsubscribe();
    };
  }, [flightId, subscribe]);

  // Hold countdown timer with automatic expiry handling
  useEffect(() => {
    const interval = setInterval(() => {
      setHoldTimeLeft((prev) => {
        let hasExpired = false;
        const next = {};
        const now = Date.now();

        for (const [seatId, expiresAt] of Object.entries(prev)) {
          const remaining = Math.max(0, Math.ceil((new Date(expiresAt) - now) / 1000));
          if (remaining > 0) {
            next[seatId] = expiresAt;
          } else {
            hasExpired = true;
          }
        }

        if (hasExpired) {
          const expiredSeatIds = Object.keys(prev).filter((id) => !next[id]);
          const retained = selectedSeatsRef.current.filter((s) => !expiredSeatIds.includes(String(s.id)));
          selectedSeatsRef.current = retained;
          setSelectedSeats(retained);
          if (onSeatSelectedRef.current) onSeatSelectedRef.current(retained);
          setInfoMessage('Hold on seat has expired. Seat availability has been refreshed.');
          fetchSeatMap(true);
        }

        return next;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [fetchSeatMap]);

  // Normalize seat array to guarantee row, column, seatNumber, and pricing properties
  const normalizedSeats = useMemo(() => {
    if (!seatMap || !Array.isArray(seatMap.seats)) return [];
    return seatMap.seats.map((s) => {
      const rawRow = s.row ?? s.rowNumber;
      const row = rawRow != null ? Number(rawRow) : s.seatNumber ? parseInt(s.seatNumber, 10) : null;
      const rawCol = s.column ?? s.columnLetter;
      const col = rawCol != null ? String(rawCol).toUpperCase() : s.seatNumber ? s.seatNumber.replace(/^[0-9]+/, '').toUpperCase() : '';
      const seatNumber = s.seatNumber || (row && col ? `${row}${col}` : `S-${s.id}`);
      const isExit = Boolean(s.emergencyExit || s.isEmergencyExit || row === 12 || row === 13);
      const isExtra = Boolean(s.extraLegroom || s.isExtraLegroom || (s.seatType && s.seatType.includes('EXTRA_LEGROOM')));
      const isWindow = Boolean(s.window || s.isWindow || col === 'A' || col === 'F');
      const isAisle = Boolean(s.aisle || s.isAisle || col === 'C' || col === 'D');
      const isMiddle = Boolean(s.middle || s.isMiddle || col === 'B' || col === 'E');
      const price = Number(s.price || 0);
      const premiumSurcharge = Number(s.premiumSurcharge || 0);
      const totalPrice = Number(s.totalPrice || price + premiumSurcharge);
      const status = String(s.status || (s.available ? 'AVAILABLE' : 'BOOKED')).toUpperCase();

      return {
        ...s,
        id: s.id,
        row,
        column: col,
        seatNumber,
        emergencyExit: isExit,
        extraLegroom: isExtra,
        window: isWindow,
        aisle: isAisle,
        middle: isMiddle,
        price,
        premiumSurcharge,
        totalPrice,
        status,
        preferenceMatch: Boolean(s.preferenceMatch || s.matchesUserPreference),
      };
    });
  }, [seatMap]);

  // Dynamic layout columns: left side vs right side
  const { leftCols, rightCols, rows, sortedRowNumbers } = useMemo(() => {
    const allCols = Array.from(new Set(normalizedSeats.map((s) => s.column).filter(Boolean))).sort();
    let left = ['A', 'B', 'C'];
    let right = ['D', 'E', 'F'];

    if (allCols.length > 0) {
      if (!allCols.includes('B') && !allCols.includes('E') && allCols.includes('C') && allCols.includes('D')) {
        left = ['A', 'C'];
        right = ['D', 'F'];
      } else {
        const hasA = allCols.includes('A');
        const hasB = allCols.includes('B');
        const hasC = allCols.includes('C');
        const hasD = allCols.includes('D');
        const hasE = allCols.includes('E');
        const hasF = allCols.includes('F');
        if (hasA || hasB || hasC) {
          left = ['A', hasB ? 'B' : null, hasC ? 'C' : null].filter(Boolean);
        }
        if (hasD || hasE || hasF) {
          right = [hasD ? 'D' : null, hasE ? 'E' : null, hasF ? 'F' : null].filter(Boolean);
        }
      }
    }

    const rowMap = {};
    normalizedSeats.forEach((seat) => {
      if (seat.row != null && !isNaN(seat.row)) {
        if (!rowMap[seat.row]) rowMap[seat.row] = [];
        rowMap[seat.row].push(seat);
      }
    });

    const sortedRows = Object.keys(rowMap)
      .map(Number)
      .sort((a, b) => a - b);

    return {
      leftCols: left.length > 0 ? left : ['A', 'B', 'C'],
      rightCols: right.length > 0 ? right : ['D', 'E', 'F'],
      rows: rowMap,
      sortedRowNumbers: sortedRows,
    };
  }, [normalizedSeats]);

  // Deselect a seat
  const handleDeselectSeat = useCallback(
    async (seatId) => {
      setHoldingSeatId(seatId);
      holdingSeatIdRef.current = seatId;
      try {
        await releaseSeatHold(seatId);
        const updated = selectedSeatsRef.current.filter((s) => String(s.id) !== String(seatId));
        selectedSeatsRef.current = updated;
        setSelectedSeats(updated);
        setHoldTimeLeft((prev) => {
          const next = { ...prev };
          delete next[seatId];
          return next;
        });
        setSeatMap((prev) => {
          if (!prev || !prev.seats) return prev;
          return {
            ...prev,
            seats: prev.seats.map((s) => (String(s.id) === String(seatId) ? { ...s, status: 'AVAILABLE' } : s)),
          };
        });
        setError(null);
        if (onSeatSelectedRef.current) onSeatSelectedRef.current(updated);
      } catch (e) {
        setError('Failed to release seat hold. Please try again.');
      } finally {
        setHoldingSeatId(null);
        holdingSeatIdRef.current = null;
      }
    },
    []
  );

  // Click handler on seat button
  const handleHoldSeat = useCallback(
    async (seat) => {
      if (!isAuthenticated) {
        navigate('/login', { state: { from: { pathname: `/flights/${flightId}` } } });
        return;
      }

      // Check if already selected by this user -> Release hold
      const isAlreadySelected = selectedSeatsRef.current.some((s) => String(s.id) === String(seat.id));
      if (isAlreadySelected) {
        await handleDeselectSeat(seat.id);
        return;
      }

      // If seat is booked or held by another passenger, reject and show notice
      if (seat.status === 'BOOKED' || seat.status === 'OCCUPIED') {
        setError(`Seat ${seat.seatNumber} is no longer available.`);
        return;
      }
      if (seat.status === 'HELD') {
        setError(`Seat ${seat.seatNumber} is currently held by another passenger.`);
        return;
      }

      if (selectedSeatsRef.current.length >= maxSeats) {
        setError(`You can select a maximum of ${maxSeats} ${maxSeats === 1 ? 'seat' : 'seats'} for this booking.`);
        return;
      }

      // Hold seat on backend
      setHoldingSeatId(seat.id);
      holdingSeatIdRef.current = seat.id;
      try {
        const hold = await holdSeat(seat.id);
        const holdExpiresAt = hold?.expiresAt || hold?.heldUntil || new Date(Date.now() + 10 * 60 * 1000).toISOString();
        const newSelectedSeat = {
          ...seat,
          status: 'SELECTED',
          holdId: hold?.id || hold?.holdId,
          expiresAt: holdExpiresAt,
          premiumSurcharge: hold?.premiumSurcharge != null ? Number(hold.premiumSurcharge) : Number(seat.premiumSurcharge || 0),
          price: hold?.seatPrice != null ? Number(hold.seatPrice) : Number(seat.price || 0),
        };
        const updated = [...selectedSeatsRef.current, newSelectedSeat];
        selectedSeatsRef.current = updated;
        setSelectedSeats(updated);
        setHoldTimeLeft((prev) => ({ ...prev, [seat.id]: holdExpiresAt }));
        setSeatMap((prev) => {
          if (!prev || !prev.seats) return prev;
          return {
            ...prev,
            seats: prev.seats.map((s) => (String(s.id) === String(seat.id) ? { ...s, status: 'SELECTED' } : s)),
          };
        });
        setError(null);
        setInfoMessage(null);
        if (onSeatSelectedRef.current) onSeatSelectedRef.current(updated);
      } catch (e) {
        const status = e.response?.status;
        if (status === 401 || status === 403) {
          setError('Your session has expired. Please sign in to hold seats.');
          navigate('/login', { state: { from: { pathname: `/flights/${flightId}` } } });
          return;
        }

        // Automatic refresh on conflict without full page reload
        await fetchSeatMap(true);
        const serverMsg = e.response?.data?.message;
        const conflictCode = e.response?.data?.data?.code || e.response?.data?.code;
        if (
          conflictCode === 'SEAT_NO_LONGER_AVAILABLE' ||
          (serverMsg && (serverMsg.includes('held') || serverMsg.includes('booked') || serverMsg.includes('no longer')))
        ) {
          setError(`Seat ${seat.seatNumber} is no longer available. Seat availability has been refreshed.`);
        } else {
          setError(serverMsg || `Seat ${seat.seatNumber} could not be held. Please try another seat.`);
        }
      } finally {
        setHoldingSeatId(null);
        holdingSeatIdRef.current = null;
      }
    },
    [isAuthenticated, navigate, flightId, maxSeats, handleDeselectSeat, fetchSeatMap]
  );

  // Defensive handler for confirming seat selection
  const handleConfirmSelection = useCallback(() => {
    if (!selectedSeatsRef.current || selectedSeatsRef.current.length === 0) {
      setError('Please select at least 1 seat before continuing.');
      return;
    }
    setError(null);
    if (onContinueRef.current) {
      onContinueRef.current();
    } else if (onSeatSelectedRef.current) {
      onSeatSelectedRef.current(selectedSeatsRef.current);
    }
  }, []);

  // Summary calculations
  const totalSeatSurcharges = useMemo(() => {
    return selectedSeats.reduce((sum, s) => sum + Number(s.premiumSurcharge || 0), 0);
  }, [selectedSeats]);

  const effectivePaxCount = Math.max(1, selectedSeats.length);
  const totalBaseFare = Number(baseFare || 0) * effectivePaxCount;
  const grandTotal = totalBaseFare + totalSeatSurcharges;

  // Loading skeleton with aircraft shape
  if (loading) {
    return (
      <div className="bg-white rounded-2xl p-6 sm:p-8 border border-gray-100 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pb-4 border-b border-gray-100">
          <div className="space-y-2">
            <div className="h-6 bg-gray-200 rounded-md w-48 animate-pulse" />
            <div className="h-4 bg-gray-100 rounded-md w-64 animate-pulse" />
          </div>
          <div className="h-8 bg-gray-100 rounded-xl w-36 animate-pulse" />
        </div>

        <div className="flex flex-col items-center py-6">
          <div className="w-44 h-12 bg-gray-200 rounded-t-full mb-1 animate-pulse" />
          <div className="border-2 border-gray-200 rounded-b-3xl p-6 max-w-md w-full bg-gray-50/50 space-y-3">
            <div className="flex justify-center gap-2 mb-4">
              <div className="h-4 bg-gray-200 rounded w-48 animate-pulse" />
            </div>
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="flex items-center justify-center gap-2">
                <div className="w-6 h-4 bg-gray-200 rounded animate-pulse" />
                <div className="flex gap-1.5">
                  <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse" />
                  <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse" />
                  <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse" />
                </div>
                <div className="w-8 h-4 bg-gray-100 rounded animate-pulse" />
                <div className="flex gap-1.5">
                  <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse" />
                  <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse" />
                  <div className="w-8 h-8 bg-gray-200 rounded-lg animate-pulse" />
                </div>
                <div className="w-6 h-4 bg-gray-200 rounded animate-pulse" />
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Error state if zero seats could be fetched
  if (error && normalizedSeats.length === 0) {
    return (
      <div className="bg-white rounded-2xl p-8 border border-red-100 shadow-sm text-center space-y-4">
        <div className="w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mx-auto">
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div className="space-y-1">
          <h4 className="font-bold text-gray-900 text-base">Unable to load seat map</h4>
          <p className="text-xs text-gray-500 max-w-md mx-auto">{error}</p>
        </div>
        <button
          type="button"
          onClick={() => fetchSeatMap()}
          className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold transition shadow-sm"
        >
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header Card */}
      <div className="bg-white rounded-2xl p-4 sm:p-6 border border-gray-100 shadow-sm">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pb-4 border-b border-gray-100">
          <div>
            <h3 className="font-bold text-gray-900 text-base flex items-center gap-2">
              <span className="text-blue-600">
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z" />
                </svg>
              </span>
              Select Your Seats
              {seatMap?.flightNumber && (
                <span className="text-xs font-mono font-medium text-gray-500 bg-gray-100 px-2 py-0.5 rounded-md">
                  {seatMap.flightNumber}
                </span>
              )}
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">
              Interactive aircraft seat map with live availability, 10-minute holds, and preference matching.
            </p>
          </div>

          <button
            type="button"
            onClick={() => setShowPreferencesModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold rounded-xl border border-blue-200 transition shrink-0"
          >
            <span className="text-amber-500">★</span> Travel Preferences
          </button>
        </div>

        {/* Guest Authentication Banner */}
        {!isAuthenticated && (
          <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-800 font-medium flex items-center justify-between animate-fade-in shadow-xs">
            <div className="flex items-center gap-2">
              <span className="text-blue-600 text-base">👤</span>
              <span>Sign in to select and hold your seats for 10 minutes.</span>
            </div>
            <button
              type="button"
              onClick={() => navigate('/login', { state: { from: { pathname: `/flights/${flightId}` } } })}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold text-xs transition shrink-0"
            >
              Sign In to Select
            </button>
          </div>
        )}

        {/* Dynamic Contextual Conflict Alert */}
        {error && (
          <div className="mt-4 p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium flex items-center justify-between animate-fade-in shadow-xs">
            <div className="flex items-center gap-2">
              <span className="text-red-500 text-sm font-bold">⚠️</span>
              <span>{error}</span>
            </div>
            <button
              type="button"
              onClick={() => setError(null)}
              className="text-red-500 hover:text-red-800 font-bold ml-2 text-sm px-1"
            >
              ×
            </button>
          </div>
        )}

        {/* Expiry / Info notice */}
        {infoMessage && (
          <div className="mt-4 p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 font-medium flex items-center justify-between animate-fade-in shadow-xs">
            <div className="flex items-center gap-2">
              <span className="text-amber-600 text-sm">⏱</span>
              <span>{infoMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setInfoMessage(null)}
              className="text-amber-600 hover:text-amber-800 font-bold ml-2 text-sm px-1"
            >
              ×
            </button>
          </div>
        )}

        {/* Seat Legend */}
        <div className="mt-4 bg-slate-50/90 p-3.5 rounded-xl text-xs border border-slate-100">
          <div className="flex flex-wrap items-center justify-between gap-3 sm:gap-4">
            <span className="flex items-center gap-1.5 font-medium text-gray-700">
              <span className="w-4 h-4 rounded-md bg-emerald-500 shadow-xs flex items-center justify-center text-[10px] text-white">●</span>
              Available
            </span>
            <span className="flex items-center gap-1.5 font-medium text-gray-700">
              <span className="w-4 h-4 rounded-md bg-blue-600 shadow-xs flex items-center justify-center text-[10px] text-white">✓</span>
              Selected
            </span>
            <span className="flex items-center gap-1.5 font-medium text-gray-700">
              <span className="w-4 h-4 rounded-md bg-amber-500 shadow-xs flex items-center justify-center text-[10px] text-white">⏱</span>
              Held (10m)
            </span>
            <span className="flex items-center gap-1.5 font-medium text-gray-700">
              <span className="w-4 h-4 rounded-md bg-slate-300 shadow-xs flex items-center justify-center text-[10px] text-slate-600">✕</span>
              Occupied
            </span>
            <span className="flex items-center gap-1.5 font-medium text-purple-700">
              <span className="w-4 h-4 rounded-md border-2 border-purple-500 bg-white flex items-center justify-center text-[9px] text-purple-600 font-bold">★</span>
              Premium / Extra Legroom
            </span>
            <span className="flex items-center gap-1.5 font-medium text-amber-700">
              <span className="w-4 h-4 rounded-md border-2 border-amber-400 bg-amber-50 flex items-center justify-center text-[9px] text-amber-600 font-bold">★</span>
              Matches Preference
            </span>
          </div>
        </div>
      </div>

      {/* Main Grid: Aircraft Map Left/Center + Authoritative Selected Seat Summary Sticky Panel Right */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left / Center: Interactive Aircraft Seat Map */}
        <div className="lg:col-span-8 bg-white rounded-2xl p-4 sm:p-6 border border-gray-100 shadow-sm flex flex-col items-center select-none overflow-x-auto">
          {/* Cockpit Nose */}
          <div className="w-48 h-14 bg-gradient-to-t from-slate-200 via-slate-100 to-slate-200 rounded-t-full border-t-2 border-x-2 border-slate-300 flex items-center justify-center text-xs font-bold text-slate-600 shadow-xs mb-0.5">
            <span className="tracking-widest flex items-center gap-1.5">
              <svg className="w-3.5 h-3.5 text-slate-500" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2L4 14h6v8h4v-8h6z" />
              </svg>
              FRONT / COCKPIT
            </span>
          </div>

          {/* Fuselage Container */}
          <div
            role="grid"
            aria-label="Aircraft Seat Map"
            className="border-2 border-slate-300 rounded-b-3xl p-4 sm:p-5 min-w-[320px] max-w-lg w-full bg-slate-50/50 shadow-inner relative"
          >
            {/* Column Header (# A B C | AISLE | D E F #) */}
            <div className="flex items-center justify-center gap-2 mb-3 pb-2 border-b border-slate-200 text-xs font-bold text-slate-500">
              <div className="w-6 text-center font-mono text-slate-400">#</div>
              <div className="flex gap-1.5">
                {leftCols.map((col) => (
                  <span key={col} className="w-8 sm:w-9 text-center font-bold text-slate-700">
                    {col}
                  </span>
                ))}
              </div>
              <div className="w-8 text-center text-[10px] font-bold text-slate-400 tracking-wider select-none">
                AISLE
              </div>
              <div className="flex gap-1.5">
                {rightCols.map((col) => (
                  <span key={col} className="w-8 sm:w-9 text-center font-bold text-slate-700">
                    {col}
                  </span>
                ))}
              </div>
              <div className="w-6 text-center font-mono text-slate-400">#</div>
            </div>

            {/* Rows */}
            <div className="space-y-2">
              {sortedRowNumbers.map((rowNum) => {
                const rowSeats = rows[rowNum] || [];
                const isExitRow = rowSeats.some((s) => s.emergencyExit);

                return (
                  <div key={rowNum} className="relative">
                    {isExitRow && (
                      <div className="text-[10px] text-amber-800 font-bold text-center py-0.5 mb-1 bg-amber-50 rounded-md border border-dashed border-amber-300 flex items-center justify-center gap-1 shadow-xs">
                        <span>⚡</span> Exit Row — Extra Legroom
                      </div>
                    )}

                    <div className="flex items-center justify-center gap-2">
                      <span className="w-6 text-center text-xs font-semibold text-slate-400 font-mono">
                        {rowNum}
                      </span>

                      {/* Left seats */}
                      <div className="flex gap-1.5">
                        {leftCols.map((col) => {
                          const seat = rowSeats.find((s) => s.column === col);
                          return renderSeatButton(seat, col);
                        })}
                      </div>

                      {/* Aisle */}
                      <div className="w-8 text-center text-[10px] font-mono text-slate-300 select-none flex items-center justify-center">
                        <span className="w-1.5 h-1.5 rounded-full bg-slate-300" />
                      </div>

                      {/* Right seats */}
                      <div className="flex gap-1.5">
                        {rightCols.map((col) => {
                          const seat = rowSeats.find((s) => s.column === col);
                          return renderSeatButton(seat, col);
                        })}
                      </div>

                      <span className="w-6 text-center text-xs font-semibold text-slate-400 font-mono">
                        {rowNum}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Fuselage Tail */}
            <div className="mt-6 pt-3 border-t border-slate-200 text-center text-xs text-slate-400 font-semibold flex items-center justify-center gap-4">
              <span>🚪 Galley</span>
              <span>•</span>
              <span>🚻 Restrooms</span>
            </div>
          </div>
        </div>

        {/* Right / Bottom: Authoritative Selected Seat Summary Component */}
        <div className="lg:col-span-4 w-full">
          <div className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm space-y-5 sticky top-6">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h4 className="font-bold text-gray-900 text-sm">Selected Seats</h4>
                <p className="text-[11px] text-gray-500 mt-0.5">Authoritative seat pricing & hold</p>
              </div>
              <span className="text-xs px-2.5 py-1 rounded-full font-bold bg-blue-50 text-blue-700 border border-blue-100">
                {selectedSeats.length} / {maxSeats} {maxSeats === 1 ? 'seat' : 'seats'}
              </span>
            </div>

            {/* Empty Selection State */}
            {selectedSeats.length === 0 ? (
              <div className="space-y-4">
                <div className="py-8 text-center space-y-2.5">
                  <div className="w-12 h-12 rounded-2xl bg-slate-50 text-slate-400 flex items-center justify-center mx-auto text-xl border border-slate-100">
                    💺
                  </div>
                  <div className="text-xs font-semibold text-gray-700">No seat selected yet</div>
                  <p className="text-[11px] text-gray-400 max-w-xs mx-auto">
                    Click any available green seat on the aircraft map. Holds are reserved on the backend for 10 minutes.
                  </p>
                </div>

                {error && (
                  <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2 font-medium animate-fade-in shadow-xs">
                    <span className="text-red-500 font-bold">⚠️</span>
                    <span>{error}</span>
                  </div>
                )}

                <button
                  type="button"
                  onClick={handleConfirmSelection}
                  disabled={true}
                  aria-disabled="true"
                  className="w-full py-3 px-4 bg-gray-200 text-gray-400 rounded-xl text-xs font-bold border border-gray-300 cursor-not-allowed flex items-center justify-center gap-2 transition"
                >
                  <span>Confirm Selection (0 Seats Selected)</span>
                  <span>→</span>
                </button>
              </div>
            ) : (
              /* Populated Selection List */
              <div className="space-y-4">
                <div className="space-y-2.5 divide-y divide-gray-100">
                  {selectedSeats.map((seat, idx) => {
                    const timeLeft = holdTimeLeft[seat.id]
                      ? Math.max(0, Math.ceil((new Date(holdTimeLeft[seat.id]) - Date.now()) / 1000))
                      : 0;
                    const surcharge = Number(seat.premiumSurcharge || 0);

                    return (
                      <div key={seat.id} className="pt-2.5 first:pt-0 flex items-start justify-between gap-3">
                        <div className="space-y-1 flex-1">
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-bold text-gray-500 uppercase tracking-wider">
                              Passenger {idx + 1}
                            </span>
                            {timeLeft > 0 && (
                              <span className="text-[10px] font-mono font-bold bg-amber-50 text-amber-700 border border-amber-200 px-1.5 py-0.2 rounded">
                                ⏱ {Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, '0')}
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="text-sm font-bold text-gray-900 flex items-center gap-1">
                              <span>✈</span> {seat.seatNumber}
                            </span>
                            <span className="text-[11px] text-gray-500">
                              {seat.window ? 'Window' : seat.aisle ? 'Aisle' : 'Middle'}
                              {seat.extraLegroom ? ' • Extra Legroom' : seat.seatType ? ` • ${seat.seatType.replace('_', ' ')}` : ''}
                            </span>
                          </div>

                          <div className="text-xs font-semibold">
                            <span className="text-gray-500 font-normal">Seat Fare: </span>
                            {surcharge > 0 ? (
                              <span className="text-purple-700 font-bold">+₹{surcharge.toLocaleString()}</span>
                            ) : (
                              <span className="text-emerald-600 font-bold">₹0 (Included)</span>
                            )}
                          </div>
                        </div>

                        {/* Deselection Action */}
                        <button
                          type="button"
                          onClick={() => handleDeselectSeat(seat.id)}
                          disabled={holdingSeatId === seat.id}
                          title="Remove seat"
                          className="text-gray-400 hover:text-red-600 transition p-1 rounded-lg hover:bg-red-50 text-sm font-bold shrink-0"
                        >
                          ✕
                        </button>
                      </div>
                    );
                  })}
                </div>

                {/* Authoritative Fare Breakdown */}
                <div className="bg-slate-50/80 rounded-xl p-3.5 border border-slate-100 space-y-2 text-xs">
                  <div className="text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                    Authoritative Fare Breakdown
                  </div>
                  {baseFare > 0 && (
                    <div className="flex justify-between text-gray-600">
                      <span>Base Flight Fare ({effectivePaxCount} pax)</span>
                      <span>₹{totalBaseFare.toLocaleString()}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-gray-600">
                    <span>Seat Selection Charges</span>
                    <span className={totalSeatSurcharges > 0 ? 'text-purple-700 font-bold' : 'text-gray-900'}>
                      ₹{totalSeatSurcharges.toLocaleString()}
                    </span>
                  </div>
                  <div className="border-t border-slate-200 pt-2 flex justify-between font-bold text-sm text-gray-900">
                    <span>Total Fare</span>
                    <span className="text-blue-600 text-base">
                      ₹{(baseFare > 0 ? grandTotal : totalSeatSurcharges).toLocaleString()}
                    </span>
                  </div>
                </div>

                {error && (
                  <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2 font-medium animate-fade-in shadow-xs">
                    <span className="text-red-500 font-bold">⚠️</span>
                    <span>{error}</span>
                  </div>
                )}

                {/* Action CTA */}
                <button
                  type="button"
                  onClick={handleConfirmSelection}
                  className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span>Confirm Selection ({selectedSeats.length} {selectedSeats.length === 1 ? 'Seat' : 'Seats'})</span>
                  <span>→</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Preferences Modal */}
      <TravelPreferencesModal
        isOpen={showPreferencesModal}
        onClose={() => setShowPreferencesModal(false)}
        onPreferencesUpdated={() => fetchSeatMap()}
      />
    </div>
  );

  function renderSeatButton(seat, col) {
    if (!seat) {
      return <div key={col} className="w-8 sm:w-9 h-8 sm:h-9 opacity-0 pointer-events-none" />;
    }

    const isSelected = selectedSeats.some((s) => String(s.id) === String(seat.id)) || seat.status === 'SELECTED';
    const isHeld = (seat.status === 'HELD' || (seat.heldUntil && new Date(seat.heldUntil) > new Date())) && !isSelected;
    const isBooked = (seat.status === 'BOOKED' || seat.status === 'OCCUPIED' || seat.status === 'UNAVAILABLE') && !isSelected;
    const isPremium =
      Number(seat.premiumSurcharge || 0) > 0 || seat.extraLegroom || seat.seatType === 'PREMIUM' || seat.seatType === 'EXTRA_LEGROOM';
    const matchesPref = Boolean(seat.preferenceMatch);
    const isPending = holdingSeatId === seat.id;

    const timeLeft = holdTimeLeft[seat.id]
      ? Math.max(0, Math.ceil((new Date(holdTimeLeft[seat.id]) - Date.now()) / 1000))
      : 0;

    let bgColor = STATUS_COLORS.AVAILABLE;
    if (isBooked) bgColor = STATUS_COLORS.BOOKED;
    else if (isHeld) bgColor = STATUS_COLORS.HELD;
    else if (isSelected) bgColor = STATUS_COLORS.SELECTED;

    const accessibleLabel = `Seat ${seat.seatNumber}, ${seat.seatType || 'Standard'}, ₹${seat.price + Number(seat.premiumSurcharge || 0)}, ${
      isSelected ? 'Selected' : isHeld ? 'Held by another passenger' : isBooked ? 'Occupied' : 'Available'
    }${matchesPref ? ', matches travel preference' : ''}`;

    return (
      <div key={seat.id} className="relative group">
        <button
          type="button"
          onClick={() => handleHoldSeat(seat)}
          disabled={isBooked || isHeld || isPending}
          role="gridcell"
          aria-label={accessibleLabel}
          aria-selected={isSelected}
          aria-disabled={isBooked || isHeld}
          className={`w-8 sm:w-9 h-8 sm:h-9 rounded-lg text-xs font-bold text-white transition-all duration-150 flex flex-col items-center justify-center relative ${
            isBooked
              ? 'cursor-not-allowed opacity-40 bg-slate-400'
              : isHeld
              ? 'cursor-not-allowed opacity-80 shadow-xs'
              : 'hover:scale-105 active:scale-95 shadow-xs cursor-pointer hover:shadow-md'
          } ${
            matchesPref && !isSelected && !isBooked ? 'ring-2 ring-amber-400 ring-offset-1' : ''
          } ${
            isPremium && !isSelected && !isBooked && !matchesPref ? 'ring-1 ring-purple-400 ring-offset-0.5' : ''
          } ${
            isSelected ? 'ring-2 ring-blue-500 ring-offset-2 scale-105 shadow-md z-10' : ''
          }`}
          style={{ backgroundColor: isBooked ? '#94a3b8' : bgColor }}
        >
          {isPending ? (
            <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : (
            <span>{seat.column}</span>
          )}

          {matchesPref && !isSelected && !isBooked && (
            <span className="absolute -top-1 -right-1 text-[8px] bg-amber-400 text-gray-900 rounded-full w-3.5 h-3.5 flex items-center justify-center font-bold shadow-xs">
              ★
            </span>
          )}
          {isSelected && (
            <span className="absolute -top-1 -right-1 text-[8px] bg-blue-900 text-white rounded-full w-3.5 h-3.5 flex items-center justify-center font-bold shadow-xs">
              ✓
            </span>
          )}
          {isHeld && !isSelected && (
            <span className="absolute -top-1 -right-1 text-[8px] bg-amber-600 text-white rounded-full w-3.5 h-3.5 flex items-center justify-center font-bold shadow-xs">
              ⏱
            </span>
          )}
        </button>

        {/* Live Hold Countdown Badge */}
        {timeLeft > 0 && isSelected && (
          <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 text-[9px] text-amber-900 font-mono font-bold bg-amber-200 border border-amber-300 rounded px-1 shadow-xs whitespace-nowrap z-20">
            {Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, '0')}
          </div>
        )}

        {/* Accessible Hover Tooltip */}
        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:flex flex-col items-center z-30 pointer-events-none animate-fade-in">
          <div className="bg-slate-900 text-white text-[11px] rounded-lg py-1.5 px-2.5 shadow-xl whitespace-nowrap space-y-0.5 border border-slate-700">
            <div className="font-bold flex items-center gap-1.5">
              <span>Seat {seat.seatNumber}</span>
              {matchesPref && <span className="text-amber-300 font-semibold">★ Matches Preference</span>}
            </div>
            <div className="text-slate-300 text-[10px]">
              {seat.window ? 'Window' : seat.aisle ? 'Aisle' : 'Middle'} ·{' '}
              {seat.seatType ? seat.seatType.replace('_', ' ') : 'Standard'}
            </div>
            <div className="text-emerald-400 font-bold text-[10px]">
              {isPremium ? `+₹${seat.premiumSurcharge} surcharge` : 'Standard Fare (₹0)'}
            </div>
            <div className="text-slate-400 text-[9px] uppercase tracking-wider">Status: {seat.status}</div>
          </div>
          <div className="w-2 h-2 bg-slate-900 rotate-45 -mt-1 border-b border-r border-slate-700" />
        </div>
      </div>
    );
  }
}
