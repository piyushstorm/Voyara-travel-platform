import api from './axios'

export const reviewApi = {
  // Reviews listing
  getFlightReviews: (flightId, params = {}) =>
    api.get(`/reviews/flight/${flightId}`, { params }),

  getHotelReviews: (hotelId, params = {}) =>
    api.get(`/reviews/hotel/${hotelId}`, { params }),

  // Summary
  getFlightRatingSummary: (flightId) =>
    api.get(`/reviews/flight/${flightId}/summary`),

  getHotelRatingSummary: (hotelId) =>
    api.get(`/reviews/hotel/${hotelId}/summary`),

  // Create review
  createReview: (data) =>
    api.post('/reviews', data),

  // Check eligibility for a booking
  checkEligibility: (bookingId) =>
    api.get('/reviews/eligibility', { params: { bookingId } }),

  // Photo management
  uploadPhotos: (reviewId, formData) =>
    api.post(`/reviews/${reviewId}/photos`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  deletePhoto: (photoId) =>
    api.delete(`/reviews/photos/${photoId}`),

  // Replies
  addReply: (reviewId, text) =>
    api.post(`/reviews/${reviewId}/replies`, { text }),

  updateReply: (replyId, text) =>
    api.put(`/reviews/replies/${replyId}`, { text }),

  deleteReply: (replyId) =>
    api.delete(`/reviews/replies/${replyId}`),

  // Voting
  voteReview: (reviewId, voteType) =>
    api.post(`/reviews/${reviewId}/vote`, { voteType }),

  // Reporting / Flagging
  flagReview: (reviewId, reason) =>
    api.post(`/reviews/${reviewId}/flag`, { reason }),

  reportReview: (reviewId, { reason, description }) =>
    api.post(`/reviews/${reviewId}/report`, { reason, description }),

  // Admin / Moderation
  getModerationQueue: () =>
    api.get('/reviews/moderation/queue'),

  moderateReview: (reviewId, { action, reason }) =>
    api.post(`/reviews/${reviewId}/moderate`, { action, reason }),
}

export default reviewApi
