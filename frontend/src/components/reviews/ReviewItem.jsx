import React, { useState } from 'react';
import reviewApi from '../../api/reviewApi';
import ReviewPhotoLightbox from './ReviewPhotoLightbox';
import ReviewReportModal from './ReviewReportModal';

export default function ReviewItem({
  review,
  currentUser,
  onReviewUpdated,
  onPhotoClick,
}) {
  const [isReplying, setIsReplying] = useState(false);
  const [replyText, setReplyText] = useState('');
  const [submittingReply, setSubmittingReply] = useState(false);
  const [replyError, setReplyError] = useState(null);
  const [reportModalOpen, setReportModalOpen] = useState(false);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [activePhotoIdx, setActivePhotoIdx] = useState(0);

  // Voting state
  const [helpfulCount, setHelpfulCount] = useState(review.helpfulCount || 0);
  const [hasVotedHelpful, setHasVotedHelpful] = useState(review.userVotedHelpful === true);
  const [isVoting, setIsVoting] = useState(false);

  const handleVote = async () => {
    if (!currentUser) {
      alert('Please sign in to vote on reviews');
      return;
    }
    if (isVoting) return;

    setIsVoting(true);
    try {
      await reviewApi.voteReview(review.id, 'HELPFUL');
      setHasVotedHelpful((prev) => {
        const next = !prev;
        setHelpfulCount((count) => (next ? count + 1 : Math.max(0, count - 1)));
        return next;
      });
      if (onReviewUpdated) onReviewUpdated();
    } catch (err) {
      console.error('Vote failed', err);
    } finally {
      setIsVoting(false);
    }
  };

  const handleReplySubmit = async (e) => {
    e.preventDefault();
    if (!replyText.trim()) return;
    setSubmittingReply(true);
    setReplyError(null);

    try {
      await reviewApi.addReply(review.id, replyText.trim());
      setReplyText('');
      setIsReplying(false);
      if (onReviewUpdated) onReviewUpdated();
    } catch (err) {
      setReplyError(err.response?.data?.message || err.message || 'Failed to submit reply');
    } finally {
      setSubmittingReply(false);
    }
  };

  const handleDeleteReply = async (replyId) => {
    if (!window.confirm('Are you sure you want to delete this reply?')) return;
    try {
      await reviewApi.deleteReply(replyId);
      if (onReviewUpdated) onReviewUpdated();
    } catch (err) {
      alert('Could not delete reply: ' + (err.response?.data?.message || err.message));
    }
  };

  const openPhoto = (idx) => {
    setActivePhotoIdx(idx);
    setLightboxOpen(true);
    if (onPhotoClick) onPhotoClick(review.photos, idx);
  };

  const authorName = review.userName || review.user?.name || 'Anonymous';
  const authorInitial = authorName.charAt(0).toUpperCase();
  const formattedDate = review.createdAt
    ? new Date(review.createdAt).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      })
    : '';

  return (
    <article className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 mb-4 transition hover:border-gray-200">
      {/* Author & Rating Header */}
      <div className="flex items-start justify-between gap-4 mb-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary to-blue-500 text-white font-bold flex items-center justify-center text-sm shadow-sm">
            {authorInitial}
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-bold text-gray-900 text-sm">{authorName}</span>
              {review.verifiedBooking && (
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/60 px-2 py-0.5 rounded-full">
                  <svg className="w-3 h-3 text-emerald-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                  Verified Traveller
                </span>
              )}
            </div>
            <div className="flex items-center gap-2 mt-0.5">
              <div className="flex items-center text-amber-400 text-sm">
                {[1, 2, 3, 4, 5].map((s) => (
                  <span key={s}>{s <= review.rating ? '★' : '☆'}</span>
                ))}
              </div>
              <span className="text-xs text-gray-400">• {formattedDate}</span>
            </div>
          </div>
        </div>

        {/* Report Action */}
        <button
          onClick={() => setReportModalOpen(true)}
          className="text-gray-400 hover:text-red-500 p-1.5 rounded-lg hover:bg-red-50 transition text-xs flex items-center gap-1"
          title="Report this review as inappropriate"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 21v-4m0 0V5a2 2 0 012-2h6.5l1 1H21l-3 6 3 6h-8.5l-1-1H5a2 2 0 00-2 2zm9-13.5V9" />
          </svg>
          <span className="hidden sm:inline">Report</span>
        </button>
      </div>

      {/* Review Title */}
      {review.title && (
        <h4 className="font-bold text-gray-900 text-base mb-1.5">{review.title}</h4>
      )}

      {/* Review Body */}
      <p className="text-gray-700 text-sm leading-relaxed mb-4 whitespace-pre-line">
        {review.text}
      </p>

      {/* Photos Grid */}
      {review.photos && review.photos.length > 0 && (
        <div className="flex flex-wrap gap-2.5 mb-4">
          {review.photos.map((photo, idx) => (
            <button
              key={photo.id || idx}
              type="button"
              onClick={() => openPhoto(idx)}
              className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-xl overflow-hidden border border-gray-200 group focus:outline-none focus:ring-2 focus:ring-primary shadow-sm hover:opacity-95 transition"
            >
              <img
                src={photo.photoUrl}
                alt={photo.caption || 'Review user photo'}
                className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                loading="lazy"
              />
              <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition flex items-center justify-center text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7" />
                </svg>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* Actions footer */}
      <div className="flex items-center gap-4 pt-3 border-t border-gray-100 text-xs">
        {/* Helpful button */}
        <button
          onClick={handleVote}
          disabled={isVoting}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition ${
            hasVotedHelpful
              ? 'bg-emerald-50 text-emerald-700 font-semibold ring-1 ring-emerald-300'
              : 'text-gray-600 hover:bg-gray-100'
          }`}
        >
          <svg className="w-4 h-4" fill={hasVotedHelpful ? 'currentColor' : 'none'} stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5" />
          </svg>
          Helpful ({helpfulCount})
        </button>

        {/* Reply button */}
        <button
          onClick={() => setIsReplying(!isReplying)}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-gray-600 hover:bg-gray-100 font-medium transition"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
          </svg>
          Reply ({review.replies?.length || 0})
        </button>
      </div>

      {/* Reply Composer */}
      {isReplying && (
        <form onSubmit={handleReplySubmit} className="mt-4 pt-4 border-t border-gray-100 animate-fadeIn">
          {replyError && (
            <div className="mb-2 p-2 bg-red-50 text-red-600 text-xs rounded-lg">{replyError}</div>
          )}
          <div className="flex gap-2">
            <input
              type="text"
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              placeholder="Write a public reply..."
              maxLength={1000}
              className="flex-1 text-sm border border-gray-300 rounded-xl px-3.5 py-2 outline-none focus:border-primary focus:ring-1 focus:ring-primary"
            />
            <button
              type="submit"
              disabled={submittingReply || !replyText.trim()}
              className="px-4 py-2 bg-primary hover:bg-primary-dark text-white rounded-xl text-xs font-semibold shadow-sm disabled:opacity-50 transition"
            >
              {submittingReply ? 'Posting...' : 'Reply'}
            </button>
            <button
              type="button"
              onClick={() => setIsReplying(false)}
              className="px-3 py-2 text-gray-500 hover:bg-gray-100 rounded-xl text-xs font-medium transition"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {/* Replies Thread */}
      {review.replies && review.replies.length > 0 && (
        <div className="mt-4 space-y-2.5 pl-4 border-l-2 border-primary/20">
          {review.replies.map((rep) => {
            const isAuthor = currentUser && (currentUser.id === rep.userId || currentUser.userId === rep.userId);
            const isAdmin = currentUser && currentUser.role === 'ADMIN';

            return (
              <div key={rep.id} className="bg-gray-50/80 rounded-xl p-3 text-xs">
                <div className="flex items-center justify-between mb-1">
                  <div className="flex items-center gap-1.5">
                    <span className="font-semibold text-gray-800">{rep.userName}</span>
                    {rep.userRole === 'ADMIN' && (
                      <span className="bg-purple-100 text-purple-700 text-[10px] font-bold px-1.5 py-0.2 rounded">
                        STAFF
                      </span>
                    )}
                    <span className="text-gray-400 text-[10px]">
                      • {rep.createdAt ? new Date(rep.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : ''}
                    </span>
                  </div>

                  {(isAuthor || isAdmin) && (
                    <button
                      onClick={() => handleDeleteReply(rep.id)}
                      className="text-gray-400 hover:text-red-500 p-0.5 rounded transition"
                      title="Delete reply"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                </div>
                <p className="text-gray-700 leading-normal">{rep.text}</p>
              </div>
            );
          })}
        </div>
      )}

      {/* Lightbox Modal */}
      <ReviewPhotoLightbox
        photos={review.photos || []}
        initialIndex={activePhotoIdx}
        isOpen={lightboxOpen}
        onClose={() => setLightboxOpen(false)}
      />

      {/* Report Modal */}
      <ReviewReportModal
        reviewId={review.id}
        isOpen={reportModalOpen}
        onClose={() => setReportModalOpen(false)}
        onReportSuccess={onReviewUpdated}
      />
    </article>
  );
}
