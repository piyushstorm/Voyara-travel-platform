import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBookingByReference, cancelBooking } from '../api/bookingApi';
import { reviewApi } from '../api/phase3Api';
import CancellationFlow from '../components/CancellationFlow';
import RefundStatusTracker from '../components/RefundStatusTracker';

const STATUS_STYLES = {
  CONFIRMED: 'bg-green-100 text-green-700 border-green-200',
  PENDING: 'bg-amber-100 text-amber-700 border-amber-200',
  CANCELLED: 'bg-red-100 text-red-700 border-red-200',
  COMPLETED: 'bg-blue-100 text-blue-700 border-blue-200',
};

function Section({ title, children, className = '' }) {
  return (
    <div className={`bg-white rounded-2xl shadow-sm border border-gray-100 p-5 sm:p-6 ${className}`}>
      {title && <h3 className="font-bold text-gray-900 mb-4">{title}</h3>}
      {children}
    </div>
  );
}

function InfoRow({ label, value, bold = false, mono = false }) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
      <span className="text-sm text-gray-500">{label}</span>
      <span className={`text-sm ${bold ? 'font-bold' : 'font-medium'} ${mono ? 'font-mono' : ''} text-gray-900`}>{value || '—'}</span>
    </div>
  );
}

export default function TripDetail() {
  const { reference } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showRefundTracker, setShowRefundTracker] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [toast, setToast] = useState(null);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewRating, setReviewRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [reviewHoverRating, setReviewHoverRating] = useState(0);
  const [reviewSubmitted, setReviewSubmitted] = useState(false);
  const [reviewError, setReviewError] = useState('');
  const [reviewLoading, setReviewLoading] = useState(false);

  const { data: booking, isLoading, error } = useQuery({
    queryKey: ['booking', reference],
    queryFn: () => getBookingByReference(reference),
    enabled: !!reference,
  });

  const cancelMutation = useMutation({
    mutationFn: (reason) => cancelBooking(reference, reason),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      setToast({ type: 'success', message: 'Booking cancelled successfully' });
      setShowCancelModal(false);
      setCancelReason('');
    },
    onError: (err) => {
      setToast({ type: 'error', message: err.response?.data?.message || 'Failed to cancel booking' });
    },
  });

  const { data: reviewEligibility } = useQuery({
    queryKey: ['reviewEligibility', booking?.id],
    queryFn: () => reviewApi.checkEligibility(booking.id).then(r => r.data),
    enabled: !!booking?.id,
  });

  const handleReviewSubmit = async () => {
    if (reviewRating === 0) { setReviewError('Please select a rating'); return; }
    if (!reviewText.trim()) { setReviewError('Please write a review'); return; }
    setReviewLoading(true);
    setReviewError('');
    try {
      const body = { rating: reviewRating, text: reviewText.trim() };
      if (reviewEligibility?.targetType === 'FLIGHT') body.flightId = reviewEligibility.targetId;
      if (reviewEligibility?.targetType === 'HOTEL') body.hotelId = reviewEligibility.targetId;
      await reviewApi.create(body);
      setReviewSubmitted(true);
      setShowReviewForm(false);
      queryClient.invalidateQueries({ queryKey: ['reviewEligibility', booking?.id] });
    } catch (err) {
      setReviewError(err.response?.data?.message || 'Failed to submit review');
    } finally {
      setReviewLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-8 space-y-4 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-48" />
        <div className="h-32 bg-gray-200 rounded-2xl" />
        <div className="h-48 bg-gray-200 rounded-2xl" />
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="text-5xl mb-4">🔍</div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Booking not found</h2>
        <p className="text-gray-500 text-sm mb-6">We couldn't find a booking with reference {reference}.</p>
        <Link to="/dashboard" className="bg-primary text-white px-6 py-3 rounded-xl text-sm font-semibold hover:bg-blue-700 transition">Back to My Trips</Link>
      </div>
    );
  }

  const isFlight = booking.bookingType === 'FLIGHT';
  const depTime = booking.departureTime ? new Date(booking.departureTime) : null;
  const arrTime = booking.arrivalTime ? new Date(booking.arrivalTime) : null;
  const dur = booking.durationMinutes || 0;
  const createdAt = booking.createdAt ? new Date(booking.createdAt) : null;
  const checkIn = booking.checkInDate ? new Date(booking.checkInDate) : null;
  const checkOut = booking.checkOutDate ? new Date(booking.checkOutDate) : null;

  const handlePrint = () => window.print();

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 sm:py-8">
      {toast && (
        <div className={`fixed top-20 right-4 z-[100] px-4 py-3 rounded-xl border shadow-lg text-sm font-medium ${
          toast.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'
        }`}>
          {toast.message}
        </div>
      )}

      {/* Back */}
      <button onClick={() => navigate('/dashboard')} className="flex items-center gap-2 text-sm text-gray-500 hover:text-gray-700 mb-6 transition">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        Back to My Trips
      </button>

      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">
            {isFlight ? `${booking.airlineName} · ${booking.flightNumber}` : booking.hotelName}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Booking Reference: <span className="font-mono font-semibold text-gray-700">{booking.bookingReference}</span>
          </p>
          {createdAt && <p className="text-xs text-gray-400 mt-1">Booked on {createdAt.toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}</p>}
        </div>
        <span className={`px-3 py-1.5 rounded-full text-xs font-bold border ${STATUS_STYLES[booking.status] || ''}`}>
          {booking.status}
        </span>
      </div>

      {/* Flight Itinerary */}
      {isFlight && (
        <Section title="Flight Itinerary" className="mb-4">
          <div className="flex items-center justify-between py-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-gray-900">{booking.originCode}</p>
              {depTime && (
                <>
                  <p className="text-sm text-gray-500 mt-1">{depTime.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}</p>
                  <p className="text-xs text-gray-400">{depTime.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</p>
                </>
              )}
            </div>
            <div className="flex-1 mx-6 flex flex-col items-center">
              <p className="text-xs text-gray-400 mb-2">{Math.floor(dur / 60)}h {dur % 60}m</p>
              <div className="w-full flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-primary" />
                <div className="flex-1 h-px bg-gray-300 border-dashed border-t-2 border-gray-300" />
                <span className="text-sm">✈️</span>
                <div className="flex-1 h-px bg-gray-300 border-dashed border-t-2 border-gray-300" />
                <div className="w-2 h-2 rounded-full bg-primary" />
              </div>
              <p className="text-xs text-gray-400 mt-2">{booking.stops === 0 ? 'Non-stop' : `${booking.stops} stop${booking.stops > 1 ? 's' : ''}`}</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-gray-900">{booking.destinationCode}</p>
              {arrTime && (
                <>
                  <p className="text-sm text-gray-500 mt-1">{arrTime.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}</p>
                  <p className="text-xs text-gray-400">{arrTime.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</p>
                </>
              )}
            </div>
          </div>
        </Section>
      )}

      {/* Hotel Details */}
      {!isFlight && (
        <Section title="Hotel Details" className="mb-4">
          <div className="grid grid-cols-2 gap-4">
            {checkIn && (
              <div>
                <p className="text-xs text-gray-400 uppercase">Check-in</p>
                <p className="font-medium text-gray-900">{checkIn.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</p>
              </div>
            )}
            {checkOut && (
              <div>
                <p className="text-xs text-gray-400 uppercase">Check-out</p>
                <p className="font-medium text-gray-900">{checkOut.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</p>
              </div>
            )}
          </div>
          <div className="mt-3 pt-3 border-t border-gray-100 grid grid-cols-3 gap-4 text-sm">
            <div><span className="text-gray-400">Room</span><p className="font-medium">{booking.roomName || booking.roomType || '—'}</p></div>
            <div><span className="text-gray-400">Nights</span><p className="font-medium">{booking.numberOfNights || 1}</p></div>
            {booking.specialRequests && <div><span className="text-gray-400">Special Requests</span><p className="font-medium">{booking.specialRequests}</p></div>}
          </div>
        </Section>
      )}

      {/* Booking Info */}
      <Section className="mb-4">
        <InfoRow label="Booking Reference" value={booking.bookingReference} mono bold />
        <InfoRow label="Status" value={booking.status} bold />
        <InfoRow label="Booking Type" value={booking.bookingType} />
        {isFlight && <InfoRow label="Cabin Class" value={booking.cabinClass?.replace('_', ' ')} />}
        {isFlight && booking.seatNumbers && <InfoRow label="Seat(s)" value={booking.seatNumbers} />}
        {isFlight && booking.fareType && <InfoRow label="Fare Type" value={booking.fareType} />}
        {isFlight && <InfoRow label="Passengers" value={`${booking.passengerCount} traveller${booking.passengerCount !== 1 ? 's' : ''}`} />}
      </Section>

      {/* Payment Summary */}
      <Section title="Payment Summary" className="mb-4">
        <div className="space-y-2 text-sm">
          {booking.baseFare && (
            <div className="flex justify-between">
              <span className="text-gray-500">Base fare</span>
              <span>₹{Number(booking.baseFare).toLocaleString()}</span>
            </div>
          )}
          {booking.addOnsTotal && Number(booking.addOnsTotal) > 0 && (
            <div className="flex justify-between">
              <span className="text-gray-500">Add-ons</span>
              <span>₹{Number(booking.addOnsTotal).toLocaleString()}</span>
            </div>
          )}
          {booking.baseFare && (
            <div className="flex justify-between">
              <span className="text-gray-500">Taxes & fees</span>
              <span>₹{(Number(booking.totalAmount) - Number(booking.baseFare) - Number(booking.addOnsTotal || 0)).toLocaleString()}</span>
            </div>
          )}
          <div className="flex justify-between font-bold text-lg pt-2 border-t border-gray-200">
            <span>Total Paid</span>
            <span className="text-primary">₹{booking.totalAmount?.toLocaleString()}</span>
          </div>
        </div>
        {booking.paymentId && (
          <div className="mt-4 pt-3 border-t border-gray-100 space-y-1 text-xs text-gray-500">
            {booking.paymentMethod && <p>Payment Method: {booking.paymentMethod}</p>}
            <p>Transaction ID: {booking.paymentId}</p>
            {booking.paymentStatus && <p>Payment Status: {booking.paymentStatus}</p>}
          </div>
        )}
        {booking.status === 'CANCELLED' && booking.refundAmount && (
          <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-xl text-sm">
            <p className="text-green-700 font-semibold">Refund: ₹{Number(booking.refundAmount).toLocaleString()}</p>
            {booking.cancellationReason && <p className="text-xs text-green-600 mt-1">Reason: {booking.cancellationReason}</p>}
          </div>
        )}
      </Section>

      {/* Actions */}
      {booking.status === 'CONFIRMED' && (
        <div className="flex gap-3 flex-wrap">
          {isFlight && (
            <Link to="/live-tracker"
              className="bg-primary text-white px-6 py-3 rounded-xl text-sm font-semibold hover:bg-blue-700 transition-colors flex items-center gap-2">
              📡 Track Flight
            </Link>
          )}
          <button onClick={handlePrint}
            className="bg-gray-100 text-gray-700 px-6 py-3 rounded-xl text-sm font-semibold hover:bg-gray-200 transition-colors">
            🖨️ Print Itinerary
          </button>
          <button onClick={() => setShowCancelModal(true)}
            className="bg-red-50 text-red-600 px-6 py-3 rounded-xl text-sm font-semibold hover:bg-red-100 transition-colors">
            Cancel Booking
          </button>
        </div>
      )}

      {booking.status === 'CANCELLED' && (
        <div className="flex gap-3 flex-wrap">
          <button onClick={() => setShowRefundTracker(true)}
            className="bg-indigo-600 text-white px-6 py-3 rounded-xl text-sm font-semibold hover:bg-indigo-700 transition-colors flex items-center gap-2 shadow-sm">
            💳 Track Refund
          </button>
        </div>
      )}

      {/* Review Prompt */}
      {(booking.status === 'COMPLETED' || booking.status === 'CONFIRMED') && reviewEligibility && (
        <div className="mt-6">
          {reviewSubmitted || reviewEligibility.reason === 'ALREADY_REVIEWED' ? (
            <div className="bg-green-50 border border-green-200 rounded-2xl p-5 flex items-center gap-4">
              <div className="text-3xl">✅</div>
              <div>
                <p className="font-semibold text-green-800">Review submitted</p>
                <p className="text-sm text-green-600">Thank you for sharing your experience with other travelers.</p>
              </div>
            </div>
          ) : reviewEligibility.eligible && !showReviewForm ? (
            <button
              onClick={() => setShowReviewForm(true)}
              className="w-full bg-white border border-gray-200 rounded-2xl p-5 text-left hover:shadow-md hover:border-blue-200 transition-all group"
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-bold text-gray-900 group-hover:text-blue-600 transition-colors">
                    How was your {isFlight ? 'flight' : 'stay'}? ⭐
                  </p>
                  <p className="text-sm text-gray-500 mt-1">Share your experience with other travelers</p>
                  <div className="flex gap-0.5 mt-2">
                    {[1, 2, 3, 4, 5].map(s => (
                      <span key={s} className="text-xl text-gray-300">☆</span>
                    ))}
                  </div>
                </div>
                <span className="text-sm text-blue-600 font-semibold group-hover:translate-x-1 transition-transform">
                  Write a Review →
                </span>
              </div>
            </button>
          ) : showReviewForm ? (
            <div className="bg-white border border-gray-200 rounded-2xl p-5">
              <p className="font-bold text-gray-900 mb-4">
                Review your {isFlight ? 'flight experience' : 'stay'}
              </p>
              {/* Star rating */}
              <div className="mb-4">
                <p className="text-sm text-gray-600 mb-2">Your rating</p>
                <div className="flex gap-1">
                  {[1, 2, 3, 4, 5].map(star => (
                    <button
                      key={star}
                      type="button"
                      onMouseEnter={() => setReviewHoverRating(star)}
                      onMouseLeave={() => setReviewHoverRating(0)}
                      onClick={() => setReviewRating(star)}
                      className="text-3xl transition-colors"
                    >
                      {(reviewHoverRating || reviewRating) >= star ? '★' : '☆'}
                    </button>
                  ))}
                </div>
                {reviewRating > 0 && (
                  <p className="text-xs text-gray-500 mt-1">
                    {reviewRating === 1 ? 'Poor' : reviewRating === 2 ? 'Fair' : reviewRating === 3 ? 'Good' : reviewRating === 4 ? 'Very Good' : 'Excellent'}
                  </p>
                )}
              </div>
              <textarea
                value={reviewText}
                onChange={e => setReviewText(e.target.value)}
                placeholder="What did you like? What could be improved?"
                rows={3}
                maxLength={2000}
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary outline-none resize-none"
              />
              <p className="text-xs text-gray-400 mt-1">{reviewText.length}/2000</p>
              {reviewError && <p className="text-red-500 text-sm mt-2">{reviewError}</p>}
              <div className="flex gap-3 mt-4">
                <button onClick={() => { setShowReviewForm(false); setReviewRating(0); setReviewText(''); setReviewError(''); }}
                  className="px-4 py-2.5 border border-gray-200 rounded-xl text-sm font-semibold text-gray-700 hover:bg-gray-50 transition">
                  Cancel
                </button>
                <button onClick={handleReviewSubmit} disabled={reviewLoading || reviewRating === 0}
                  className="flex-1 bg-primary text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition">
                  {reviewLoading ? 'Submitting...' : 'Submit Review'}
                </button>
              </div>
            </div>
          ) : null}
        </div>
      )}

      {/* Cancellation Flow Modal */}
      {showCancelModal && (
        <CancellationFlow
          booking={booking}
          onCancel={() => {
            queryClient.invalidateQueries(['booking', reference]);
            setShowCancelModal(false);
          }}
          onClose={() => setShowCancelModal(false)}
        />
      )}

      {/* Refund Status Tracker Modal */}
      {showRefundTracker && (
        <RefundStatusTracker
          refundId={`REF-${booking.bookingReference?.replace('TP-', '')}`}
          initialData={{
            refundAmount: booking.refundAmount,
            originalAmount: booking.totalAmount,
            status: 'PENDING',
            cancellationReason: booking.cancellationReason,
          }}
          onClose={() => setShowRefundTracker(false)}
        />
      )}
    </div>
  );
}
