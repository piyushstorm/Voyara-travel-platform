import { useState, useRef, useEffect } from 'react';

const AIRPORTS = [
  { code: 'DEL', city: 'Delhi', name: 'Indira Gandhi Intl' },
  { code: 'BOM', city: 'Mumbai', name: 'Chhatrapati Shivaji' },
  { code: 'BLR', city: 'Bangalore', name: 'Kempegowda Intl' },
  { code: 'MAA', city: 'Chennai', name: 'Chennai Intl' },
  { code: 'CCU', city: 'Kolkata', name: 'NSC Bose Intl' },
  { code: 'HYD', city: 'Hyderabad', name: 'Rajiv Gandhi Intl' },
  { code: 'PNQ', city: 'Pune', name: 'Pune Airport' },
  { code: 'GOI', city: 'Goa', name: 'Goa Intl' },
  { code: 'JAI', city: 'Jaipur', name: 'Jaipur Intl' },
  { code: 'LKO', city: 'Lucknow', name: 'Chaudhary Charan Singh' },
  { code: 'AMD', city: 'Ahmedabad', name: 'Sardar Vallabhbhai Patel' },
  { code: 'COK', city: 'Kochi', name: 'Cochin Intl' },
  { code: 'IXC', city: 'Chandigarh', name: 'Chandigarh Intl' },
  { code: 'PAT', city: 'Patna', name: 'Jay Prakash Narayan' },
  { code: 'TRV', city: 'Trivandrum', name: 'Trivandrum Intl' },
];

const RECENT_KEY = 'tp_recent_airports';

function getRecent() {
  try { return JSON.parse(localStorage.getItem(RECENT_KEY) || '[]'); } catch { return []; }
}

function saveRecent(airport) {
  const recent = getRecent().filter(a => a.code !== airport.code);
  recent.unshift(airport);
  localStorage.setItem(RECENT_KEY, JSON.stringify(recent.slice(0, 5)));
}

export default function AirportPicker({ label, value, onChange, placeholder = 'City or airport' }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [recent, setRecent] = useState(getRecent);
  const inputRef = useRef(null);
  const containerRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const selected = AIRPORTS.find(a => a.code === value);

  const filtered = query.length > 0
    ? AIRPORTS.filter(a =>
        a.code.toLowerCase().includes(query.toLowerCase()) ||
        a.city.toLowerCase().includes(query.toLowerCase()) ||
        a.name.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 8)
    : AIRPORTS.slice(0, 6);

  const selectAirport = (airport) => {
    onChange(airport.code);
    setQuery('');
    setOpen(false);
    saveRecent(airport);
    setRecent(getRecent());
  };

  return (
    <div className="relative" ref={containerRef}>
      <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">{label}</label>
      <button
        type="button"
        onClick={() => { setOpen(!open); setTimeout(() => inputRef.current?.focus(), 100); }}
        className="w-full text-left px-4 py-3.5 bg-white border border-gray-200 rounded-xl hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
      >
        {selected ? (
          <div className="flex items-center gap-3">
            <span className="text-lg font-bold text-primary">{selected.code}</span>
            <div>
              <div className="text-sm font-semibold text-gray-900">{selected.city}</div>
              <div className="text-xs text-gray-500">{selected.name}</div>
            </div>
          </div>
        ) : (
          <span className="text-gray-400 text-sm">{placeholder}</span>
        )}
      </button>

      {open && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-2xl shadow-xl border border-gray-100 z-50 overflow-hidden animate-[slideDown_0.15s_ease-out]">
          <div className="p-3 border-b border-gray-100">
            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search city or airport..."
              className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              autoFocus
            />
          </div>

          <div className="max-h-72 overflow-y-auto">
            {recent.length > 0 && query.length === 0 && (
              <div className="p-3">
                <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider px-2 mb-2">Recent</div>
                {recent.map((a) => (
                  <button
                    key={a.code}
                    onClick={() => selectAirport(a)}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-gray-50 text-left transition-colors"
                  >
                    <span className="text-xs text-gray-400">🕐</span>
                    <span className="font-bold text-primary text-sm">{a.code}</span>
                    <span className="text-sm text-gray-700">{a.city}</span>
                    <span className="text-xs text-gray-400 ml-auto">{a.name}</span>
                  </button>
                ))}
              </div>
            )}

            <div className="p-3">
              <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider px-2 mb-2">
                {query.length > 0 ? 'Results' : 'Popular'}
              </div>
              {filtered.length === 0 ? (
                <div className="text-center py-6 text-gray-400 text-sm">No airports found</div>
              ) : (
                filtered.map((airport) => (
                  <button
                    key={airport.code}
                    onClick={() => selectAirport(airport)}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-primary-light text-left transition-colors group"
                  >
                    <span className="w-10 h-10 rounded-lg bg-gray-100 group-hover:bg-primary/10 flex items-center justify-center font-bold text-primary text-sm transition-colors">
                      {airport.code}
                    </span>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-semibold text-gray-900">{airport.city}</div>
                      <div className="text-xs text-gray-500 truncate">{airport.name}</div>
                    </div>
                    <span className="text-xs text-gray-400 font-mono">{airport.code}</span>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
