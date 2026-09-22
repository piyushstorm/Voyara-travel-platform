import { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { holidayApi } from '../api/holidayApi';

const SORT_OPTIONS = [
  { value: 'featured', label: 'Recommended' },
  { value: 'price-asc', label: 'Price: Low to High' },
  { value: 'price-desc', label: 'Price: High to Low' },
  { value: 'rating', label: 'Highest Rated' },
  { value: 'duration', label: 'Duration' },
];

const TRIP_TYPES = ['DOMESTIC', 'INTERNATIONAL'];
const HOTEL_CATEGORIES = ['BUDGET', '3_STAR', '4_STAR', '5_STAR', 'LUXURY'];
const MEAL_PLANS = ['BREAKFAST', 'HALF_BOARD', 'FULL_BOARD', 'ALL_INCLUSIVE'];
const TRANSPORT_TYPES = ['FLIGHT', 'CAR', 'BUS', 'TRAIN'];

function PackageCardSkeleton() {
  return (
    <div className="bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 animate-pulse">
      <div className="h-48 bg-gray-200" />
      <div className="p-4 space-y-3">
        <div className="h-5 bg-gray-200 rounded w-3/4" />
        <div className="h-3 bg-gray-100 rounded w-full" />
        <div className="h-3 bg-gray-100 rounded w-1/2" />
        <div className="flex justify-between items-end pt-3 border-t border-gray-100">
          <div className="space-y-1">
            <div className="h-3 bg-gray-100 rounded w-16" />
            <div className="h-6 bg-gray-200 rounded w-24" />
          </div>
          <div className="h-9 bg-blue-200 rounded-lg w-24" />
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
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Trip Type</h4>
            {TRIP_TYPES.map(t => (
              <label key={t} className="flex items-center gap-2 cursor-pointer py-1">
                <input type="radio" checked={filters.tripType === t} onChange={() => setFilters({...filters, tripType: filters.tripType === t ? '' : t})} className="accent-blue-600" />
                <span className="text-sm text-gray-700">{t === 'DOMESTIC' ? 'Domestic' : 'International'}</span>
              </label>
            ))}
          </div>
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Max Price/Person</h4>
            <input type="range" min="5000" max="150000" step="5000" value={filters.maxPrice}
              onChange={e => setFilters({...filters, maxPrice: Number(e.target.value)})} className="w-full accent-blue-600" />
            <div className="text-right text-sm text-gray-700 font-medium">₹{filters.maxPrice.toLocaleString()}</div>
          </div>
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Hotel Category</h4>
            {HOTEL_CATEGORIES.map(c => (
              <label key={c} className="flex items-center gap-2 cursor-pointer py-1">
                <input type="checkbox" checked={filters.categories.includes(c)}
                  onChange={e => {
                    const next = e.target.checked ? [...filters.categories, c] : filters.categories.filter(x => x !== c);
                    setFilters({...filters, categories: next});
                  }} className="w-4 h-4 accent-blue-600 rounded" />
                <span className="text-sm text-gray-700">{c.replace('_', ' ')}</span>
              </label>
            ))}
          </div>
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Meal Plan</h4>
            {MEAL_PLANS.map(m => (
              <label key={m} className="flex items-center gap-2 cursor-pointer py-1">
                <input type="checkbox" checked={filters.mealPlans.includes(m)}
                  onChange={e => {
                    const next = e.target.checked ? [...filters.mealPlans, m] : filters.mealPlans.filter(x => x !== m);
                    setFilters({...filters, mealPlans: next});
                  }} className="w-4 h-4 accent-blue-600 rounded" />
                <span className="text-sm text-gray-700">{m.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}</span>
              </label>
            ))}
          </div>
          <div>
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Transport</h4>
            {TRANSPORT_TYPES.map(t => (
              <label key={t} className="flex items-center gap-2 cursor-pointer py-1">
                <input type="checkbox" checked={filters.transportTypes.includes(t)}
                  onChange={e => {
                    const next = e.target.checked ? [...filters.transportTypes, t] : filters.transportTypes.filter(x => x !== t);
                    setFilters({...filters, transportTypes: next});
                  }} className="w-4 h-4 accent-blue-600 rounded" />
                <span className="text-sm text-gray-700">{t}</span>
              </label>
            ))}
          </div>
        </div>
        <div className="sticky bottom-0 bg-white border-t border-gray-100 p-4 flex gap-3">
          <button onClick={onClear} className="flex-1 py-2.5 border border-gray-200 rounded-lg text-sm font-medium text-gray-600">Clear All</button>
          <button onClick={onClose} className="flex-1 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-semibold">Show Results</button>
        </div>
      </div>
    </div>
  );
}

export default function HolidaysResults() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const destination = params.get('destination') || '';
  const initialTripType = params.get('tripType') || '';

  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sortBy, setSortBy] = useState('featured');
  const [sortOrder, setSortOrder] = useState('desc');
  const [filters, setFilters] = useState({ tripType: initialTripType, maxPrice: 150000, categories: [], mealPlans: [], transportTypes: [] });
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    holidayApi.search({ destination: destination || undefined, tripType: filters.tripType || undefined, maxPrice: filters.maxPrice < 150000 ? filters.maxPrice : undefined })
      .then(res => setPackages(res.data?.data || []))
      .catch(err => setError(err.response?.data?.message || 'Failed to load packages'))
      .finally(() => setLoading(false));
  }, [destination, filters.tripType, filters.maxPrice]);

  const filteredPackages = useMemo(() => {
    let result = [...packages];
    if (filters.categories.length > 0) result = result.filter(p => filters.categories.includes(p.hotelCategory));
    if (filters.mealPlans.length > 0) result = result.filter(p => filters.mealPlans.includes(p.mealPlan));
    if (filters.transportTypes.length > 0) result = result.filter(p => filters.transportTypes.includes(p.transportType));
    return result;
  }, [packages, filters]);

  const sortedPackages = useMemo(() => {
    const arr = [...filteredPackages];
    switch (sortBy) {
      case 'price-asc': return arr.sort((a, b) => a.pricePerPerson - b.pricePerPerson);
      case 'price-desc': return arr.sort((a, b) => b.pricePerPerson - a.pricePerPerson);
      case 'rating': return arr.sort((a, b) => (b.rating || 0) - (a.rating || 0));
      case 'duration': return arr.sort((a, b) => (a.durationNights || 0) - (b.durationNights || 0));
      default: return arr.sort((a, b) => (b.featured ? 1 : 0) - (a.featured ? 1 : 0) || (b.rating || 0) - (a.rating || 0));
    }
  }, [filteredPackages, sortBy]);

  const hasActiveFilters = filters.tripType || filters.categories.length > 0 || filters.mealPlans.length > 0 || filters.transportTypes.length > 0 || filters.maxPrice < 150000;
  const activeFilterCount = (filters.tripType ? 1 : 0) + filters.categories.length + filters.mealPlans.length + filters.transportTypes.length + (filters.maxPrice < 150000 ? 1 : 0);

  const clearFilters = () => setFilters({ tripType: '', maxPrice: 150000, categories: [], mealPlans: [], transportTypes: [] });

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-4 md:py-6">
        {/* Header */}
        <div className="bg-white rounded-xl p-3 md:p-4 shadow-sm border border-gray-100 mb-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2 flex-wrap text-sm">
              <span className="font-bold text-gray-900">{destination ? `Packages in ${destination}` : 'All Holiday Packages'}</span>
              {filters.tripType && <><span className="text-gray-400">·</span><span className="text-gray-600">{filters.tripType === 'DOMESTIC' ? 'Domestic' : 'International'}</span></>}
            </div>
            <div className="text-sm text-gray-500">
              <span className="font-semibold text-gray-800">{sortedPackages.length}</span> packages
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
                  onChange={e => { const [s, o] = e.target.value.split('-'); setSortBy(s); setSortOrder(o || 'desc'); }}
                  className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 outline-none bg-gray-50">
                  {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Trip Type</h3>
                {TRIP_TYPES.map(t => (
                  <label key={t} className="flex items-center gap-2 cursor-pointer py-1">
                    <input type="radio" checked={filters.tripType === t}
                      onChange={() => setFilters({...filters, tripType: filters.tripType === t ? '' : t})}
                      className="w-3.5 h-3.5 accent-blue-600" />
                    <span className="text-xs text-gray-600">{t === 'DOMESTIC' ? 'Domestic' : 'International'}</span>
                  </label>
                ))}
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Max Price/Person</h3>
                <input type="range" min="5000" max="150000" step="5000" value={filters.maxPrice}
                  onChange={e => setFilters({...filters, maxPrice: Number(e.target.value)})} className="w-full accent-blue-600" />
                <div className="text-right text-xs text-gray-600 font-medium">₹{filters.maxPrice.toLocaleString()}</div>
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Hotel Category</h3>
                {HOTEL_CATEGORIES.map(c => (
                  <label key={c} className="flex items-center gap-2 cursor-pointer py-0.5">
                    <input type="checkbox" checked={filters.categories.includes(c)}
                      onChange={e => {
                        const next = e.target.checked ? [...filters.categories, c] : filters.categories.filter(x => x !== c);
                        setFilters({...filters, categories: next});
                      }} className="w-3.5 h-3.5 accent-blue-600 rounded" />
                    <span className="text-xs text-gray-600">{c.replace('_', ' ')}</span>
                  </label>
                ))}
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Meal Plan</h3>
                {MEAL_PLANS.map(m => (
                  <label key={m} className="flex items-center gap-2 cursor-pointer py-0.5">
                    <input type="checkbox" checked={filters.mealPlans.includes(m)}
                      onChange={e => {
                        const next = e.target.checked ? [...filters.mealPlans, m] : filters.mealPlans.filter(x => x !== m);
                        setFilters({...filters, mealPlans: next});
                      }} className="w-3.5 h-3.5 accent-blue-600 rounded" />
                    <span className="text-xs text-gray-600">{m.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}</span>
                  </label>
                ))}
              </div>

              <div>
                <h3 className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-2">Transport</h3>
                {TRANSPORT_TYPES.map(t => (
                  <label key={t} className="flex items-center gap-2 cursor-pointer py-0.5">
                    <input type="checkbox" checked={filters.transportTypes.includes(t)}
                      onChange={e => {
                        const next = e.target.checked ? [...filters.transportTypes, t] : filters.transportTypes.filter(x => x !== t);
                        setFilters({...filters, transportTypes: next});
                      }} className="w-3.5 h-3.5 accent-blue-600 rounded" />
                    <span className="text-xs text-gray-600">{t}</span>
                  </label>
                ))}
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
                onChange={e => { const [s, o] = e.target.value.split('-'); setSortBy(s); setSortOrder(o || 'desc'); }}
                className="flex-1 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm text-gray-700 focus:ring-2 focus:ring-blue-500 outline-none">
                {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>

            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {[1, 2, 3].map(i => <PackageCardSkeleton key={i} />)}
              </div>
            ) : error ? (
              <div className="bg-white rounded-xl p-8 text-center shadow-sm border border-gray-100">
                <div className="w-12 h-12 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-3">
                  <svg className="w-6 h-6 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" /></svg>
                </div>
                <h3 className="text-base font-semibold text-gray-900 mb-1">Search Failed</h3>
                <p className="text-gray-500 text-sm mb-3">{error}</p>
                <button onClick={() => window.location.reload()} className="text-blue-600 text-sm font-medium hover:underline">Try Again</button>
              </div>
            ) : sortedPackages.length === 0 ? (
              <div className="bg-white rounded-xl p-8 text-center shadow-sm border border-gray-100">
                <div className="text-4xl mb-3">🌴</div>
                <h3 className="text-base font-semibold text-gray-900 mb-1">No Packages Found</h3>
                <p className="text-gray-500 text-sm mb-3">
                  {hasActiveFilters ? 'No packages match your filters. Try adjusting them.' : 'No packages available for this destination.'}
                </p>
                {hasActiveFilters && (
                  <button onClick={clearFilters} className="text-blue-600 text-sm font-medium hover:underline">Clear Filters</button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {sortedPackages.map(pkg => (
                  <div key={pkg.id}
                    className="bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-md hover:border-blue-200 transition-all cursor-pointer group"
                    onClick={() => navigate(`/holidays/${pkg.id}`)}>
                    {/* Image */}
                    <div className="h-48 relative overflow-hidden">
                      {pkg.imageUrl ? (
                        <img src={pkg.imageUrl} alt={pkg.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                      ) : (
                        <div className="w-full h-full bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
                          <span className="text-5xl">🌴</span>
                        </div>
                      )}
                      {pkg.tripType === 'INTERNATIONAL' && (
                        <span className="absolute top-3 left-3 bg-blue-600 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm uppercase tracking-wider">International</span>
                      )}
                      {pkg.originalPrice && pkg.originalPrice > pkg.pricePerPerson && (
                        <span className="absolute top-3 right-3 bg-red-500 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">
                          {Math.round((1 - pkg.pricePerPerson / pkg.originalPrice) * 100)}% OFF
                        </span>
                      )}
                      {pkg.featured && (
                        <span className="absolute bottom-3 left-3 bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">FEATURED</span>
                      )}
                    </div>

                    {/* Content */}
                    <div className="p-4">
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <h3 className="font-bold text-gray-900 text-sm group-hover:text-blue-600 transition line-clamp-1">{pkg.title}</h3>
                        {pkg.rating && (
                          <span className="shrink-0 bg-blue-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-md">{pkg.rating}★</span>
                        )}
                      </div>

                      <div className="flex items-center gap-1 text-xs text-gray-500 mb-2">
                        <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" /></svg>
                        {pkg.destination}
                        <span className="text-gray-300">·</span>
                        {pkg.durationDays}D/{pkg.durationNights}N
                      </div>

                      <p className="text-xs text-gray-500 mb-3 line-clamp-2">{pkg.description}</p>

                      {/* Tags */}
                      <div className="flex flex-wrap gap-1 mb-3">
                        <span className="text-[10px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{pkg.hotelCategory?.replace('_', ' ')}</span>
                        <span className="text-[10px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{pkg.mealPlan?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}</span>
                        <span className="text-[10px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{pkg.transportType}</span>
                      </div>

                      {/* Highlights */}
                      {pkg.highlights && (
                        <div className="flex flex-wrap gap-1 mb-3">
                          {pkg.highlights.split(',').slice(0, 3).map((h, i) => (
                            <span key={i} className="text-[10px] bg-blue-50 text-blue-600 px-2 py-0.5 rounded-full">{h.trim()}</span>
                          ))}
                          {pkg.highlights.split(',').length > 3 && (
                            <span className="text-[10px] text-gray-400">+{pkg.highlights.split(',').length - 3} more</span>
                          )}
                        </div>
                      )}

                      {/* Price */}
                      <div className="flex items-end justify-between pt-3 border-t border-gray-100">
                        <div>
                          {pkg.originalPrice && pkg.originalPrice > pkg.pricePerPerson && (
                            <span className="text-xs text-gray-400 line-through block">₹{pkg.originalPrice.toLocaleString()}</span>
                          )}
                          <span className="text-lg font-bold text-gray-900">₹{pkg.pricePerPerson.toLocaleString()}</span>
                          <span className="text-xs text-gray-500 ml-1">per person</span>
                        </div>
                        <span className="text-xs text-blue-600 font-semibold group-hover:underline shrink-0">View Details →</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <MobileFilterDrawer open={showMobileFilters} onClose={() => setShowMobileFilters(false)}
        filters={filters} setFilters={setFilters} onClear={clearFilters} />
    </div>
  );
}
