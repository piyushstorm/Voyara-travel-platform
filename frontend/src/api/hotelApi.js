import api from './axios'

export async function searchHotels(params) {
  const { city, checkIn, checkOut, guests, rooms, minPrice, maxPrice, minStar, maxStar, amenity, sortBy, sortOrder, page, size } = params
  const res = await api.get('/hotels/search', {
    params: {
      city,
      checkIn: checkIn || undefined,
      checkOut: checkOut || undefined,
      guests: guests || 2,
      rooms: rooms || 1,
      minPrice: minPrice || undefined,
      maxPrice: maxPrice || undefined,
      minStar: minStar || undefined,
      maxStar: maxStar || undefined,
      amenity: amenity || undefined,
      sortBy: sortBy || 'startingPrice',
      sortOrder: sortOrder || 'asc',
      page: page || 0,
      size: size || 10,
    },
  })
  return res.data.data
}

export async function getHotelById(id) {
  const res = await api.get(`/hotels/${id}`)
  return res.data.data
}

export async function getHotelRooms(hotelId) {
  const res = await api.get(`/hotels/${hotelId}/rooms`)
  return res.data.data
}

export async function getPopularCities() {
  const res = await api.get('/hotels/popular-cities')
  return res.data.data
}
