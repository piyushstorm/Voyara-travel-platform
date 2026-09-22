import api from './axios';

export async function searchFlights(params) {
  const {
    origin, destination, departureDate, returnDate, cabinClass,
    airlineCode, maxStops, minPrice, maxPrice,
    sortBy, sortOrder, page, size
  } = params;
  const res = await api.get('/flights/search', {
    params: {
      origin,
      destination,
      departureDate: departureDate || undefined,
      returnDate: returnDate || undefined,
      cabinClass: cabinClass || 'ECONOMY',
      airlineCode: airlineCode || undefined,
      maxStops: maxStops !== undefined && maxStops !== null ? maxStops : undefined,
      minPrice: minPrice || undefined,
      maxPrice: maxPrice || undefined,
      sortBy: sortBy || 'departureTime',
      sortOrder: sortOrder || 'asc',
      page: page || 0,
      size: size || 20,
    },
  });
  return res.data.data;
}

export async function getFlightById(id, cabinClass = 'ECONOMY') {
  const res = await api.get(`/flights/${id}`, { params: { cabinClass } });
  return res.data.data;
}

// Client-side filter helpers for results that come from server
export function applyClientFilters(flights, filters) {
  let result = [...flights];

  if (filters.stops !== null && filters.stops !== undefined) {
    result = result.filter(f => f.stops === filters.stops);
  }
  if (filters.airlines && filters.airlines.length > 0) {
    result = result.filter(f => filters.airlines.includes(f.airlineCode));
  }
  if (filters.maxPrice !== null && filters.maxPrice !== undefined) {
    result = result.filter(f => (f.price || 0) <= filters.maxPrice);
  }
  if (filters.minPrice !== null && filters.minPrice !== undefined) {
    result = result.filter(f => (f.price || 0) >= filters.minPrice);
  }
  if (filters.timeRange) {
    const ranges = {
      EARLY: [0, 6], MORNING: [6, 12], AFTERNOON: [12, 18], EVENING: [18, 21], NIGHT: [21, 24]
    };
    const [min, max] = ranges[filters.timeRange] || [0, 24];
    result = result.filter(f => {
      const h = new Date(f.departureTime).getHours();
      return h >= min && h < max;
    });
  }
  if (filters.arrivalTimeRange) {
    const ranges = {
      EARLY: [0, 6], MORNING: [6, 12], AFTERNOON: [12, 18], EVENING: [18, 21], NIGHT: [21, 24]
    };
    const [min, max] = ranges[filters.arrivalTimeRange] || [0, 24];
    result = result.filter(f => {
      const h = new Date(f.arrivalTime).getHours();
      return h >= min && h < max;
    });
  }
  if (filters.maxDuration) {
    result = result.filter(f => (f.durationMinutes || 0) <= filters.maxDuration);
  }

  return result;
}

export function sortFlights(flights, sortBy, sortOrder) {
  const sorted = [...flights];
  const dir = sortOrder === 'desc' ? -1 : 1;

  switch (sortBy) {
    case 'price':
      sorted.sort((a, b) => (a.price - b.price) * dir);
      break;
    case 'duration':
      sorted.sort((a, b) => ((a.durationMinutes || 0) - (b.durationMinutes || 0)) * dir);
      break;
    case 'departureTime':
      sorted.sort((a, b) => (new Date(a.departureTime) - new Date(b.departureTime)) * dir);
      break;
    case 'arrivalTime':
      sorted.sort((a, b) => (new Date(a.arrivalTime) - new Date(b.arrivalTime)) * dir);
      break;
    case 'stops':
      sorted.sort((a, b) => ((a.stops || 0) - (b.stops || 0)) * dir);
      break;
    default:
      sorted.sort((a, b) => (new Date(a.departureTime) - new Date(b.departureTime)) * dir);
  }
  return sorted;
}

// Baggage mock data (would come from backend in production)
export const BAGGAGE_INFO = {
  ECONOMY: { cabin: '7 kg', checkin: '15 kg', meal: 'Paid', seat: 'Paid' },
  PREMIUM_ECONOMY: { cabin: '7 kg', checkin: '25 kg', meal: 'Included', seat: 'Free selection' },
  BUSINESS: { cabin: '12 kg', checkin: '32 kg', meal: 'Included', seat: 'Free selection' },
  FIRST: { cabin: '15 kg', checkin: '40 kg', meal: 'Included', seat: 'Suite' },
};

export const CANCELLATION_POLICIES = [
  { label: 'Free cancellation', description: 'Full refund if cancelled more than 7 days before departure', icon: '✅' },
  { label: 'Partial refund', description: '50% refund if cancelled 3-7 days before departure', icon: '💰' },
  { label: 'No refund', description: 'No refund for cancellations within 24 hours', icon: '⚠️' },
];

export const FARE_RULES = {
  ECONOMY: 'Standard fare. Changes subject to fee. Non-refundable within 24h.',
  PREMIUM_ECONOMY: 'Flexible fare. One free change. Partial refund available.',
  BUSINESS: 'Premium flexible fare. Unlimited changes. Full refund available.',
  FIRST: 'Most flexible fare. Full refund and change benefits.',
};

// === Multi-City Search ===
export async function searchMultiCity(params) {
  const res = await api.post('/flights/multi-city', {
    legs: params.legs,
    passengers: params.passengers || 1,
    cabinClass: params.cabinClass || 'ECONOMY',
    sortBy: params.sortBy || 'departureTime',
    sortOrder: params.sortOrder || 'asc',
  });
  return res.data.data;
}

// === Fare Options ===
export async function getFareOptions(flightId) {
  const res = await api.get(`/flights/${flightId}/fare-options`);
  return res.data.data;
}

// === Pricing Factors ===
export async function getPricingFactors(flightId, cabinClass) {
  const res = await api.get(`/flights/${flightId}/pricing-factors`, { params: { cabinClass } });
  return res.data.data;
}

// === Add-Ons ===
export async function getAddOns(cabinClass) {
  const res = await api.get('/addons', { params: { cabinClass } });
  return res.data.data;
}

// === Seat Map ===
export async function getSeatMap(flightId, cabinClass = 'ECONOMY') {
  const res = await api.get('/seats/map', { params: { flightId, cabinClass } });
  return res.data?.data ?? res.data;
}

export async function holdSeat(seatId) {
  const res = await api.post(`/seats/${seatId}/hold`);
  return res.data?.data ?? res.data;
}

export async function releaseSeat(seatId) {
  const res = await api.delete(`/seats/${seatId}/hold`);
  return res.data?.data ?? res.data;
}

export async function getMyHolds() {
  const res = await api.get('/seats/holds');
  return res.data?.data ?? res.data;
}
