import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckIcon, AlertTriangleIcon, RefreshCwIcon, ShieldCheckIcon } from '../common/Icons';
import { voyaraApi } from '../../api/phase3Api';

export default function TripReadinessCard({ bookingId, readiness, onRecalculate, className = '' }) {
  const [recalculating, setRecalculating] = useState(false);

  if (!readiness) return null;

  const score = readiness.score ?? 0;
  let checks = [];
  try {
    if (Array.isArray(readiness.checks)) {
      checks = readiness.checks;
    } else if (readiness.checksJson) {
      checks = JSON.parse(readiness.checksJson);
    }
  } catch {
    checks = [];
  }

  const scoreColor =
    score >= 80 ? 'text-emerald-600 bg-emerald-50 border-emerald-200' :
    score >= 50 ? 'text-amber-600 bg-amber-50 border-amber-200' :
    'text-red-600 bg-red-50 border-red-200';

  const barColor =
    score >= 80 ? 'bg-gradient-to-r from-emerald-500 to-teal-500' :
    score >= 50 ? 'bg-gradient-to-r from-amber-500 to-orange-500' :
    'bg-gradient-to-r from-red-500 to-rose-500';

  const handleRefresh = async () => {
    if (!bookingId || recalculating) return;
    setRecalculating(true);
    try {
      await voyaraApi.recalculateReadiness(bookingId);
      if (onRecalculate) onRecalculate();
    } catch {
      // silently handle
    } finally {
      setRecalculating(false);
    }
  };

  return (
    <div className={`voyara-card p-5 sm:p-6 ${className}`}>
      {/* Top Header */}
      <div className="flex items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-blue-50 text-primary flex items-center justify-center">
            <ShieldCheckIcon className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-bold text-slate-900">Trip Readiness Score</h4>
            <p className="text-xs text-slate-500">Voyara pre-departure verification</p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleRefresh}
          disabled={recalculating}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition active:scale-95 disabled:opacity-50"
          title="Recalculate Readiness"
        >
          <RefreshCwIcon className={`w-3.5 h-3.5 ${recalculating ? 'animate-spin' : ''}`} />
          <span className="hidden sm:inline">Refresh</span>
        </button>
      </div>

      {/* Score Header & Bar */}
      <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4 mb-5">
        <div className="flex items-baseline justify-between mb-2">
          <div>
            <span className="text-3xl sm:text-4xl font-black text-slate-900 leading-none">{score}%</span>
            <span className="text-xs text-slate-500 font-semibold ml-2">
              {score >= 80 ? 'Ready for Departure' : score >= 50 ? 'Action Required' : 'Critical Items Incomplete'}
            </span>
          </div>
          <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${scoreColor}`}>
            {score >= 80 ? 'Optimal' : score >= 50 ? 'Moderate' : 'Action Needed'}
          </span>
        </div>

        {/* Progress Bar */}
        <div className="h-3 w-full rounded-full bg-slate-200 overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-700 ease-out ${barColor}`}
            style={{ width: `${Math.min(100, Math.max(5, score))}%` }}
          />
        </div>
      </div>

      {/* Checklist items */}
      {checks.length > 0 && (
        <div className="space-y-2.5">
          <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block mb-1">
            Pre-Flight Checklist:
          </span>
          {checks.map((item, idx) => {
            const passed = item.passed;
            return (
              <div
                key={item.id || idx}
                className={`flex items-center justify-between gap-3 p-3 rounded-xl border transition-all ${
                  passed
                    ? 'bg-emerald-50/40 border-emerald-100 text-slate-800'
                    : 'bg-amber-50/60 border-amber-200 text-slate-900'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 text-xs font-bold ${
                      passed ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
                    }`}
                  >
                    {passed ? <CheckIcon className="w-3.5 h-3.5" /> : <AlertTriangleIcon className="w-3.5 h-3.5" />}
                  </div>

                  <div className="min-w-0">
                    <p className="text-xs font-bold truncate">{item.title || item.name}</p>
                    {item.actionNeeded && !passed && (
                      <p className="text-[11px] text-amber-700 mt-0.5">{item.actionNeeded}</p>
                    )}
                  </div>
                </div>

                {item.actionRoute && !passed && (
                  <Link
                    to={item.actionRoute}
                    className="shrink-0 px-3 py-1 rounded-lg bg-white border border-amber-300 text-xs font-bold text-amber-800 hover:bg-amber-50 shadow-sm transition"
                  >
                    Fix Now
                  </Link>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
