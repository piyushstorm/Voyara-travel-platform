import api from './axios';

export const flightStatusApi = {
  /** Get live status for a single flight */
  getStatus: (flightId) => api.get(`/flight-status/${flightId}`),

  /** Get live status for all tracked flights (authenticated) */
  getMyTrackedFlights: () => api.get('/flight-status/tracked'),

  /** Track a flight for the current user */
  trackFlight: (flightId) => api.post(`/flight-status/${flightId}/track`),

  /** Untrack a flight for the current user */
  untrackFlight: (flightId) => api.delete(`/flight-status/${flightId}/untrack`),

  /** Get status change history */
  getHistory: (flightId) => api.get(`/flight-status/${flightId}/history`),

  /** Search flights with live status */
  searchFlights: (query = '') => api.get('/flight-status/search', { params: { query } }),

  /** Simulate flight transition (dev / testing) */
  simulateStatus: (flightId, scenario) => api.post(`/flight-status/${flightId}/simulate`, { scenario }),
};

export const priceApi = {
  /** Get price history for chart */
  getHistory: (entityType, entityId, cabinClass) =>
    api.get('/prices/history', { params: { entityType, entityId, cabinClass } }),

  /** Get transparent pricing breakdown */
  getBreakdown: (entityType, entityId, cabinClass) =>
    api.get('/prices/breakdown', { params: { entityType, entityId, cabinClass } }),

  /** Create a price freeze */
  createFreeze: (flightId, cabinClass) =>
    api.post('/prices/freeze', null, { params: { flightId, cabinClass } }),

  /** Get active freeze */
  getActiveFreeze: (entityType, entityId) =>
    api.get('/prices/freeze/active', { params: { entityType, entityId } }),

  /** Get all user freezes */
  getMyFreezes: () => api.get('/prices/freezes'),

  /** Get a freeze by ID */
  getFreezeById: (freezeId) => api.get(`/prices/freezes/${freezeId}`),
};

export const refundApi = {
  /** Get all refunds for current user */
  getMyRefunds: () => api.get('/refunds'),
};

export const paymentApi = {
  /** Create a payment order */
  createOrder: (amount, currency = 'INR', paymentMethod = 'CARD', bookingId = null) =>
    api.post('/payments/create-order', { amount, currency, paymentMethod, bookingId }),

  /** Verify payment */
  verifyPayment: (orderId, paymentId, signature) =>
    api.post('/payments/verify', { orderId, paymentId, signature }),

  /** Get payment status */
  getStatus: (paymentId) => api.get(`/payments/status/${paymentId}`),
};

export const couponApi = {
  /** Validate a coupon code */
  validate: (code, amount, module = 'ALL') =>
    api.post('/coupons/validate', { code, amount, module }),
};

export const seatApi = {
  /** Get seat map for a flight */
  getMap: (flightId, cabinClass) =>
    api.get('/seats/map', { params: { flightId, cabinClass } }),

  /** Hold a seat */
  holdSeat: (seatId) => api.post(`/seats/${seatId}/hold`),

  /** Release a seat hold */
  releaseSeat: (seatId) => api.delete(`/seats/${seatId}/hold`),

  /** Get user's active holds */
  getMyHolds: () => api.get('/seats/holds'),
};

export const reviewApi = {
  /** Create a review */
  create: (data) => api.post('/reviews', data),

  /** Get reviews for a flight */
  getFlightReviews: (flightId, sortBy = 'NEWEST', page = 0, size = 10) =>
    api.get(`/reviews/flight/${flightId}`, { params: { sortBy, page, size } }),

  /** Get reviews for a hotel */
  getHotelReviews: (hotelId, sortBy = 'NEWEST', page = 0, size = 10) =>
    api.get(`/reviews/hotel/${hotelId}`, { params: { sortBy, page, size } }),

  /** Get rating summary */
  getFlightSummary: (flightId) => api.get(`/reviews/flight/${flightId}/summary`),
  getHotelSummary: (hotelId) => api.get(`/reviews/hotel/${hotelId}/summary`),

  /** Add reply */
  addReply: (reviewId, text) => api.post(`/reviews/${reviewId}/replies`, { text }),

  /** Vote on a review */
  vote: (reviewId, voteType) => api.post(`/reviews/${reviewId}/vote`, { voteType }),

  /** Flag a review */
  flag: (reviewId, reason) => api.post(`/reviews/${reviewId}/flag`, { reason }),

  /** Check if user is eligible to review a booking */
  checkEligibility: (bookingId) => api.get('/reviews/eligibility', { params: { bookingId } }),
};

export const recommendationApi = {
  /** Get personalized recommendations */
  get: (entityType, limit) => api.get('/recommendations', { params: { entityType, limit } }),

  /** Submit feedback on a specific recommendation */
  feedback: (id, feedback) => api.post(`/recommendations/${id}/feedback`, { feedback }),

  /** Submit feedback directly for an entity */
  entityFeedback: (entityType, entityId, feedback) =>
    api.post('/recommendations/feedback', { entityType, entityId, feedback }),

  /** Force recomputation for authenticated user */
  recompute: () => api.post('/recommendations/recompute'),
};

