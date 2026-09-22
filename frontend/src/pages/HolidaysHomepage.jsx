import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const DESTINATIONS = [
  { name: 'Goa', type: 'DOMESTIC', emoji: '🏖️', desc: 'Beaches & nightlife' },
  { name: 'Kerala', type: 'DOMESTIC', emoji: '🌴', desc: 'Backwaters & hill stations' },
  { name: 'Rajasthan', type: 'DOMESTIC', emoji: '🏰', desc: 'Palaces & desert safari' },
  { name: 'Kashmir', type: 'DOMESTIC', emoji: '🏔️', desc: 'Valleys & houseboats' },
  { name: 'Himachal Pradesh', type: 'DOMESTIC', emoji: '⛰️', desc: 'Mountains & adventure' },
  { name: 'Andaman', type: 'DOMESTIC', emoji: '🏝️', desc: 'Islands & scuba' },
  { name: 'Uttarakhand', type: 'DOMESTIC', emoji: '🛕', desc: 'Char Dham & trekking' },
  { name: 'Ladakh', type: 'DOMESTIC', emoji: '🏍️', desc: 'Road trips & monasteries' },
  { name: 'Bali', type: 'INTERNATIONAL', emoji: '🌺', desc: 'Temples & rice terraces' },
  { name: 'Thailand', type: 'INTERNATIONAL', emoji: '🛕', desc: 'Temples & islands' },
  { name: 'Singapore', type: 'INTERNATIONAL', emoji: '🌆', desc: 'City & theme parks' },
  { name: 'Maldives', type: 'INTERNATIONAL', emoji: '🐠', desc: 'Overwater villas' },
  { name: 'Europe', type: 'INTERNATIONAL', emoji: '🗼', desc: 'Paris, Switzerland & more' },
  { name: 'Dubai', type: 'INTERNATIONAL', emoji: '🏗️', desc: 'Desert safari & malls' },
  { name: 'Japan', type: 'INTERNATIONAL', emoji: '⛩️', desc: 'Cherry blossoms & culture' },
  { name: 'Sri Lanka', type: 'INTERNATIONAL', emoji: '🐾', desc: 'Wildlife & tea gardens' },
];

const DEALS = [
  { title: 'Weekend Getaway Sale', desc: 'Flat 20% off on all domestic holiday packages', tag: 'HOT DEAL', code: 'HOLIDAY20', color: 'from-blue-600 to-indigo-800' },
  { title: 'Honeymoon Special', desc: 'Free room upgrade + couples spa on select packages', tag: 'SPECIAL', code: 'HONEYMOON', color: 'from-pink-500 to-rose-700' },
  { title: 'International Early Bird', desc: 'Book 30 days ahead and save up to ₹15,000', tag: 'EARLY BIRD', code: 'EARLY15K', color: 'from-emerald-500 to-teal-700' },
];

const CATEGORIES = [
  { icon: '🏖️', label: 'Beach', desc: 'Sun, sand & surf' },
  { icon: '⛰️', label: 'Mountain', desc: 'Peaks & valleys' },
  { icon: '🏰', label: 'Heritage', desc: 'History & culture' },
  { icon: '🌿', label: 'Adventure', desc: 'Trekking & sports' },
  { icon: '💑', label: 'Honeymoon', desc: 'Romantic getaways' },
  { icon: '👨‍👩‍👧‍👦', label: 'Family', desc: 'Fun for all ages' },
  { icon: '🧘', label: 'Wellness', desc: 'Spa & rejuvenation' },
  { icon: '🌍', label: 'International', desc: 'Global destinations' },
];

const FEATURES = [
  { title: 'All-Inclusive Packages', desc: 'Flights, hotels, meals, and sightseeing — everything covered', icon: '✈️', color: 'bg-blue-50 text-blue-600 border-blue-200' },
  { title: 'Best Price Guarantee', desc: 'We match any lower price you find, instantly', icon: '💰', color: 'bg-emerald-50 text-emerald-600 border-emerald-200' },
  { title: 'Free Cancellation', desc: 'Cancel up to 48 hours before departure with full refund', icon: '🔄', color: 'bg-purple-50 text-purple-600 border-purple-200' },
  { title: 'Verified Hotels', desc: 'Every property is inspected for quality and safety', icon: '✓', color: 'bg-amber-50 text-amber-600 border-amber-200' },
  { title: 'Expert Local Guides', desc: 'Professional guides who know every destination', icon: '🎓', color: 'bg-cyan-50 text-cyan-600 border-cyan-200' },
  { title: '24/7 Trip Support', desc: 'Help whenever you need it during your holiday', icon: '📞', color: 'bg-rose-50 text-rose-600 border-rose-200' },
];

