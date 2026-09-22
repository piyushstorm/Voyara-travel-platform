import React, { useState, useEffect, useCallback } from 'react';
import reviewApi from '../../api/reviewApi';
import RatingSummaryCard from './RatingSummaryCard';
import ReviewItem from './ReviewItem';
import ReviewComposer from './ReviewComposer';
import { useAuth } from '../../context/AuthContext';

export default function ReviewSection({
  targetType = 'HOTEL', // 'HOTEL' or 'FLIGHT'
  targetId,
  targetName = '',
}) {
  const { user } = useAuth();

  const [summary, setSummary] = useState({
    averageRating: 0,
    totalReviews: 0,
    ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
  });
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & sorting
  const [sortBy, setSortBy] = useState('NEWEST');
  const [selectedRating, setSelectedRating] = useState(null);
  const [verifiedOnly, setVerifiedOnly] = useState(false);
  const [hasPhotos, setHasPhotos] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Composer modal
  const [composerOpen, setComposerOpen] = useState(false);

  // Fetch rating summary
  const fetchSummary = useCallback(async () => {
    if (!targetId) return;
    try {
      const res =
        targetType === 'FLIGHT'
          ? await reviewApi.getFlightRatingSummary(targetId)
          : await reviewApi.getHotelRatingSummary(targetId);
      setSummary(res.data);
    } catch (err) {
      console.error('Failed to fetch rating summary:', err);
    }
  }, [targetType, targetId]);

  // Fetch reviews page
  const fetchReviews = useCallback(async () => {
    if (!targetId) return;
    setLoading(true);
    setError(null);

    try {
      const params = {
        sortBy,
        page,
        size: 10,
      };
      if (selectedRating !== null) params.rating = selectedRating;
      if (verifiedOnly) params.verifiedOnly = true;
      if (hasPhotos) params.hasPhotos = true;

      const res =
        targetType === 'FLIGHT'
          ? await reviewApi.getFlightReviews(targetId, params)
          : await reviewApi.getHotelReviews(targetId, params);

      const pageData = res.data;
      setReviews(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      setTotalElements(pageData.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load reviews');
    } finally {
      setLoading(false);
    }
  }, [targetType, targetId, sortBy, selectedRating, verifiedOnly, hasPhotos, page]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    fetchReviews();
  }, [fetchReviews]);

  const handleReviewSubmitted = () => {
    fetchSummary();
    setPage(0);
    fetchReviews();
  };

  return (
    <section aria-labelledby="reviews-section-heading" className="w-full">
      <div className="flex items-center justify-between mb-4">
        <h3 id="reviews-section-heading" className="text-xl font-bold text-gray-900 flex items-center gap-2">
          <span>Guest Reviews & Ratings</span>
          {totalElements > 0 && (
            <span className="text-xs font-semibold bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
              {totalElements}
            </span>
          )}
        </h3>
      </div>

      {/* Summary Card with Interactive Filters */}
      <RatingSummaryCard
        summary={summary}
        selectedRating={selectedRating}
        onSelectRating={(r) => {
          setSelectedRating(r);
          setPage(0);
        }}
        verifiedOnly={verifiedOnly}
        onToggleVerified={() => {
          setVerifiedOnly(!verifiedOnly);
          setPage(0);
        }}
        hasPhotos={hasPhotos}
        onTogglePhotos={() => {
          setHasPhotos(!hasPhotos);
          setPage(0);
        }}
        sortBy={sortBy}
        onSortChange={(s) => {
          setSortBy(s);
          setPage(0);
        }}
        onOpenComposer={() => setComposerOpen(true)}
        canReview={true}
      />

      {/* Reviews List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((n) => (
            <div key={n} className="bg-white rounded-2xl border border-gray-100 p-6 animate-pulse">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-full bg-gray-200" />
                <div className="space-y-1.5 flex-1">
                  <div className="h-3.5 bg-gray-200 rounded w-1/4" />
                  <div className="h-3 bg-gray-100 rounded w-1/6" />
                </div>
              </div>
              <div className="h-4 bg-gray-200 rounded w-1/3 mb-2" />
              <div className="h-3 bg-gray-100 rounded w-full mb-1" />
              <div className="h-3 bg-gray-100 rounded w-5/6" />
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-700 p-6 rounded-2xl text-center">
          <p className="font-semibold mb-2">{error}</p>
          <button
            onClick={fetchReviews}
            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-semibold rounded-xl transition"
          >
            Try Again
          </button>
        </div>
      ) : reviews.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-100 p-12 text-center shadow-sm">
          <div className="w-16 h-16 bg-blue-50 text-primary rounded-full flex items-center justify-center mx-auto mb-4 text-2xl">
            ✍️
          </div>
          <h4 className="text-base font-bold text-gray-900 mb-1">No reviews match your filter</h4>
          <p className="text-sm text-gray-500 max-w-md mx-auto mb-5">
            {selectedRating || verifiedOnly || hasPhotos
              ? 'Try resetting the filters above to see all guest feedback.'
              : 'Be the first traveller to share an authentic review and photos!'}
          </p>
          {(selectedRating || verifiedOnly || hasPhotos) && (
            <button
              onClick={() => {
                setSelectedRating(null);
                setVerifiedOnly(false);
                setHasPhotos(false);
                setPage(0);
              }}
              className="px-4 py-2 text-xs font-semibold text-primary bg-primary/10 hover:bg-primary/20 rounded-xl transition"
            >
              Clear All Filters
            </button>
          )}
        </div>
      ) : (
        <div>
          {reviews.map((rev) => (
            <ReviewItem
              key={rev.id}
              review={rev}
              currentUser={user}
              onReviewUpdated={fetchReviews}
            />
          ))}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 pb-2 px-2">
              <span className="text-xs text-gray-500 font-medium">
                Page {page + 1} of {totalPages} ({totalElements} total reviews)
              </span>
              <div className="flex items-center gap-2">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-3.5 py-1.5 text-xs font-semibold border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Previous
                </button>
                <button
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="px-3.5 py-1.5 text-xs font-semibold border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Review Composer Modal */}
      <ReviewComposer
        flightId={targetType === 'FLIGHT' ? targetId : null}
        hotelId={targetType === 'HOTEL' ? targetId : null}
        targetName={targetName}
        isOpen={composerOpen}
        onClose={() => setComposerOpen(false)}
        onSuccess={handleReviewSubmitted}
      />
    </section>
  );
}