export const roomApi = {
  /** Get room selection data */
  getSelection: (hotelId) => api.get(`/rooms/selection/${hotelId}`),

  /** Get upgrade options */
  getUpgrades: (hotelId, currentRoomType) =>
    api.get(`/rooms/upgrades/${hotelId}`, { params: { currentRoomType } }),
};

export const rewardsApi = {
  /** Get current user's rewards account */
  getAccount: () => api.get('/rewards'),

  /** Get points balance */
  getBalance: () => api.get('/rewards/balance'),

  /** Get tier info */
  getTier: () => api.get('/rewards/tier'),

  /** Get tier benefits */
  getBenefits: () => api.get('/rewards/benefits'),

  /** Get transaction history */
  getTransactions: (type, page = 0, size = 20) =>
    api.get('/rewards/transactions', { params: { type, page, size } }),

  /** Validate redemption */
  validateRedemption: (points, amount) =>
    api.post('/rewards/redeem/validate', { points, amount }),

  /** Get rewards config */
  getConfig: () => api.get('/rewards/config'),

  // Admin endpoints
  adminGetUserRewards: (userId) => api.get(`/rewards/admin/user/${userId}`),
  adminGetUserTransactions: (userId, type, page = 0, size = 20) =>
    api.get(`/rewards/admin/user/${userId}/transactions`, { params: { type, page, size } }),
  adminAdjustPoints: (userId, points, reason, isAdd = true) =>
    api.post(`/rewards/admin/user/${userId}/adjust`, { points, reason, isAdd }),
  adminUpdateConfig: (config) => api.put('/rewards/admin/config', config),
};

export const notificationsApi = {
  /** Get notifications */
  getNotifications: (params = {}) => api.get('/notifications', { params }),

  /** Get unread count */
  getUnreadCount: () => api.get('/notifications/unread-count'),

  /** Mark one as read */
  markAsRead: (id) => api.patch(`/notifications/${id}/read`),

  /** Mark all as read */
  markAllAsRead: () => api.patch('/notifications/read-all'),

  /** Get preferences */
  getPreferences: () => api.get('/notifications/preferences'),

  /** Update preferences */
  updatePreferences: (prefs) => api.put('/notifications/preferences', prefs),
};

// ═══════════════════════════════════════════════════
// VOYARA DIFFERENTIATOR APIs
// ═══════════════════════════════════════════════════

export const voyaraApi = {
  // Travel Guardian
  getGuardianAlerts: () => api.get('/voyara/guardian/alerts'),
  analyzeTrips: () => api.post('/voyara/guardian/analyze'),
  markAlertRead: (id) => api.post(`/voyara/guardian/alerts/${id}/read`),
  markAllAlertsRead: () => api.post('/voyara/guardian/alerts/read-all'),
  dismissAlert: (id) => api.post(`/voyara/guardian/alerts/${id}/dismiss`),

  // Trip Readiness
  getReadiness: (bookingId) => api.get(`/voyara/readiness/${bookingId}`),
  recalculateReadiness: (bookingId) => api.post(`/voyara/readiness/${bookingId}/recalculate`),

  // Smart Timeline
  getTimeline: (bookingId) => api.get(`/voyara/timeline/${bookingId}`),
  regenerateTimeline: (bookingId) => api.post(`/voyara/timeline/${bookingId}/regenerate`),
  getUserTimeline: () => api.get('/voyara/timeline'),

  // Connection Risk
  getConnectionRisk: (bookingId) => api.get(`/voyara/connection-risk/${bookingId}`),
  calculateConnectionRisk: (firstFlightId, secondFlightId) =>
    api.post(`/voyara/connection-risk/calculate?firstFlightId=${firstFlightId}&secondFlightId=${secondFlightId}`),

  // Group Trips
  getGroupTrips: () => api.get('/voyara/group-trips'),
  createGroupTrip: (data) => api.post('/voyara/group-trips', data),
  getGroupTrip: (id) => api.get(`/voyara/group-trips/${id}`),
  getInvitationByToken: (token) => api.get(`/voyara/invitations/${token}`),
  inviteCompanion: (tripId, data) => api.post(`/voyara/group-trips/${tripId}/invite`, data),
  acceptInvitation: (token) => api.post(`/voyara/invitations/${token}/accept`),
  declineInvitation: (token) => api.post(`/voyara/invitations/${token}/decline`),
  addBookingToTrip: (tripId, bookingId) => api.post(`/voyara/group-trips/${tripId}/bookings/${bookingId}`),

  // Expenses
  getExpenses: (tripId) => api.get(`/voyara/group-trips/${tripId}/expenses`),
  addExpense: (tripId, data) => api.post(`/voyara/group-trips/${tripId}/expenses`, data),
  getSettlements: (tripId) => api.get(`/voyara/group-trips/${tripId}/settlements`),
  markSettled: (settlementId) => api.post(`/voyara/settlements/${settlementId}/settle`),
};
