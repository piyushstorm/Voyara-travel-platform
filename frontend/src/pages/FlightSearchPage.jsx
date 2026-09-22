import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AirportPicker from '../components/search/AirportPicker.jsx';
import TravellerPicker from '../components/search/TravellerPicker.jsx';
import { getTodayDate, isPastDate } from '../utils/dateUtils';

export default function FlightSearchPage() {
  const navigate = useNavigate();
  const [tripType, setTripType] = useState('ONE_WAY');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [departureDate, setDepartureDate] = useState('');
  const [returnDate, setReturnDate] = useState('');
  const [adults, setAdults] = useState(1);
  const [childCount, setChildCount] = useState(0);
  const [infants, setInfants] = useState(0);
  const [cabinClass, setCabinClass] = useState('Economy');
  const [multiCityLegs, setMultiCityLegs] = useState([
    { origin: '', destination: '', date: '' },
    { origin: '', destination: '', date: '' },
  ]);
  const [errors, setErrors] = useState({});

  const today = getTodayDate();
  const totalTravellers = adults + childCount + infants;

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

  const handleSearch = (e) => {
    e.preventDefault();
    setErrors({});

    if (tripType === 'MULTI_CITY') {
      const validLegs = multiCityLegs.filter(l => l.origin && l.destination && l.date);
      if (validLegs.length < 2) { setErrors({ legs: 'At least 2 legs required for multi-city' }); return; }
      const params = new URLSearchParams({ tripType: 'MULTI_CITY', cabinClass: cabinClass.replace(' ', '_').toUpperCase(), passengers: String(totalTravellers) });
      validLegs.forEach((leg, i) => { params.set(`origin${i}`, leg.origin); params.set(`dest${i}`, leg.destination); params.set(`date${i}`, leg.date); });
      navigate(`/flights/search?${params.toString()}`);
      return;
    }

    const newErrors = {};
    if (!from) newErrors.from = 'Select origin';
    if (!to) newErrors.to = 'Select destination';
    if (from && to && from === to) newErrors.to = 'Origin and destination cannot be the same';
    if (!departureDate) newErrors.date = 'Select departure date';
    else if (isPastDate(departureDate)) newErrors.date = 'Departure date cannot be in the past';
    if (tripType === 'ROUND_TRIP' && departureDate && returnDate && returnDate < departureDate) newErrors.returnDate = 'Return must be after departure';
    if (Object.keys(newErrors).length > 0) { setErrors(newErrors); return; }

    navigate(`/flights/search?origin=${from}&destination=${to}&departureDate=${departureDate}${tripType === 'ROUND_TRIP' && returnDate ? `&returnDate=${returnDate}` : ''}&cabinClass=${cabinClass.replace(' ', '_').toUpperCase()}&passengers=${totalTravellers}&tripType=${tripType}`);
  };

  return (
    <div className="min-h-[80vh] bg-background">
      <div className="section-shell max-w-4xl pt-10 pb-16">
        <div className="text-center mb-8">
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-primary">Flight search</p>
          <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">Find your next flight</h1>
          <p className="mt-2 text-sm text-slate-500">Compare routes, timings, and fares in one clear view.</p>
        </div>

        <div className="voyara-card p-5 shadow-lg sm:p-8">
          {/* Trip type */}
          <div className="mb-6 flex w-fit gap-1 rounded-xl bg-slate-100 p-1">
            {[{ key: 'ONE_WAY', label: 'One Way' }, { key: 'ROUND_TRIP', label: 'Round Trip' }, { key: 'MULTI_CITY', label: 'Multi City' }].map(t => (
              <button key={t.key} type="button" onClick={() => setTripType(t.key)}
                className={`rounded-lg px-5 py-2 text-sm font-semibold transition-all ${tripType === t.key ? 'bg-white text-primary shadow-sm' : 'text-slate-500 hover:text-slate-800'}`}>
                {t.label}
              </button>
            ))}
          </div>

          <form onSubmit={handleSearch}>
            {tripType !== 'MULTI_CITY' ? (
              <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
                <div className="md:col-span-3">
                  <AirportPicker label="From" value={from} onChange={setFrom} placeholder="Delhi (DEL)" />
                  {errors.from && <p className="text-red-500 text-xs mt-1">{errors.from}</p>}
                </div>
                <div className="md:col-span-1 flex items-end justify-center pb-2">
                  <button type="button" onClick={() => { setFrom(to); setTo(from); }}
                    className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-slate-500 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary" title="Swap">
                    ⇄
                  </button>
                </div>
                <div className="md:col-span-3">
                  <AirportPicker label="To" value={to} onChange={setTo} placeholder="Mumbai (BOM)" />
                  {errors.to && <p className="text-red-500 text-xs mt-1">{errors.to}</p>}
                </div>
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Departure</label>
                  <input type="date" value={departureDate} onChange={e => setDepartureDate(e.target.value)} min={today} required
                    className="voyara-input [color-scheme:light]" />
                  {errors.date && <p className="text-red-500 text-xs mt-1">{errors.date}</p>}
                </div>
                {tripType === 'ROUND_TRIP' && (
                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Return</label>
                    <input type="date" value={returnDate} onChange={e => setReturnDate(e.target.value)} min={departureDate || today}
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 outline-none [color-scheme:light]" />
                    {errors.returnDate && <p className="text-red-500 text-xs mt-1">{errors.returnDate}</p>}
                  </div>
                )}
                <div className={`${tripType === 'ROUND_TRIP' ? 'md:col-span-1' : 'md:col-span-3'}`}>
                  <TravellerPicker adults={adults} setAdults={setAdults} children={childCount} setChildren={setChildCount} infants={infants} setInfants={setInfants} cabinClass={cabinClass} setCabinClass={setCabinClass} />
                </div>
                <div className="md:col-span-12">
                  <button type="submit" className="travel-button-primary w-full py-3.5 text-sm active:scale-[0.98]">
                    Search Flights
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                {multiCityLegs.map((leg, i) => (
                  <div key={i} className="flex items-end gap-2 rounded-xl border border-slate-200 bg-slate-50 p-4">
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-semibold text-gray-400 mb-1">Leg {i + 1}</div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                        <AirportPicker label="From" value={leg.origin} onChange={v => updateMultiCityLeg(i, 'origin', v)} placeholder="Origin" />
                        <AirportPicker label="To" value={leg.destination} onChange={v => updateMultiCityLeg(i, 'destination', v)} placeholder="Destination" />
                        <div>
                          <label className="block text-xs font-semibold text-gray-500 mb-1.5">Date</label>
                          <input type="date" value={leg.date} onChange={e => updateMultiCityLeg(i, 'date', e.target.value)} min={today}
                            className="voyara-input [color-scheme:light]" />
                        </div>
                      </div>
                    </div>
                    {multiCityLegs.length > 2 && (
                      <button type="button" onClick={() => removeMultiCityLeg(i)} className="w-8 h-8 rounded-lg bg-red-50 text-red-500 hover:bg-red-100 flex items-center justify-center text-sm font-bold mb-0.5">✕</button>
                    )}
                  </div>
                ))}
                {errors.legs && <p className="text-red-500 text-xs">{errors.legs}</p>}
                <div className="flex items-center justify-between">
                  {multiCityLegs.length < 6 && (
                    <button type="button" onClick={addMultiCityLeg} className="text-xs font-semibold text-primary hover:underline">+ Add another leg</button>
                  )}
                  <div className="flex items-center gap-3">
                    <TravellerPicker adults={adults} setAdults={setAdults} children={childCount} setChildren={setChildCount} infants={infants} setInfants={setInfants} cabinClass={cabinClass} setCabinClass={setCabinClass} />
                  </div>
                </div>
                <button type="submit" className="travel-button-primary w-full py-3.5 text-sm active:scale-[0.98]">
                  Search Multi-City Flights
                </button>
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
