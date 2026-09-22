import React, { useState } from 'react';
import reviewApi from '../../api/reviewApi';

const REPORT_REASONS = [
  { value: 'SPAM', label: 'Spam or Promotional', description: 'Advertising, repeated text, or promotional content' },
  { value: 'OFFENSIVE', label: 'Offensive Content', description: 'Profanity, hate speech, or discriminatory language' },
  { value: 'HARASSMENT', label: 'Harassment or Bullying', description: 'Direct attacks, threats, or aggressive behavior' },
  { value: 'FAKE_REVIEW', label: 'Fake or Misleading Review', description: 'Fabricated experience or conflict of interest' },
  { value: 'IRRELEVANT', label: 'Irrelevant to Product', description: 'Does not pertain to the actual flight or hotel' },
  { value: 'INAPPROPRIATE_CONTENT', label: 'Inappropriate Content', description: 'Sexually suggestive, violent, or vulgar material' },
  { value: 'PERSONAL_INFORMATION', label: 'Personal Information', description: 'Exposes private details like phone numbers or emails' },
  { value: 'OTHER', label: 'Other Concern', description: 'Other issue not covered above' },
];

export default function ReviewReportModal({ reviewId, isOpen, onClose, onReportSuccess }) {
  const [reason, setReason] = useState('SPAM');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await reviewApi.reportReview(reviewId, { reason, description });
      setSuccess(true);
      setTimeout(() => {
        setSuccess(false);
        onClose();
        if (onReportSuccess) onReportSuccess();
      }, 1500);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to submit report');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="report-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl relative"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          aria-label="Close modal"
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 p-1 rounded-full transition"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        {success ? (
          <div className="text-center py-8">
            <div className="w-14 h-14 bg-green-100 text-green-600 rounded-full flex items-center justify-center mx-auto mb-3 text-2xl">
              ✓
            </div>
            <h3 className="text-xl font-bold text-gray-900">Thank You</h3>
            <p className="text-gray-600 text-sm mt-1">
              Your report has been submitted for moderator review.
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="flex items-center gap-2 mb-2 text-red-600">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <h2 id="report-modal-title" className="text-lg font-bold text-gray-900">Report Review</h2>
            </div>
            <p className="text-sm text-gray-500 mb-4">
              Help us understand why this review is inappropriate. Moderators will evaluate this content.
            </p>

            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                {error}
              </div>
            )}

            {/* Predefined Reasons */}
            <div className="space-y-2 mb-4 max-h-56 overflow-y-auto pr-1">
              {REPORT_REASONS.map((r) => (
                <label
                  key={r.value}
                  className={`flex items-start gap-3 p-2.5 rounded-lg border cursor-pointer transition ${
                    reason === r.value
                      ? 'border-primary bg-primary/5 text-primary'
                      : 'border-gray-200 hover:bg-gray-50 text-gray-800'
                  }`}
                >
                  <input
                    type="radio"
                    name="reportReason"
                    value={r.value}
                    checked={reason === r.value}
                    onChange={() => setReason(r.value)}
                    className="mt-0.5 text-primary focus:ring-primary"
                  />
                  <div>
                    <div className="text-sm font-semibold">{r.label}</div>
                    <div className="text-xs text-gray-500">{r.description}</div>
                  </div>
                </label>
              ))}
            </div>

            {/* Additional details */}
            <div className="mb-5">
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Additional Details (Optional)
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={500}
                rows={3}
                placeholder="Provide extra context to assist moderators..."
                className="w-full text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none resize-none"
              />
              <div className="text-right text-[11px] text-gray-400 mt-0.5">
                {description.length}/500
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                disabled={submitting}
                className="px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100 rounded-lg transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-5 py-2 text-sm font-semibold text-white bg-red-600 hover:bg-red-700 rounded-lg shadow transition flex items-center gap-1.5 disabled:opacity-50"
              >
                {submitting ? 'Submitting...' : 'Submit Report'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
