import React, { useState, useRef } from 'react';
import reviewApi from '../../api/reviewApi';

const RATING_LABELS = {
  1: 'Poor',
  2: 'Fair',
  3: 'Good',
  4: 'Very Good',
  5: 'Excellent',
};

export default function ReviewComposer({
  flightId = null,
  hotelId = null,
  bookingId = null,
  targetName = '',
  isOpen = false,
  onClose,
  onSuccess,
}) {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [title, setTitle] = useState('');
  const [text, setText] = useState('');
  const [photos, setPhotos] = useState([]);
  const [photoPreviews, setPhotoPreviews] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  if (!isOpen) return null;

  const handlePhotoSelect = (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;

    if (photos.length + files.length > 5) {
      setError('You can upload a maximum of 5 photos');
      return;
    }

    const validFiles = [];
    const validPreviews = [];

    for (const file of files) {
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        setError('Only JPG, PNG, and WebP images are allowed');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        setError('Each photo must be smaller than 5MB');
        return;
      }
      validFiles.push(file);
      validPreviews.push(URL.createObjectURL(file));
    }

    setError(null);
    setPhotos((prev) => [...prev, ...validFiles]);
    setPhotoPreviews((prev) => [...prev, ...validPreviews]);
  };

  const removePhoto = (index) => {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
    setPhotoPreviews((prev) => {
      URL.revokeObjectURL(prev[index]);
      return prev.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (rating === 0) {
      setError('Please select a 1 to 5 star rating');
      return;
    }
    if (text.trim().length < 3) {
      setError('Please write at least 3 characters in your review');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const payload = {
        flightId: flightId ? Number(flightId) : null,
        hotelId: hotelId ? Number(hotelId) : null,
        bookingId: bookingId ? Number(bookingId) : null,
        rating,
        title: title.trim() || null,
        reviewText: text.trim(),
      };

      const res = await reviewApi.createReview(payload);
      const createdReview = res.data;

      // Upload photos if any selected
      if (photos.length > 0 && createdReview.id) {
        const formData = new FormData();
        photos.forEach((file) => formData.append('files', file));
        try {
          await reviewApi.uploadPhotos(createdReview.id, formData);
        } catch (photoErr) {
          console.error('Photo upload error:', photoErr);
        }
      }

      onClose();
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to submit review');
    } finally {
      setSubmitting(false);
    }
  };

  const currentDisplayRating = hoverRating || rating;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="composer-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl max-w-xl w-full p-6 sm:p-8 shadow-2xl relative max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close button */}
        <button
          onClick={onClose}
          aria-label="Close review composer"
          className="absolute top-5 right-5 text-gray-400 hover:text-gray-600 p-1.5 rounded-full hover:bg-gray-100 transition"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div className="mb-5">
          <h2 id="composer-modal-title" className="text-xl font-bold text-gray-900">
            Write a Review
          </h2>
          {targetName && (
            <p className="text-sm text-gray-500 mt-0.5">
              Share your feedback for <span className="font-semibold text-gray-800">{targetName}</span>
            </p>
          )}
        </div>

        {error && (
          <div className="mb-4 p-3.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-start gap-2">
            <svg className="w-4 h-4 text-red-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Star Rating Selection */}
          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
              Overall Rating *
            </label>
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1" onMouseLeave={() => setHoverRating(0)}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    aria-label={`${star} Star${star > 1 ? 's' : ''}`}
                    className="p-1 text-2xl sm:text-3xl transition transform hover:scale-110 focus:outline-none"
                  >
                    <span className={star <= currentDisplayRating ? 'text-amber-400' : 'text-gray-200'}>
                      ★
                    </span>
                  </button>
                ))}
              </div>
              {currentDisplayRating > 0 && (
                <span className="text-sm font-semibold text-amber-600 ml-2">
                  {RATING_LABELS[currentDisplayRating]}
                </span>
              )}
            </div>
          </div>

          {/* Title input */}
          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
              Headline / Title (Optional)
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Wonderful stay and courteous staff"
              maxLength={120}
              className="w-full text-sm border border-gray-300 rounded-xl px-4 py-2.5 outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
            />
          </div>

          {/* Review text */}
          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
              Your Review *
            </label>
            <textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="What did you like or dislike? How was the service, cleanliness, or amenities?"
              maxLength={2000}
              rows={4}
              required
              className="w-full text-sm border border-gray-300 rounded-xl p-4 outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition resize-none"
            />
            <div className="flex justify-between text-[11px] text-gray-400 mt-1">
              <span>Minimum 3 characters</span>
              <span>{text.length}/2000</span>
            </div>
          </div>

          {/* Photos Upload */}
          <div>
            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
              Photos (Optional, max 5)
            </label>
            
            {/* Previews grid */}
            {photoPreviews.length > 0 && (
              <div className="flex flex-wrap gap-2.5 mb-3">
                {photoPreviews.map((src, i) => (
                  <div key={i} className="relative w-16 h-16 rounded-xl overflow-hidden border border-gray-200 group">
                    <img src={src} alt="Preview" className="w-full h-full object-cover" />
                    <button
                      type="button"
                      onClick={() => removePhoto(i)}
                      aria-label="Remove photo"
                      className="absolute top-1 right-1 bg-black/70 hover:bg-black text-white rounded-full p-1 transition"
                    >
                      <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}

            {photos.length < 5 && (
              <div>
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handlePhotoSelect}
                  multiple
                  accept="image/jpeg,image/png,image/webp"
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="border-2 border-dashed border-gray-300 hover:border-primary rounded-2xl p-4 w-full text-center transition flex flex-col items-center justify-center gap-1 group bg-gray-50/50 hover:bg-primary/5"
                >
                  <svg className="w-6 h-6 text-gray-400 group-hover:text-primary transition" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span className="text-xs font-semibold text-gray-700 group-hover:text-primary">
                    Upload Photos
                  </span>
                  <span className="text-[11px] text-gray-400">
                    PNG, JPG or WebP up to 5MB ({5 - photos.length} remaining)
                  </span>
                </button>
              </div>
            )}
          </div>

          {/* Submit buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-5 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-100 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting || rating === 0 || text.trim().length < 3}
              className="px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-xl text-sm font-semibold shadow-md hover:shadow-lg disabled:opacity-50 transition flex items-center gap-2"
            >
              {submitting ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>Submitting...</span>
                </>
              ) : (
                'Submit Review'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
