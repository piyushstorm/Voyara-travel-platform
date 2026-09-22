import { useState, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { busApi } from '../api/busApi';
import { useAuth } from '../context/AuthContext';
import { BusIcon } from '../components/common/Icons';

const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Chennai', 'Kolkata', 'Hyderabad', 'Jaipur', 'Pune', 'Lucknow', 'Kochi', 'Goa', 'Shimla', 'Ahmedabad', 'Coimbatore', 'Jodhpur', 'Bhopal', 'Nagpur', 'Varanasi'];
const BUS_TYPES = ['AC_SLEEPER', 'AC_SEATER', 'NON_AC_SLEEPER', 'NON_AC_SEATER', 'SEMI_SLEEPER', 'VOLVO', 'LUXURY'];
const BUS_TYPE_LABELS = { AC_SLEEPER: 'AC Sleeper', AC_SEATER: 'AC Seater', NON_AC_SLEEPER: 'Non-AC Sleeper', NON_AC_SEATER: 'Non-AC Seater', SEMI_SLEEPER: 'Semi-Sleeper', VOLVO: 'Volvo', LUXURY: 'Luxury' };

function CityPicker({ value, onChange, label, exclude }) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const filtered = CITIES.filter(c => c !== exclude && c.toLowerCase().includes(q.toLowerCase()));
  return (
    <div className="relative">
      <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">{label}</label>
      <button type="button" onClick={() => setOpen(!open)}
        className="w-full h-14 px-3.5 bg-white border border-gray-200 rounded-xl text-sm text-left flex items-center justify-between focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 outline-none transition">
        {value ? <span className="font-medium text-gray-900">{value}</span> : <span className="text-gray-400">Select city</span>}
        <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" /></svg>
      </button>
      {open && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-xl border border-gray-200 z-50 max-h-64 overflow-y-auto">
          <div className="p-2">
            <input type="text" value={q} onChange={e => setQ(e.target.value)} placeholder="Search cities..."
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm mb-1 outline-none focus:ring-1 focus:ring-blue-500" autoFocus />
            {filtered.map(c => (
              <button key={c} type="button" onClick={() => { onChange(c); setOpen(false); setQ(''); }}
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-blue-50 rounded-lg text-gray-700 font-medium">{c}</button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function BusCardSkeleton() {
  return (
    <div className="bg-white rounded-xl border p-5 animate-pulse">
      <div className="flex items-center justify-between"><div className="space-y-2 flex-1"><div className="h-5 bg-gray-200 rounded w-48" /><div className="h-4 bg-gray-100 rounded w-64" /></div><div className="h-10 bg-gray-200 rounded w-24" /></div>
    </div>
  );
}

export default function BusesPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [origin, setOrigin] = useState(params.get('origin') || '');
  const [destination, setDestination] = useState(params.get('destination') || '');
  const [searched, setSearched] = useState(false);
  const [sortBy, setSortBy] = useState('departure');
  const [filterType, setFilterType] = useState([]);
  const [showFilters, setShowFilters] = useState(false);
  const [expandedBus, setExpandedBus] = useState(null);

  const swap = () => { setOrigin(destination); setDestination(origin); };

  const handleSearch = (e) => {
    e.preventDefault();
    if (!origin || !destination) return;
    if (origin === destination) { setError('Origin and destination cannot be the same'); return; }
    setLoading(true); setError(null); setSearched(true); setExpandedBus(null);
    busApi.search(origin, destination, sortBy)
      .then(res => setBuses(res.data?.data || []))
      .catch(err => setError(err.response?.data?.message || 'Failed to search buses'))
      .finally(() => setLoading(false));
  };

  const formatDuration = (min) => { const h = Math.floor(min / 60); const m = min % 60; return `${h}h ${m}m`; };

  const filteredBuses = useMemo(() => {
    let result = [...buses];
    if (filterType.length > 0) result = result.filter(b => filterType.includes(b.busType));
    return result;
  }, [buses, filterType]);

  const handleBook = (bus) => {
    if (!isAuthenticated) { navigate('/login'); return; }
    navigate(`/booking?type=BUS&id=${bus.id}&passengers=1&origin=${origin}&destination=${destination}`);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero */}
      <section className="bg-gradient-to-br from-[#0a1628] via-[#152238] to-[#0d3150]">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-10 pb-12">
          <div className="text-center mb-8">
            <h1 className="text-3xl sm:text-4xl font-bold text-white mb-2">Search Buses</h1>
            <p className="text-blue-200/60 text-sm">Affordable bus travel across India with live tracking</p>
          </div>
          <form onSubmit={handleSearch} className="bg-white rounded-2xl shadow-xl p-5 sm:p-6 max-w-4xl mx-auto">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 items-end">
              <CityPicker value={origin} onChange={setOrigin} label="From" exclude={destination} />
              <div className="hidden sm:flex items-end justify-center pb-2">
                <button type="button" onClick={swap} className="w-10 h-10 rounded-full border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition text-gray-400">⇄</button>
              </div>
              <CityPicker value={destination} onChange={setDestination} label="To" exclude={origin} />
              <div className="flex items-end">
                <button type="submit" className="w-full h-14 bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white rounded-xl font-bold text-sm transition-all shadow-lg active:scale-[0.98]">
                  Search Buses
                </button>
              </div>
            </div>
          </form>
        </div>
      </section>

      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {searched && !loading && (
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <h2 className="text-lg font-bold text-gray-900">{origin} → {destination} · {filteredBuses.length} buses</h2>
            <div className="flex items-center gap-2">
              <button onClick={() => setShowFilters(!showFilters)} className="lg:hidden flex items-center gap-1.5 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm font-medium">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 11-3 0m3 0a1.5 1.5 0 10-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-9.75 0h9.75" /></svg>
                Filters
              </button>
              <select value={sortBy} onChange={e => setSortBy(e.target.value)} className="px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none">
                <option value="departure">Departure</option>
                <option value="price">Price</option>
                <option value="duration">Duration</option>
                <option value="rating">Rating</option>
              </select>
            </div>
          </div>
        )}

        <div className="flex gap-5">
          {/* Filter Sidebar */}
          <div className="w-48 shrink-0 hidden lg:block">
            <div className="bg-white rounded-xl p-4 shadow-sm border border-gray-100 sticky top-20 space-y-4">
              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Bus Type</h3>
                {BUS_TYPES.map(t => (
                  <label key={t} className="flex items-center gap-2 cursor-pointer py-0.5">
                    <input type="checkbox" checked={filterType.includes(t)} onChange={() => setFilterType(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t])} className="w-3.5 h-3.5 accent-blue-600 rounded" />
                    <span className="text-xs text-gray-600">{BUS_TYPE_LABELS[t]}</span>
                  </label>
                ))}
              </div>
              {filterType.length > 0 && (
                <button onClick={() => setFilterType([])} className="w-full py-2 text-xs text-blue-600 font-medium hover:underline">Clear Filters</button>
              )}
            </div>
          </div>

          <div className="flex-1 min-w-0">
            {showFilters && (
              <div className="lg:hidden bg-white rounded-xl p-4 shadow-sm border mb-4 space-y-3">
                <div className="flex items-center justify-between"><h3 className="font-bold text-sm">Filters</h3><button onClick={() => setShowFilters(false)} className="text-gray-400">×</button></div>
                <div className="flex flex-wrap gap-2">
                  {BUS_TYPES.map(t => (
                    <button key={t} onClick={() => setFilterType(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t])}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${filterType.includes(t) ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-200'}`}>
                      {BUS_TYPE_LABELS[t]}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {loading && <div className="space-y-3">{[1, 2, 3].map(i => <BusCardSkeleton key={i} />)}</div>}

            {error && (
              <div className="bg-white rounded-xl p-8 text-center shadow-sm border">
                <h3 className="font-semibold text-gray-900 mb-1">{error}</h3>
                <button onClick={() => window.location.reload()} className="text-blue-600 text-sm font-medium hover:underline">Try Again</button>
              </div>
            )}

            {!loading && searched && filteredBuses.length === 0 && !error && (
              <div className="voyara-card p-12 text-center shadow-sm">
                <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
                  <BusIcon className="h-8 w-8" />
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1">No buses found</h3>
                <p className="text-xs text-slate-500">Try different cities or adjust your route filters</p>
              </div>
            )}

            {!loading && filteredBuses.length > 0 && (
              <div className="space-y-3">
                {filteredBuses.map(bus => {
                  const isExpanded = expandedBus === bus.id;
                  const boardingPts = bus.boardingPoints ? JSON.parse(bus.boardingPoints) : [];
                  const droppingPts = bus.droppingPoints ? JSON.parse(bus.droppingPoints) : [];
                  const amenities = bus.amenities ? bus.amenities.split(',').map(a => a.trim()).filter(Boolean) : [];
                  return (
                    <div key={bus.id} className={`bg-white rounded-xl border p-4 sm:p-5 transition-all ${isExpanded ? 'border-blue-500 ring-1 ring-blue-200' : 'border-gray-100 hover:shadow-md'}`}>
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1 flex-wrap">
                            <span className="text-sm font-bold text-gray-900">{bus.operatorName}</span>
                            <span className="text-[10px] bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-medium">{BUS_TYPE_LABELS[bus.busType] || bus.busType}</span>
                            {bus.rating && <span className="text-[10px] bg-green-100 text-green-700 px-2 py-0.5 rounded font-semibold">★ {bus.rating}</span>}
                          </div>
                          <div className="flex items-center gap-3 text-sm">
                            <div><span className="font-bold text-gray-900">{bus.departureTime}</span> <span className="text-gray-500 text-xs">{bus.originCity}</span></div>
                            <div className="flex-1 flex items-center gap-1 max-w-[180px]">
                              <div className="h-0.5 flex-1 bg-gray-200 rounded" />
                              <span className="text-[10px] text-gray-400 shrink-0">{formatDuration(bus.durationMinutes)}</span>
                              <div className="h-0.5 flex-1 bg-gray-200 rounded" />
                            </div>
                            <div><span className="font-bold text-gray-900">{bus.arrivalTime}</span> <span className="text-gray-500 text-xs">{bus.destinationCity}</span></div>
                          </div>
                          <div className="flex flex-wrap gap-1.5 mt-2">
                            {amenities.slice(0, 4).map(a => (
                              <span key={a} className="text-[10px] bg-gray-100 text-gray-500 px-2 py-0.5 rounded">{a}</span>
                            ))}
                            {amenities.length > 4 && <span className="text-[10px] text-gray-400">+{amenities.length - 4} more</span>}
                          </div>
                        </div>
                        <div className="text-right shrink-0 flex flex-col items-end gap-1">
                          <span className="text-xs text-gray-400">{bus.availableSeats} seats left</span>
                          <span className="text-lg font-bold text-gray-900">₹{bus.basePrice?.toLocaleString()}</span>
                          <button onClick={() => handleBook(bus)}
                            className="bg-blue-600 text-white px-5 py-2 rounded-lg text-xs font-semibold hover:bg-blue-700 transition">
                            Book
                          </button>
                        </div>
                      </div>

                      {/* Expanded Details */}
                      <div className="mt-3 flex items-center gap-3">
                        <button onClick={() => setExpandedBus(isExpanded ? null : bus.id)} className="text-xs text-blue-600 font-medium hover:underline">
                          {isExpanded ? 'Hide details' : 'Boarding & dropping points'}
                        </button>
                      </div>
                      {isExpanded && (
                        <div className="border-t border-gray-100 pt-3 mt-3 grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <div>
                            <h4 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">Boarding Points</h4>
                            {boardingPts.map((p, i) => <div key={i} className="text-xs text-gray-600 py-1">📍 {p}</div>)}
                          </div>
                          <div>
                            <h4 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">Dropping Points</h4>
                            {droppingPts.map((p, i) => <div key={i} className="text-xs text-gray-600 py-1">📍 {p}</div>)}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {!searched && (
              <div className="bg-white rounded-xl border p-12 text-center shadow-sm">
                <div className="text-4xl mb-4">🚌</div>
                <h3 className="font-bold text-gray-900 mb-2">Search for buses</h3>
                <p className="text-sm text-gray-500">Select origin and destination to find available buses</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
