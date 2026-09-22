import { useState, useMemo, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { searchFlights } from '../api/flightApi';
import { FlightCardSkeleton } from '../components/LoadingSkeleton';
import { formatFlightDisplayDate, isPastDate, getTomorrowDate } from '../utils/dateUtils';

const TIME_RANGES = [
  { label: 'Early Morning', value: 'EARLY', icon: '🌅', hours: [0, 6] },
  { label: 'Morning', value: 'MORNING', icon: '☀️', hours: [6, 12] },
  { label: 'Afternoon', value: 'AFTERNOON', icon: '🌤️', hours: [12, 18] },
  { label: 'Night', value: 'NIGHT', icon: '🌙', hours: [18, 24] },
];

function fmtDuration(min) { if (!min) return ''; return `${Math.floor(min/60)}h ${min%60}m`; }
function fmtTime(d) { if (!d) return ''; return new Date(d).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false }); }
function fmtDate(d) { return formatFlightDisplayDate(d); }

export default function FlightResults() {
  const [sp] = useSearchParams();
  const origin = sp.get('origin');
  const destination = sp.get('destination');
  const departureDate = sp.get('departureDate');

  if (!origin || !destination || !departureDate) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-20 text-center">
        <div className="text-5xl mb-4">✈️</div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">No search parameters</h2>
        <p className="text-gray-500 text-sm mb-4">Please search for flights from the home page or navigation.</p>
        <a href="/" className="inline-block bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-xl font-semibold text-sm transition-colors">
          Search Flights
        </a>
      </div>
    );
  }

  return <FlightResultsContent searchParams={sp} />;
}

