import { ClockIcon, AlertTriangleIcon, CheckIcon, ShieldCheckIcon } from '../common/Icons';

/**
 * ConnectionRiskMeter — Visual risk analyzer for connecting flights.
 * Shows connection duration buffer vs required MCT (Minimum Connection Time),
 * risk level (LOW, MEDIUM, HIGH, CRITICAL), risk factors, and recommended actions.
 */

const RISK_CONFIG = {
  LOW: {
    label: 'Low Risk',
    score: 20,
    color: '#10b981',
    bgColor: 'bg-emerald-50',
    textColor: 'text-emerald-700',
    borderColor: 'border-emerald-200',
    icon: CheckIcon,
    summary: 'Generous transfer window. Connection is safe under standard conditions.',
  },
  MEDIUM: {
    label: 'Medium Risk',
    score: 50,
    color: '#f59e0b',
    bgColor: 'bg-amber-50',
    textColor: 'text-amber-700',
    borderColor: 'border-amber-200',
    icon: ClockIcon,
    summary: 'Moderate transit window. Mind terminal transfers and check-in checkpoints.',
  },
  HIGH: {
    label: 'High Risk',
    score: 80,
    color: '#f97316',
    bgColor: 'bg-orange-50',
    textColor: 'text-orange-700',
    borderColor: 'border-orange-200',
    icon: AlertTriangleIcon,
    summary: 'Tight transit buffer. Any inbound flight delay may jeopardize your connection.',
  },
  CRITICAL: {
    label: 'Critical Risk',
    score: 95,
    color: '#ef4444',
    bgColor: 'bg-red-50',
    textColor: 'text-red-700',
    borderColor: 'border-red-200',
    icon: AlertTriangleIcon,
    summary: 'Insufficient connection time. Immediate rebooking or priority transfer recommended.',
  },
};

export default function ConnectionRiskMeter({ riskData, className = '' }) {
  if (!riskData) return null;

  const level = riskData.riskLevel || 'LOW';
  const config = RISK_CONFIG[level] || RISK_CONFIG.LOW;
  const IconComponent = config.icon;

  const connectionMins = riskData.connectionMinutes || 0;
  const requiredMins = riskData.requiredMinutes || 90;
  const bufferMins = connectionMins - requiredMins;

  // Parse risk factors if string
  let factors = [];
  if (Array.isArray(riskData.riskFactors)) {
    factors = riskData.riskFactors;
  } else if (typeof riskData.riskFactors === 'string' && riskData.riskFactors.trim()) {
    factors = riskData.riskFactors.split('\n').filter(Boolean);
  }

  // Semi-circle gauge calculation
  const radius = 54;
  const circumference = Math.PI * radius;
  const strokeDashoffset = circumference - (config.score / 100) * circumference;

  return (
    <div className={`voyara-card p-5 sm:p-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between gap-3 mb-5">
        <div className="flex items-center gap-2.5">
          <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${config.bgColor}`}>
            <ShieldCheckIcon className={`w-5 h-5 ${config.textColor}`} />
          </div>
          <div>
            <h4 className="text-sm font-bold text-slate-900">Connection Risk Analysis</h4>
            <p className="text-xs text-slate-500">Voyara Guardian transit buffer evaluation</p>
          </div>
        </div>

        <span
          className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${config.bgColor} ${config.textColor} ${config.borderColor}`}
        >
          <IconComponent className="w-3.5 h-3.5" />
          {config.label}
        </span>
      </div>

      <div className="grid sm:grid-cols-12 gap-6 items-center">
        {/* Semi-Circle Gauge */}
        <div className="sm:col-span-5 flex flex-col items-center justify-center">
          <div className="relative w-36 h-20 overflow-hidden flex items-end justify-center">
            <svg className="w-36 h-36 -rotate-180" viewBox="0 0 120 120">
              {/* Background Arc */}
              <circle
                cx="60"
                cy="60"
                r={radius}
                fill="none"
                stroke="#e2e8f0"
                strokeWidth="10"
                strokeDasharray={`${circumference} ${circumference}`}
                strokeDashoffset="0"
                strokeLinecap="round"
              />
              {/* Progress Arc */}
              <circle
                cx="60"
                cy="60"
                r={radius}
                fill="none"
                stroke={config.color}
                strokeWidth="10"
                strokeDasharray={`${circumference} ${circumference}`}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                className="transition-all duration-1000 ease-out"
              />
            </svg>
            <div className="absolute bottom-1 text-center">
              <span className="text-2xl font-black text-slate-900 leading-none">{config.score}</span>
              <span className="text-xs text-slate-400 font-bold block">/ 100</span>
            </div>
          </div>

          <div className="mt-2 text-center">
            <span className="text-xs font-semibold text-slate-500">
              {bufferMins >= 0 ? `${bufferMins} min safety margin` : `${Math.abs(bufferMins)} min below MCT`}
            </span>
          </div>
        </div>

        {/* Transit Metrics Breakdown */}
        <div className="sm:col-span-7 space-y-3">
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-slate-400 block text-[10px] uppercase font-bold tracking-wider">Layover Time</span>
              <span className="text-sm font-bold text-slate-900">{connectionMins} mins</span>
            </div>
            <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
              <span className="text-slate-400 block text-[10px] uppercase font-bold tracking-wider">Required MCT</span>
              <span className="text-sm font-bold text-slate-900">{requiredMins} mins</span>
            </div>
          </div>

          <p className="text-xs text-slate-600 leading-relaxed font-medium bg-slate-50/80 p-2.5 rounded-xl border border-slate-100">
            {config.summary}
          </p>

          {factors.length > 0 && (
            <div className="space-y-1 pt-1">
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block">Risk Factors:</span>
              {factors.map((f, i) => (
                <div key={i} className="flex items-start gap-2 text-xs text-slate-600">
                  <span className="text-amber-500 font-bold">•</span>
                  <span>{f}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
