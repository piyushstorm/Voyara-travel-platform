import api from './axios'

export async function createBooking(data) {
  const res = await api.post('/bookings', data)
  return res.data.data
}

export async function getMyBookings(page = 0, size = 20) {
  const res = await api.get('/bookings', { params: { page, size } })
  return res.data.data
}

export async function getUpcomingBookings(page = 0, size = 20) {
  const res = await api.get('/bookings/upcoming', { params: { page, size } })
  return res.data.data
}

export async function getPastBookings(page = 0, size = 20) {
  const res = await api.get('/bookings/past', { params: { page, size } })
  return res.data.data
}

export async function getBookingByReference(reference) {
  const res = await api.get(`/bookings/${reference}`)
  return res.data.data
}

export async function cancelBooking(reference, reason, comment) {
  const res = await api.post(`/bookings/${reference}/cancel`, { reason, comment }, {
    params: { reason: reason || undefined },
  })
  return res.data.data
}
