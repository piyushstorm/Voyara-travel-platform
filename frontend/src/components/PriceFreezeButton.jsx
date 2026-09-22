import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { priceApi } from '../api/phase3Api';

export default function PriceFreezeButton({ entityType = 'FLIGHT', entityId, cabinClass = 'ECONOMY', currentPrice }) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [freeze, setFreeze] = useState(null);
  const [error, setError] = useState(null);
  const [timeLeft, setTimeLeft] = useState(null);

  useEffect(() => {
    if (!entityId) return;
    priceApi.getActiveFreeze(entityType, entityId)
      .then((res) => {
        if (res.data?.active && res.data?.freeze) {
          const f = res.data.freeze;
          setFreeze(f);
          const expiresAt = new Date(f.expiresAt).getTime();
          const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
          setTimeLeft(remaining);
        }
      })
      .catch(() => {});
  }, [entityType, entityId]);

  useEffect(() => {
    if (!freeze || freeze.status !== 'ACTIVE' || !freeze.expiresAt) return;
    const expiresAt = new Date(freeze.expiresAt).getTime();
    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
      setTimeLeft(remaining);
      if (remaining <= 0) {
        clearInterval(interval);
        setFreeze((prev) => prev ? { ...prev, status: 'EXPIRED' } : null);
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [freeze]);

  const handleFreeze = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await priceApi.createFreeze(entityId, cabinClass);
      const newFreeze = res.data?.data || res.data;
      setFreeze(newFreeze);
      const expiresAt = new Date(newFreeze.expiresAt).getTime();
      setTimeLeft(Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000)));
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to create price freeze');
    } finally {
      setLoading(false);
    }
  };

  const handleBookNow = () => {
    if (!freeze) return;
    navigate(`/booking?type=${entityType}&id=${entityId}&cabinClass=${cabinClass}&freezeId=${freeze.id}`);
  };

  if (freeze?.status === 'ACTIVE') {
    return (
      <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-5 shadow-sm">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-12 h-12 rounded-xl bg-emerald-100 flex items-center justify-center text-2xl text-emerald-600">
              🔒
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-emerald-900 text-base">Price Frozen & Guaranteed</span>
                <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-200 text-emerald-800">ACTIVE</span>
              </div>
              <p className="text-sm text-emerald-800 mt-0.5">
                Locked at <span className="font-bold">₹{Number(freeze.frozenPrice).toLocaleString()}</span> per passenger
              </p>
              {timeLeft !== null && timeLeft > 0 ? (
                <p className="text-xs text-emerald-700 mt-1 font-mono font-medium">
                  ⏱️ Expires in {Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, '0')}
                </p>
              ) : (
                <p className="text-xs text-red-600 mt-1">Expired</p>
              )}
            </div>
          </div>
          <button
            onClick={handleBookNow}
            className="w-full sm:w-auto px-5 py-2.5 bg-emerald-600 text-white rounded-xl font-semibold hover:bg-emerald-700 transition shadow-sm hover:shadow text-sm whitespace-nowrap"
          >
            Book at Locked Price →
          </button>
        </div>
        <div className="mt-3 pt-3 border-t border-emerald-200/60 flex items-center justify-between text-xs text-emerald-700/80">
          <span>Lock Fee: ₹{Number(freeze.freezeFee).toLocaleString()}</span>
          <span>Guaranteed against future surge pricing</span>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-2xl p-5 shadow-sm">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-amber-100 flex items-center justify-center text-2xl">
            ❄️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-gray-900 text-base">Lock this fare for 15 minutes</span>
              <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">Price Freeze</span>
            </div>
            <p className="text-xs sm:text-sm text-gray-600 mt-0.5">
              Protect against peak increases & demand surges while you finalize plans.
            </p>
            <p className="text-xs text-amber-800 mt-1 font-medium">
              Freeze Fee: ₹{currentPrice ? Math.max(99, Math.round(currentPrice * 0.05)).toLocaleString() : '99'}
            </p>
          </div>
        </div>
        <button
          onClick={handleFreeze}
          disabled={loading}
          className="w-full sm:w-auto px-5 py-2.5 bg-amber-500 text-white rounded-xl font-semibold hover:bg-amber-600 transition disabled:opacity-50 text-sm whitespace-nowrap shadow-sm hover:shadow"
        >
          {loading ? 'Securing Price...' : 'Freeze Price Now'}
        </button>
      </div>
      {error && (
        <div className="mt-3 p-2.5 bg-red-50 border border-red-200 rounded-lg text-red-600 text-xs font-medium">
          ⚠️ {error}
        </div>
      )}
    </div>
  );
}
