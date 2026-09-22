import { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { trainApi } from '../api/trainApi';
import { useAuth } from '../context/AuthContext';
import { TrainIcon, AlertTriangleIcon } from '../components/common/Icons';

const STATIONS = ['Delhi', 'Mumbai', 'Bangalore', 'Chennai', 'Kolkata', 'Hyderabad', 'Jaipur', 'Pune', 'Lucknow', 'Kochi', 'Trivandrum', 'Goa', 'Ahmedabad', 'Varanasi', 'Shimla', 'Coimbatore', 'Jodhpur', 'Bhopal', 'Nagpur', 'Prayagraj'];
const TRAIN_TYPES = ['RAJDHANI', 'SHATABDI', 'VANDE_BHARAT', 'DURONTO', 'EXPRESS', 'MAIL', 'SUPERFAST'];
const CLASS_CODES = ['SL', '3A', '2A', '1A', 'CC', 'EC'];
const CLASS_LABELS = { SL: 'Sleeper', '3A': '3-Tier AC', '2A': '2-Tier AC', '1A': 'First AC', CC: 'Chair Car', EC: 'Executive Chair' };

function StationPicker({ value, onChange, label, exclude }) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const filtered = STATIONS.filter(s => s !== exclude && s.toLowerCase().includes(q.toLowerCase()));
  return (
    <div className="relative">
      <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">{label}</label>
      <button type="button" onClick={() => setOpen(!open)}
        className="w-full h-14 px-3.5 bg-white border border-gray-200 rounded-xl text-sm text-left flex items-center justify-between focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 outline-none transition">
        {value ? <span className="font-medium text-gray-900">{value}</span> : <span className="text-gray-400">Select station</span>}
        <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" /></svg>
      </button>
      {open && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-xl border border-gray-200 z-50 max-h-64 overflow-y-auto">
          <div className="p-2">
            <input type="text" value={q} onChange={e => setQ(e.target.value)} placeholder="Search stations..."
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm mb-1 outline-none focus:ring-1 focus:ring-blue-500" autoFocus />
            {filtered.map(s => (
              <button key={s} type="button" onClick={() => { onChange(s); setOpen(false); setQ(''); }}
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-blue-50 rounded-lg text-gray-700 font-medium">{s}</button>
            ))}
            {filtered.length === 0 && <div className="px-3 py-4 text-center text-sm text-gray-400">No stations found</div>}
          </div>
        </div>
      )}
    </div>
  );
}

function TrainCardSkeleton() {
  return (
    <div className="bg-white rounded-xl border border-gray-100 p-5 animate-pulse">
      <div className="flex items-center justify-between">
        <div className="space-y-2 flex-1"><div className="h-5 bg-gray-200 rounded w-48" /><div className="h-4 bg-gray-100 rounded w-32" /><div className="h-4 bg-gray-100 rounded w-64" /></div>
        <div className="h-10 bg-gray-200 rounded w-24" />
      </div>
    </div>
  );
}

