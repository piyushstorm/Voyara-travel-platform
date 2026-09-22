import api from './axios';

export const cabApi = {
  getAll: () => api.get('/cabs'),
  getById: (id) => api.get(`/cabs/${id}`),
};