export default function HolidaysHomepage() {
  const navigate = useNavigate();
  const [destination, setDestination] = useState('');
  const [tripType, setTripType] = useState('');
  const [showDest, setShowDest] = useState(false);
  const [activeFilter, setActiveFilter] = useState('ALL');
  const destRef = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (destRef.current && !destRef.current.contains(e.target)) setShowDest(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (destination) params.set('destination', destination);
    if (tripType) params.set('tripType', tripType);
    navigate(`/holidays/search?${params.toString()}`);
  };

  const filteredDests = activeFilter === 'ALL'
    ? DESTINATIONS
    : DESTINATIONS.filter(d => d.type === activeFilter);

  return (
    <div className="min-h-screen w-full max-w-full overflow-x-hidden bg-gray-50">
      {/* Hero */}
      <section className="relative bg-gradient-to-br from-[#0a1628] via-[#152238] to-[#0d3150] overflow-visible">
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
          <div className="absolute top-0 right-0 w-56 h-56 sm:w-[500px] sm:h-[500px] bg-blue-500/8 rounded-full blur-3xl translate-x-1/4 -translate-y-1/4" />
          <div className="absolute bottom-0 left-0 w-48 h-48 sm:w-[350px] sm:h-[350px] bg-cyan-400/6 rounded-full blur-3xl -translate-x-1/4 translate-y-1/4" />
        </div>

        <div className="relative w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pt-9 pb-10 sm:pt-14 sm:pb-14 md:pt-16 md:pb-20">
          <div className="text-center mb-7 sm:mb-9">
            <h1 className="text-[1.6rem] min-[380px]:text-[1.75rem] sm:text-4xl md:text-5xl leading-tight font-bold text-white mb-3 px-2">
              Holiday Packages for Every Dream
            </h1>
            <p className="text-blue-200/60 text-sm sm:text-base max-w-lg mx-auto px-3 leading-relaxed">
              Curated holidays with flights, hotels, meals, and sightseeing — all inclusive
            </p>
          </div>

          {/* Search Card */}
          <div className="w-full max-w-4xl mx-auto">
            <div className="bg-white rounded-2xl shadow-xl overflow-visible border border-gray-100">
              <form onSubmit={handleSearch} className="p-3.5 sm:p-5 md:p-6">
                <div className="grid grid-cols-1 min-[480px]:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
                  {/* Destination */}
                  <div className="min-[480px]:col-span-2 relative" ref={destRef}>
                    <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">Destination</label>
                    <button type="button" onClick={() => setShowDest(!showDest)}
                      className={`w-full h-14 px-3.5 bg-white border border-gray-200 rounded-xl text-sm text-left flex items-center justify-between focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 outline-none transition ${showDest ? 'border-blue-500 ring-2 ring-blue-500/10' : ''}`}>
                      {destination ? <span className="font-medium text-gray-900 truncate">{destination}</span> : <span className="text-gray-400 truncate">Where do you want to go?</span>}
                      <svg className="w-4 h-4 text-gray-400 shrink-0 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" /></svg>
                    </button>
                    {showDest && (
                      <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-xl border border-gray-200 z-50 max-h-72 overflow-y-auto">
                        <div className="p-2">
                          <div className="flex gap-1 px-2 py-1.5 border-b border-gray-100 mb-1">
                            {['ALL', 'DOMESTIC', 'INTERNATIONAL'].map(f => (
                              <button key={f} type="button" onClick={(e) => { e.stopPropagation(); setActiveFilter(f); }}
                                className={`text-[10px] font-bold px-2 py-1 rounded-full transition ${activeFilter === f ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'}`}>
                                {f === 'ALL' ? 'All' : f === 'DOMESTIC' ? 'Domestic' : 'Intl'}
                              </button>
                            ))}
                          </div>
                          {filteredDests.map(d => (
                            <button key={d.name} type="button"
                              onClick={() => { setDestination(d.name); setTripType(d.type); setShowDest(false); }}
                              className="w-full text-left px-3 py-2.5 text-sm hover:bg-blue-50 rounded-lg flex items-center justify-between gap-2 group">
                              <div className="flex items-center gap-2 min-w-0">
                                <span className="text-base shrink-0">{d.emoji}</span>
                                <div className="min-w-0">
                                  <span className="text-gray-900 font-medium group-hover:text-blue-600">{d.name}</span>
                                  <span className="text-xs text-gray-400 ml-1.5 hidden min-[380px]:inline">{d.desc}</span>
                                </div>
                              </div>
                              <span className="text-[10px] text-gray-400 shrink-0">{d.type === 'DOMESTIC' ? '🇮🇳' : '🌏'}</span>
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Trip Type */}
                  <div className="min-w-0">
                    <label className="block text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">Trip Type</label>
                    <select value={tripType} onChange={e => setTripType(e.target.value)}
                      className="w-full h-14 px-3.5 bg-white border border-gray-200 rounded-xl text-sm text-gray-900 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10 outline-none transition [color-scheme:light]">
                      <option value="">All Types</option>
                      <option value="DOMESTIC">Domestic</option>
                      <option value="INTERNATIONAL">International</option>
                    </select>
                  </div>

                  {/* Search Button */}
                  <div className="flex items-end min-w-0">
                    <button type="submit" className="w-full h-14 bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white rounded-xl font-bold text-sm transition-all shadow-lg shadow-orange-500/20 active:scale-[0.98]">
                      Search Packages
                    </button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </section>

      {/* Trust Bar */}
      <section className="bg-white border-b border-gray-100">
        <div className="w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
          <div className="flex items-center justify-center gap-4 sm:gap-6 flex-wrap text-xs text-gray-500">
            {['Best Price Guarantee', 'Free Cancellation', 'Verified Hotels', 'Secure Payments', '24/7 Support'].map(f => (
              <div key={f} className="flex items-center gap-1.5">
                <svg className="w-3.5 h-3.5 text-green-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"><path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" /></svg>
                <span className="font-medium">{f}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Deals */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900">Holiday Offers</h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {DEALS.map(d => (
            <div key={d.title} className={`bg-gradient-to-br ${d.color} rounded-xl p-4 text-white relative overflow-hidden`}>
              <span className="text-[10px] font-bold bg-white/20 px-2 py-0.5 rounded-full">{d.tag}</span>
              <h3 className="font-bold text-sm mt-2 mb-1">{d.title}</h3>
              <p className="text-xs opacity-80 mb-2">{d.desc}</p>
              <div className="flex items-center gap-2 text-xs">
                <span className="opacity-60">USE:</span>
                <span className="font-bold bg-white/20 px-2 py-0.5 rounded">{d.code}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Popular Destinations */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="text-center mb-6">
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900 mb-1">Popular Holiday Destinations</h2>
          <p className="text-sm text-gray-500">Handpicked destinations for your dream vacation</p>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
          {DESTINATIONS.slice(0, 8).map(d => (
            <button key={d.name}
              onClick={() => navigate(`/holidays/search?destination=${d.name}`)}
              className="bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-md hover:border-blue-200 transition-all text-left group">
              <div className="h-28 bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center relative">
                <span className="text-4xl group-hover:scale-110 transition-transform duration-300">{d.emoji}</span>
                {d.type === 'INTERNATIONAL' && (
                  <span className="absolute top-2 right-2 bg-blue-600 text-white text-[9px] font-bold px-1.5 py-0.5 rounded-full">INTL</span>
                )}
              </div>
              <div className="p-3">
                <h3 className="font-semibold text-gray-900 text-sm group-hover:text-blue-600 transition">{d.name}</h3>
                <p className="text-xs text-gray-400 mt-0.5">{d.desc}</p>
              </div>
            </button>
          ))}
        </div>
        <div className="text-center mt-4">
          <button onClick={() => navigate('/holidays/search')}
            className="text-sm text-blue-600 font-semibold hover:underline">
            View All Destinations →
          </button>
        </div>
      </section>

      {/* Browse by Category */}
      <section className="bg-white border-t border-gray-100 py-8">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900 mb-4 text-center">Browse by Category</h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
            {CATEGORIES.map(c => (
              <button key={c.label}
                onClick={() => navigate('/holidays/search')}
                className="flex flex-col items-center gap-1.5 p-3 rounded-xl hover:bg-blue-50 transition group">
                <span className="text-2xl group-hover:scale-110 transition-transform">{c.icon}</span>
                <span className="text-xs font-semibold text-gray-700 group-hover:text-blue-600">{c.label}</span>
                <span className="text-[10px] text-gray-400 hidden sm:block">{c.desc}</span>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="text-center mb-6">
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900 mb-1">Why Book with TravelPlatform?</h2>
          <p className="text-sm text-gray-500">Everything you need for a perfect holiday</p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {FEATURES.map(f => (
            <div key={f.title} className={`rounded-xl p-4 border ${f.color}`}>
              <div className="flex items-start gap-3">
                <span className="text-xl shrink-0">{f.icon}</span>
                <div>
                  <h3 className="font-bold text-sm mb-0.5">{f.title}</h3>
                  <p className="text-xs opacity-75">{f.desc}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="bg-gradient-to-br from-[#0a1628] via-[#152238] to-[#0d3150] py-12">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h2 className="text-2xl sm:text-3xl font-bold text-white mb-3">Ready to explore?</h2>
          <p className="text-blue-200/60 text-sm sm:text-base mb-6 max-w-md mx-auto">
            Join thousands of travellers who book their dream holidays on TravelPlatform
          </p>
          <button onClick={() => navigate('/holidays/search')}
            className="bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white px-8 py-3.5 rounded-xl font-bold text-sm transition-all shadow-lg shadow-orange-500/20 active:scale-[0.98]">
            Browse All Packages
          </button>
        </div>
      </section>
    </div>
  );
}
