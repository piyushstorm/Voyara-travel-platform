import { useState } from 'react'
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getHotelById, getHotelRooms } from '../api/hotelApi'
import { useAuth } from '../context/AuthContext'
import ReviewForm from '../components/ReviewForm'
import ReviewList from '../components/ReviewList'
import ReviewSection from '../components/reviews/ReviewSection'
import { reviewApi, recommendationApi } from '../api/phase3Api'
import RecommendationCard from '../components/RecommendationCard'
import RoomSelectionGrid from '../components/RoomSelectionGrid'

function StarRating({ rating, size = 'sm' }) {
  const cls = size === 'lg' ? 'text-lg' : size === 'md' ? 'text-base' : 'text-sm';
  return (
    <div className="flex gap-0.5">
      {Array.from({ length: 5 }, (_, i) => (
        <span key={i} className={`${cls} ${i < rating ? 'text-amber-400' : 'text-gray-300'}`}>★</span>
      ))}
    </div>
  );
}

const POLICIES = [
  { label: 'Check-in', value: '2:00 PM', icon: '📅' },
  { label: 'Check-out', value: '11:00 AM', icon: '📅' },
  { label: 'Cancellation', value: 'Free cancellation up to 24 hours before check-in', icon: '✓' },
  { label: 'Child Policy', value: 'Children under 5 stay free with existing bedding', icon: '👶' },
  { label: 'Pet Policy', value: 'Pets not allowed', icon: '🐾' },
  { label: 'Payment', value: 'Cash and card accepted. ID required at check-in.', icon: '💳' },
];

const GUEST_RATING_LABEL = (r) => {
  if (r >= 9) return 'Exceptional';
  if (r >= 8) return 'Excellent';
  if (r >= 7) return 'Very Good';
  if (r >= 6) return 'Good';
  return 'Average';
};

