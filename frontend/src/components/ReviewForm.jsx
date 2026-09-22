import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function ReviewForm({ flightId, hotelId, onReviewSubmitted }) {
  const { user } = useAuth();
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [text, setText] = useState('');
  const [photos, setPhotos] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handlePhotoChange = (e) => {
    if (e.target.files) {
      const selected = Array.from(e.target.files);
      if (photos.length + selected.length > 5) {
        setError('Maximum 5 photos allowed');
        return;
      }
      setPhotos([...photos, ...selected]);
      setError(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (rating === 0) { setError('Please select a rating'); return; }
    if (!text.trim()) { setError('Please write a review'); return; }

    setSubmitting(true);
    setError(null);
    try {
      const body = { rating, text: text.trim() };
      if (flightId) body.flightId = flightId;
      if (hotelId) body.hotelId = hotelId;

      const res = await fetch('/api/reviews', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify(body)
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to submit review');
      }

      const reviewData = await res.json();

      // Upload photos if any
      if (photos.length > 0 && reviewData.id) {
        const formData = new FormData();
        photos.forEach(photo => formData.append('files', photo));
        const photoRes = await fetch(`/api/reviews/${reviewData.id}/photos`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${localStorage.getItem('accessToken')}`
          },
          body: formData
        });
        if (!photoRes.ok) {
           console.error('Failed to upload photos');
        }
      }

      setSuccess(true);
      setRating(0);
      setText('');
      setPhotos([]);
      if (onReviewSubmitted) onReviewSubmitted();
    } catch (e) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (!user) {
    return (
      <div className="bg-gray-50 rounded-xl p-6 text-center text-gray-500">
        <p>Sign in to leave a review</p>
      </div>
    );
  }

  if (success) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center">
        <div className="text-3xl mb-2">✅</div>
        <p className="font-semibold text-green-800">Review submitted!</p>
        <p className="text-sm text-green-600 mt-1">Thank you for your feedback.</p>
        <button
          onClick={() => setSuccess(false)}
          className="mt-3 text-sm text-green-700 underline hover:no-underline"
        >
          Write another review
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white rounded-xl p-6 border">
      <h3 className="font-bold text-gray-900 mb-4">Write a Review</h3>

      {/* Star rating */}
      <div className="mb-4">
        <p className="text-sm text-gray-600 mb-2">Your rating</p>
        <div className="flex gap-1">
          {[1, 2, 3, 4, 5].map(star => (
            <button
              key={star}
              type="button"
              onMouseEnter={() => setHoverRating(star)}
              onMouseLeave={() => setHoverRating(0)}
              onClick={() => setRating(star)}
              className="text-3xl transition-colors"
            >
              {(hoverRating || rating) >= star ? '★' : '☆'}
            </button>
          ))}
        </div>
        {rating > 0 && (
          <p className="text-xs text-gray-500 mt-1">
            {rating === 1 ? 'Poor' : rating === 2 ? 'Fair' : rating === 3 ? 'Good' : rating === 4 ? 'Very Good' : 'Excellent'}
          </p>
        )}
      </div>

      {/* Review text */}
      <div className="mb-4">
        <textarea
          value={text}
          onChange={e => setText(e.target.value)}
          placeholder="Share your experience... (What did you like? What could be improved?)"
          rows={4}
          maxLength={2000}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary outline-none text-sm resize-none"
        />
        <p className="text-xs text-gray-400 mt-1">{text.length}/2000</p>
      </div>

      {/* Photo upload */}
      <div className="mb-4">
        <label className="block text-sm text-gray-600 mb-2">Add photos (Optional, max 5)</label>
        <input 
          type="file" 
          multiple 
          accept="image/jpeg, image/png, image/webp"
          onChange={handlePhotoChange}
          className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
        />
        {photos.length > 0 && (
          <p className="text-xs text-gray-500 mt-2">{photos.length} photo(s) selected</p>
        )}
      </div>

      {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

      <button
        type="submit"
        disabled={submitting || rating === 0}
        className="bg-primary text-white px-6 py-2.5 rounded-lg font-semibold text-sm hover:bg-primary-dark transition disabled:opacity-50"
      >
        {submitting ? 'Submitting...' : 'Submit Review'}
      </button>
    </form>
  );
}
