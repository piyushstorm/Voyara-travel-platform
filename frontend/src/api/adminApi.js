import api from './axios';

const adminApi = {
  getDashboard: async () => {
    const res = await api.get('/admin/dashboard');
    return res.data?.data || res.data;
  },
  getBookings: async (params = {}) => {
    const { page = 0, size = 20, status, type, search } = params;
    const query = new URLSearchParams({ page, size });
    if (status) query.append('status', status);
    if (type) query.append('type', type);
    if (search) query.append('search', search);
    const res = await api.get(`/admin/bookings?${query}`);
    return res.data?.data || res.data;
  },
  getBookingDetail: async (reference) => {
    const res = await api.get(`/admin/bookings/${reference}`);
    return res.data?.data || res.data;
  },
  getUsers: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/users?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getUserDetail: async (id) => {
    const res = await api.get(`/admin/users/${id}`);
    return res.data?.data || res.data;
  },
  getFlights: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/flights?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getHotels: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/hotels?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getHotelDetail: async (id) => {
    const res = await api.get(`/admin/hotels/${id}`);
    return res.data?.data || res.data;
  },
  getHolidays: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/holidays?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getTrains: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/trains?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getBuses: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/buses?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getCabs: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/cabs?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getPayments: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/payments?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getRefunds: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/refunds?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getReviews: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/reviews?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getAnalytics: async () => {
    const res = await api.get('/admin/analytics');
    return res.data?.data || res.data;
  },
  getCancellationAnalytics: async () => {
    const res = await api.get('/admin/cancellations/analytics');
    return res.data?.data || res.data;
  },
  changeUserRole: async (userId, role) => {
    const res = await api.put(`/admin/users/${userId}/role`, { role });
    return res.data;
  },
  getAuditLogs: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/audit-logs?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  // CRUD Operations
  createFlight: async (data) => {
    const res = await api.post('/admin/flights', data);
    return res.data;
  },
  updateFlight: async (id, data) => {
    const res = await api.put(`/admin/flights/${id}`, data);
    return res.data;
  },
  toggleFlight: async (id) => {
    const res = await api.put(`/admin/flights/${id}/toggle`);
    return res.data;
  },
  deleteFlight: async (id) => {
    const res = await api.delete(`/admin/flights/${id}`);
    return res.data;
  },
  createHotel: async (data) => {
    const res = await api.post('/admin/hotels', data);
    return res.data;
  },
  updateHotel: async (id, data) => {
    const res = await api.put(`/admin/hotels/${id}`, data);
    return res.data;
  },
  toggleHotel: async (id) => {
    const res = await api.put(`/admin/hotels/${id}/toggle`);
    return res.data;
  },
  createHoliday: async (data) => {
    const res = await api.post('/admin/holidays', data);
    return res.data;
  },
  updateHoliday: async (id, data) => {
    const res = await api.put(`/admin/holidays/${id}`, data);
    return res.data;
  },
  createTrain: async (data) => {
    const res = await api.post('/admin/trains', data);
    return res.data;
  },
  updateTrain: async (id, data) => {
    const res = await api.put(`/admin/trains/${id}`, data);
    return res.data;
  },
  createBus: async (data) => {
    const res = await api.post('/admin/buses', data);
    return res.data;
  },
  updateBus: async (id, data) => {
    const res = await api.put(`/admin/buses/${id}`, data);
    return res.data;
  },
  createCab: async (data) => {
    const res = await api.post('/admin/cabs', data);
    return res.data;
  },
  updateCab: async (id, data) => {
    const res = await api.put(`/admin/cabs/${id}`, data);
    return res.data;
  },
  toggleUser: async (id) => {
    const res = await api.put(`/admin/users/${id}/toggle`);
    return res.data;
  },
  // Coupons
  getCoupons: async (params = {}) => {
    const { page = 0, size = 20 } = params;
    const res = await api.get(`/admin/coupons?page=${page}&size=${size}`);
    return res.data?.data || res.data;
  },
  getCoupon: async (id) => {
    const res = await api.get(`/admin/coupons/${id}`);
    return res.data?.data || res.data;
  },
  createCoupon: async (data) => {
    const res = await api.post('/admin/coupons', data);
    return res.data;
  },
  updateCoupon: async (id, data) => {
    const res = await api.put(`/admin/coupons/${id}`, data);
    return res.data;
  },
  deleteCoupon: async (id) => {
    const res = await api.delete(`/admin/coupons/${id}`);
    return res.data;
  },
  // Export
  exportData: async (type) => {
    const res = await api.get(`/admin/export/${type}`);
    return res.data;
  },
  // Room CRUD
  getHotelRooms: async (hotelId) => {
    const res = await api.get(`/admin/hotels/${hotelId}/rooms`);
    return res.data?.data || res.data;
  },
  createRoom: async (hotelId, data) => {
    const res = await api.post(`/admin/hotels/${hotelId}/rooms`, data);
    return res.data;
  },
  updateRoom: async (id, data) => {
    const res = await api.put(`/admin/rooms/${id}`, data);
    return res.data;
  },
  toggleRoom: async (id) => {
    const res = await api.put(`/admin/rooms/${id}/toggle`);
    return res.data;
  },
  deleteRoom: async (id) => {
    const res = await api.delete(`/admin/rooms/${id}`);
    return res.data;
  },
  // Bulk
  bulkToggle: async (type, ids, active) => {
    const res = await api.put('/admin/bulk/toggle', { type, ids, active });
    return res.data;
  },
  // Search
  globalSearch: async (q) => {
    const res = await api.get(`/admin/search?q=${encodeURIComponent(q)}`);
    return res.data?.data || res.data;
  },
  // Enhanced analytics
  getAnalyticsSummary: async () => {
    const res = await api.get('/admin/analytics/summary');
    return res.data?.data || res.data;
  },
};

export default adminApi;
