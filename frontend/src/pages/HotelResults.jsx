import { useState, useMemo } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchHotels } from '../api/hotelApi'
import { HotelIcon, SparklesIcon, MapPinIcon } from '../components/common/Icons'

const AMENITY_OPTIONS = [
  { key: 'WIFI', label: 'Free WiFi' },
  { key: 'POOL', label: 'Pool' },
  { key: 'SPA', label: 'Spa' },
  { key: 'GYM', label: 'Gym' },
  { key: 'RESTAURANT', label: 'Restaurant' },
  { key: 'ROOM_SERVICE', label: 'Room Service' },
  { key: 'PARKING', label: 'Free Parking' },
  { key: 'BREAKFAST_INCLUDED', label: 'Breakfast Included' },
  { key: 'AIRPORT_SHUTTLE', label: 'Airport Shuttle' },
  { key: 'BAR', label: 'Bar' },
  { key: 'FITNESS_CENTER', label: 'Fitness Center' },
  { key: 'BUSINESS_CENTER', label: 'Business Center' },
];

const SORT_OPTIONS = [
  { value: 'startingPrice-asc', label: 'Price: Low to High' },
  { value: 'startingPrice-desc', label: 'Price: High to Low' },
  { value: 'starRating-desc', label: 'Star Rating: High to Low' },
  { value: 'guestRating-desc', label: 'Guest Rating: High to Low' },
];

function StarRating({ rating, size = 'sm' }) {
  const sz = size === 'xs' ? 'text-xs' : 'text-sm';
  return (
    <div className="flex gap-0.5">
      {Array.from({ length: 5 }, (_, i) => (
        <span key={i} className={`${sz} ${i < rating ? 'text-amber-400' : 'text-gray-300'}`}>★</span>
      ))}
    </div>
  );
}

function HotelCardSkeleton() {
  return (
    <div className="bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 animate-pulse">
      <div className="flex flex-col sm:flex-row">
        <div className="sm:w-56 h-44 sm:h-auto bg-gray-200 shrink-0" />
        <div className="flex-1 p-4 space-y-3">
          <div className="h-5 bg-gray-200 rounded w-2/3" />
          <div className="h-3 bg-gray-100 rounded w-1/3" />
          <div className="h-3 bg-gray-100 rounded w-full" />
          <div className="flex gap-2 mt-2">
            {[1, 2, 3].map(i => <div key={i} className="h-5 bg-gray-100 rounded-full w-16" />)}
          </div>
          <div className="flex justify-between items-end pt-3 border-t border-gray-100 mt-3">
            <div className="space-y-1">
              <div className="h-3 bg-gray-100 rounded w-16" />
              <div className="h-6 bg-gray-200 rounded w-20" />
            </div>
            <div className="h-9 bg-blue-200 rounded-lg w-24" />
          </div>
        </div>
      </div>
    </div>
  );
}

