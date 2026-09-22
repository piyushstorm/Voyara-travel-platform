import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { cancelBooking } from '../api/bookingApi';
import { previewRefund, getCancellationReasons } from '../api/refundApi';
import RefundStatusTracker from './RefundStatusTracker';

const DEFAULT_REASONS = [
  { code: 'CHANGE_OF_PLANS', label: 'Change of plans', description: 'Personal change in travel plans' },
  { code: 'TRAVEL_DATE_CHANGED', label: 'Travel dates changed', description: 'Dates shifted or rescheduled' },
  { code: 'FOUND_BETTER_PRICE', label: 'Found a better deal', description: 'Found better pricing elsewhere' },
  { code: 'BOOKED_BY_MISTAKE', label: 'Booked by mistake', description: 'Accidental booking or incorrect details' },
  { code: 'MEDICAL_EMERGENCY', label: 'Medical emergency', description: 'Medical issue or health concern' },
  { code: 'PERSONAL_REASONS', label: 'Personal reasons', description: 'Personal or family emergency' },
  { code: 'VISA_ISSUE', label: 'Visa or travel document issue', description: 'Visa rejection or delay' },
  { code: 'FLIGHT_OR_TRANSPORT_ISSUE', label: 'Transport schedule issue', description: 'Carrier cancellation or schedule shift' },
  { code: 'DUPLICATE_BOOKING', label: 'Duplicate booking', description: 'Booked the same itinerary twice' },
  { code: 'OTHER', label: 'Other', description: 'Other unforeseen reasons' },
];

