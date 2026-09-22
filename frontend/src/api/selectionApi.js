import api from './axios';

/**
 * Seat Selection API
 */
export async function getSeatMap(flightId, cabinClass = 'ECONOMY') {
  const res = await api.get('/seats/map', { params: { flightId, cabinClass } });
  return res.data?.data ?? res.data;
}

export async function holdSeat(seatId) {
  const res = await api.post(`/seats/${seatId}/hold`);
  return res.data?.data ?? res.data;
}

export async function releaseSeatHold(seatId) {
  const res = await api.delete(`/seats/${seatId}/hold`);
  return res.data?.data ?? res.data;
}

/**
 * Room Selection & Upsell API
 */
export async function getHotelRoomsSelection(hotelId) {
  const res = await api.get(`/rooms/selection/${hotelId}`);
  return res.data;
}

export async function getRoomUpgrades(hotelId) {
  const res = await api.get(`/rooms/upgrades/${hotelId}`);
  return res.data;
}

export async function holdRoom(roomId) {
  const res = await api.post(`/rooms/${roomId}/hold`);
  return res.data;
}

export async function releaseRoomHold(roomId) {
  const res = await api.delete(`/rooms/${roomId}/hold`);
  return res.data;
}

/**
 * Travel Preferences API
 */
export async function getTravelPreferences() {
  const res = await api.get('/preferences/travel');
  return res.data;
}

export async function updateTravelPreferences(preferences) {
  const res = await api.put('/preferences/travel', preferences);
  return res.data;
}
