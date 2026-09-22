import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AirportPicker from '../components/search/AirportPicker.jsx';
import TravellerPicker from '../components/search/TravellerPicker.jsx';
import InteractiveGlobe from '../components/home/InteractiveGlobe.jsx';
import { useAuth } from '../context/AuthContext';
import {
  ShieldCheckIcon,
  ClockIcon,
  CompassIcon,
  SparklesIcon,
  ArrowRightIcon,
  MapPinIcon,
  TagIcon,
} from '../components/common/Icons';
import { getTodayDate, getTomorrowDate, normalizeFlightDate } from '../utils/dateUtils';

const POPULAR_ROUTES = [
  { from: 'DEL', fromCity: 'New Delhi', to: 'BOM', toCity: 'Mumbai', price: '₹2,899', duration: '2h 10m' },
  { from: 'BOM', fromCity: 'Mumbai', to: 'BLR', toCity: 'Bangalore', price: '₹1,999', duration: '1h 45m' },
  { from: 'DEL', fromCity: 'New Delhi', to: 'BLR', toCity: 'Bangalore', price: '₹3,499', duration: '2h 40m' },
  { from: 'DEL', fromCity: 'New Delhi', to: 'GOI', toCity: 'Goa', price: '₹2,499', duration: '2h 30m' },
  { from: 'BOM', fromCity: 'Mumbai', to: 'GOI', toCity: 'Goa', price: '₹1,799', duration: '1h 15m' },
  { from: 'DEL', fromCity: 'New Delhi', to: 'HYD', toCity: 'Hyderabad', price: '₹2,699', duration: '2h 15m' },
];

const INTERNATIONAL_ROUTES = [
  { from: 'DEL', fromCity: 'Delhi', to: 'DXB', toCity: 'Dubai', price: '₹12,999', flag: '🇦🇪' },
  { from: 'BOM', fromCity: 'Mumbai', to: 'SIN', toCity: 'Singapore', price: '₹15,499', flag: '🇸🇬' },
  { from: 'DEL', fromCity: 'Delhi', to: 'BKK', toCity: 'Bangkok', price: '₹11,999', flag: '🇹🇭' },
  { from: 'BLR', fromCity: 'Bangalore', to: 'CMB', toCity: 'Colombo', price: '₹9,999', flag: '🇱🇰' },
];

const TRENDING_DESTINATIONS = [
  { name: 'Goa', code: 'GOI', state: 'Coastal Haven', price: '₹2,499', gradient: 'from-blue-600 via-cyan-600 to-teal-500' },
  { name: 'Mumbai', code: 'BOM', state: 'The Metropolis', price: '₹2,899', gradient: 'from-amber-600 via-orange-600 to-rose-600' },
  { name: 'Delhi', code: 'DEL', state: 'Heritage Capital', price: '₹1,999', gradient: 'from-rose-600 via-red-600 to-amber-600' },
  { name: 'Bangalore', code: 'BLR', state: 'Garden & Tech City', price: '₹1,999', gradient: 'from-emerald-600 via-teal-600 to-cyan-600' },
  { name: 'Jaipur', code: 'JAI', state: 'Royal Palaces', price: '₹2,199', gradient: 'from-pink-600 via-rose-600 to-purple-600' },
  { name: 'Kashmir', code: 'SXR', state: 'Alpine Valleys', price: '₹3,499', gradient: 'from-indigo-600 via-blue-600 to-sky-600' },
  { name: 'Kerala', code: 'COK', state: 'Lush Backwaters', price: '₹2,799', gradient: 'from-teal-600 via-emerald-600 to-lime-600' },
  { name: 'Dubai', code: 'DXB', state: 'Futuristic Luxury', price: '₹12,999', gradient: 'from-amber-500 via-yellow-600 to-orange-700' },
];

