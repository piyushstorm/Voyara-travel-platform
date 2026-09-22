import React, { useState } from 'react';
import ReviewReportModal from './reviews/ReviewReportModal';
import ReviewPhotoLightbox from './reviews/ReviewPhotoLightbox';

export default function ReviewList({ reviews, onVote, onFlag, onReply }) {
  const [sortBy, setSortBy] = useState('NEWEST');
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [reportReviewId, setReportReviewId] = useState(null);
  const [lightboxPhotos, setLightboxPhotos] = useState([]);
  const [lightboxIdx, setLightboxIdx] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  const sortOptions = [
    { key: 'NEWEST', label: 'Newest First' },
    { key: 'HELPFUL', label: 'Most Helpful' },
    { key: 'RATING', label: 'Highest Rated' },
  ];

  if (!reviews || reviews.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        <div className="text-3xl mb-2">📝</div>
        <p>No reviews yet. Be the first to review!</p>
      </div>
    );
  }

  const sortedReviews = [...reviews].sort((a, b) => {
    if (sortBy === 'HELPFUL') return (b.helpfulCount || 0) - (a.helpfulCount || 0);
    if (sortBy === 'RATING') return (b.rating || 0) - (a.rating || 0);
    return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
  });

  return (
    <div>
      {/* Sort bar */}
      <div className="flex items-center gap-2 mb-4">
        <span className="text-sm text-gray-500">Sort by:</span>
        {sortOptions.map((opt) => (
          <button
            key={opt.key}
            onClick={() => setSortBy(opt.key)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition ${
              sortBy === opt.key
                ? 'bg-primary text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* Reviews */}
      <div className="space-y-4">
        {sortedReviews.map((review) => {
          const authorName = review.userName || review.user?.name || 'Anonymous';
          const authorInitial = authorName.charAt(0).toUpperCase();

          return (
            <div key={review.id} className="bg-white rounded-xl p-5 border border-gray-100 shadow-sm">
              {/* Header */}
              <div className="flex items-start justify-between mb-2">
                <div>
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-sm font-bold">
                      {authorInitial}
                    </div>
                    <div>
                      <span className="font-medium text-sm text-gray-900">{authorName}</span>
                      {review.verifiedBooking && (
                        <span className="ml-2 text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">
                          ✓ Verified Traveller
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                <span className="text-xs text-gray-400">
                  {review.createdAt ? new Date(review.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : ''}
                </span>
              </div>

              {/* Stars */}
              <div className="flex items-center gap-1 mb-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <span key={star} className={`text-lg ${star <= review.rating ? 'text-yellow-400' : 'text-gray-300'}`}>
                    {star <= review.rating ? '★' : '☆'}
                  </span>
                ))}
              </div>

              {/* Title & Text */}
              {review.title && <h4 className="font-bold text-gray-900 text-sm mb-1">{review.title}</h4>}
              {review.text && (
                <p className="text-sm text-gray-700 mb-3 leading-relaxed">{review.text}</p>
              )}

              {/* Photos */}
              {review.photos && review.photos.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-3">
                  {review.photos.map((p, idx) => (
                    <button
                      key={p.id || idx}
                      type="button"
                      onClick={() => {
                        setLightboxPhotos(review.photos);
                        setLightboxIdx(idx);
                        setLightboxOpen(true);
                      }}
                      className="w-16 h-16 rounded-lg overflow-hidden border border-gray-200 hover:opacity-90"
                    >
                      <img src={p.photoUrl} alt="" className="w-full h-full object-cover" />
                    </button>
                  ))}
                </div>
              )}

              {/* Actions */}
              <div className="flex items-center gap-4 text-xs">
                <button
                  onClick={() => onVote && onVote(review.id, 'HELPFUL')}
                  className="flex items-center gap-1 text-gray-500 hover:text-green-600 transition"
                >
                  👍 Helpful ({review.helpfulCount || 0})
                </button>
                <button
                  onClick={() => onVote && onVote(review.id, 'NOT_HELPFUL')}
                  className="flex items-center gap-1 text-gray-500 hover:text-red-600 transition"
                >
                  👎 ({review.notHelpfulCount || 0})
                </button>
                <button
                  onClick={() => setReplyingTo(replyingTo === review.id ? null : review.id)}
                  className="text-gray-500 hover:text-primary transition"
                >
                  💬 Reply ({review.replies?.length || 0})
                </button>
                <button
                  onClick={() => setReportReviewId(review.id)}
                  className="text-gray-500 hover:text-red-600 transition"
                >
                  🚩 Report
                </button>
              </div>

              {/* Reply form */}
              {replyingTo === review.id && (
                <div className="mt-3 flex gap-2">
                  <input
                    type="text"
                    value={replyText}
                    onChange={(e) => setReplyText(e.target.value)}
                    placeholder="Write a reply..."
                    className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-1 focus:ring-primary outline-none"
                  />
                  <button
                    onClick={() => {
                      if (replyText.trim() && onReply) {
                        onReply(review.id, replyText.trim());
                        setReplyText('');
                        setReplyingTo(null);
                      }
                    }}
                    className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium"
                  >
                    Reply
                  </button>
                </div>
              )}

              {/* Replies list */}
              {review.replies && review.replies.length > 0 && (
                <div className="mt-3 space-y-2 pl-3 border-l-2 border-primary/20 text-xs">
                  {review.replies.map((rep) => (
                    <div key={rep.id} className="bg-gray-50 p-2.5 rounded-lg">
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-semibold text-gray-800">{rep.userName || 'User'}</span>
                        <span className="text-[10px] text-gray-400">
                          {rep.createdAt ? new Date(rep.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : ''}
                        </span>
                      </div>
                      <p className="text-gray-700">{rep.text}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Lightbox */}
      <ReviewPhotoLightbox
        photos={lightboxPhotos}
        initialIndex={lightboxIdx}
        isOpen={lightboxOpen}
        onClose={() => setLightboxOpen(false)}
      />

      {/* Report Modal */}
      <ReviewReportModal
        reviewId={reportReviewId}
        isOpen={!!reportReviewId}
        onClose={() => setReportReviewId(null)}
        onReportSuccess={() => {
          if (onFlag) onFlag(reportReviewId, 'REPORTED');
          setReportReviewId(null);
        }}
      />
    </div>
  );
}
