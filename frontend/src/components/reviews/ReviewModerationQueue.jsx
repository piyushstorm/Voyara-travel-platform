import React, { useState, useEffect, useCallback } from 'react';
import reviewApi from '../../api/reviewApi';
import ReviewPhotoLightbox from './ReviewPhotoLightbox';

export default function ReviewModerationQueue() {
  const [queue, setQueue] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [toast, setToast] = useState(null);
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [lightboxPhotos, setLightboxPhotos] = useState([]);
  const [lightboxIdx, setLightboxIdx] = useState(0);

  // Modal for action confirmation
  const [selectedAction, setSelectedAction] = useState(null); // { reviewId, action, reason }

  const fetchQueue = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reviewApi.getModerationQueue();
      setQueue(res.data || []);
    } catch (err) {
      console.error('Failed to load moderation queue:', err);
      showToast('error', 'Failed to load moderation queue');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchQueue();
  }, [fetchQueue]);

  const showToast = (type, message) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };

  const handleExecuteAction = async () => {
    if (!selectedAction) return;
    const { reviewId, action, reason } = selectedAction;
    setActionLoading(reviewId);

    try {
      await reviewApi.moderateReview(reviewId, { action, reason });
      showToast('success', `Review ${action.toLowerCase()}ed successfully`);
      setSelectedAction(null);
      fetchQueue();
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message || 'Action failed');
    } finally {
      setActionLoading(null);
    }
  };

  const openPhotos = (photos, idx = 0) => {
    setLightboxPhotos(photos);
    setLightboxIdx(idx);
    setLightboxOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Review Moderation Queue</h2>
          <p className="text-xs text-gray-500 mt-1">
            Evaluate flagged user-generated reviews, inspect report reasons, and take authoritative moderation actions.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold bg-amber-100 text-amber-800 px-3 py-1 rounded-full">
            {queue.length} Pending Item{queue.length !== 1 ? 's' : ''}
          </span>
          <button
            onClick={fetchQueue}
            className="p-2 text-gray-500 hover:text-primary rounded-lg border border-gray-200 hover:bg-gray-50 transition"
            title="Refresh queue"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
      </div>

      {/* Toast Feedback */}
      {toast && (
        <div
          className={`px-4 py-3 rounded-xl text-xs font-semibold flex items-center gap-2 animate-fadeIn ${
            toast.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : 'bg-red-50 text-red-800 border border-red-200'
          }`}
        >
          <span>{toast.type === 'success' ? '✓' : '⚠️'}</span>
          <span>{toast.message}</span>
        </div>
      )}

      {/* Queue Listing */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-2xl border border-gray-100 p-6 animate-pulse space-y-3">
              <div className="h-4 bg-gray-200 rounded w-1/4" />
              <div className="h-3 bg-gray-100 rounded w-1/2" />
              <div className="h-16 bg-gray-50 rounded-xl" />
            </div>
          ))}
        </div>
      ) : queue.length === 0 ? (
        <div className="bg-white rounded-2xl border border-gray-100 p-12 text-center shadow-sm">
          <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-3 text-2xl">
            ✓
          </div>
          <h3 className="text-base font-bold text-gray-900">Moderation Queue is Clean</h3>
          <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
            All flagged user reviews and reports have been moderated. New flagged content will appear here in real-time.
          </p>
        </div>
      ) : (
        <div className="space-y-5">
          {queue.map((item) => {
            const isProcessing = actionLoading === item.id;
            return (
              <div
                key={item.id}
                className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 space-y-4 hover:border-gray-300 transition"
              >
                {/* Header row */}
                <div className="flex flex-wrap items-start justify-between gap-3 pb-3 border-b border-gray-100">
                  <div className="flex items-center gap-3">
                    <span className="px-2.5 py-1 rounded-lg text-xs font-bold uppercase tracking-wider bg-purple-100 text-purple-700">
                      {item.targetType || 'TARGET'}
                    </span>
                    <div>
                      <h4 className="font-bold text-gray-900 text-sm">{item.targetName || `ID: ${item.targetId}`}</h4>
                      <p className="text-xs text-gray-400">Review #{item.id}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-700 border border-red-200 flex items-center gap-1">
                      <span>🚩</span>
                      <span>{item.reportCount || 1} Report{item.reportCount !== 1 ? 's' : ''}</span>
                    </span>
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
                      Status: {item.status}
                    </span>
                  </div>
                </div>

                {/* Author & Rating info */}
                <div className="flex items-center gap-3 text-xs text-gray-600">
                  <span className="font-semibold text-gray-900">{item.userName || 'Anonymous'}</span>
                  {item.userEmail && <span>({item.userEmail})</span>}
                  <span>•</span>
                  <div className="flex items-center text-amber-400">
                    {[1, 2, 3, 4, 5].map((s) => (
                      <span key={s}>{s <= item.rating ? '★' : '☆'}</span>
                    ))}
                  </div>
                  <span>•</span>
                  <span>{item.createdAt ? new Date(item.createdAt).toLocaleString('en-IN') : ''}</span>
                </div>

                {/* Title & Review Text */}
                <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
                  {item.title && (
                    <h5 className="font-bold text-gray-900 text-sm mb-1">{item.title}</h5>
                  )}
                  <p className="text-gray-700 text-sm leading-relaxed whitespace-pre-line">{item.text}</p>
                </div>

                {/* Attached Photos */}
                {item.photos && item.photos.length > 0 && (
                  <div>
                    <span className="text-xs font-semibold text-gray-500 block mb-2">Attached Photos:</span>
                    <div className="flex flex-wrap gap-2">
                      {item.photos.map((p, idx) => (
                        <button
                          key={p.id || idx}
                          onClick={() => openPhotos(item.photos, idx)}
                          className="w-16 h-16 rounded-xl overflow-hidden border border-gray-200 hover:opacity-90 transition"
                        >
                          <img src={p.photoUrl} alt="" className="w-full h-full object-cover" />
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Reports Breakdown */}
                {item.reports && item.reports.length > 0 && (
                  <div className="space-y-2 pt-2">
                    <span className="text-xs font-bold text-gray-700 uppercase tracking-wider">Report Details:</span>
                    <div className="space-y-1.5 max-h-40 overflow-y-auto pr-1">
                      {item.reports.map((rep) => (
                        <div key={rep.id} className="p-2.5 bg-red-50/60 rounded-xl border border-red-100 text-xs flex items-start justify-between gap-2">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-red-700">{rep.reason}</span>
                              <span className="text-gray-500">by {rep.reporterName || 'User'}</span>
                            </div>
                            {rep.description && (
                              <p className="text-gray-600 mt-1 text-[11px] italic">"{rep.description}"</p>
                            )}
                          </div>
                          <span className="text-[10px] text-gray-400 shrink-0">
                            {rep.createdAt ? new Date(rep.createdAt).toLocaleDateString('en-IN') : ''}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Actions Row */}
                <div className="flex flex-wrap items-center justify-end gap-2.5 pt-3 border-t border-gray-100">
                  <button
                    disabled={isProcessing}
                    onClick={() => setSelectedAction({ reviewId: item.id, action: 'APPROVE', reason: 'Review approved by moderator' })}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-xl shadow-sm transition disabled:opacity-50 flex items-center gap-1.5"
                  >
                    <span>✓</span> Approve Review
                  </button>

                  <button
                    disabled={isProcessing}
                    onClick={() => setSelectedAction({ reviewId: item.id, action: 'DISMISS_REPORTS', reason: 'False alarm / reports dismissed' })}
                    className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl transition disabled:opacity-50"
                  >
                    Dismiss Reports
                  </button>

                  <button
                    disabled={isProcessing}
                    onClick={() => setSelectedAction({ reviewId: item.id, action: 'REMOVE', reason: 'Violated community guidelines' })}
                    className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-semibold rounded-xl shadow-sm transition disabled:opacity-50 flex items-center gap-1.5"
                  >
                    <span>🗑️</span> Remove Review
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal */}
      {selectedAction && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn"
          onClick={() => setSelectedAction(null)}
        >
          <div
            className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl relative"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-base font-bold text-gray-900 mb-2">
              Confirm Action: {selectedAction.action}
            </h3>
            <p className="text-xs text-gray-500 mb-4">
              Please enter an audit reason for this moderation action. This will be recorded in the immutable audit log.
            </p>

            <textarea
              value={selectedAction.reason}
              onChange={(e) => setSelectedAction({ ...selectedAction, reason: e.target.value })}
              rows={3}
              placeholder="Enter reason for audit record..."
              className="w-full text-xs border border-gray-300 rounded-xl p-3 outline-none focus:ring-1 focus:ring-primary focus:border-primary mb-4"
            />

            <div className="flex justify-end gap-2">
              <button
                onClick={() => setSelectedAction(null)}
                className="px-4 py-2 text-xs font-medium text-gray-600 hover:bg-gray-100 rounded-xl transition"
              >
                Cancel
              </button>
              <button
                onClick={handleExecuteAction}
                disabled={actionLoading !== null}
                className="px-5 py-2 text-xs font-semibold text-white bg-primary hover:bg-primary-dark rounded-xl shadow-sm transition disabled:opacity-50"
              >
                {actionLoading !== null ? 'Applying...' : 'Confirm Action'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Photo Lightbox */}
      <ReviewPhotoLightbox
        photos={lightboxPhotos}
        initialIndex={lightboxIdx}
        isOpen={lightboxOpen}
        onClose={() => setLightboxOpen(false)}
      />
    </div>
  );
}