function FlightResultsContent({ searchParams }) {
  const sp = searchParams;
  const navigate = useNavigate();
  const origin = sp.get('origin');
  const destination = sp.get('destination');
  const departureDate = sp.get('departureDate');
  const cabinClass = sp.get('cabinClass') || 'ECONOMY';
  const passengers = parseInt(sp.get('passengers') || '1');

  // Guard: if incoming departure date is in the past, automatically update to a valid future date
  useEffect(() => {
    if (departureDate && isPastDate(departureDate)) {
      const validDate = getTomorrowDate();
      const newParams = new URLSearchParams(sp);
      newParams.set('departureDate', validDate);
      navigate(`/flights/search?${newParams.toString()}`, { replace: true });
    }
  }, [departureDate, sp, navigate]);

  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('departureTime');
  const [sortOrder, setSortOrder] = useState('asc');
  const [filterStops, setFilterStops] = useState(null);
  const [filterAirlines, setFilterAirlines] = useState([]);
  const [maxPrice, setMaxPrice] = useState(50000);
  const [timeFilter, setTimeFilter] = useState(null);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  const isDateInPast = isPastDate(departureDate);

  const { data, isLoading, error, refetch, isFetching } = useQuery({
    queryKey: ['flights', origin, destination, departureDate, cabinClass, sortBy, sortOrder, page],
    queryFn: () => searchFlights({ origin, destination, departureDate, cabinClass: cabinClass.replace(' ', '_'), sortBy, sortOrder, page, size: 20 }),
    enabled: !isDateInPast && !!origin && !!destination && !!departureDate,
  });

  const flights = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const filtered = useMemo(() => {
    let r = [...flights];
    if (filterStops !== null) r = r.filter(f => f.stops === filterStops);
    if (filterAirlines.length > 0) r = r.filter(f => filterAirlines.includes(f.airlineCode));
    r = r.filter(f => (f.price || 0) <= maxPrice);
    if (timeFilter) {
      const range = TIME_RANGES.find(t => t.value === timeFilter);
      if (range) r = r.filter(f => { const h = new Date(f.departureTime).getHours(); return h >= range.hours[0] && h < range.hours[1]; });
    }
    return r;
  }, [flights, filterStops, filterAirlines, maxPrice, timeFilter]);

  const airlineList = useMemo(() => {
    const m = {}; flights.forEach(f => { if (!m[f.airlineCode]) m[f.airlineCode] = f.airlineName; });
    return Object.entries(m);
  }, [flights]);

  const hasFilters = filterStops !== null || filterAirlines.length > 0 || maxPrice < 50000 || timeFilter;

  const FilterSidebar = ({ className = '' }) => (
    <div className={`w-full bg-white rounded-2xl p-4 sm:p-5 shadow-sm border border-gray-100 sticky top-20 space-y-5 ${className}`}>
      <div>
        <h3 className="font-bold text-gray-900 text-xs uppercase tracking-wider mb-3">Sort By</h3>
        <select value={`${sortBy}-${sortOrder}`} onChange={e => { const [s, o] = e.target.value.split('-'); setSortBy(s); setSortOrder(o); }}
          className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-primary outline-none">
          <option value="departureTime-asc">Earliest Departure</option>
          <option value="departureTime-desc">Latest Departure</option>
          <option value="duration-asc">Shortest Duration</option>
          <option value="price-asc">Cheapest First</option>
          <option value="price-desc">Most Expensive</option>
        </select>
      </div>
      <div>
        <h3 className="font-bold text-gray-900 text-xs uppercase tracking-wider mb-3">Stops</h3>
        <div className="space-y-1">
          {[null, 0, 1, 2].map(s => (
            <label key={s ?? 'all'} className="flex items-center gap-2.5 cursor-pointer py-1.5 px-2 rounded-lg hover:bg-gray-50 transition-colors">
              <input type="radio" checked={filterStops === s} onChange={() => setFilterStops(s)} className="w-4 h-4 text-primary accent-primary" />
              <span className="text-sm text-gray-700">{s === null ? 'All Stops' : s === 0 ? 'Non-stop' : `${s} Stop${s > 1 ? 's' : ''}`}</span>
            </label>
          ))}
        </div>
      </div>
      <div>
        <h3 className="font-bold text-gray-900 text-xs uppercase tracking-wider mb-3">Departure Time</h3>
        <div className="grid grid-cols-2 gap-1.5">
          {TIME_RANGES.map(t => (
            <button key={t.value} onClick={() => setTimeFilter(timeFilter === t.value ? null : t.value)}
              className={`px-2.5 py-2 rounded-xl text-xs font-medium border transition-all ${
                timeFilter === t.value ? 'bg-primary text-white border-primary shadow-sm' : 'bg-gray-50 text-gray-600 border-gray-100 hover:border-primary/30'
              }`}>
              {t.icon} {t.label.split(' ')[0]}
            </button>
          ))}
        </div>
      </div>
      <div>
        <h3 className="font-bold text-gray-900 text-xs uppercase tracking-wider mb-3">Max Price</h3>
        <input type="range" min="0" max="50000" step="500" value={maxPrice} onChange={e => setMaxPrice(Number(e.target.value))}
          className="w-full accent-primary h-1.5" />
        <div className="flex justify-between text-xs text-gray-500 mt-1">
          <span>₹0</span><span className="font-semibold text-gray-700">Up to ₹{maxPrice.toLocaleString()}</span>
        </div>
      </div>
      {airlineList.length > 1 && (
        <div>
          <h3 className="font-bold text-gray-900 text-xs uppercase tracking-wider mb-3">Airlines</h3>
          <div className="space-y-1">
            {airlineList.map(([code, name]) => (
              <label key={code} className="flex items-center gap-2.5 cursor-pointer py-1.5 px-2 rounded-lg hover:bg-gray-50 transition-colors">
                <input type="checkbox" checked={filterAirlines.includes(code)}
                  onChange={e => e.target.checked ? setFilterAirlines([...filterAirlines, code]) : setFilterAirlines(filterAirlines.filter(a => a !== code))}
                  className="w-4 h-4 text-primary accent-primary" />
                <span className="text-sm text-gray-700">{name}</span>
              </label>
            ))}
          </div>
        </div>
      )}
      {hasFilters && (
        <button onClick={() => { setFilterStops(null); setFilterAirlines([]); setMaxPrice(50000); setTimeFilter(null); }}
          className="w-full py-2.5 text-sm text-primary font-semibold hover:bg-primary-light rounded-xl transition-colors">
          Clear All Filters
        </button>
      )}
    </div>
  );

  return (
    <div className="w-full max-w-7xl mx-auto px-3 sm:px-5 lg:px-6 xl:px-8 py-4 sm:py-6 overflow-x-hidden">
      {/* Search Summary */}
      <div className="bg-white rounded-2xl p-3.5 sm:p-4 shadow-sm border border-gray-100 mb-4 sm:mb-5">
        <div className="flex flex-col sm:flex-row sm:flex-wrap sm:items-center sm:justify-between gap-2.5 sm:gap-3">
          <div className="flex items-center gap-x-2.5 gap-y-1.5 flex-wrap text-sm min-w-0">
            <div className="font-bold text-gray-900 text-sm sm:text-base">
              <span className="font-mono">{origin}</span>
              <span className="text-primary mx-1.5">→</span>
              <span className="font-mono">{destination}</span>
            </div>
            <span className="text-gray-300">|</span>
            <span className="text-gray-600">{fmtDate(departureDate)}</span>
            <span className="text-gray-300">|</span>
            <span className="text-gray-600">{passengers} Traveller{passengers > 1 ? 's' : ''}</span>
            <span className="text-gray-300">|</span>
            <span className="text-gray-600">{cabinClass.replace('_', ' ')}</span>
          </div>
          <span className="text-sm text-gray-500"><b className="text-gray-800">{data?.totalElements || 0}</b> flights found</span>
        </div>
      </div>

      <div className="flex flex-col lg:flex-row gap-4 lg:gap-6">
        {/* Desktop sidebar */}
        <aside className="w-full lg:w-60 xl:w-64 shrink-0 hidden lg:block"><FilterSidebar /></aside>

        {/* Mobile Filters + Sort */}
        <div className="lg:hidden w-full mb-3 grid grid-cols-[minmax(104px,auto)_minmax(0,1fr)] gap-2">
          <button
            type="button"
            onClick={() => setMobileFilterOpen(true)}
            className="min-w-0 h-11 sm:h-12 px-3 sm:px-4 bg-white border border-gray-200 rounded-xl shadow-sm flex items-center justify-center gap-1.5 sm:gap-2 text-sm font-semibold text-gray-800"
          >
            <span className="shrink-0">🔍</span>
            <span>Filters</span>
            {hasFilters && (
              <span className="bg-primary text-white text-[10px] px-1.5 py-0.5 rounded-full shrink-0">
                Active
              </span>
            )}
          </button>

          <select
            value={`${sortBy}-${sortOrder}`}
            onChange={e => {
              const [s, o] = e.target.value.split('-');
              setSortBy(s);
              setSortOrder(o);
            }}
            className="min-w-0 w-full h-11 sm:h-12 px-3 bg-white border border-gray-200 rounded-xl text-sm text-gray-700 shadow-sm focus:ring-2 focus:ring-primary outline-none"
            aria-label="Sort flights"
          >
            <option value="departureTime-asc">Earliest</option>
            <option value="departureTime-desc">Latest Departure</option>
            <option value="duration-asc">Shortest Duration</option>
            <option value="price-asc">Cheapest First</option>
            <option value="price-desc">Most Expensive</option>
          </select>
        </div>

        {/* Mobile filter drawer */}
        {mobileFilterOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            <div className="absolute inset-0 bg-black/40" onClick={() => setMobileFilterOpen(false)} />
            <div className="absolute bottom-0 left-0 right-0 bg-white rounded-t-3xl max-h-[88dvh] overflow-y-auto overscroll-contain animate-slide-up">
              <div className="p-4 sm:p-5">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-bold text-lg">Filters</h3>
                  <button onClick={() => setMobileFilterOpen(false)} className="text-gray-400 hover:text-gray-600 text-xl">✕</button>
                </div>
                <FilterSidebar className="!p-0 !shadow-none !border-0 !sticky-0" />
                <button onClick={() => setMobileFilterOpen(false)}
                  className="w-full mt-4 bg-primary text-white py-3 rounded-xl font-semibold">
                  Show {filtered.length} results
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Results */}
        <div className="flex-1 min-w-0">
          {isLoading ? (
            <div className="space-y-3">{[1,2,3,4].map(i => <FlightCardSkeleton key={i} />)}</div>
          ) : error ? (
            <div className="bg-white rounded-3xl p-8 sm:p-12 text-center shadow-sm border border-slate-200/80 max-w-lg mx-auto my-6">
              <div className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto mb-4 border border-amber-100">
                <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-2">Flight Search Unavailable</h3>
              <p className="text-slate-600 text-sm mb-6 leading-relaxed">
                {error?.response?.status === 400
                  ? (error?.response?.data?.message || 'Invalid search parameters. Please verify your origin and destination.')
                  : error?.response?.status === 502 || error?.response?.status === 503
                  ? 'Flight search is temporarily unavailable. Please try again.'
                  : (error?.response?.data?.message || 'We encountered an issue retrieving flights for this route. Please try again.')}
              </p>
              <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={() => refetch()}
                  disabled={isFetching}
                  className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition-all shadow-md shadow-primary/20 flex items-center justify-center gap-2 cursor-pointer"
                >
                  {isFetching ? (
                    <>
                      <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      <span>Retrying...</span>
                    </>
                  ) : (
                    <span>Retry Search</span>
                  )}
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/')}
                  className="w-full sm:w-auto px-6 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition-all cursor-pointer"
                >
                  Modify Search
                </button>
              </div>
            </div>
          ) : filtered.length === 0 ? (
            <div className="bg-white rounded-3xl p-8 sm:p-12 text-center shadow-sm border border-slate-200/80 max-w-lg mx-auto my-6">
              <div className="w-16 h-16 rounded-2xl bg-blue-50 text-primary flex items-center justify-center mx-auto mb-4 border border-blue-100">
                <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-2">No Flights Found</h3>
              <p className="text-slate-600 text-sm max-w-md mx-auto mb-6 leading-relaxed">
                {hasFilters
                  ? 'No flights match your active filter criteria. Try clearing or expanding your filters.'
                  : `No direct or connecting flights found between ${origin} and ${destination} on ${fmtDate(departureDate)}.`}
              </p>
              <div className="text-left bg-slate-50 rounded-2xl p-4 mb-6 border border-slate-100 text-xs text-slate-600 space-y-2">
                <div className="font-bold text-slate-900 text-xs uppercase tracking-wider">Helpful Suggestions</div>
                <div className="flex items-center gap-2">
                  <span className="text-primary font-bold">•</span>
                  <span>Try searching for dates within +/- 2 days.</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-primary font-bold">•</span>
                  <span>Check high-connectivity regional hubs like DEL, BOM, or BLR.</span>
                </div>
                {hasFilters && (
                  <div className="flex items-center gap-2">
                    <span className="text-primary font-bold">•</span>
                    <span>Reset price range, stops, or time-of-day filters.</span>
                  </div>
                )}
              </div>
              <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
                {hasFilters ? (
                  <button
                    type="button"
                    onClick={() => {
                      setFilterStops(null);
                      setFilterAirlines([]);
                      setMaxPrice(50000);
                      setTimeFilter(null);
                    }}
                    className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition-all cursor-pointer"
                  >
                    Reset Filters
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => navigate('/')}
                    className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition-all cursor-pointer"
                  >
                    Modify Route or Date
                  </button>
                )}
              </div>
            </div>
          ) : (
            <div className="space-y-3.5">
              {filtered.map((flight) => (
                <div
                  key={flight.id}
                  className="voyara-card p-4 sm:p-5 hover:border-primary/40 hover:shadow-card-hover transition-all duration-200 cursor-pointer group"
                  onClick={() => navigate(`/flights/${flight.id}?cabinClass=${cabinClass}&passengers=${passengers}`)}
                >
                  <div className="grid grid-cols-[auto_minmax(0,1fr)] sm:flex sm:items-center gap-3 sm:gap-5">
                    {/* Airline Monogram & Code */}
                    <div className="flex items-center gap-3 w-auto sm:w-40 shrink-0">
                      <div className="w-11 h-11 rounded-2xl bg-blue-50 text-primary flex items-center justify-center font-black text-xs border border-blue-100 shadow-sm shrink-0 group-hover:bg-primary group-hover:text-white transition-colors">
                        {flight.airlineCode || 'FL'}
                      </div>
                      <div className="min-w-0 hidden sm:block">
                        <div className="font-bold text-slate-900 text-sm truncate">{flight.airlineName}</div>
                        <div className="text-xs text-slate-400 font-mono">{flight.flightNumber}</div>
                      </div>
                    </div>

                    {/* Flight Timeline Visualizer */}
                    <div className="min-w-0 w-full sm:flex-1">
                      <div className="grid grid-cols-[auto_minmax(60px,1fr)_auto] items-center gap-2 sm:gap-4">
                        <div className="text-center min-w-0">
                          <div className="text-base sm:text-xl font-black text-slate-900 whitespace-nowrap">
                            {fmtTime(flight.departureTime)}
                          </div>
                          <div className="text-xs text-slate-500 font-mono font-bold">{flight.originCode}</div>
                        </div>

                        {/* Visual Route Track */}
                        <div className="min-w-0 text-center px-1">
                          <div className="text-[10px] sm:text-xs text-slate-400 font-semibold mb-1 whitespace-nowrap">
                            {fmtDuration(flight.durationMinutes)}
                          </div>
                          <div className="relative flex items-center justify-center">
                            <div className="w-full h-0.5 bg-slate-200" />
                            <div className="absolute inset-x-0 flex items-center justify-center">
                              <span className="w-2 h-2 rounded-full bg-primary ring-4 ring-white" />
                            </div>
                          </div>
                          <div className="text-[10px] sm:text-xs font-bold mt-1 text-slate-600">
                            {flight.stops === 0 ? (
                              <span className="text-emerald-600">Non-stop</span>
                            ) : (
                              <span className="text-amber-600">{flight.stops} stop{flight.stops > 1 ? 's' : ''}</span>
                            )}
                          </div>
                        </div>

                        <div className="text-center min-w-0">
                          <div className="text-base sm:text-xl font-black text-slate-900 whitespace-nowrap">
                            {fmtTime(flight.arrivalTime)}
                          </div>
                          <div className="text-xs text-slate-500 font-mono font-bold">{flight.destinationCode}</div>
                        </div>
                      </div>
                    </div>

                    {/* Price & Action CTA */}
                    <div className="col-span-2 sm:col-span-1 w-full sm:w-36 sm:shrink-0 flex items-center justify-between gap-3 border-t sm:border-t-0 border-slate-100 pt-3 sm:pt-0 sm:block sm:text-right">
                      <div className="min-w-0">
                        <span className="text-[10px] uppercase font-bold text-slate-400 block sm:mb-0.5">Starting at</span>
                        <div className="text-lg sm:text-2xl font-black text-slate-900">
                          ₹{flight.price?.toLocaleString()}
                        </div>
                        <div className="text-[10px] sm:text-xs text-slate-400 font-medium">
                          {cabinClass.replace('_', ' ')} · Per person
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/flights/${flight.id}?cabinClass=${cabinClass}&passengers=${passengers}`);
                        }}
                        className="travel-button-primary px-5 py-2.5 text-xs font-bold w-auto sm:w-full sm:mt-2 shrink-0"
                      >
                        Select
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex flex-wrap items-center justify-center gap-1.5 sm:gap-2 mt-6">
              <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}
                className="px-3 sm:px-4 py-2 rounded-xl border border-gray-200 text-sm font-medium disabled:opacity-40 hover:bg-gray-50 transition-colors">← Prev</button>
              {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                let p2 = totalPages <= 7 ? i : page < 3 ? i : page > totalPages - 4 ? totalPages - 7 + i : page - 3 + i;
                return (
                  <button key={p2} onClick={() => setPage(p2)}
                    className={`w-9 h-9 sm:w-10 sm:h-10 rounded-xl text-sm font-medium transition-all ${page === p2 ? 'bg-primary text-white shadow-sm' : 'border border-gray-200 hover:bg-gray-50'}`}>
                    {p2 + 1}
                  </button>
                );
              })}
              <button onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}
                className="px-3 sm:px-4 py-2 rounded-xl border border-gray-200 text-sm font-medium disabled:opacity-40 hover:bg-gray-50 transition-colors">Next →</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
