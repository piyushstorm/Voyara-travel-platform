import api from './axios';

export const trainApi = {
  search: (origin, destination) => api.get('/trains/search', { params: { origin, destination } }),
  getById: (id) => api.get(`/trains/${id}`),
  getSeats: (id) => api.get(`/trains/${id}/seats`),
};
