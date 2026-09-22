import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTodayDate } from '../utils/dateUtils';
import GuestRoomSelector from '../components/search/GuestRoomSelector';

const POPULAR_CITIES = ['Mumbai', 'Delhi', 'Goa', 'Bangalore', 'Jaipur', 'Chennai', 'Hyderabad', 'Kolkata'];

export default function HotelSearchPage() {
  const navigate = useNavigate();
  const [city, setCity] = useState('');
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [roomConfig, setRoomConfig] = useState([{ adults: 2, children: 0, childAges: [] }]);
  const guests = roomConfig.reduce((s, r) => s + r.adults + r.children, 0);
  const rooms = roomConfig.length;
  const [showGuests, setShowGuests] = useState(false);
  const [errors, setErrors] = useState({});
  const guestRef = useRef(null);

  useEffect(() => {
    const handler = (e) => { if (guestRef.current && !guestRef.current.contains(e.target)) setShowGuests(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const today = getTodayDate();

  const handleSearch = (e) => {
    e.preventDefault();
    setErrors({});
    const newErrors = {};
    if (!city.trim()) newErrors.city = 'Enter a destination city';
    if (!checkIn) newErrors.checkIn = 'Select check-in date';
    if (!checkOut) newErrors.checkOut = 'Select check-out date';
    if (checkIn && checkOut && checkOut <= checkIn) newErrors.checkOut = 'Check-out must be after check-in';
    if (Object.keys(newErrors).length > 0) { setErrors(newErrors); return; }
    navigate(`/hotels/search?city=${encodeURIComponent(city.trim())}&checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}&rooms=${rooms}`);
  };

  return (
    <div className="min-h-[80vh] bg-background">
      <div className="section-shell max-w-4xl pt-10 pb-16">
        <div className="text-center mb-8">
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-primary">Hotel search</p>
          <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">Find a stay that fits</h1>
          <p className="mt-2 text-sm text-slate-500">Search destinations, dates, rooms, and guests together.</p>
        </div>

        <div className="voyara-card p-5 shadow-lg sm:p-8">
          <form onSubmit={handleSearch}>
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
              <div className="md:col-span-5">
                <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Destination</label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
                  <input type="text" value={city} onChange={e => setCity(e.target.value)} placeholder="City or hotel name" required
                    className="voyara-input pl-10" />
                </div>
                {errors.city && <p className="text-red-500 text-xs mt-1">{errors.city}</p>}
                <div className="flex flex-wrap gap-1.5 mt-2">
                  {POPULAR_CITIES.map(c => (
                    <button key={c} type="button" onClick={() => setCity(c)}
                      className={`rounded-lg px-2.5 py-1 text-xs font-medium transition-colors ${city === c ? 'bg-primary-light text-primary' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
                      {c}
                    </button>
                  ))}
                </div>
              </div>
              <div className="md:col-span-2">
                <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Check-in</label>
                <input type="date" value={checkIn} onChange={e => setCheckIn(e.target.value)} min={today} required
                    className="voyara-input [color-scheme:light]" />
                {errors.checkIn && <p className="text-red-500 text-xs mt-1">{errors.checkIn}</p>}
              </div>
              <div className="md:col-span-2">
                <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Check-out</label>
                <input type="date" value={checkOut} onChange={e => setCheckOut(e.target.value)} min={checkIn || today} required
                    className="voyara-input [color-scheme:light]" />
                {errors.checkOut && <p className="text-red-500 text-xs mt-1">{errors.checkOut}</p>}
              </div>
              <div className="md:col-span-3 relative" ref={guestRef}>
                <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Guests & Rooms</label>
                <button type="button" onClick={() => setShowGuests(!showGuests)}
                  className="voyara-input flex items-center justify-between text-left transition">
                  <span className="font-medium text-gray-900">{guests} Guest{guests !== 1 ? 's' : ''} · {rooms} Room{rooms !== 1 ? 's' : ''}</span>
                  <svg className={`w-4 h-4 text-gray-400 transition-transform ${showGuests ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" /></svg>
                </button>
                {showGuests && <GuestRoomSelector rooms={roomConfig} setRooms={setRoomConfig} triggerRef={guestRef} onClose={() => setShowGuests(false)} />}
              </div>
              <div className="md:col-span-12">
                <button type="submit" className="travel-button-primary w-full py-3.5 text-sm active:scale-[0.98]">
                  Search Hotels
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