function MobileFilterDrawer({ open, onClose, filters, setFilters, onClear }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="absolute right-0 top-0 bottom-0 w-80 max-w-[85vw] bg-white shadow-xl overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-100 px-4 py-3 flex items-center justify-between">
          <h3 className="font-bold text-gray-900">Filters</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">×</button>
        </div>
        <div className="p-4 space-y-5">
          {/* Star Rating */}
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Star Rating</h4>
            <div className="space-y-1.5">
              {[5, 4, 3, 2].map(star => (
                <label key={star} className="flex items-center gap-2 cursor-pointer py-1">
                  <input type="checkbox" checked={filters.stars.includes(star)}
                    onChange={e => {
                      const newStars = e.target.checked ? [...filters.stars, star] : filters.stars.filter(s => s !== star);
                      setFilters({ ...filters, stars: newStars });
                    }}
                    className="w-4 h-4 text-blue-600 rounded accent-blue-600" />
                  <StarRating rating={star} size="xs" />
                  <span className="text-xs text-gray-500">& up</span>
                </label>
              ))}
            </div>
          </div>
          {/* Price */}
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Max Price/Night</h4>
            <input type="range" min="500" max="30000" step="500" value={filters.maxPrice}
              onChange={e => setFilters({ ...filters, maxPrice: Number(e.target.value) })}
              className="w-full accent-blue-600" />
            <div className="text-right text-sm text-gray-700 font-medium">₹{filters.maxPrice.toLocaleString()}</div>
          </div>
          {/* Amenities */}
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Amenities</h4>
            <div className="space-y-1.5">
              {AMENITY_OPTIONS.map(a => (
                <label key={a.key} className="flex items-center gap-2 cursor-pointer py-1">
                  <input type="checkbox" checked={filters.amenities.includes(a.key)}
                    onChange={e => {
                      const newA = e.target.checked ? [...filters.amenities, a.key] : filters.amenities.filter(x => x !== a.key);
                      setFilters({ ...filters, amenities: newA });
                    }}
                    className="w-4 h-4 text-blue-600 rounded accent-blue-600" />
                  <span className="text-sm text-gray-700">{a.label}</span>
                </label>
              ))}
            </div>
          </div>
        </div>
        <div className="sticky bottom-0 bg-white border-t border-gray-100 p-4 flex gap-3">
          <button onClick={onClear} className="flex-1 py-2.5 border border-gray-200 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50">Clear All</button>
          <button onClick={onClose} className="flex-1 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700">Show Results</button>
        </div>
      </div>
    </div>
  );
}

export default function HotelResults() {
  const [searchParams] = useSearchParams()
  const city = searchParams.get('city')

  if (!city) {
    return (
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-20 text-center">
        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
        </div>
        <h2 className="text-lg font-semibold text-gray-900 mb-1">No destination selected</h2>
        <p className="text-gray-500 text-sm mb-4">Search for hotels from the hotels page</p>
        <button onClick={() => window.location.assign('/hotels')} className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg font-semibold text-sm transition-colors">
          Search Hotels
        </button>
      </div>
    )
  }

  return <HotelResultsContent searchParams={searchParams} />
}