export default function CancellationFlow({ booking, onCancel, onClose }) {
  const [step, setStep] = useState('confirm'); // confirm → reason → processing → done
  const [reason, setReason] = useState('');
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [showTracker, setShowTracker] = useState(false);

  const { data: preview, isLoading: isLoadingPreview } = useQuery({
    queryKey: ['refundPreview', booking?.id],
    queryFn: () => previewRefund(booking.id),
    enabled: !!booking?.id,
  });

  const { data: reasonsData } = useQuery({
    queryKey: ['cancellationReasons'],
    queryFn: getCancellationReasons,
    staleTime: 600000,
  });

  const reasonsList = reasonsData && reasonsData.length > 0 ? reasonsData : DEFAULT_REASONS;

  if (!booking) return null;

  const isFlight = booking.bookingType === 'FLIGHT';
  const totalAmount = Number(booking.totalAmount || preview?.originalAmount || 0);

  const refundAmount = Number(preview?.refundAmount ?? 0);
  const refundPercentage = Number(preview?.eligibleRefundPercentage ?? preview?.refundPercentage ?? (totalAmount > 0 ? (refundAmount / totalAmount) * 100 : 0));
  const nonRefundable = Number(preview?.nonRefundableAmount ?? Math.max(0, totalAmount - refundAmount));
  const cancellationFee = Number(preview?.cancellationFee ?? 0);
  const policyApplied = preview?.policyApplied || preview?.policyName || 'Standard Policy';
  const policyExplanation = preview?.policyExplanation || 'Cancellation refund calculated deterministically by server policy.';
  const expectedTimeline = preview?.expectedTimeline || 'Expected within 5–7 business days';

  const handleCancel = async () => {
    if (!reason) {
      setError('Please select a cancellation reason');
      return;
    }
    setStep('processing');
    setLoading(true);
    setError('');

    try {
      const res = await cancelBooking(booking.bookingReference, reason, comment);
      setResult(res);
      setStep('done');
      if (onCancel) onCancel(res);
    } catch (e) {
      setError(e.response?.data?.message || 'Cancellation failed. Please try again.');
      setStep('reason');
    } finally {
      setLoading(false);
    }
  };

  if (showTracker && result?.refundAmount !== undefined) {
    return (
      <RefundStatusTracker
        refundId={result?.refundId || `REF-${booking.bookingReference?.replace('TP-', '')}`}
        initialData={{
          refundId: result?.refundId,
          refundAmount: result?.refundAmount,
          originalAmount: totalAmount,
          refundPercentage: refundPercentage,
          status: 'PENDING',
          cancellationReason: reason,
          expectedTimeline: expectedTimeline,
        }}
        onClose={onClose}
      />
    );
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden border border-slate-100">
        {/* Header */}
        <div className="bg-gradient-to-r from-rose-600 via-red-600 to-amber-600 text-white p-6 relative">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-xl font-black tracking-tight">Cancel Booking</h3>
              <p className="text-white/80 text-xs mt-0.5 font-mono">Ref: {booking.bookingReference}</p>
            </div>
            <button
              onClick={onClose}
              className="text-white/80 hover:text-white text-2xl leading-none transition"
            >
              &times;
            </button>
          </div>
        </div>

        <div className="p-6">
          {/* STEP 1: REVIEW POLICY & BREAKDOWN */}
          {step === 'confirm' && (
            <div className="space-y-5">
              {/* Trip Context Box */}
              <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4">
                <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  {isFlight ? 'Flight Booking' : `${booking.bookingType || 'Travel'} Booking`}
                </p>
                <p className="font-bold text-slate-900 mt-0.5 text-base">
                  {isFlight ? `${booking.originCode} → ${booking.destinationCode}` : (booking.hotelName || booking.bookingType)}
                </p>
                <p className="text-xs text-slate-500 mt-1">
                  Total Paid: <span className="font-black text-slate-900">₹{totalAmount.toLocaleString()}</span>
                </p>
              </div>

              {/* Policy & Refund Transparency Card */}
              <div className="rounded-2xl border border-slate-200/80 bg-slate-50/50 p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-600">Applicable Policy</span>
                  <span className="text-xs font-black text-indigo-700 bg-indigo-50 px-2.5 py-0.5 rounded-full border border-indigo-100">
                    {isLoadingPreview ? 'Calculating...' : policyApplied}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-200/60">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400">Eligible Refund</span>
                    <p className="text-xl font-black text-emerald-600 mt-0.5">
                      {refundAmount > 0 ? `₹${refundAmount.toLocaleString()}` : '₹0'}
                      <span className="text-xs font-semibold text-emerald-700 ml-1.5">
                        ({refundPercentage.toFixed(0)}%)
                      </span>
                    </p>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400">Non-Refundable</span>
                    <p className="text-xl font-black text-slate-700 mt-0.5">
                      ₹{nonRefundable.toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="bg-white/80 rounded-xl p-3 text-[11px] text-slate-600 space-y-1 border border-slate-100">
                  <p><span className="font-bold text-slate-700">Policy: </span>{policyExplanation}</p>
                  <p><span className="font-bold text-slate-700">Timeline: </span>{expectedTimeline}</p>
                  {cancellationFee > 0 && (
                    <p className="text-amber-700"><span className="font-bold">Cancellation Fee: </span>₹{cancellationFee.toLocaleString()}</p>
                  )}
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  onClick={onClose}
                  className="flex-1 px-4 py-3 border border-slate-300 rounded-xl text-slate-700 text-sm font-bold hover:bg-slate-50 transition"
                >
                  Keep Booking
                </button>
                <button
                  onClick={() => setStep('reason')}
                  className="flex-1 px-4 py-3 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-sm font-bold shadow-md transition"
                >
                  Continue to Cancel
                </button>
              </div>
            </div>
          )}

          {/* STEP 2: SELECT PREDEFINED REASON & COMMENT */}
          {step === 'reason' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1">
                  Cancellation Reason <span className="text-rose-500">*</span>
                </label>
                <select
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full px-3.5 py-3 rounded-xl border border-slate-200 text-slate-800 text-sm font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none bg-slate-50"
                >
                  <option value="">-- Select a reason --</option>
                  {reasonsList.map((r) => (
                    <option key={r.code} value={r.code}>
                      {r.label}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-400 mt-1">
                  Required by policy to track trends and process appropriate refunds.
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1">
                  Additional Details / Comment <span className="text-slate-400 font-normal">(Optional)</span>
                </label>
                <textarea
                  rows={3}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Provide any additional context regarding your cancellation..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-slate-800 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  maxLength={500}
                />
              </div>

              {error && (
                <div className="bg-rose-50 border border-rose-200 rounded-xl p-3 text-xs text-rose-700 font-medium">
                  {error}
                </div>
              )}

              <div className="flex gap-3 pt-3">
                <button
                  onClick={() => setStep('confirm')}
                  className="flex-1 px-4 py-3 border border-slate-300 rounded-xl text-slate-700 text-sm font-bold hover:bg-slate-50 transition"
                >
                  Back
                </button>
                <button
                  onClick={handleCancel}
                  disabled={loading || !reason}
                  className="flex-1 px-4 py-3 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-sm font-bold shadow-md transition disabled:opacity-50"
                >
                  {loading ? 'Cancelling...' : 'Confirm Cancellation'}
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: PROCESSING SPINNER */}
          {step === 'processing' && (
            <div className="text-center py-10 space-y-4">
              <div className="animate-spin w-12 h-12 border-4 border-rose-500 border-t-transparent rounded-full mx-auto" />
              <div>
                <h4 className="font-bold text-slate-900 text-base">Processing Cancellation</h4>
                <p className="text-xs text-slate-500 mt-1">Applying policy rules and generating authoritative refund record...</p>
              </div>
            </div>
          )}

          {/* STEP 4: SUCCESS & STATUS LAUNCH */}
          {step === 'done' && (
            <div className="text-center py-4 space-y-4">
              <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto text-3xl font-black shadow-inner">
                ✓
              </div>
              <div>
                <h4 className="text-lg font-black text-slate-900">Booking Cancelled Successfully</h4>
                <p className="text-xs text-slate-500 mt-1">
                  {refundAmount > 0
                    ? `Eligible for a ₹${refundAmount.toLocaleString()} refund (${refundPercentage.toFixed(0)}%).`
                    : 'No refund is applicable for this booking based on policy rules.'}
                </p>
                <p className="text-[11px] text-indigo-700 font-bold mt-1">
                  Timeline: {expectedTimeline}
                </p>
              </div>

              <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100 text-left text-xs space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-400">Booking Reference:</span>
                  <span className="font-mono font-bold text-slate-700">{booking.bookingReference}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Refund Status:</span>
                  <span className="font-bold text-amber-600">PENDING / PROCESSING</span>
                </div>
                {result?.refundId && (
                  <div className="flex justify-between">
                    <span className="text-slate-400">Refund Reference:</span>
                    <span className="font-mono text-slate-700">{result.refundId}</span>
                  </div>
                )}
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  onClick={() => setShowTracker(true)}
                  className="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-sm"
                >
                  Track Refund Progress
                </button>
                <button
                  onClick={onClose}
                  className="flex-1 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition"
                >
                  Done
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