const DEALS = [
  { title: 'Weekend Getaway Sale', desc: 'Flat 20% instant discount on flights booked for this weekend.', tag: 'Flight Special', code: 'WEEKEND20', bg: 'from-blue-900 to-slate-900' },
  { title: 'First Booking Bonus', desc: 'Earn 500 Voyara loyalty points on your maiden journey.', tag: 'New Member', code: 'VOYARA500', bg: 'from-emerald-950 to-slate-900' },
  { title: 'Business Cabin Upgrade', desc: 'Complimentary priority check-in and lounge vouchers.', tag: 'Premium Tier', code: 'UPGRADE', bg: 'from-violet-950 to-slate-900' },
];

const INTELLIGENCE_FEATURES = [
  {
    icon: ShieldCheckIcon,
    title: 'Travel Guardian',
    desc: 'Autonomous trip monitoring that evaluates connection buffers, weather alerts, and terminal transfers.',
    color: 'text-blue-600 bg-blue-50',
  },
  {
    icon: ClockIcon,
    title: 'Connection Risk Radar',
    desc: 'Real-time calculation of minimum connection times (MCT) with risk alerts for tight transits.',
    color: 'text-amber-600 bg-amber-50',
  },
  {
    icon: CompassIcon,
    title: 'Live Flight Tracker',
    desc: 'Track actual flight progress, speed, altitude, and delay forecasts on interactive flight maps.',
    color: 'text-cyan-600 bg-cyan-50',
  },
  {
    icon: SparklesIcon,
    title: '15-Min Price Freeze',
    desc: 'Lock promising fares while coordinating with family, free from unpredictable surge spikes.',
    color: 'text-emerald-600 bg-emerald-50',
  },
];

