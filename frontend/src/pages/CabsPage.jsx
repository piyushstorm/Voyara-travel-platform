import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { cabApi } from '../api/cabApi';
import { CabIcon, AlertTriangleIcon } from '../components/common/Icons';
import { getTodayDate } from '../utils/dateUtils';

const CAB_TYPES = [
  { key: 'LOCAL', label: 'Local', icon: '🏙️', desc: 'City rides', sub: '8 hrs / 80 km' },
  { key: 'AIRPORT', label: 'Airport', icon: '✈️', desc: 'Airport transfers', sub: 'Drop only' },
  { key: 'OUTSTATION', label: 'Outstation', icon: '🛣️', desc: 'Intercity travel', sub: 'One way / Round trip' },
];

const CITIES = [
  'Mumbai', 'Delhi', 'Bangalore', 'Pune', 'Chennai', 'Kolkata', 'Hyderabad',
  'Jaipur', 'Goa', 'Ahmedabad', 'Lucknow', 'Chandigarh', 'Coimbatore', 'Indore',
];

const VEHICLE_DATA = {
  SEDAN: { img: 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=400&h=250&fit=crop', color: 'from-blue-500 to-blue-700' },
  SUV: { img: 'https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?w=400&h=250&fit=crop', color: 'from-emerald-500 to-emerald-700' },
  HATCHBACK: { img: 'https://images.unsplash.com/photo-1502877338535-766e1452684a?w=400&h=250&fit=crop', color: 'from-amber-500 to-orange-600' },
  LUXURY: { img: 'https://images.unsplash.com/photo-1563720223185-11003d516935?w=400&h=250&fit=crop', color: 'from-purple-500 to-purple-700' },
  TEMPO: { img: 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=400&h=250&fit=crop', color: 'from-red-500 to-red-700' },
};

function CabAutocomplete({ value, onChange, placeholder, icon }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState(value || '');
  const ref = useRef(null);
  const filtered = CITIES.filter(c => c.toLowerCase().includes(query.toLowerCase()));

  useEffect(() => {
    setQuery(value || '');
  }, [value]);

  useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div ref={ref} className="relative">
      <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">{icon}</label>
      <input
        type="text" value={query}
        onChange={e => { setQuery(e.target.value); onChange(''); }}
        onFocus={() => setOpen(true)}
        placeholder={placeholder}
        className="w-full h-12 px-3.5 bg-white border border-gray-200 rounded-xl text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 outline-none transition-all"
      />
      {open && query.length > 0 && filtered.length > 0 && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl z-30 max-h-48 overflow-y-auto">
          {filtered.map(city => (
            <button key={city} type="button"
              onClick={() => { setQuery(city); onChange(city); setOpen(false); }}
              className="w-full text-left px-4 py-2.5 text-sm hover:bg-blue-50 transition-colors flex items-center gap-2">
              <span className="text-gray-400">📍</span> {city}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function CabsPage() {
  const navigate = useNavigate();
  const [cabs, setCabs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cabType, setCabType] = useState('LOCAL');
  const [pickup, setPickup] = useState('');
  const [drop, setDrop] = useState('');
  const [searchDate, setSearchDate] = useState('');
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    cabApi.getAll()
      .then(res => setCabs(res.data?.data || []))
      .catch(err => setError(err.response?.data?.message || 'Failed to load cabs'))
      .finally(() => setLoading(false));
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (!pickup.trim()) return;
    setSearched(true);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-[#0a1628] via-[#152238] to-[#0d3150] relative overflow-hidden">
        <div className="absolute inset-0 opacity-5">
          <div className="absolute top-10 left-10 w-96 h-96 bg-blue-400 rounded-full blur-3xl" />
          <div className="absolute bottom-0 right-10 w-80 h-80 bg-purple-400 rounded-full blur-3xl" />
        </div>
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pt-10 pb-14 relative">
          <div className="text-center mb-8">
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-white mb-3">
              Book a <span className="text-transparent bg-clip-text bg-gradient-to-r from-orange-400 to-red-400">Reliable Cab</span>
            </h1>
            <p className="text-blue-200/60 text-sm sm:text-base max-w-md mx-auto">
              Safe, affordable rides at your doorstep — local, airport & outstation
            </p>
          </div>

          {/* Cab Type Selector */}
          <div className="flex justify-center gap-3 mb-6 flex-wrap">
            {CAB_TYPES.map(t => (
              <button key={t.key} onClick={() => setCabType(t.key)}
                className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                  cabType === t.key
                    ? 'bg-white text-gray-900 shadow-lg'
                    : 'bg-white/10 text-white/70 hover:bg-white/20 hover:text-white'
                }`}>
                <span className="text-lg">{t.icon}</span>
                <div className="text-left">
                  <div className="text-xs font-bold">{t.label}</div>
                  <div className="text-[10px] opacity-70">{t.sub}</div>
                </div>
              </button>
            ))}
          </div>

          {/* Search Form */}
          <form onSubmit={handleSearch} className="bg-white rounded-2xl shadow-2xl p-5 sm:p-6 max-w-4xl mx-auto">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
              <CabAutocomplete value={pickup} onChange={setPickup} placeholder="Pickup location" icon="Pickup" />
              <CabAutocomplete value={drop} onChange={setDrop} placeholder="Drop location" icon="Drop" />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">Date</label>
                <input type="date" value={searchDate} onChange={e => setSearchDate(e.target.value)}
                  min={getTodayDate()}
                  className="w-full h-12 px-3.5 bg-white border border-gray-200 rounded-xl text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 outline-none" />
              </div>
              <div className="flex items-end">
                <button type="submit"
                  className="w-full h-12 bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white rounded-xl font-bold text-sm transition-all shadow-lg active:scale-[0.98]">
                  Find Cabs
                </button>
              </div>
            </div>
          </form>
        </div>
      </section>

      {/* Features Bar */}
      <div className="bg-white border-b border-gray-100">
        <div className="max-w-5xl mx-auto px-4 py-4 flex justify-center gap-6 sm:gap-10 text-xs text-gray-600 flex-wrap">
          <span className="flex items-center gap-1.5"><span className="text-green-500 font-bold">✓</span> Verified Drivers</span>
          <span className="flex items-center gap-1.5"><span className="text-green-500 font-bold">✓</span> No Hidden Charges</span>
          <span className="flex items-center gap-1.5"><span className="text-green-500 font-bold">✓</span> 24/7 Support</span>
          <span className="flex items-center gap-1.5"><span className="text-green-500 font-bold">✓</span> GPS Tracking</span>
        </div>
      </div>

      {/* Vehicles */}
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Available Vehicles</h2>
            <p className="text-sm text-gray-500 mt-1">{cabs.length} vehicle types available</p>
          </div>
        </div>

        {loading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {[1,2,3].map(i => (
              <div key={i} className="bg-white rounded-2xl border border-gray-100 overflow-hidden animate-pulse">
                <div className="h-40 bg-gray-200" />
                <div className="p-5 space-y-3">
                  <div className="h-5 bg-gray-200 rounded w-2/3" />
                  <div className="h-4 bg-gray-200 rounded w-1/3" />
                  <div className="h-8 bg-gray-200 rounded w-1/2" />
                </div>
              </div>
            ))}
          </div>
        )}

        {error && (
          <div className="voyara-card border-red-200 p-8 text-center">
            <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-red-500">
              <AlertTriangleIcon className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-slate-900 mb-1 text-base">Unable to load vehicles</h3>
            <p className="text-xs text-slate-500 mb-4">{error}</p>
            <button onClick={() => window.location.reload()} className="travel-button-primary h-10 px-5 text-xs font-bold">
              Retry
            </button>
          </div>
        )}

        {!loading && cabs.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {cabs.map(cab => {
              const vData = VEHICLE_DATA[cab.vehicleType] || VEHICLE_DATA.SEDAN;
              return (
                <div key={cab.id} className="bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-lg transition-all duration-300 group">
                  {/* Vehicle Image */}
                  <div className="relative h-40 overflow-hidden">
                    <img src={vData.img} alt={cab.vehicleName} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" loading="lazy"
                      onError={e => { e.target.style.display = 'none'; }} />
                    <div className={`absolute inset-0 bg-gradient-to-t ${vData.color} opacity-60`} />
                    <div className="absolute bottom-3 left-4 right-4 flex items-end justify-between">
                      <div>
                        <h3 className="font-bold text-white text-lg">{cab.vehicleName}</h3>
                        <span className="text-xs text-white/80">{cab.vehicleType}</span>
                      </div>
                      {cab.rating && (
                        <span className="bg-white/20 backdrop-blur-sm text-white text-xs font-bold px-2 py-1 rounded-lg">
                          ★ {Number(cab.rating).toFixed(1)}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Details */}
                  <div className="p-5">
                    <div className="flex items-center gap-4 text-xs text-gray-500 mb-4">
                      <span className="flex items-center gap-1">👤 {cab.capacity} seats</span>
                      <span className="flex items-center gap-1">🧳 {cab.luggageCapacity} bags</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${cab.ac ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                        {cab.ac ? 'AC' : 'Non-AC'}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs text-gray-500 mb-4">
                      {cabType === 'LOCAL' && (
                        <>
                          <div className="bg-gray-50 rounded-lg p-2 text-center">
                            <div className="font-bold text-gray-900">{cab.localHours || 8} hrs</div>
                            <div>Time limit</div>
                          </div>
                          <div className="bg-gray-50 rounded-lg p-2 text-center">
                            <div className="font-bold text-gray-900">{cab.localKm || 80} km</div>
                            <div>Distance</div>
                          </div>
                        </>
                      )}
                      {cabType === 'AIRPORT' && (
                        <div className="bg-gray-50 rounded-lg p-2 text-center col-span-2">
                          <div className="font-bold text-gray-900">Airport Transfer</div>
                          <div>Meet & greet at terminal</div>
                        </div>
                      )}
                      {cabType === 'OUTSTATION' && (
                        <>
                          <div className="bg-gray-50 rounded-lg p-2 text-center">
                            <div className="font-bold text-gray-900">One Way</div>
                            <div>Drop trip</div>
                          </div>
                          <div className="bg-gray-50 rounded-lg p-2 text-center">
                            <div className="font-bold text-gray-900">Round Trip</div>
                            <div>Return trip</div>
                          </div>
                        </>
                      )}
                    </div>

                    {/* Price */}
                    <div className="border-t border-gray-100 pt-4 flex items-end justify-between">
                      <div>
                        <div className="flex items-baseline gap-1">
                          <span className="text-2xl font-bold text-gray-900">₹{cab.baseFare}</span>
                          <span className="text-xs text-gray-500">base fare</span>
                        </div>
                        <div className="text-xs text-gray-400 mt-0.5">₹{cab.perKmRate}/km · ₹{cab.perMinRate || 12}/min</div>
                      </div>
                      <button
                        onClick={() => {
                          if (pickup && drop) {
                            navigate(`/booking?type=CAB&id=${cab.id}&pickup=${encodeURIComponent(pickup)}&drop=${encodeURIComponent(drop)}&cabType=${cabType}`);
                          } else {
                            alert('Please enter pickup and drop locations first');
                          }
                        }}
                        className="bg-blue-600 text-white px-5 py-2.5 rounded-xl text-sm font-bold hover:bg-blue-700 transition-all shadow-sm active:scale-95">
                        Book Now
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {!loading && cabs.length === 0 && !error && (
          <div className="bg-white rounded-2xl border border-gray-100 p-12 text-center">
            <div className="text-5xl mb-4">🚕</div>
            <h3 className="font-bold text-gray-900 mb-2">No vehicles available right now</h3>
            <p className="text-sm text-gray-500">Please check back later or try a different location.</p>
          </div>
        )}
      </div>

      {/* Why Book Section */}
      <div className="bg-white border-t border-gray-100">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
          <h2 className="text-xl font-bold text-gray-900 text-center mb-8">Why Book with TravelPlatform?</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {[
              { icon: '🛡️', title: 'Verified Drivers', desc: 'All drivers are background verified' },
              { icon: '💰', title: 'No Surge Pricing', desc: 'Fixed transparent pricing' },
              { icon: '📱', title: 'Live Tracking', desc: 'Track your cab in real-time' },
              { icon: '💳', title: 'Easy Payment', desc: 'Cash, UPI or card' },
            ].map((f, i) => (
              <div key={i} className="text-center">
                <div className="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center mx-auto mb-3 text-2xl">{f.icon}</div>
                <h3 className="font-bold text-gray-900 text-sm mb-1">{f.title}</h3>
                <p className="text-xs text-gray-500">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
