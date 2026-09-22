import api from './axios';

export const holidayApi = {
  search: (params) => api.get('/holidays/search', { params }),
  getById: (id) => api.get(`/holidays/${id}`),
  getFeatured: () => api.get('/holidays/featured'),
  getDestinations: () => api.get('/holidays/destinations'),
};