export default function Home() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [tripType, setTripType] = useState('ONE_WAY');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [departureDate, setDepartureDate] = useState('');
  const [returnDate, setReturnDate] = useState('');
  const [adults, setAdults] = useState(1);
  const [childCount, setChildCount] = useState(0);
  const [infants, setInfants] = useState(0);
  const [cabinClass, setCabinClass] = useState('Economy');
  const [isSearching, setIsSearching] = useState(false);
  const [isSwapping, setIsSwapping] = useState(false);
  const [multiCityLegs, setMultiCityLegs] = useState([
    { origin: '', destination: '', date: '' },
    { origin: '', destination: '', date: '' },
  ]);

  const today = getTodayDate();
  const totalTravellers = adults + childCount + infants;

  const handleSwap = () => {
    setIsSwapping(true);
    const temp = from;
    setFrom(to);
    setTo(temp);
    setTimeout(() => setIsSwapping(false), 300);
  };

  const updateMultiCityLeg = (index, field, value) => {
    const newLegs = [...multiCityLegs];
    newLegs[index] = { ...newLegs[index], [field]: value };
    setMultiCityLegs(newLegs);
  };

  const addMultiCityLeg = () => {
    if (multiCityLegs.length < 6) setMultiCityLegs([...multiCityLegs, { origin: '', destination: '', date: '' }]);
  };

  const removeMultiCityLeg = (index) => {
    if (multiCityLegs.length > 2) setMultiCityLegs(multiCityLegs.filter((_, i) => i !== index));
  };

  const handleFlightSearch = (e) => {
    e.preventDefault();
    if (tripType === 'MULTI_CITY') {
      const validLegs = multiCityLegs.filter((leg) => leg.origin && leg.destination && leg.date);
      if (validLegs.length < 2) return;
      setIsSearching(true);
      const params = new URLSearchParams({
        tripType: 'MULTI_CITY',
        cabinClass: cabinClass.replace(' ', '_').toUpperCase(),
        passengers: String(totalTravellers),
      });
      validLegs.forEach((leg, i) => {
        params.set(`origin${i}`, leg.origin);
        params.set(`dest${i}`, leg.destination);
        params.set(`date${i}`, leg.date);
      });
      navigate(`/flights/search?${params.toString()}`);
      return;
    }

    if (!from || !to || !departureDate) return;

    setIsSearching(true);
    navigate(
      `/flights/search?origin=${from}&destination=${to}&departureDate=${departureDate}&cabinClass=${cabinClass
        .replace(' ', '_')
        .toUpperCase()}&passengers=${totalTravellers}&tripType=${tripType}`
    );
  };

  const searchRoute = (fromCode, toCode) => {
    // Preserve valid future user-selected departure date or fallback to valid default date (tomorrow or today)
    const validDate = normalizeFlightDate(departureDate, getTomorrowDate());
    const validClass = cabinClass ? cabinClass.replace(' ', '_').toUpperCase() : 'ECONOMY';
    const validPassengers = totalTravellers > 0 ? totalTravellers : 1;
    navigate(`/flights/search?origin=${fromCode}&destination=${toCode}&departureDate=${validDate}&cabinClass=${validClass}&passengers=${validPassengers}&tripType=ONE_WAY`);
  };

  const searchDestination = (destCode) => {
    const validDate = normalizeFlightDate(departureDate, getTomorrowDate());
    const validClass = cabinClass ? cabinClass.replace(' ', '_').toUpperCase() : 'ECONOMY';
    const validPassengers = totalTravellers > 0 ? totalTravellers : 1;
    navigate(`/flights/search?origin=DEL&destination=${destCode}&departureDate=${validDate}&cabinClass=${validClass}&passengers=${validPassengers}&tripType=ONE_WAY`);
  };

  return (
    <div className="min-w-0 w-full">
      {/* ═══ CINEMATIC HERO SECTION ═══ */}
      <section className="relative overflow-hidden bg-gradient-to-b from-[#071326] via-[#0a1e3f] to-[#0d2752] text-white">
        {/* Subtle Ambient Radial Lighting */}
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute top-0 right-1/4 h-[500px] w-[500px] rounded-full bg-primary/20 blur-[130px]" />
          <div className="absolute bottom-0 left-10 h-[400px] w-[400px] rounded-full bg-cyan-400/10 blur-[110px]" />
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_transparent_0%,_#071326_90%)] opacity-70" />
        </div>

        <div className="relative section-shell pt-10 pb-16 lg:pt-14 lg:pb-20">
          <div className="grid lg:grid-cols-12 gap-8 items-center">
            {/* Left Hero Content & Headline */}
            <div className="lg:col-span-7 xl:col-span-7 space-y-4 text-center lg:text-left">
              <div className="inline-flex items-center gap-2 rounded-full border border-sky-400/25 bg-sky-500/10 px-3.5 py-1.5 text-xs font-semibold tracking-wide text-sky-200 backdrop-blur-md">
                <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>Intelligent Travel Engine · Real-time Guardian</span>
              </div>

              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight text-white leading-[1.1]">
                Where will you <br className="hidden sm:inline" />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-300 via-blue-200 to-cyan-300">
                  go next?
                </span>
              </h1>

              <p className="max-w-xl mx-auto lg:mx-0 text-sm sm:text-base leading-relaxed text-slate-300 font-normal">
                Search verified flights, compare fares, discover smart stays, and navigate with proactive connection risk monitoring.
              </p>

              {/* Trust Indicators Pill List */}
              <div className="pt-2 flex flex-wrap items-center justify-center lg:justify-start gap-x-5 gap-y-2 text-xs font-medium text-slate-300">
                <span className="flex items-center gap-1.5 text-sky-200">
                  <ShieldCheckIcon className="w-4 h-4 text-emerald-400" />
                  Verified Airline Fares
                </span>
                <span className="flex items-center gap-1.5 text-sky-200">
                  <ClockIcon className="w-4 h-4 text-cyan-400" />
                  Live Delay Tracking
                </span>
                <span className="flex items-center gap-1.5 text-sky-200">
                  <SparklesIcon className="w-4 h-4 text-amber-300" />
                  Zero Hidden Fees
                </span>
              </div>
            </div>

            {/* Right Interactive 3D Globe Visual */}
            <div className="lg:col-span-5 xl:col-span-5 flex justify-center">
              <InteractiveGlobe className="w-full max-w-[380px] lg:max-w-[420px]" />
            </div>
          </div>

          {/* ═══ PRIMARY SEARCH MODULE ═══ */}
          <div className="mt-8 sm:mt-10 max-w-6xl mx-auto">
            {/* Main Search Container */}
            <div className="rounded-3xl border border-white/20 bg-white p-4 sm:p-6 lg:p-7 shadow-2xl text-slate-900">
              <form onSubmit={handleFlightSearch} className="space-y-5">
                {/* Trip Type Segmented Control */}
                <div className="inline-flex p-1 bg-slate-100 rounded-xl border border-slate-200/70 max-w-full overflow-x-auto no-scrollbar">
                  {[
                    { key: 'ONE_WAY', label: 'One Way' },
                    { key: 'ROUND_TRIP', label: 'Round Trip' },
                    { key: 'MULTI_CITY', label: 'Multi City' },
                  ].map((option) => (
                    <button
                      key={option.key}
                      type="button"
                      onClick={() => setTripType(option.key)}
                      className={`px-3.5 py-1.5 sm:px-4 sm:py-2 text-xs sm:text-sm font-bold rounded-lg transition-all whitespace-nowrap touch-target ${
                        tripType === option.key
                          ? 'bg-white text-primary shadow-sm'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>

                {tripType !== 'MULTI_CITY' ? (
                  <>
                    {/* Desktop layout: >= 1280px (xl) */}
                    <div className="hidden xl:flex xl:items-end xl:gap-2.5">
                      <div className="flex-[2.5] min-w-0">
                        <AirportPicker label="From" value={from} onChange={setFrom} placeholder="Origin airport or city" />
                      </div>

                      <div className="flex items-center justify-center pb-2 shrink-0">
                        <button
                          type="button"
                          onClick={handleSwap}
                          className={`h-11 w-11 rounded-full border border-slate-200 bg-white hover:bg-primary/5 hover:border-primary text-slate-600 hover:text-primary transition-all duration-300 shadow-sm flex items-center justify-center shrink-0 active:scale-90 ${
                            isSwapping ? 'rotate-180 text-primary border-primary' : ''
                          }`}
                          aria-label="Swap origin and destination"
                          title="Swap cities"
                        >
                          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M7.5 21L3 16.5m0 0L7.5 12M3 16.5h13.5m0-9L21 7.5m0 0L16.5 3M21 7.5H7.5" />
                          </svg>
                        </button>
                      </div>

                      <div className="flex-[2.5] min-w-0">
                        <AirportPicker label="To" value={to} onChange={setTo} placeholder="Destination airport or city" />
                      </div>

                      <div className="flex-[1.8] min-w-0">
                        <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                          Departure
                        </label>
                        <input
                          type="date"
                          value={departureDate}
                          min={today}
                          onChange={(e) => setDepartureDate(e.target.value)}
                          className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                          required
                        />
                      </div>

                      {tripType === 'ROUND_TRIP' && (
                        <div className="flex-[1.8] min-w-0">
                          <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                            Return
                          </label>
                          <input
                            type="date"
                            value={returnDate}
                            min={departureDate || today}
                            onChange={(e) => setReturnDate(e.target.value)}
                            className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                            required
                          />
                        </div>
                      )}

                      <div className="flex-[2] min-w-0">
                        <TravellerPicker
                          adults={adults}
                          setAdults={setAdults}
                          children={childCount}
                          setChildren={setChildCount}
                          infants={infants}
                          setInfants={setInfants}
                          cabinClass={cabinClass}
                          setCabinClass={setCabinClass}
                        />
                      </div>
                    </div>

                    {/* Tablet layout: 768px - 1279px (md to xl) */}
                    <div className="hidden md:flex xl:hidden flex-col gap-3.5">
                      <div className="flex items-end gap-2.5">
                        <div className="flex-1 min-w-0">
                          <AirportPicker label="From" value={from} onChange={setFrom} placeholder="Origin airport or city" />
                        </div>
                        <div className="flex items-center justify-center pb-2 shrink-0">
                          <button
                            type="button"
                            onClick={handleSwap}
                            className={`h-11 w-11 rounded-full border border-slate-200 bg-white hover:bg-primary/5 hover:border-primary text-slate-600 hover:text-primary transition-all duration-300 shadow-sm flex items-center justify-center shrink-0 active:scale-90 ${
                              isSwapping ? 'rotate-180 text-primary border-primary' : ''
                            }`}
                            aria-label="Swap origin and destination"
                            title="Swap cities"
                          >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                              <path strokeLinecap="round" strokeLinejoin="round" d="M7.5 21L3 16.5m0 0L7.5 12M3 16.5h13.5m0-9L21 7.5m0 0L16.5 3M21 7.5H7.5" />
                            </svg>
                          </button>
                        </div>
                        <div className="flex-1 min-w-0">
                          <AirportPicker label="To" value={to} onChange={setTo} placeholder="Destination airport or city" />
                        </div>
                      </div>

                      <div className="grid grid-cols-2 lg:grid-cols-3 gap-3.5 items-end">
                        <div>
                          <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                            Departure
                          </label>
                          <input
                            type="date"
                            value={departureDate}
                            min={today}
                            onChange={(e) => setDepartureDate(e.target.value)}
                            className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                            required
                          />
                        </div>

                        {tripType === 'ROUND_TRIP' && (
                          <div>
                            <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                              Return
                            </label>
                            <input
                              type="date"
                              value={returnDate}
                              min={departureDate || today}
                              onChange={(e) => setReturnDate(e.target.value)}
                              className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                              required
                            />
                          </div>
                        )}

                        <div className={tripType === 'ROUND_TRIP' ? 'col-span-2 lg:col-span-1' : ''}>
                          <TravellerPicker
                            adults={adults}
                            setAdults={setAdults}
                            children={childCount}
                            setChildren={setChildCount}
                            infants={infants}
                            setInfants={setInfants}
                            cabinClass={cabinClass}
                            setCabinClass={setCabinClass}
                          />
                        </div>
                      </div>
                    </div>

                    {/* Mobile layout: < 768px */}
                    <div className="flex md:hidden flex-col gap-3">
                      <div>
                        <AirportPicker label="From" value={from} onChange={setFrom} placeholder="Origin airport or city" />
                      </div>

                      <div className="relative flex items-center justify-center my-0.5">
                        <div className="absolute inset-0 flex items-center">
                          <div className="w-full border-t border-slate-200" />
                        </div>
                        <div className="relative flex justify-center">
                          <button
                            type="button"
                            onClick={handleSwap}
                            className={`h-10 w-10 rounded-full border border-slate-200 bg-white hover:bg-primary/5 hover:border-primary text-slate-600 hover:text-primary transition-all duration-300 shadow-sm flex items-center justify-center active:scale-90 ${
                              isSwapping ? 'rotate-180 text-primary border-primary' : ''
                            }`}
                            aria-label="Swap origin and destination"
                            title="Swap cities"
                          >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2">
                              <path strokeLinecap="round" strokeLinejoin="round" d="M7.5 21L3 16.5m0 0L7.5 12M3 16.5h13.5m0-9L21 7.5m0 0L16.5 3M21 7.5H7.5" />
                            </svg>
                          </button>
                        </div>
                      </div>

                      <div>
                        <AirportPicker label="To" value={to} onChange={setTo} placeholder="Destination airport or city" />
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                          Departure
                        </label>
                        <input
                          type="date"
                          value={departureDate}
                          min={today}
                          onChange={(e) => setDepartureDate(e.target.value)}
                          className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                          required
                        />
                      </div>

                      {tripType === 'ROUND_TRIP' && (
                        <div>
                          <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                            Return
                          </label>
                          <input
                            type="date"
                            value={returnDate}
                            min={departureDate || today}
                            onChange={(e) => setReturnDate(e.target.value)}
                            className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                            required
                          />
                        </div>
                      )}

                      <div>
                        <TravellerPicker
                          adults={adults}
                          setAdults={setAdults}
                          children={childCount}
                          setChildren={setChildCount}
                          infants={infants}
                          setInfants={setInfants}
                          cabinClass={cabinClass}
                          setCabinClass={setCabinClass}
                        />
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="space-y-3.5">
                    {multiCityLegs.map((leg, index) => (
                      <div key={index} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-3.5 sm:p-4">
                        <div className="mb-2 flex items-center justify-between">
                          <span className="text-xs font-bold uppercase tracking-wider text-primary">
                            Flight Leg {index + 1}
                          </span>
                          {multiCityLegs.length > 2 && (
                            <button
                              type="button"
                              onClick={() => removeMultiCityLeg(index)}
                              className="text-xs font-semibold text-red-600 hover:text-red-700"
                            >
                              Remove
                            </button>
                          )}
                        </div>
                        <div className="grid gap-3 md:grid-cols-3 items-end">
                          <AirportPicker
                            label="Origin"
                            value={leg.origin}
                            onChange={(val) => updateMultiCityLeg(index, 'origin', val)}
                            placeholder="From"
                          />
                          <AirportPicker
                            label="Destination"
                            value={leg.destination}
                            onChange={(val) => updateMultiCityLeg(index, 'destination', val)}
                            placeholder="To"
                          />
                          <div>
                            <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">
                              Date
                            </label>
                            <input
                              type="date"
                              value={leg.date}
                              min={today}
                              onChange={(e) => updateMultiCityLeg(index, 'date', e.target.value)}
                              className="w-full h-[58px] px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm font-semibold text-gray-900 hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                            />
                          </div>
                        </div>
                      </div>
                    ))}

                    <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
                      {multiCityLegs.length < 6 && (
                        <button
                          type="button"
                          onClick={addMultiCityLeg}
                          className="text-xs font-bold text-primary hover:underline flex items-center gap-1"
                        >
                          + Add Another Leg
                        </button>
                      )}
                      <div className="w-full sm:w-auto">
                        <TravellerPicker
                          adults={adults}
                          setAdults={setAdults}
                          children={childCount}
                          setChildren={setChildCount}
                          infants={infants}
                          setInfants={setInfants}
                          cabinClass={cabinClass}
                          setCabinClass={setCabinClass}
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* Submit Search Button */}
                <button
                  type="submit"
                  disabled={isSearching}
                  className="w-full h-12 sm:h-13 rounded-xl bg-primary hover:bg-primary-hover active:scale-[0.99] text-white font-bold text-sm sm:text-base transition-all duration-200 shadow-lg shadow-primary/25 hover:shadow-xl hover:shadow-primary/35 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer"
                >
                  {isSearching ? (
                    <>
                      <svg className="animate-spin h-5 w-5 text-white" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                      </svg>
                      <span>Searching Verified Flights...</span>
                    </>
                  ) : (
                    <span>{tripType === 'MULTI_CITY' ? 'Search Multi-City Flights' : 'Search Verified Flights'}</span>
                  )}
                </button>
              </form>
            </div>
          </div>
        </div>
      </section>

      {/* ═══ VOYARA INTELLIGENCE SUITE ═══ */}
      <section className="section-shell py-12 lg:py-16">
        <div className="max-w-2xl mb-8">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">Intelligence & Trust</p>
          <h2 className="mt-1 text-2xl sm:text-3xl font-black text-slate-900">
            Powered by Voyara Travel Guardian
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            Commercial-grade trip protection, automated risk detection, and seamless price transparency built into every search.
          </p>
        </div>

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {INTELLIGENCE_FEATURES.map((item) => {
            const IconComponent = item.icon;
            return (
              <div
                key={item.title}
                className="voyara-card p-6 flex flex-col justify-between hover:shadow-card-hover transition-all duration-200"
              >
                <div>
                  <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-4 ${item.color}`}>
                    <IconComponent className="w-6 h-6" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900 mb-1.5">{item.title}</h3>
                  <p className="text-xs text-slate-600 leading-relaxed">{item.desc}</p>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* ═══ POPULAR CITY PAIRS ═══ */}
      <section className="section-shell py-10">
        <div className="flex items-end justify-between mb-6">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">High-Frequency Routes</p>
            <h2 className="mt-1 text-2xl font-black text-slate-900">Popular Domestic Flights</h2>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {POPULAR_ROUTES.map((route) => (
            <button
              key={`${route.from}-${route.to}`}
              type="button"
              onClick={() => searchRoute(route.from, route.to)}
              className="voyara-card p-4 text-left flex items-center justify-between hover:border-primary/40 hover:-translate-y-0.5 transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-xs font-black text-primary group-hover:bg-primary group-hover:text-white transition-colors">
                  {route.from}
                </div>
                <div>
                  <div className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
                    <span>{route.fromCity}</span>
                    <span className="text-slate-400">→</span>
                    <span>{route.toCity}</span>
                  </div>
                  <div className="text-xs text-slate-500">{route.duration} · Daily flights</div>
                </div>
              </div>

              <div className="text-right">
                <div className="text-xs text-slate-400">From</div>
                <div className="text-sm font-black text-primary">{route.price}</div>
              </div>
            </button>
          ))}
        </div>
      </section>

      {/* ═══ TRENDING DESTINATIONS ═══ */}
      <section className="section-shell py-10">
        <div className="mb-6">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">Curated Travel</p>
          <h2 className="mt-1 text-2xl font-black text-slate-900">Trending Destinations</h2>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {TRENDING_DESTINATIONS.map((dest) => (
            <button
              key={dest.code}
              type="button"
              onClick={() => searchDestination(dest.code)}
              className="group overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm hover:shadow-xl transition-all duration-300 text-left"
            >
              <div className={`h-32 bg-gradient-to-br ${dest.gradient} p-4 flex flex-col justify-between text-white transition-transform duration-300 group-hover:scale-105`}>
                <span className="text-xs font-bold uppercase tracking-widest bg-black/20 backdrop-blur-sm px-2 py-0.5 rounded-md w-fit">
                  {dest.code}
                </span>
                <div>
                  <h3 className="text-lg font-black">{dest.name}</h3>
                  <p className="text-xs text-white/80">{dest.state}</p>
                </div>
              </div>
              <div className="p-3.5 flex items-center justify-between bg-white">
                <span className="text-xs text-slate-500 font-medium">Starting from</span>
                <span className="text-sm font-black text-primary">{dest.price}</span>
              </div>
            </button>
          ))}
        </div>
      </section>

      {/* ═══ EXCLUSIVE MEMBER OFFERS ═══ */}
      <section className="section-shell py-10 mb-10">
        <div className="mb-6">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">Member Privileges</p>
          <h2 className="mt-1 text-2xl font-black text-slate-900">Featured Offers</h2>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          {DEALS.map((deal) => (
            <div
              key={deal.code}
              className={`rounded-2xl bg-gradient-to-br ${deal.bg} text-white p-6 shadow-lg flex flex-col justify-between`}
            >
              <div>
                <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-sky-200 border border-white/15 mb-4">
                  <TagIcon className="w-3 h-3" />
                  {deal.tag}
                </span>
                <h3 className="text-lg font-bold text-white mb-2">{deal.title}</h3>
                <p className="text-xs text-slate-300 leading-relaxed">{deal.desc}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-white/10 flex items-center justify-between text-xs">
                <span className="font-mono font-bold bg-white/15 px-2.5 py-1 rounded-md text-sky-100">
                  {deal.code}
                </span>
                <span className="text-slate-400">Valid on checkout</span>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
