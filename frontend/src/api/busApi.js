import api from './axios';

export const busApi = {
  search: (origin, destination) => api.get('/buses/search', { params: { origin, destination } }),
  getById: (id) => api.get(`/buses/${id}`),
};