function HotelResultsContent({ searchParams }) {
  const navigate = useNavigate()

  const city = searchParams.get('city')
  const checkIn = searchParams.get('checkIn') || ''
  const checkOut = searchParams.get('checkOut') || ''
  const guests = parseInt(searchParams.get('guests') || '2')
  const rooms = parseInt(searchParams.get('rooms') || '1')

  const [page, setPage] = useState(0)
  const [sortBy, setSortBy] = useState('startingPrice')
  const [sortOrder, setSortOrder] = useState('asc')
  const [filters, setFilters] = useState({ stars: [], amenities: [], maxPrice: 30000 })
  const [showMobileFilters, setShowMobileFilters] = useState(false)

  const nights = checkIn && checkOut ? Math.max(1, Math.round((new Date(checkOut) - new Date(checkIn)) / (1000 * 60 * 60 * 24))) : 0;

  // Build query params for the first filter amenity (API supports one at a time)
  const queryAmenity = filters.amenities.length > 0 ? filters.amenities[0] : undefined;

  const { data, isLoading, error } = useQuery({
    queryKey: ['hotels', city, sortBy, sortOrder, page, filters.stars, filters.amenities, filters.maxPrice],
    queryFn: () => searchHotels({
      city, checkIn, checkOut, guests, rooms,
      sortBy, sortOrder, page, size: 20,
      maxStar: filters.stars.length > 0 ? Math.max(...filters.stars) : undefined,
      minStar: filters.stars.length > 0 ? Math.min(...filters.stars) : undefined,
      amenity: queryAmenity,
      maxPrice: filters.maxPrice < 30000 ? filters.maxPrice : undefined,
    }),
  })

  const hotels = data?.content || []
  const totalElements = data?.totalElements || 0
  const totalPages = data?.totalPages || 0

  // Client-side filtering for star rating (API filters are limited)
  const filteredHotels = useMemo(() => {
    let result = [...hotels]
    if (filters.stars.length > 0) result = result.filter(h => filters.stars.includes(h.starRating))
    result = result.filter(h => h.startingPrice <= filters.maxPrice)
    // Filter by additional amenities beyond the first one sent to API
    if (filters.amenities.length > 1) {
      result = result.filter(h => filters.amenities.every(a => (h.amenities || []).includes(a)))
    }
    return result
  }, [hotels, filters])

  const hasActiveFilters = filters.stars.length > 0 || filters.amenities.length > 0 || filters.maxPrice < 30000
  const activeFilterCount = (filters.stars.length > 0 ? 1 : 0) + (filters.amenities.length > 0 ? 1 : 0) + (filters.maxPrice < 30000 ? 1 : 0);

  const handleViewHotel = (hotelId) => {
    const params = new URLSearchParams({ checkIn, checkOut, guests: String(guests), rooms: String(rooms) })
    navigate(`/hotels/${hotelId}?${params.toString()}`)
  }

  const clearFilters = () => setFilters({ stars: [], amenities: [], maxPrice: 30000 });

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-4 md:py-6">
        {/* Search Summary */}
        <div className="bg-white rounded-xl p-3 md:p-4 shadow-sm border border-gray-100 mb-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2 flex-wrap text-sm">
              <span className="font-bold text-gray-900">Hotels in {city}</span>
              {nights > 0 && <span className="text-gray-400">·</span>}
              {nights > 0 && <span className="text-gray-600">{nights} night{nights > 1 ? 's' : ''}</span>}
              <span className="text-gray-400">·</span>
              <span className="text-gray-600">{guests} Guest{guests > 1 ? 's' : ''} · {rooms} Room{rooms > 1 ? 's' : ''}</span>
            </div>
            <div className="text-sm text-gray-500">
              <span className="font-semibold text-gray-800">{totalElements}</span> hotel{totalElements !== 1 ? 's' : ''}
            </div>
          </div>
        </div>

        <div className="flex gap-5">
          {/* Filter Sidebar - Desktop */}
          <div className="w-56 shrink-0 hidden lg:block">
            <div className="bg-white rounded-xl p-4 shadow-sm border border-gray-100 sticky top-20 space-y-4">
              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Sort By</h3>
                <select value={`${sortBy}-${sortOrder}`}
                  onChange={e => { const [s, o] = e.target.value.split('-'); setSortBy(s); setSortOrder(o) }}
                  className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none bg-gray-50">
                  {SORT_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                </select>
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Star Rating</h3>
                <div className="space-y-1">
                  {[5, 4, 3, 2].map(star => (
                    <label key={star} className="flex items-center gap-2 cursor-pointer py-1">
                      <input type="checkbox" checked={filters.stars.includes(star)}
                        onChange={e => {
                          const newStars = e.target.checked ? [...filters.stars, star] : filters.stars.filter(s => s !== star);
                          setFilters({ ...filters, stars: newStars });
                        }}
                        className="w-3.5 h-3.5 text-blue-600 rounded accent-blue-600" />
                      <StarRating rating={star} size="xs" />
                      <span className="text-[10px] text-gray-400">& up</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Max Price/Night</h3>
                <input type="range" min="500" max="30000" step="500" value={filters.maxPrice}
                  onChange={e => setFilters({ ...filters, maxPrice: Number(e.target.value) })}
                  className="w-full accent-blue-600" />
                <div className="text-right text-xs text-gray-600 font-medium">₹{filters.maxPrice.toLocaleString()}</div>
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Amenities</h3>
                <div className="space-y-1 max-h-48 overflow-y-auto">
                  {AMENITY_OPTIONS.map(a => (
                    <label key={a.key} className="flex items-center gap-2 cursor-pointer py-0.5">
                      <input type="checkbox" checked={filters.amenities.includes(a.key)}
                        onChange={e => {
                          const newA = e.target.checked ? [...filters.amenities, a.key] : filters.amenities.filter(x => x !== a.key);
                          setFilters({ ...filters, amenities: newA });
                        }}
                        className="w-3.5 h-3.5 text-blue-600 rounded accent-blue-600" />
                      <span className="text-xs text-gray-600">{a.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              {hasActiveFilters && (
                <button onClick={clearFilters} className="w-full py-2 text-xs text-blue-600 font-medium hover:underline">
                  Clear All Filters
                </button>
              )}
            </div>
          </div>

          {/* Results */}
          <div className="flex-1 min-w-0">
            {/* Mobile Controls */}
            <div className="lg:hidden flex items-center gap-2 mb-3">
              <button onClick={() => setShowMobileFilters(true)}
                className="flex items-center gap-1.5 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 11-3 0m3 0a1.5 1.5 0 10-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-9.75 0h9.75" /></svg>
                Filters {activeFilterCount > 0 && <span className="bg-blue-600 text-white text-[10px] px-1.5 py-0.5 rounded-full">{activeFilterCount}</span>}
              </button>
              <select value={`${sortBy}-${sortOrder}`}
                onChange={e => { const [s, o] = e.target.value.split('-'); setSortBy(s); setSortOrder(o) }}
                className="flex-1 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm text-gray-700 focus:ring-2 focus:ring-blue-500 outline-none">
                {SORT_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
              </select>
            </div>

            {isLoading ? (
              <div className="space-y-3">{[1, 2, 3].map(i => <HotelCardSkeleton key={i} />)}</div>
            ) : error ? (
              <div className="bg-white rounded-xl p-8 text-center shadow-sm border border-gray-100">
                <div className="w-12 h-12 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-3">
                  <svg className="w-6 h-6 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                </div>
                <h3 className="text-base font-semibold text-gray-900 mb-1">Search Failed</h3>
                <p className="text-gray-500 text-sm mb-3">{error.message || 'Unable to fetch hotels.'}</p>
                <button onClick={() => window.location.reload()} className="text-blue-600 text-sm font-medium hover:underline">Try Again</button>
              </div>
            ) : filteredHotels.length === 0 ? (
              <div className="bg-white rounded-xl p-8 text-center shadow-sm border border-gray-100">
                <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3">
                  <svg className="w-6 h-6 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
                </div>
                <h3 className="text-base font-semibold text-gray-900 mb-1">No Hotels Found</h3>
                <p className="text-gray-500 text-sm mb-3">
                  {hasActiveFilters ? 'No hotels match your filters. Try adjusting or clearing them.' : 'No hotels available in this area.'}
                </p>
                {hasActiveFilters && (
                  <button onClick={clearFilters} className="text-blue-600 text-sm font-medium hover:underline">Clear Filters</button>
                )}
              </div>
            ) : (
              <>
                <div className="space-y-3">
                  {filteredHotels.map(hotel => (
                    <div key={hotel.id}
                      onClick={() => handleViewHotel(hotel.id)}
                      className="bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-md hover:border-blue-200 transition-all cursor-pointer group">
                      <div className="flex flex-col sm:flex-row">
                        {/* Image */}
                        <div className="sm:w-52 h-44 sm:h-auto bg-gradient-to-br from-blue-50 to-indigo-50 flex items-center justify-center shrink-0 relative overflow-hidden">
                          {hotel.imageUrl ? (
                            <img
                              src={hotel.imageUrl}
                              alt={hotel.name}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                              onError={(e) => {
                                e.currentTarget.onerror = null;
                                e.currentTarget.src = 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&auto=format&fit=crop&q=80';
                              }}
                            />
                          ) : (
                            <div className="flex flex-col items-center gap-1.5 text-slate-400">
                              <HotelIcon className="w-9 h-9 text-slate-300" />
                              <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">{hotel.city}</span>
                            </div>
                          )}
                          {hotel.starRating >= 4 && (
                            <span className="absolute top-2.5 left-2.5 bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">PREMIUM</span>
                          )}
                          {/* Wishlist Heart */}
                          <button onClick={e => { e.stopPropagation(); }} className="absolute top-2.5 right-2.5 w-7 h-7 bg-white/80 backdrop-blur rounded-full flex items-center justify-center hover:bg-white transition shadow-sm">
                            <svg className="w-3.5 h-3.5 text-gray-500 hover:text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
                          </button>
                        </div>

                        {/* Details */}
                        <div className="flex-1 p-3.5 sm:p-4 flex flex-col justify-between">
                          <div>
                            <div className="flex items-start justify-between gap-2">
                              <div className="min-w-0">
                                <h3 className="font-bold text-gray-900 text-sm sm:text-base group-hover:text-blue-600 transition-colors truncate">{hotel.name}</h3>
                                <div className="flex items-center gap-1.5 mt-0.5">
                                  <StarRating rating={hotel.starRating} />
                                  <span className="text-[10px] text-gray-400">{hotel.starRating}-star</span>
                                  {hotel.address && <span className="text-[10px] text-gray-400">· {hotel.city}</span>}
                                </div>
                              </div>
                              <div className="shrink-0 text-right">
                                <div className="bg-blue-600 text-white px-2 py-1 rounded-md text-xs font-bold">
                                  {hotel.guestRating?.toFixed(1)}
                                </div>
                                <div className="text-[10px] text-gray-400 mt-0.5">{hotel.reviewCount} reviews</div>
                              </div>
                            </div>

                            {hotel.description && (
                              <p className="text-xs text-gray-500 mt-2 line-clamp-2 leading-relaxed">{hotel.description}</p>
                            )}

                            {/* Amenities */}
                            <div className="flex flex-wrap gap-1 mt-2">
                              {(hotel.amenities || []).slice(0, 4).map(a => (
                                <span key={a} className="bg-gray-50 text-gray-600 px-2 py-0.5 rounded text-[10px] border border-gray-100">
                                  {a.replace(/_/g, ' ').toLowerCase()}
                                </span>
                              ))}
                              {(hotel.amenities || []).length > 4 && (
                                <span className="text-gray-400 text-[10px] px-1">+{(hotel.amenities || []).length - 4}</span>
                              )}
                            </div>

                            {/* Cancellation info */}
                            <div className="flex items-center gap-1 mt-2">
                              <svg className="w-3 h-3 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                              <span className="text-[10px] text-emerald-600 font-medium">Free cancellation</span>
                            </div>
                          </div>

                          {/* Price & CTA */}
                          <div className="flex items-end justify-between mt-3 pt-3 border-t border-gray-100">
                            <div>
                              {hotel.startingPrice > 3000 && (
                                <div className="text-[10px] text-gray-400 line-through">₹{(hotel.startingPrice * 1.15).toFixed(0)}</div>
                              )}
                              <div className="text-xl font-bold text-gray-900">₹{hotel.startingPrice?.toLocaleString()}</div>
                              <div className="text-[10px] text-gray-400">per night · {nights > 0 ? `₹${(hotel.startingPrice * nights).toLocaleString()} total` : '+ taxes'}</div>
                            </div>
                            <button onClick={e => { e.stopPropagation(); handleViewHotel(hotel.id) }}
                              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors shrink-0">
                              View Details
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-center gap-1.5 mt-6">
                    <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}
                      className="px-3 py-2 rounded-lg border border-gray-200 text-xs font-medium disabled:opacity-40 hover:bg-gray-50">← Prev</button>
                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => (
                      <button key={i} onClick={() => setPage(i)}
                        className={`w-9 h-9 rounded-lg text-xs font-medium ${page === i ? 'bg-blue-600 text-white' : 'border border-gray-200 hover:bg-gray-50'}`}>{i + 1}</button>
                    ))}
                    <button onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}
                      className="px-3 py-2 rounded-lg border border-gray-200 text-xs font-medium disabled:opacity-40 hover:bg-gray-50">Next →</button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Filter Drawer */}
      <MobileFilterDrawer open={showMobileFilters} onClose={() => setShowMobileFilters(false)}
        filters={filters} setFilters={setFilters} onClear={clearFilters} />
    </div>
  )
}
