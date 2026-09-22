import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getRefundStatus } from '../api/refundApi';

const STATUS_STEPS = [
  { key: 'REQUESTED', label: 'Cancellation Requested', desc: 'Cancellation submitted by user' },
  { key: 'CALCULATED', label: 'Refund Calculated', desc: 'Authoritative policy amount determined' },
  { key: 'INITIATED', label: 'Refund Initiated', desc: 'Refund created in payment gateway' },
  { key: 'PROCESSING', label: 'Bank Processing', desc: 'Gateway / banking network clearance' },
  { key: 'COMPLETED', label: 'Refund Completed', desc: 'Funds credited to original payment method' },
];

export default function RefundStatusTracker({ refundId, initialData, onClose }) {
  const [activeRefundId, setActiveRefundId] = useState(refundId || initialData?.refundId);

  const { data: refund, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['refundStatus', activeRefundId],
    queryFn: () => getRefundStatus(activeRefundId),
    enabled: !!activeRefundId,
    initialData: initialData || undefined,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return (status === 'PENDING' || status === 'PROCESSING') ? 5000 : false;
    },
  });

  const currentStatus = refund?.status || 'PENDING';
  const isFailed = currentStatus === 'REJECTED' || currentStatus === 'FAILED';
  const isCompleted = currentStatus === 'COMPLETED';

  // Determine current active step index (0 to 4)
  const getStepIndex = () => {
    if (isFailed) return 2; // Failed at initiation/gateway
    if (isCompleted) return 4;
    if (currentStatus === 'PROCESSING') return 3;
    return 2; // PENDING: Requested & Calculated are complete, initiated
  };

  const currentStep = getStepIndex();

  const refundAmount = Number(refund?.refundAmount || 0);
  const originalAmount = Number(refund?.originalAmount || 0);
  const nonRefundable = Math.max(0, originalAmount - refundAmount);

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden border border-slate-100">
        {/* Header */}
        <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white p-6 relative">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-2xl bg-indigo-500/20 border border-indigo-400/30 flex items-center justify-center text-xl">
                💳
              </div>
              <div>
                <h3 className="text-lg font-bold">Refund Status Tracker</h3>
                <p className="text-xs text-slate-300 font-mono">Ref: {refund?.refundId || activeRefundId}</p>
              </div>
            </div>
            {onClose && (
              <button
                onClick={onClose}
                className="text-white/70 hover:text-white text-2xl leading-none transition"
              >
                &times;
              </button>
            )}
          </div>

          <div className="mt-4 flex items-center justify-between pt-3 border-t border-white/10">
            <div className="flex items-center gap-2">
              <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold ${
                isCompleted ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' :
                isFailed ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' :
                'bg-amber-500/20 text-amber-300 border border-amber-500/30 animate-pulse'
              }`}>
                ● {currentStatus}
              </span>
              {isFetching && <span className="text-[10px] text-indigo-300 animate-pulse">Syncing...</span>}
            </div>

            <button
              onClick={() => refetch()}
              disabled={isLoading || isFetching}
              className="text-xs text-indigo-300 hover:text-white flex items-center gap-1 bg-white/5 hover:bg-white/10 px-2.5 py-1 rounded-lg transition"
            >
              🔄 Refresh
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div className="p-6 space-y-6">
          {/* Amount Summary Cards */}
          <div className="grid grid-cols-3 gap-2.5">
            <div className="bg-slate-50 border border-slate-100 rounded-2xl p-3 text-center">
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400">Paid Amount</span>
              <p className="text-sm font-black text-slate-800 mt-0.5">₹{originalAmount.toLocaleString()}</p>
            </div>
            <div className="bg-emerald-50/70 border border-emerald-100 rounded-2xl p-3 text-center">
              <span className="text-[10px] uppercase font-bold tracking-wider text-emerald-600">Refund Amount</span>
              <p className="text-sm font-black text-emerald-700 mt-0.5">₹{refundAmount.toLocaleString()}</p>
            </div>
            <div className="bg-slate-50 border border-slate-100 rounded-2xl p-3 text-center">
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400">Deduction</span>
              <p className="text-sm font-black text-slate-700 mt-0.5">₹{nonRefundable.toLocaleString()}</p>
            </div>
          </div>

          {/* Stepper Progress Bar */}
          <div className="bg-slate-50/80 border border-slate-200/70 rounded-2xl p-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-4">Tracking Progress</h4>
            <div className="space-y-3">
              {STATUS_STEPS.map((step, idx) => {
                const isPassed = idx < currentStep;
                const isCurrent = idx === currentStep;
                const isUpcoming = idx > currentStep;

                return (
                  <div key={step.key} className="flex items-start gap-3">
                    <div className="flex flex-col items-center">
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                        isPassed ? 'bg-emerald-500 text-white' :
                        isCurrent && !isFailed ? 'bg-indigo-600 text-white ring-4 ring-indigo-100' :
                        isCurrent && isFailed ? 'bg-rose-500 text-white ring-4 ring-rose-100' :
                        'bg-slate-200 text-slate-500'
                      }`}>
                        {isPassed ? '✓' : idx + 1}
                      </div>
                      {idx < STATUS_STEPS.length - 1 && (
                        <div className={`w-0.5 h-6 mt-1 ${isPassed ? 'bg-emerald-400' : 'bg-slate-200'}`} />
                      )}
                    </div>
                    <div className="pt-0.5 min-w-0 flex-1">
                      <div className="flex items-center justify-between">
                        <p className={`text-xs font-bold ${
                          isCurrent ? 'text-indigo-900' :
                          isPassed ? 'text-slate-800' :
                          'text-slate-400'
                        }`}>
                          {step.label}
                        </p>
                        {isCurrent && !isCompleted && !isFailed && (
                          <span className="text-[10px] bg-indigo-50 text-indigo-700 font-semibold px-2 py-0.5 rounded-full border border-indigo-200/50 animate-pulse">
                            In Progress
                          </span>
                        )}
                        {isPassed && (
                          <span className="text-[10px] text-emerald-600 font-semibold">Complete</span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-500 mt-0.5 truncate">{step.desc}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Expected Timeline & Information */}
          <div className="bg-indigo-50/70 border border-indigo-100 rounded-2xl p-4 space-y-2 text-xs text-indigo-950">
            <div className="flex items-center gap-2">
              <span className="text-base">⏱️</span>
              <div>
                <span className="font-bold">Expected Timeline: </span>
                <span className="font-semibold text-indigo-800">
                  {refund?.expectedTimeline || 'Expected within 5–7 business days'}
                </span>
              </div>
            </div>
            {refund?.cancellationPolicy && (
              <div className="flex items-start gap-2 pt-1">
                <span className="text-base">📋</span>
                <div>
                  <span className="font-bold">Policy: </span>
                  <span className="text-slate-600">{refund.cancellationPolicy}</span>
                </div>
              </div>
            )}
            {refund?.cancellationReason && (
              <div className="flex items-start gap-2 pt-1">
                <span className="text-base">💬</span>
                <div>
                  <span className="font-bold">Reason: </span>
                  <span className="text-slate-600">{refund.cancellationReason.replace(/_/g, ' ')}</span>
                  {refund.cancellationComment && (
                    <span className="block text-slate-500 italic mt-0.5">"{refund.cancellationComment}"</span>
                  )}
                </div>
              </div>
            )}
            {refund?.externalRefundId && (
              <div className="flex items-center gap-2 pt-1">
                <span className="text-base">🔒</span>
                <div>
                  <span className="font-bold">Gateway Ref: </span>
                  <span className="font-mono text-[11px] text-slate-600">{refund.externalRefundId}</span>
                </div>
              </div>
            )}
          </div>

          {/* Error display if rejected */}
          {isFailed && (
            <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-xs text-rose-800">
              <p className="font-bold flex items-center gap-1.5 text-rose-900">
                <span>⚠️</span> Refund Failed or Rejected
              </p>
              <p className="mt-1 text-rose-700">
                {refund?.failureReason || refund?.rejectionReason || 'Please contact Voyara customer support.'}
              </p>
            </div>
          )}

          {/* Footer Action */}
          <div className="pt-2">
            <button
              onClick={onClose}
              className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-sm font-bold shadow-md transition"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