export default function TrainsPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [trains, setTrains] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [origin, setOrigin] = useState(params.get('origin') || '');
  const [destination, setDestination] = useState(params.get('destination') || '');
  const [searched, setSearched] = useState(false);
  const [sortBy, setSortBy] = useState('departure');
  const [filterType, setFilterType] = useState([]);
  const [filterClass, setFilterClass] = useState([]);
  const [showFilters, setShowFilters] = useState(false);
  const [selectedTrain, setSelectedTrain] = useState(null);
  const [selectedClass, setSelectedClass] = useState(null);

  const swap = () => { setOrigin(destination); setDestination(origin); };

  const handleSearch = (e) => {
    e.preventDefault();
    if (!origin || !destination) return;
    if (origin === destination) { setError('Origin and destination cannot be the same'); return; }
    setLoading(true); setError(null); setSearched(true); setSelectedTrain(null);
    trainApi.search(origin, destination, sortBy)
      .then(res => setTrains(res.data?.data || []))
      .catch(err => setError(err.response?.data?.message || 'Failed to search trains'))
      .finally(() => setLoading(false));
  };

  const formatDuration = (min) => { const h = Math.floor(min / 60); const m = min % 60; return `${h}h ${m}m`; };

  const filteredTrains = useMemo(() => {
    let result = [...trains];
    if (filterType.length > 0) result = result.filter(t => filterType.includes(t.trainType));
    if (filterClass.length > 0) result = result.filter(t => t.classes && filterClass.some(c => t.classes.includes(c)));
    return result;
  }, [trains, filterType, filterClass]);

  const handleBook = (train, cls) => {
    if (!isAuthenticated) { navigate('/login'); return; }
    navigate(`/booking?type=TRAIN&id=${train.id}&classCode=${cls.code}&fare=${cls.fare}&passengers=1&origin=${origin}&destination=${destination}`);
  };

  const toggleTypeFilter = (t) => setFilterType(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t]);
  const toggleClassFilter = (c) => setFilterClass(prev => prev.includes(c) ? prev.filter(x => x !== c) : [...prev, c]);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero */}
      <section className="bg-gradient-to-br from-[#0a1628] via-[#152238] to-[#0d3150]">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-10 pb-12">
          <div className="text-center mb-8">
            <h1 className="text-3xl sm:text-4xl font-bold text-white mb-2">Search Trains</h1>
            <p className="text-blue-200/60 text-sm">Find train routes across India with real-time availability</p>
          </div>
          <form onSubmit={handleSearch} className="bg-white rounded-2xl shadow-xl p-5 sm:p-6 max-w-4xl mx-auto">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 items-end">
              <StationPicker value={origin} onChange={setOrigin} label="From" exclude={destination} />
              <div className="hidden sm:flex items-end justify-center pb-2">
                <button type="button" onClick={swap} className="w-10 h-10 rounded-full border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition text-gray-400">⇄</button>
              </div>
              <StationPicker value={destination} onChange={setDestination} label="To" exclude={origin} />
              <div className="flex items-end">
                <button type="submit" className="w-full h-14 bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white rounded-xl font-bold text-sm transition-all shadow-lg active:scale-[0.98]">
                  Search Trains
                </button>
              </div>
            </div>
          </form>
        </div>
      </section>

      {/* Trust Bar */}
      <section className="bg-white border-b border-gray-100">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-center gap-4 sm:gap-6 flex-wrap text-xs text-gray-500">
          {['IRCTC Partner', 'Real-time Availability', 'Instant Refunds', 'PNR Status', '24/7 Support'].map(f => (
            <div key={f} className="flex items-center gap-1.5"><svg className="w-3.5 h-3.5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg><span className="font-medium">{f}</span></div>
          ))}
        </div>
      </section>

      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Results Header */}
        {searched && !loading && (
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <h2 className="text-lg font-bold text-gray-900">{origin} → {destination} · {filteredTrains.length} trains</h2>
            <div className="flex items-center gap-2">
              <button onClick={() => setShowFilters(!showFilters)} className="lg:hidden flex items-center gap-1.5 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm font-medium">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 11-3 0m3 0a1.5 1.5 0 10-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-9.75 0h9.75" /></svg>
                Filters
              </button>
              <select value={sortBy} onChange={e => setSortBy(e.target.value)} className="px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none">
                <option value="departure">Departure</option>
                <option value="duration">Duration</option>
                <option value="arrival">Arrival</option>
                <option value="name">Name</option>
              </select>
            </div>
          </div>
        )}

        <div className="flex gap-5">
          {/* Filter Sidebar - Desktop */}
          <div className="w-48 shrink-0 hidden lg:block">
            <div className="bg-white rounded-xl p-4 shadow-sm border border-gray-100 sticky top-20 space-y-4">
              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Train Type</h3>
                {TRAIN_TYPES.map(t => (
                  <label key={t} className="flex items-center gap-2 cursor-pointer py-0.5">
                    <input type="checkbox" checked={filterType.includes(t)} onChange={() => toggleTypeFilter(t)} className="w-3.5 h-3.5 accent-blue-600 rounded" />
                    <span className="text-xs text-gray-600">{t.replace(/_/g, ' ')}</span>
                  </label>
                ))}
              </div>
              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Class</h3>
                {CLASS_CODES.map(c => (
                  <label key={c} className="flex items-center gap-2 cursor-pointer py-0.5">
                    <input type="checkbox" checked={filterClass.includes(c)} onChange={() => toggleClassFilter(c)} className="w-3.5 h-3.5 accent-blue-600 rounded" />
                    <span className="text-xs text-gray-600">{CLASS_LABELS[c]}</span>
                  </label>
                ))}
              </div>
              {(filterType.length > 0 || filterClass.length > 0) && (
                <button onClick={() => { setFilterType([]); setFilterClass([]); }} className="w-full py-2 text-xs text-blue-600 font-medium hover:underline">Clear Filters</button>
              )}
            </div>
          </div>

          {/* Results */}
          <div className="flex-1 min-w-0">
            {/* Mobile Filters */}
            {showFilters && (
              <div className="lg:hidden bg-white rounded-xl p-4 shadow-sm border mb-4 space-y-3">
                <div className="flex items-center justify-between"><h3 className="font-bold text-sm">Filters</h3><button onClick={() => setShowFilters(false)} className="text-gray-400">×</button></div>
                <div className="flex flex-wrap gap-2">
                  {TRAIN_TYPES.map(t => (
                    <button key={t} onClick={() => toggleTypeFilter(t)}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${filterType.includes(t) ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-200'}`}>
                      {t.replace(/_/g, ' ')}
                    </button>
                  ))}
                </div>
                <div className="flex flex-wrap gap-2">
                  {CLASS_CODES.map(c => (
                    <button key={c} onClick={() => toggleClassFilter(c)}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${filterClass.includes(c) ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-200'}`}>
                      {CLASS_LABELS[c]}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {loading && <div className="space-y-3">{[1, 2, 3].map(i => <TrainCardSkeleton key={i} />)}</div>}

            {error && (
              <div className="bg-white rounded-xl p-8 text-center shadow-sm border">
                <div className="w-12 h-12 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-3">
                  <svg className="w-6 h-6 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                </div>
                <h3 className="font-semibold text-gray-900 mb-1">{error}</h3>
                <button onClick={() => window.location.reload()} className="text-blue-600 text-sm font-medium hover:underline">Try Again</button>
              </div>
            )}

            {!loading && searched && filteredTrains.length === 0 && !error && (
              <div className="voyara-card p-12 text-center shadow-sm">
                <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
                  <TrainIcon className="h-8 w-8" />
                </div>
                <h3 className="text-base font-bold text-slate-900 mb-1">No trains found</h3>
                <p className="text-xs text-slate-500">Try different stations or adjust your filters</p>
              </div>
            )}

            {!loading && filteredTrains.length > 0 && (
              <div className="space-y-3">
                {filteredTrains.map(train => {
                  const classes = train.classes ? JSON.parse(train.classes) : [];
                  const isSelected = selectedTrain === train.id;
                  return (
                    <div key={train.id} className={`bg-white rounded-xl border p-4 sm:p-5 transition-all ${isSelected ? 'border-blue-500 ring-1 ring-blue-200' : 'border-gray-100 hover:shadow-md'}`}>
                      {/* Train Header */}
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1 flex-wrap">
                            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-mono font-bold">{train.trainNumber}</span>
                            <span className="text-sm font-bold text-gray-900">{train.trainName}</span>
                            <span className="text-[10px] bg-gray-100 text-gray-500 px-2 py-0.5 rounded">{train.trainType?.replace(/_/g, ' ')}</span>
                          </div>
                          <div className="flex items-center gap-3 text-sm">
                            <div><span className="font-bold text-gray-900">{train.departureTime}</span> <span className="text-gray-500 text-xs">{train.originCode}</span></div>
                            <div className="flex-1 flex items-center gap-1 max-w-[200px]">
                              <div className="h-0.5 flex-1 bg-gray-200 rounded" />
                              <span className="text-[10px] text-gray-400 shrink-0">{formatDuration(train.durationMinutes)}</span>
                              <div className="h-0.5 flex-1 bg-gray-200 rounded" />
                            </div>
                            <div><span className="font-bold text-gray-900">{train.arrivalTime}</span> <span className="text-gray-500 text-xs">{train.destinationCode}</span></div>
                          </div>
                          {train.runningDays && <p className="text-[10px] text-gray-400 mt-1">Runs: {train.runningDays}</p>}
                          {train.intermediateStations && <p className="text-[10px] text-gray-400">Via: {train.intermediateStations}</p>}
                        </div>
                        <button onClick={() => setSelectedTrain(isSelected ? null : train.id)}
                          className={`px-4 py-2 rounded-lg text-xs font-semibold transition ${isSelected ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}>
                          {isSelected ? 'Hide Classes' : 'View Classes'}
                        </button>
                      </div>

                      {/* Class Options */}
                      {isSelected && (
                        <div className="border-t border-gray-100 pt-3 mt-3">
                          <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Available Classes</h4>
                          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                            {classes.map(cls => (
                              <div key={cls.code} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                                <div>
                                  <div className="text-xs font-semibold text-gray-900">{cls.name}</div>
                                  <div className="text-xs text-gray-500">₹{cls.fare?.toLocaleString()}</div>
                                </div>
                                <button onClick={() => handleBook(train, cls)}
                                  className="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-xs font-semibold hover:bg-blue-700 transition">
                                  Book
                                </button>
                              </div>
                            ))}
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
                <div className="text-4xl mb-4">🚂</div>
                <h3 className="font-bold text-gray-900 mb-2">Search for trains</h3>
                <p className="text-sm text-gray-500">Select origin and destination to find available trains</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
