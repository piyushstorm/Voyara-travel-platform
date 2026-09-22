import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { holidayApi } from '../api/holidayApi';

function StarRating({ rating, size = 'sm' }) {
  const cls = size === 'lg' ? 'text-lg' : size === 'md' ? 'text-base' : 'text-sm';
  return (
    <div className="flex gap-0.5">
      {Array.from({ length: 5 }, (_, i) => (
        <span key={i} className={`${cls} ${i < Math.round(rating) ? 'text-amber-400' : 'text-gray-300'}`}>★</span>
      ))}
    </div>
  );
}

export default function HolidayDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [pkg, setPkg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [paxCount, setPaxCount] = useState(2);
  const [showFullGallery, setShowFullGallery] = useState(false);

  useEffect(() => {
    setLoading(true);
    holidayApi.getById(id)
      .then(res => setPkg(res.data?.data))
      .catch(err => setError(err.response?.data?.message || 'Package not found'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-5xl mx-auto px-4 py-6 space-y-4 animate-pulse">
          <div className="h-3 bg-gray-200 rounded w-48" />
          <div className="h-64 bg-gray-200 rounded-2xl" />
          <div className="h-24 bg-gray-100 rounded-xl" />
          <div className="h-48 bg-gray-100 rounded-xl" />
        </div>
      </div>
    );
  }

  if (error || !pkg) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center px-4">
          <div className="text-5xl mb-4">🌴</div>
          <h2 className="text-lg font-semibold text-gray-900 mb-1">Package not found</h2>
          <p className="text-gray-500 text-sm mb-4">{error || 'This package may no longer be available.'}</p>
          <Link to="/holidays" className="text-blue-600 hover:underline text-sm font-medium">Browse all packages</Link>
        </div>
      </div>
    );
  }

  const total = pkg.pricePerPerson * paxCount;
  const savings = pkg.originalPrice ? (pkg.originalPrice - pkg.pricePerPerson) * paxCount : 0;

  const inclusions = pkg.inclusions ? pkg.inclusions.split(',').map(s => s.trim()) : [];
  const exclusions = pkg.exclusions ? pkg.exclusions.split(',').map(s => s.trim()) : [];
  const highlights = pkg.highlights ? pkg.highlights.split(',').map(s => s.trim()) : [];

  // Parse itinerary into day-by-day sections
  const itineraryDays = pkg.itinerary ? pkg.itinerary.split(/Day \d+:/i).filter(Boolean).map(s => s.trim()) : [];

  const handleBook = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/holidays/${id}` } } });
      return;
    }
    navigate(`/booking?type=HOLIDAY&id=${id}&passengers=${paxCount}&nights=${pkg.durationNights}`);
  };

  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'itinerary', label: 'Itinerary' },
    { key: 'inclusions', label: 'Inclusions' },
    { key: 'policies', label: 'Policies' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-4 md:py-6">
        {/* Breadcrumb */}
        <nav className="text-xs text-gray-400 mb-3 flex items-center gap-1">
          <Link to="/holidays" className="hover:text-blue-600 transition-colors">Holidays</Link>
          <span>›</span>
          <span className="text-gray-600">{pkg.destination}</span>
          <span>›</span>
          <span className="text-gray-800 font-medium">{pkg.title}</span>
        </nav>

        {/* Header Card */}
        <div className="bg-white rounded-2xl overflow-hidden shadow-sm border border-gray-100 mb-5">
          {/* Image Gallery */}
          <div className="relative">
            {pkg.imageUrl ? (
              <div className="h-56 sm:h-72 md:h-80">
                <img src={pkg.imageUrl} alt={pkg.title} className="w-full h-full object-cover" />
              </div>
            ) : (
              <div className="h-56 sm:h-72 md:h-80 bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
                <span className="text-7xl">🌴</span>
              </div>
            )}
            {pkg.tripType === 'INTERNATIONAL' && (
              <span className="absolute top-3 left-3 bg-blue-600 text-white text-xs font-bold px-3 py-1 rounded-lg shadow-sm uppercase tracking-wider">International</span>
            )}
            {pkg.originalPrice && pkg.originalPrice > pkg.pricePerPerson && (
              <span className="absolute top-3 right-3 bg-red-500 text-white text-xs font-bold px-3 py-1 rounded-lg shadow-sm">
                {Math.round((1 - pkg.pricePerPerson / pkg.originalPrice) * 100)}% OFF
              </span>
            )}
            {pkg.featured && (
              <span className="absolute bottom-3 left-3 bg-amber-500 text-white text-xs font-bold px-3 py-1 rounded-lg shadow-sm">★ FEATURED</span>
            )}
          </div>

          {/* Header Info */}
          <div className="p-5 sm:p-6">
            <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <h1 className="text-xl sm:text-2xl font-bold text-gray-900 leading-tight">{pkg.title}</h1>
                <div className="flex items-center gap-2 mt-2 flex-wrap">
                  <div className="flex items-center gap-1 text-sm text-gray-500">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" /></svg>
                    {pkg.destination}
                  </div>
                  <span className="text-gray-300">·</span>
                  <span className="text-sm text-gray-500">{pkg.durationDays} Days / {pkg.durationNights} Nights</span>
                  <span className="text-gray-300">·</span>
                  <span className="text-sm text-gray-500">From {pkg.departureCity}</span>
                </div>
                {pkg.rating && (
                  <div className="flex items-center gap-2 mt-2">
                    <StarRating rating={pkg.rating} />
                    <span className="text-sm font-semibold text-gray-900">{pkg.rating}</span>
                    <span className="text-xs text-gray-400">({pkg.reviewCount} reviews)</span>
                  </div>
                )}
              </div>
              <div className="sm:text-right shrink-0">
                {pkg.originalPrice && pkg.originalPrice > pkg.pricePerPerson && (
                  <span className="text-sm text-gray-400 line-through block">₹{pkg.originalPrice.toLocaleString()}</span>
                )}
                <span className="text-2xl sm:text-3xl font-bold text-gray-900">₹{pkg.pricePerPerson.toLocaleString()}</span>
                <span className="text-sm text-gray-500 block">per person</span>
              </div>
            </div>

            {/* Quick Tags */}
            <div className="flex flex-wrap gap-2 mt-4">
              {[pkg.hotelCategory, pkg.mealPlan, pkg.transportType].filter(Boolean).map(t => (
                <span key={t} className="text-xs bg-blue-50 text-blue-600 px-3 py-1 rounded-full font-medium">{t.replace(/_/g, ' ')}</span>
              ))}
              {highlights.slice(0, 3).map((h, i) => (
                <span key={i} className="text-xs bg-gray-100 text-gray-600 px-3 py-1 rounded-full">{h}</span>
              ))}
            </div>
          </div>
        </div>

        <div className="flex flex-col lg:flex-row gap-5">
          {/* Main Content */}
          <div className="flex-1 min-w-0 space-y-5">
            {/* Tabs */}
            <div className="flex gap-0.5 bg-gray-100 rounded-lg p-1 overflow-x-auto scrollbar-hide">
              {tabs.map(tab => (
                <button key={tab.key} onClick={() => setActiveTab(tab.key)}
                  className={`px-3 md:px-4 py-2 rounded-md text-xs md:text-sm font-semibold transition-all whitespace-nowrap ${activeTab === tab.key ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
                  {tab.label}
                </button>
              ))}
            </div>

            {/* Overview */}
            {activeTab === 'overview' && (
              <div className="space-y-5">
                <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                  <h2 className="font-bold text-gray-900 mb-3">About this Package</h2>
                  <p className="text-sm text-gray-600 leading-relaxed">{pkg.description}</p>
                </div>

                {highlights.length > 0 && (
                  <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                    <h2 className="font-bold text-gray-900 mb-3">Highlights</h2>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {highlights.map((h, i) => (
                        <div key={i} className="flex items-center gap-2.5 p-2.5 bg-gray-50 rounded-lg">
                          <div className="w-8 h-8 bg-green-50 rounded-lg flex items-center justify-center shrink-0">
                            <svg className="w-4 h-4 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                          </div>
                          <span className="text-sm text-gray-700">{h}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Quick Facts */}
                <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                  <h2 className="font-bold text-gray-900 mb-3">Quick Facts</h2>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {[
                      { label: 'Duration', value: `${pkg.durationDays}D/${pkg.durationNights}N`, icon: '📅' },
                      { label: 'Hotel', value: pkg.hotelCategory?.replace('_', ' '), icon: '🏨' },
                      { label: 'Meals', value: pkg.mealPlan?.replace(/_/g, ' '), icon: '🍽️' },
                      { label: 'Transport', value: pkg.transportType, icon: '✈️' },
                    ].map(f => (
                      <div key={f.label} className="bg-gray-50 rounded-lg p-3 text-center">
                        <span className="text-xl">{f.icon}</span>
                        <div className="text-[10px] text-gray-400 uppercase tracking-wider mt-1">{f.label}</div>
                        <div className="text-xs font-semibold text-gray-900 mt-0.5">{f.value}</div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Itinerary */}
            {activeTab === 'itinerary' && (
              <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <h2 className="font-bold text-gray-900 mb-4">Day-wise Itinerary</h2>
                {pkg.itinerary ? (
                  <div className="space-y-4">
                    {itineraryDays.map((day, i) => (
                      <div key={i} className="flex gap-3">
                        <div className="w-9 h-9 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold shrink-0 mt-0.5">
                          D{i + 1}
                        </div>
                        <div className="flex-1 border-l-2 border-blue-100 pl-4 pb-1">
                          <h4 className="text-sm font-semibold text-gray-800">Day {i + 1}</h4>
                          <p className="text-sm text-gray-600 mt-1 leading-relaxed">{day}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500">Detailed itinerary will be provided upon booking confirmation.</p>
                )}
              </div>
            )}

            {/* Inclusions */}
            {activeTab === 'inclusions' && (
              <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  <div>
                    <h4 className="font-bold text-green-700 text-sm mb-3 flex items-center gap-1.5">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                      What's Included
                    </h4>
                    <ul className="space-y-2">
                      {inclusions.map((inc, i) => (
                        <li key={i} className="text-sm text-gray-600 flex items-start gap-2">
                          <span className="text-green-500 mt-0.5 shrink-0">✓</span> {inc}
                        </li>
                      ))}
                    </ul>
                  </div>
                  <div>
                    <h4 className="font-bold text-red-700 text-sm mb-3 flex items-center gap-1.5">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
                      What's Excluded
                    </h4>
                    <ul className="space-y-2">
                      {exclusions.map((exc, i) => (
                        <li key={i} className="text-sm text-gray-600 flex items-start gap-2">
                          <span className="text-red-500 mt-0.5 shrink-0">✗</span> {exc}
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            )}

            {/* Policies */}
            {activeTab === 'policies' && (
              <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100 space-y-4">
                <div>
                  <h4 className="font-semibold text-gray-800 text-sm mb-2">Cancellation Policy</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• Free cancellation up to 48 hours before departure — full refund</li>
                    <li>• 3–7 days before departure — 70% refund</li>
                    <li>• 24–72 hours before departure — 50% refund</li>
                    <li>• Less than 24 hours / No-show — No refund</li>
                  </ul>
                </div>
                <div>
                  <h4 className="font-semibold text-gray-800 text-sm mb-2">Child Policy</h4>
                  <p className="text-sm text-gray-600">Children below 5 years travel free (no separate seat). Children 5–11 years get 50% discount. Children 12+ are charged full price.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-gray-800 text-sm mb-2">Payment Terms</h4>
                  <p className="text-sm text-gray-600">25% advance at booking. Remaining 75% due 15 days before departure. Full payment required for bookings within 15 days of departure.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-gray-800 text-sm mb-2">Group Size</h4>
                  <p className="text-sm text-gray-600">Maximum {pkg.maxGroupSize || 20} travellers per group. Private departures available for larger groups on request.</p>
                </div>
              </div>
            )}
          </div>

          {/* Booking Sidebar */}
          <div className="lg:w-80 shrink-0">
            <div className="bg-white rounded-xl p-5 shadow-md border border-gray-100 sticky top-20">
              <h3 className="font-bold text-gray-900 text-sm mb-4">Book This Package</h3>

              <div className="space-y-3 mb-4">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Price per person</span>
                  <span className="font-semibold text-gray-900">₹{pkg.pricePerPerson.toLocaleString()}</span>
                </div>

                {/* Travellers */}
                <div>
                  <label className="text-sm text-gray-500 mb-1 block">Travellers</label>
                  <div className="flex items-center gap-3">
                    <button onClick={() => setPaxCount(Math.max(1, paxCount - 1))}
                      className="w-10 h-10 rounded-full border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-50 disabled:opacity-30 transition"
                      disabled={paxCount <= 1}>−</button>
                    <span className="w-8 text-center font-semibold text-gray-900">{paxCount}</span>
                    <button onClick={() => setPaxCount(Math.min(pkg.maxGroupSize || 20, paxCount + 1))}
                      className="w-10 h-10 rounded-full border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-50 disabled:opacity-30 transition"
                      disabled={paxCount >= (pkg.maxGroupSize || 20)}>+</button>
                  </div>
                </div>
              </div>

              {/* Price Breakdown */}
              <div className="bg-gray-50 rounded-lg p-3 space-y-2 mb-4 text-sm">
                <div className="flex justify-between text-gray-600">
                  <span>₹{pkg.pricePerPerson.toLocaleString()} × {paxCount}</span>
                  <span className="font-medium text-gray-900">₹{total.toLocaleString()}</span>
                </div>
                {savings > 0 && (
                  <div className="flex justify-between text-green-600">
                    <span>You save</span>
                    <span className="font-medium">-₹{savings.toLocaleString()}</span>
                  </div>
                )}
                <div className="flex justify-between text-gray-600">
                  <span>Taxes & fees</span>
                  <span className="font-medium text-gray-900">Included</span>
                </div>
                <div className="border-t border-gray-200 pt-2 flex justify-between font-bold">
                  <span>Total ({paxCount} {paxCount === 1 ? 'person' : 'persons'})</span>
                  <span className="text-blue-600 text-lg">₹{total.toLocaleString()}</span>
                </div>
              </div>

              <button onClick={handleBook}
                className="w-full bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white py-3.5 rounded-xl font-bold text-sm transition-all shadow-lg shadow-orange-500/20 active:scale-[0.98]">
                Book Now
              </button>

              <div className="flex items-center gap-1 mt-2 justify-center">
                <svg className="w-3 h-3 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                <span className="text-[10px] text-emerald-600">Free cancellation up to 48h</span>
              </div>
              <p className="text-[10px] text-gray-400 text-center mt-1">No booking fees · Secure payment</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