export default function HotelDetail() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const checkIn = searchParams.get('checkIn') || ''
  const checkOut = searchParams.get('checkOut') || ''
  const guests = parseInt(searchParams.get('guests') || '2')
  const rooms = parseInt(searchParams.get('rooms') || '1')

  const [activeTab, setActiveTab] = useState('overview')
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [selectedRoomQty, setSelectedRoomQty] = useState(1)
  const [showFullGallery, setShowFullGallery] = useState(false)
  const [showWishlist, setShowWishlist] = useState(false)

  const { data: hotel, isLoading: hotelLoading } = useQuery({
    queryKey: ['hotel', id],
    queryFn: () => getHotelById(id),
  })

  const { data: roomsList, isLoading: roomsLoading } = useQuery({
    queryKey: ['hotelRooms', id],
    queryFn: () => getHotelRooms(id),
  })

  const { data: reviewSummary } = useQuery({
    queryKey: ['hotelReviewSummary', id],
    queryFn: async () => { const res = await reviewApi.getHotelSummary(id); return res.data; },
    enabled: !!id,
  })

  const { data: reviewsData } = useQuery({
    queryKey: ['hotelReviews', id],
    queryFn: async () => { const res = await reviewApi.getHotelReviews(id, 'NEWEST', 0, 10); return res.data; },
    enabled: !!id,
  })

  const { data: recommendationsData } = useQuery({
    queryKey: ['recommendations', 'HOTEL'],
    queryFn: async () => { const res = await recommendationApi.get('HOTEL'); return res.data; },
    enabled: isAuthenticated,
  })

  const nights = checkIn && checkOut ? Math.max(1, Math.round((new Date(checkOut) - new Date(checkIn)) / (1000 * 60 * 60 * 24))) : 0

  if (hotelLoading || roomsLoading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-6 space-y-4 animate-pulse">
        <div className="h-4 bg-gray-200 rounded w-48" />
        <div className="h-64 bg-gray-200 rounded-xl" />
        <div className="h-24 bg-gray-100 rounded-xl" />
        <div className="h-48 bg-gray-100 rounded-xl" />
      </div>
    )
  }

  if (!hotel) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-20 text-center">
        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <span className="text-2xl">🏨</span>
        </div>
        <h2 className="text-lg font-semibold text-gray-900 mb-1">Hotel not found</h2>
        <p className="text-gray-500 text-sm mb-4">This property may no longer be available.</p>
        <Link to="/hotels" className="text-blue-600 hover:underline text-sm font-medium">Back to search</Link>
      </div>
    )
  }

  const handleBook = (room) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/hotels/${id}` } } })
      return
    }
    navigate(`/booking?type=HOTEL&id=${id}&roomId=${room.id}&nights=${nights || 1}&checkIn=${checkIn}&checkOut=${checkOut}&qty=${selectedRoomQty}`)
  }

  const selectedRoomData = roomsList?.find(r => r.id === selectedRoom)
  const totalPrice = selectedRoomData ? selectedRoomData.pricePerNight * (nights || 1) * selectedRoomQty : 0
  const taxes = Math.round(totalPrice * 0.12)

  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'rooms', label: `Rooms (${roomsList?.length || 0})` },
    { key: 'amenities', label: 'Amenities' },
    { key: 'reviews', label: `Reviews (${hotel.reviewCount || 0})` },
    { key: 'policies', label: 'Policies' },
  ]

  const hotelImages = hotel.imageUrls && hotel.imageUrls.length > 0 ? hotel.imageUrls : []

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-4 md:py-6">
        {/* Breadcrumb */}
        <nav className="text-xs text-gray-400 mb-3 flex items-center gap-1">
          <Link to="/hotels" className="hover:text-blue-600 transition-colors">Hotels</Link>
          <span>›</span>
          <span className="text-gray-600">{hotel.city}</span>
          <span>›</span>
          <span className="text-gray-800 font-medium">{hotel.name}</span>
        </nav>

        {/* Header */}
        <div className="flex items-start justify-between gap-3 mb-3">
          <div className="min-w-0">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 leading-tight">{hotel.name}</h1>
            <div className="flex items-center gap-2 mt-1 flex-wrap">
              <StarRating rating={hotel.starRating} />
              <span className="text-xs text-gray-500">{hotel.starRating}-star hotel</span>
              {hotel.address && <span className="text-xs text-gray-400">· {hotel.address}, {hotel.city}</span>}
            </div>
          </div>
          <div className="shrink-0 flex items-center gap-2">
            <button onClick={() => setShowWishlist(!showWishlist)}
              className="w-9 h-9 bg-white border border-gray-200 rounded-lg flex items-center justify-center hover:bg-gray-50 transition shadow-sm">
              <svg className={`w-4 h-4 ${showWishlist ? 'text-red-500 fill-red-500' : 'text-gray-400'}`} fill={showWishlist ? 'currentColor' : 'none'} viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" /></svg>
            </button>
            <button className="w-9 h-9 bg-white border border-gray-200 rounded-lg flex items-center justify-center hover:bg-gray-50 transition shadow-sm">
              <svg className="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M7.217 10.907a2.25 2.25 0 100 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186l9.566-5.314m-9.566 7.5l9.566 5.314m0 0a2.25 2.25 0 103.935 2.186 2.25 2.25 0 00-3.935-2.186zm0-12.814a2.25 2.25 0 103.933-2.185 2.25 2.25 0 00-3.933 2.185z" /></svg>
            </button>
          </div>
        </div>

        {/* Rating Bar */}
        <div className="flex items-center gap-4 mb-4 flex-wrap">
          <div className="flex items-center gap-2">
            <div className="bg-blue-600 text-white px-2.5 py-1 rounded-md text-sm font-bold">{hotel.guestRating?.toFixed(1)}</div>
            <div>
              <div className="text-xs font-semibold text-gray-900">{GUEST_RATING_LABEL(hotel.guestRating)}</div>
              <div className="text-[10px] text-gray-400">{hotel.reviewCount} reviews</div>
            </div>
          </div>
        </div>

        {/* Image Gallery */}
        <div className="rounded-xl overflow-hidden mb-5 bg-gradient-to-br from-blue-50 to-indigo-50 relative">
          {hotelImages.length > 0 ? (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-1 h-48 md:h-64">
              {hotelImages.slice(0, 4).map((img, i) => (
                <div key={i} className="relative overflow-hidden bg-slate-100">
                  <img
                    src={img}
                    alt={`${hotel.name} ${i + 1}`}
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-500"
                    onError={(e) => {
                      e.currentTarget.onerror = null;
                      e.currentTarget.src = 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&auto=format&fit=crop&q=80';
                    }}
                  />
                </div>
              ))}
            </div>
          ) : (
            <div className="h-48 md:h-64 flex items-center justify-center">
              <div className="text-center">
                <span className="text-5xl">🏨</span>
                <p className="text-xs text-gray-400 mt-2">{hotel.name}</p>
              </div>
            </div>
          )}
          {hotelImages.length > 4 && (
            <button onClick={() => setShowFullGallery(true)}
              className="absolute bottom-3 right-3 bg-black/60 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-black/80 transition">
              📸 All {hotelImages.length} photos
            </button>
          )}
        </div>

        {/* Tabs */}
        <div className="flex gap-0.5 bg-gray-100 rounded-lg p-1 mb-5 overflow-x-auto scrollbar-hide">
          {tabs.map(tab => (
            <button key={tab.key} onClick={() => setActiveTab(tab.key)}
              className={`px-3 md:px-4 py-2 rounded-md text-xs md:text-sm font-semibold transition-all whitespace-nowrap ${activeTab === tab.key ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex gap-5">
          {/* Main Content */}
          <div className="flex-1 min-w-0">
            {/* Overview */}
            {activeTab === 'overview' && (
              <div className="space-y-5">
                <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                  <h2 className="font-bold text-gray-900 mb-2">About {hotel.name}</h2>
                  <p className="text-sm text-gray-600 leading-relaxed">{hotel.description || 'A wonderful property in the heart of ' + hotel.city + '.'}</p>
                </div>
                {hotel.amenities && hotel.amenities.length > 0 && (
                  <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                    <h2 className="font-bold text-gray-900 mb-3">Popular Amenities</h2>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                      {hotel.amenities.slice(0, 6).map(a => (
                        <div key={a} className="flex items-center gap-2 text-sm text-gray-600">
                          <svg className="w-4 h-4 text-blue-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                          {a.replace(/_/g, ' ').toLowerCase()}
                        </div>
                      ))}
                    </div>
                    {hotel.amenities.length > 6 && (
                      <button onClick={() => setActiveTab('amenities')} className="text-blue-600 text-xs font-medium mt-3 hover:underline">
                        View all {hotel.amenities.length} amenities →
                      </button>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Rooms */}
            {activeTab === 'rooms' && (
              <RoomSelectionGrid
                hotelId={id}
                checkIn={checkIn}
                checkOut={checkOut}
                nights={nights || 1}
                selectedRoomId={selectedRoom}
                onRoomSelect={(room) => {
                  setSelectedRoom(room ? room.id : null);
                }}
              />
            )}

            {/* Amenities */}
            {activeTab === 'amenities' && (
              <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <h2 className="font-bold text-gray-900 mb-4">Property Amenities</h2>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {(hotel.amenities || []).map(a => (
                    <div key={a} className="flex items-center gap-2.5 p-2.5 bg-gray-50 rounded-lg">
                      <div className="w-8 h-8 bg-blue-50 rounded-lg flex items-center justify-center shrink-0">
                        <svg className="w-4 h-4 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                      </div>
                      <span className="text-sm text-gray-700 capitalize">{a.replace(/_/g, ' ').toLowerCase()}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Reviews */}
            {activeTab === 'reviews' && (
              <ReviewSection targetType="HOTEL" targetId={hotel.id} targetName={hotel.name} />
            )}

            {/* Policies */}
            {activeTab === 'policies' && (
              <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <h2 className="font-bold text-gray-900 mb-4">Property Policies</h2>
                <div className="space-y-3">
                  {POLICIES.map(p => (
                    <div key={p.label} className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                      <span className="text-base shrink-0">{p.icon}</span>
                      <div>
                        <div className="font-semibold text-gray-900 text-sm">{p.label}</div>
                        <div className="text-xs text-gray-600 mt-0.5">{p.value}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Recommendations */}
            {isAuthenticated && recommendationsData && recommendationsData.length > 0 && activeTab === 'overview' && (
              <div className="mt-5">
                <h2 className="font-bold text-gray-900 mb-3">Recommended for You</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {recommendationsData.slice(0, 2).map(rec => (
                    <RecommendationCard key={rec.id} recommendation={rec} />
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Sticky Booking Summary (Desktop) */}
          {selectedRoomData && (
            <div className="hidden lg:block w-72 shrink-0">
              <div className="bg-white rounded-xl p-5 shadow-md border border-gray-100 sticky top-20">
                <h3 className="font-bold text-gray-900 text-sm mb-3">Booking Summary</h3>
                <div className="space-y-2 text-xs text-gray-600">
                  <div className="flex justify-between"><span>Room</span><span className="font-medium text-gray-900">{selectedRoomData.name}</span></div>
                  <div className="flex justify-between"><span>Check-in</span><span className="font-medium text-gray-900">{checkIn || '—'}</span></div>
                  <div className="flex justify-between"><span>Check-out</span><span className="font-medium text-gray-900">{checkOut || '—'}</span></div>
                  <div className="flex justify-between"><span>Nights</span><span className="font-medium text-gray-900">{nights || 1}</span></div>
                  <div className="flex justify-between"><span>Rooms</span><span className="font-medium text-gray-900">{selectedRoomQty}</span></div>
                </div>
                <div className="border-t border-gray-100 mt-3 pt-3 space-y-1.5 text-xs">
                  <div className="flex justify-between text-gray-600">
                    <span>₹{selectedRoomData.pricePerNight?.toLocaleString()} × {nights || 1} nights × {selectedRoomQty}</span>
                    <span className="font-medium text-gray-900">₹{totalPrice.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between text-gray-600">
                    <span>Taxes & fees (12%)</span>
                    <span className="font-medium text-gray-900">₹{taxes.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between font-bold text-sm text-gray-900 pt-2 border-t border-gray-100">
                    <span>Total</span>
                    <span>₹{(totalPrice + taxes).toLocaleString()}</span>
                  </div>
                </div>
                <button onClick={() => handleBook(selectedRoomData)}
                  className="w-full mt-4 bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-xl font-bold text-sm transition-colors active:scale-[0.98]">
                  Book Now
                </button>
                <div className="flex items-center gap-1 mt-2 justify-center">
                  <svg className="w-3 h-3 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                  <span className="text-[10px] text-emerald-600">Free cancellation up to 24h before check-in</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Mobile Book CTA */}
        {selectedRoomData && (
          <div className="lg:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-3 z-40 shadow-lg">
            <div className="max-w-5xl mx-auto flex items-center justify-between">
              <div>
                <div className="text-xs text-gray-500">{selectedRoomData.name} · {nights || 1} night{(nights || 1) > 1 ? 's' : ''}</div>
                <div className="font-bold text-gray-900">₹{(totalPrice + taxes).toLocaleString()}<span className="text-[10px] font-normal text-gray-400"> total</span></div>
              </div>
              <button onClick={() => handleBook(selectedRoomData)}
                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg font-bold text-sm transition-colors">
                Book Now
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
