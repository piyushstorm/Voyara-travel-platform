import { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getFlightById, getFareOptions, getAddOns, getPricingFactors, BAGGAGE_INFO, CANCELLATION_POLICIES, FARE_RULES } from '../api/flightApi';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import PriceHistoryChart from '../components/PriceHistoryChart';
import PriceFreezeButton from '../components/PriceFreezeButton';
import FlightStatusTracker from '../components/FlightStatusTracker';
import SeatMap from '../components/SeatMap';
import ReviewSection from '../components/reviews/ReviewSection';

function fmtDuration(min) {
  if (!min) return '';
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}
function fmtTime(d) {
  if (!d) return '';
  return new Date(d).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false });
}
function fmtDate(d) {
  if (!d) return '';
  return new Date(d).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
}
function fmtDateLong(d) {
  if (!d) return '';
  return new Date(d).toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

const CLASSES = [
  { key: 'ECONOMY', label: 'Economy', icon: '💺', desc: 'Standard seating' },
  { key: 'PREMIUM_ECONOMY', label: 'Premium Economy', icon: '✨', desc: 'Extra legroom & meals' },
  { key: 'BUSINESS', label: 'Business', icon: '🌟', desc: 'Lie-flat seats, lounge' },
  { key: 'FIRST', label: 'First Class', icon: '👑', desc: 'Suite with private cabin' },
];

export default function FlightDetail() {
  const { id } = useParams();
  const [sp] = useSearchParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { subscribe } = useWebSocket();

  const initialClass = (sp.get('cabinClass') || 'ECONOMY').replace(' ', '_').toUpperCase();
  const initialPassengers = parseInt(sp.get('passengers') || '1');
  const [selectedClass, setSelectedClass] = useState(initialClass);
  const [passengers, setPassengers] = useState(initialPassengers);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [showSeatMap, setShowSeatMap] = useState(false);
  const [activeTab, setActiveTab] = useState('fare');
  const [selectedFareOption, setSelectedFareOption] = useState(null);
  const [selectedAddOns, setSelectedAddOns] = useState([]);
  const [showImportantInfo, setShowImportantInfo] = useState(false);

  const { data: flight, isLoading } = useQuery({
    queryKey: ['flight', id, selectedClass],
    queryFn: () => getFlightById(id, selectedClass),
  });

  const { data: fareOptions = [] } = useQuery({
    queryKey: ['fareOptions', id],
    queryFn: () => getFareOptions(id),
    enabled: !!id,
  });

  const { data: addOns = [] } = useQuery({
    queryKey: ['addOns', selectedClass],
    queryFn: () => getAddOns(selectedClass),
  });

  const { data: pricingFactors } = useQuery({
    queryKey: ['pricingFactors', id, selectedClass],
    queryFn: () => getPricingFactors(id, selectedClass),
    enabled: !!id,
  });

  // Auto-select cheapest fare option
  useEffect(() => {
    if (fareOptions.length > 0 && !selectedFareOption) {
      setSelectedFareOption(fareOptions[0]);
    }
  }, [fareOptions]);

  // Build price key
  const priceKey = selectedClass === 'PREMIUM_ECONOMY' ? 'premiumEconomyPrice' :
    selectedClass === 'BUSINESS' ? 'businessPrice' :
    selectedClass === 'FIRST' ? 'firstClassPrice' : 'economyPrice';

  const basePrice = flight?.[priceKey] || flight?.price || 0;
  const farePrice = selectedFareOption?.priceMultiplier ? Math.round(basePrice * selectedFareOption.priceMultiplier) : basePrice;
  const seatCost = selectedSeats.reduce((sum, s) => sum + Number(s.premiumSurcharge || 0), 0);
  const addOnsCost = selectedAddOns.reduce((sum, a) => sum + Number(a.price || 0), 0);
  const taxes = Math.round(farePrice * 0.12);
  const totalPerPerson = farePrice + addOnsCost + taxes;
  const totalFare = (totalPerPerson * passengers) + seatCost;

  const baggage = BAGGAGE_INFO[selectedClass] || BAGGAGE_INFO.ECONOMY;

  const toggleAddOn = (addOn) => {
    setSelectedAddOns(prev => {
      const exists = prev.find(a => a.id === addOn.id);
      if (exists) return prev.filter(a => a.id !== addOn.id);
      return [...prev, addOn];
    });
  };

  if (isLoading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8 space-y-4">
        <div className="skeleton h-10 w-48 rounded-lg" />
        <div className="skeleton h-32 w-full rounded-2xl" />
        <div className="skeleton h-64 w-full rounded-2xl" />
        <div className="skeleton h-48 w-full rounded-2xl" />
      </div>
    );
  }

  if (!flight) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center">
        <div className="text-5xl mb-4">✈️</div>
        <h2 className="text-xl font-semibold text-gray-900">Flight not found</h2>
        <Link to="/flights/search" className="text-blue-600 hover:underline mt-3 inline-block text-sm font-medium">Back to search</Link>
      </div>
    );
  }

  const handleProceed = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/flights/${id}` } } });
      return;
    }
    const params = new URLSearchParams({
      type: 'FLIGHT', id, cabinClass: selectedClass, passengers: String(passengers),
    });
    if (selectedSeats.length > 0) params.set('seatIds', selectedSeats.map(s => s.id).join(','));
    if (selectedSeats.length > 0) params.set('seatNumbers', selectedSeats.map(s => s.seatNumber).join(','));
    if (selectedFareOption) params.set('fareOptionId', selectedFareOption.id);
    if (selectedAddOns.length > 0) params.set('addOnIds', selectedAddOns.map(a => a.id).join(','));
    navigate(`/booking?${params.toString()}`);
  };

  const addOnCategories = [...new Set(addOns.map(a => a.category))];

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-6">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-4 flex items-center gap-2">
        <Link to="/flights/search" className="hover:text-blue-600 transition-colors">Flights</Link>
        <span>›</span>
        <span className="text-gray-700">{flight.originCode} → {flight.destinationCode}</span>
        <span>›</span>
        <span className="text-gray-400">{flight.flightNumber}</span>
      </nav>

      {/* Live Status */}
      <div className="mb-5">
        <FlightStatusTracker flightId={flight.id} flightNumber={flight.flightNumber} origin={flight.originCode} destination={flight.destinationCode} subscribe={subscribe} />
      </div>

      {/* Flight Summary Card */}
      <div className="bg-white rounded-2xl p-5 sm:p-6 shadow-sm border border-gray-100 mb-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center text-blue-600 font-bold text-sm border border-blue-100">
              {flight.airlineCode}
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-900">{flight.airlineName}</h1>
              <p className="text-sm text-gray-500">{flight.flightNumber} · {selectedClass.replace('_', ' ')}</p>
            </div>
          </div>
          <div className="text-right">
            <div className="text-3xl font-bold text-gray-900">₹{basePrice?.toLocaleString()}</div>
            <div className="text-sm text-gray-500">per person</div>
          </div>
        </div>

        <div className="text-sm text-gray-500 mb-4">{fmtDateLong(flight.departureDate)}</div>

        {/* Route visualization */}
        <div className="bg-gray-50 rounded-xl p-5">
          <div className="flex items-center justify-between">
            <div className="text-center">
              <div className="text-2xl font-bold text-gray-900">{fmtTime(flight.departureTime)}</div>
              <div className="text-sm text-gray-600 mt-0.5">{flight.originCity}</div>
              <div className="text-xs text-gray-400 font-mono">{flight.originCode}</div>
            </div>
            <div className="flex-1 mx-6">
              <div className="text-center text-sm text-gray-500 mb-2">{fmtDuration(flight.durationMinutes)}</div>
              <div className="relative">
                <div className="border-t-2 border-dashed border-gray-300" />
                <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-white px-3 py-1 rounded-full border border-gray-200 text-xs">
                  {flight.stops === 0
                    ? <span className="text-green-600 font-medium">✈ Non-stop</span>
                    : <span className="text-orange-500 font-medium">✈ {flight.stops} stop{flight.stops > 1 ? 's' : ''}</span>}
                </div>
              </div>
              {flight.stopoverCity && (
                <div className="text-xs text-gray-400 text-center mt-1">via {flight.stopoverCity}</div>
              )}
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-gray-900">{fmtTime(flight.arrivalTime)}</div>
              <div className="text-sm text-gray-600 mt-0.5">{flight.destinationCity}</div>
              <div className="text-xs text-gray-400 font-mono">{flight.destinationCode}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-100 rounded-xl p-1 w-fit mb-5 overflow-x-auto no-scrollbar">
        {[
          { key: 'fare', label: '🏷️ Fare Options' },
          { key: 'seats', label: '💺 Seats' },
          { key: 'addons', label: '➕ Add-ons' },
          { key: 'price', label: '📈 Price' },
          { key: 'info', label: 'ℹ️ Info' },
          { key: 'reviews', label: '⭐ Reviews' },
        ].map(tab => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all whitespace-nowrap ${
              activeTab === tab.key ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}>
            {tab.label}
          </button>
        ))}
      </div>

      {/* === Fare Options Tab === */}
      {activeTab === 'fare' && (
        <div className="space-y-5">
          {/* Cabin Class Selector */}
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Cabin Class</h2>
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
              {CLASSES.map(cls => {
                const price = flight[cls.key === 'PREMIUM_ECONOMY' ? 'premiumEconomyPrice' : cls.key === 'ECONOMY' ? 'economyPrice' : cls.key === 'BUSINESS' ? 'businessPrice' : 'firstClassPrice'];
                const isSelected = selectedClass === cls.key;
                const availableSeats = flight.availableSeats || 0;
                return (
                  <button key={cls.key} onClick={() => { setSelectedClass(cls.key); setSelectedSeats([]); setSelectedFareOption(null); }}
                    className={`p-4 rounded-xl border-2 text-left transition-all ${
                      isSelected ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-gray-200 hover:border-gray-300'
                    }`}>
                    <div className="text-lg mb-1">{cls.icon}</div>
                    <div className="font-semibold text-gray-900 text-sm">{cls.label}</div>
                    <div className="text-xs text-gray-500 mt-0.5">{cls.desc}</div>
                    <div className="text-xl font-bold text-blue-600 mt-2">₹{price?.toLocaleString()}</div>
                    {availableSeats < 10 && availableSeats > 0 && (
                      <div className="text-xs text-red-500 mt-1 font-medium">Only {availableSeats} seats left!</div>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Fare Option Cards */}
          {fareOptions.length > 0 && (
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
              <h2 className="text-lg font-bold text-gray-900 mb-4">Choose Your Fare</h2>
              <div className="grid gap-4">
                {fareOptions.map(fare => {
                  const farePriceVal = Math.round(basePrice * fare.priceMultiplier);
                  const isSelected = selectedFareOption?.id === fare.id;
                  const isDefault = fare.fareType === 'STANDARD';
                  return (
                    <div key={fare.id}
                      onClick={() => setSelectedFareOption(fare)}
                      className={`relative p-5 rounded-xl border-2 cursor-pointer transition-all ${
                        isSelected ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-gray-300'
                      }`}>
                      {isDefault && (
                        <span className="absolute -top-3 left-4 bg-blue-600 text-white text-xs px-2 py-0.5 rounded-full font-medium">MOST POPULAR</span>
                      )}
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <h3 className="font-bold text-gray-900">{fare.name}</h3>
                            {fare.fareType === 'FLEX' && <span className="text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full font-medium">BEST VALUE</span>}
                          </div>
                          <p className="text-sm text-gray-500 mt-1">{fare.description}</p>
                          <div className="mt-3 grid grid-cols-2 sm:grid-cols-3 gap-2">
                            <div className={`text-xs flex items-center gap-1 ${fare.checkedBaggageKg >= 20 ? 'text-green-600' : 'text-gray-500'}`}>
                              🧳 {fare.checkedBaggageKg}kg check-in
                            </div>
                            <div className={`text-xs flex items-center gap-1 ${fare.mealIncluded ? 'text-green-600' : 'text-gray-500'}`}>
                              🍽️ {fare.mealIncluded ? 'Meal included' : 'Meal paid'}
                            </div>
                            <div className={`text-xs flex items-center gap-1 ${fare.seatSelectionIncluded ? 'text-green-600' : 'text-gray-500'}`}>
                              💺 {fare.seatSelectionIncluded ? 'Seat included' : 'Seat paid'}
                            </div>
                            <div className={`text-xs flex items-center gap-1 ${fare.changeable ? 'text-green-600' : 'text-red-500'}`}>
                              🔄 {fare.changeable ? fare.changeFee : 'Non-changeable'}
                            </div>
                            <div className={`text-xs flex items-center gap-1 ${fare.refundable ? 'text-green-600' : 'text-red-500'}`}>
                              💰 {fare.refundable ? 'Refundable' : 'Non-refundable'}
                            </div>
                            {fare.priorityBoarding && (
                              <div className="text-xs text-green-600 flex items-center gap-1">⚡ Priority boarding</div>
                            )}
                            {fare.loungeAccess && (
                              <div className="text-xs text-green-600 flex items-center gap-1">🏖️ Lounge access</div>
                            )}
                          </div>
                        </div>
                        <div className="text-right ml-4">
                          <div className="text-2xl font-bold text-gray-900">₹{farePriceVal.toLocaleString()}</div>
                          <div className="text-xs text-gray-400">per person</div>
                          {isSelected && <div className="text-xs text-blue-600 font-medium mt-1">✓ Selected</div>}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Baggage Summary */}
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <h2 className="text-lg font-bold text-gray-900 mb-4">What's Included</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {[
                { label: 'Cabin Bag', value: `${selectedFareOption?.cabinBaggageKg || baggage.cabin.replace(' kg', '')} kg`, icon: '👜' },
                { label: 'Check-in', value: `${selectedFareOption?.checkedBaggageKg || baggage.checkin.replace(' kg', '')} kg`, icon: '🧳' },
                { label: 'Meal', value: selectedFareOption?.mealIncluded ? 'Included' : 'Paid', icon: '🍽️' },
                { label: 'Seat Selection', value: selectedFareOption?.seatSelectionIncluded ? 'Included' : 'Paid', icon: '💺' },
              ].map(item => (
                <div key={item.label} className="text-center p-3 bg-gray-50 rounded-xl">
                  <div className="text-2xl mb-1">{item.icon}</div>
                  <div className="text-xs text-gray-500">{item.label}</div>
                  <div className="text-sm font-semibold text-gray-900 mt-0.5">{item.value}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Price Freeze */}
          <PriceFreezeButton entityType="FLIGHT" entityId={flight.id} cabinClass={selectedClass} currentPrice={basePrice} />
        </div>
      )}

      {/* === Seats Tab === */}
      {activeTab === 'seats' && (
        <div>
          <SeatMap
            flightId={flight.id}
            cabinClass={selectedClass}
            onSeatSelected={setSelectedSeats}
            maxSeats={passengers}
            baseFare={farePrice}
            onContinue={handleProceed}
          />
        </div>
      )}

      {/* === Add-ons Tab === */}
      {activeTab === 'addons' && (
        <div className="space-y-5">
          {addOns.length === 0 ? (
            <div className="bg-white rounded-2xl p-8 shadow-sm border border-gray-100 text-center">
              <div className="text-4xl mb-3">🎁</div>
              <h3 className="font-bold text-gray-900 mb-1">No add-ons available</h3>
              <p className="text-sm text-gray-500">Add-ons will appear here once available for this cabin class.</p>
            </div>
          ) : (
            addOnCategories.map(category => {
              const categoryLabel = { BAGGAGE: '🧳 Extra Baggage', MEAL: '🍽️ Pre-book Meals', SEAT: '💺 Seat & Boarding', PROTECTION: '🛡️ Travel Protection' };
              return (
                <div key={category} className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
                  <h2 className="text-lg font-bold text-gray-900 mb-4">{categoryLabel[category] || category}</h2>
                  <div className="space-y-3">
                    {addOns.filter(a => a.category === category).map(addOn => {
                      const isSelected = selectedAddOns.some(a => a.id === addOn.id);
                      return (
                        <div key={addOn.id}
                          onClick={() => toggleAddOn(addOn)}
                          className={`flex items-center justify-between p-4 rounded-xl border-2 cursor-pointer transition-all ${
                            isSelected ? 'border-blue-500 bg-blue-50' : 'border-gray-100 hover:border-gray-200'
                          }`}>
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${
                                isSelected ? 'border-blue-500 bg-blue-500' : 'border-gray-300'
                              }`}>
                                {isSelected && <span className="text-white text-xs">✓</span>}
                              </div>
                              <div>
                                <div className="font-semibold text-gray-900 text-sm">{addOn.name}</div>
                                <div className="text-xs text-gray-500">{addOn.description}</div>
                              </div>
                            </div>
                          </div>
                          <div className="text-lg font-bold text-gray-900 ml-4">₹{addOn.price}</div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })
          )}
          {selectedAddOns.length > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
              <div className="flex items-center justify-between">
                <div className="text-sm font-semibold text-blue-900">{selectedAddOns.length} add-on{selectedAddOns.length > 1 ? 's' : ''} selected</div>
                <div className="text-lg font-bold text-blue-900">+₹{addOnsCost.toLocaleString()} per person</div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* === Price Tab === */}
      {activeTab === 'price' && (
        <div className="space-y-5">
          {pricingFactors && (
            <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-lg font-bold text-gray-900">Dynamic Pricing Transparency</h2>
                  <p className="text-xs text-gray-500 mt-0.5">Real-time breakdown of algorithmic adjustments applied to this fare</p>
                </div>
                <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-100">
                  Backend-Authoritative
                </span>
              </div>

              {/* Factors grid */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 mb-6">
                <div className="p-3.5 bg-gray-50 rounded-xl border border-gray-100">
                  <div className="text-xs text-gray-500 font-medium">Base Inventory Rate</div>
                  <div className="text-lg font-bold text-gray-900 mt-1">₹{Number(pricingFactors.basePrice || 0).toLocaleString()}</div>
                  <div className="text-[11px] text-gray-500 mt-1">{selectedClass.replace('_', ' ')} standard fare</div>
                </div>
                <div className="p-3.5 bg-blue-50/60 rounded-xl border border-blue-100">
                  <div className="text-xs text-blue-800 font-medium">Demand Factor</div>
                  <div className="text-lg font-bold text-blue-900 mt-1">
                    {pricingFactors.demandMultiplier ? `${((pricingFactors.demandMultiplier - 1) * 100) >= 0 ? '+' : ''}${((pricingFactors.demandMultiplier - 1) * 100).toFixed(0)}%` : '0%'}
                  </div>
                  <div className="text-[11px] text-blue-700 mt-1">{pricingFactors.demandDescription || 'Normal demand'}</div>
                </div>
                <div className="p-3.5 bg-amber-50/60 rounded-xl border border-amber-100">
                  <div className="text-xs text-amber-800 font-medium">Inventory Factor</div>
                  <div className="text-lg font-bold text-amber-900 mt-1">
                    {pricingFactors.inventoryMultiplier ? `${((pricingFactors.inventoryMultiplier - 1) * 100) >= 0 ? '+' : ''}${((pricingFactors.inventoryMultiplier - 1) * 100).toFixed(0)}%` : '0%'}
                  </div>
                  <div className="text-[11px] text-amber-700 mt-1">{pricingFactors.inventoryDescription || 'Standard inventory'}</div>
                </div>
                <div className="p-3.5 bg-purple-50/60 rounded-xl border border-purple-100">
                  <div className="text-xs text-purple-800 font-medium">Seasonal / Peak</div>
                  <div className="text-lg font-bold text-purple-900 mt-1">
                    {pricingFactors.peakMultiplier ? `${((pricingFactors.peakMultiplier - 1) * 100) >= 0 ? '+' : ''}${((pricingFactors.peakMultiplier - 1) * 100).toFixed(0)}%` : '0%'}
                  </div>
                  <div className="text-[11px] text-purple-700 mt-1">{pricingFactors.isPeakPeriod ? 'Peak holiday calendar active' : 'Regular calendar season'}</div>
                </div>
              </div>

              {/* Itemized calculation breakdown */}
              <div className="border border-gray-100 rounded-xl overflow-hidden text-sm">
                <div className="bg-gray-50/80 px-4 py-2.5 font-semibold text-gray-700 text-xs uppercase tracking-wider border-b border-gray-100">
                  Calculation Breakdown
                </div>
                <div className="divide-y divide-gray-100">
                  <div className="flex justify-between px-4 py-2.5">
                    <span className="text-gray-600">Base Price</span>
                    <span className="font-semibold text-gray-900">₹{Number(pricingFactors.basePrice || 0).toLocaleString()}</span>
                  </div>
                  {pricingFactors.demandMultiplier && pricingFactors.demandMultiplier !== 1 && (
                    <div className="flex justify-between px-4 py-2.5">
                      <span className="text-gray-600">Demand adjustment ({pricingFactors.demandDescription})</span>
                      <span className={pricingFactors.demandMultiplier > 1 ? 'text-amber-600 font-medium' : 'text-green-600 font-medium'}>
                        {pricingFactors.demandMultiplier > 1 ? '+' : ''}₹{Math.round((pricingFactors.basePrice || 0) * (pricingFactors.demandMultiplier - 1)).toLocaleString()}
                      </span>
                    </div>
                  )}
                  {pricingFactors.isPeakPeriod && (
                    <div className="flex justify-between px-4 py-2.5">
                      <span className="text-gray-600">Peak holiday period (+20%)</span>
                      <span className="text-red-600 font-medium">+₹{Math.round((pricingFactors.basePrice || 0) * 0.20).toLocaleString()}</span>
                    </div>
                  )}
                  {pricingFactors.inventoryMultiplier && pricingFactors.inventoryMultiplier > 1 && (
                    <div className="flex justify-between px-4 py-2.5">
                      <span className="text-gray-600">Low seat availability adjustment</span>
                      <span className="text-amber-600 font-medium">+₹{Math.round((pricingFactors.basePrice || 0) * (pricingFactors.inventoryMultiplier - 1)).toLocaleString()}</span>
                    </div>
                  )}
                  {pricingFactors.timeMultiplier && pricingFactors.timeMultiplier !== 1 && (
                    <div className="flex justify-between px-4 py-2.5">
                      <span className="text-gray-600">Time to departure ({pricingFactors.timeDescription})</span>
                      <span className={pricingFactors.timeMultiplier > 1 ? 'text-orange-600 font-medium' : 'text-green-600 font-medium'}>
                        {pricingFactors.timeMultiplier > 1 ? '+' : ''}₹{Math.round((pricingFactors.basePrice || 0) * (pricingFactors.timeMultiplier - 1)).toLocaleString()}
                      </span>
                    </div>
                  )}
                  <div className="flex justify-between px-4 py-3 bg-gray-50 font-bold text-gray-900">
                    <span>Effective Current Price</span>
                    <span className="text-blue-600 text-base">₹{Number(pricingFactors.currentPrice || basePrice).toLocaleString()}</span>
                  </div>
                </div>
              </div>

              {pricingFactors.safetyCapApplied && (
                <div className="mt-3 p-2.5 bg-blue-50 border border-blue-200 rounded-lg text-blue-700 text-xs">
                  🛡️ Pricing safety guardrail active: Maximum price increase is capped at 2.00x base price to protect consumers.
                </div>
              )}
            </div>
          )}
          <PriceHistoryChart entityType="FLIGHT" entityId={flight.id} cabinClass={selectedClass} currentPrice={basePrice} />
          <PriceFreezeButton entityType="FLIGHT" entityId={flight.id} cabinClass={selectedClass} currentPrice={basePrice} />
        </div>
      )}

      {/* === Info Tab === */}
      {activeTab === 'info' && (
        <div className="space-y-5">
          {/* Flight Details */}
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Flight Information</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              {[
                { label: 'Flight', value: flight.flightNumber },
                { label: 'Airline', value: flight.airlineName },
                { label: 'Aircraft', value: 'Boeing 737-800' },
                { label: 'Duration', value: fmtDuration(flight.durationMinutes) },
                { label: 'Stops', value: flight.stops === 0 ? 'Non-stop' : `${flight.stops} stop${flight.stops > 1 ? 's' : ''}` },
                { label: 'Terminal', value: 'T1' },
              ].map(item => (
                <div key={item.label} className="bg-gray-50 rounded-xl p-3">
                  <div className="text-xs text-gray-500">{item.label}</div>
                  <div className="text-sm font-semibold text-gray-900 mt-0.5">{item.value}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Fare Rules */}
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Fare Rules & Cancellation</h2>
            {selectedFareOption && (
              <div className="mb-4 p-4 bg-gray-50 rounded-xl">
                <div className="text-sm font-semibold text-gray-900">{selectedFareOption.name} Fare</div>
                <div className="text-sm text-gray-600 mt-1">{selectedFareOption.refundPolicy}</div>
                <div className="text-sm text-gray-600">Changes: {selectedFareOption.changeFee}</div>
              </div>
            )}
            <div className="space-y-3">
              {CANCELLATION_POLICIES.map((policy, i) => (
                <div key={i} className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
                  <span className="text-lg">{policy.icon}</span>
                  <div>
                    <div className="text-sm font-semibold text-gray-900">{policy.label}</div>
                    <div className="text-xs text-gray-500">{policy.description}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Important Information */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
            <button onClick={() => setShowImportantInfo(!showImportantInfo)}
              className="w-full flex items-center justify-between p-5 text-left">
              <h2 className="text-lg font-bold text-gray-900">Important Information</h2>
              <span className={`text-gray-400 transition-transform ${showImportantInfo ? 'rotate-180' : ''}`}>▼</span>
            </button>
            {showImportantInfo && (
              <div className="px-5 pb-5 text-sm text-gray-600 space-y-2">
                <p>• Check-in opens 48 hours before departure and closes 60 minutes prior to departure.</p>
                <p>• Passengers must arrive at the airport at least 2 hours before domestic flights.</p>
                <p>• Valid government-issued photo ID is required for domestic travel.</p>
                <p>• Passport and visa are required for international flights.</p>
                <p>• Baggage allowances may vary based on fare type and route.</p>
                <p>• Meals are complimentary on flights longer than 2 hours.</p>
                <p>• Seat selection charges apply for Economy class unless included in fare.</p>
                <p>• Infants (under 2 years) must be held on guardian's lap throughout the flight.</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* === Reviews Tab === */}
      {activeTab === 'reviews' && (
        <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
          <ReviewSection
            targetType="FLIGHT"
            targetId={flight.id}
            targetName={`${flight.airlineName || flight.airline?.name || ''} ${flight.flightNumber} (${flight.originCode} → ${flight.destinationCode})`}
          />
        </div>
      )}

      {/* Sticky Booking Bar (mobile) */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-40 lg:relative lg:border-0 lg:bg-transparent lg:p-0 lg:mt-5">
        <div className="max-w-5xl mx-auto">
          {/* Price Breakdown (expandable on mobile) */}
          <div className="mb-3 bg-gray-50 rounded-xl p-3 text-sm space-y-1">
            <div className="flex justify-between"><span className="text-gray-500">Base fare × {passengers}</span><span>₹{(farePrice * passengers).toLocaleString()}</span></div>
            {seatCost > 0 && <div className="flex justify-between"><span className="text-gray-500">Seat selection</span><span>₹{seatCost.toLocaleString()}</span></div>}
            {addOnsCost > 0 && <div className="flex justify-between"><span className="text-gray-500">Add-ons × {passengers}</span><span>₹{(addOnsCost * passengers).toLocaleString()}</span></div>}
            <div className="flex justify-between"><span className="text-gray-500">Taxes & fees × {passengers}</span><span>₹{(taxes * passengers).toLocaleString()}</span></div>
            <div className="flex justify-between font-bold text-gray-900 border-t border-gray-200 pt-1">
              <span>Total ({passengers} pax)</span><span className="text-lg">₹{totalFare.toLocaleString()}</span>
            </div>
          </div>

          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-2 bg-gray-100 rounded-xl px-2">
              <button onClick={() => setPassengers(Math.max(1, passengers - 1))} className="w-8 h-8 rounded-lg flex items-center justify-center text-gray-500 hover:bg-white transition-colors font-bold">−</button>
              <span className="w-6 text-center font-bold text-sm">{passengers}</span>
              <button onClick={() => setPassengers(Math.min(9, passengers + 1))} className="w-8 h-8 rounded-lg flex items-center justify-center text-gray-500 hover:bg-white transition-colors font-bold">+</button>
            </div>
            <button onClick={handleProceed}
              className="bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white px-6 py-3 rounded-xl font-bold text-sm transition-all shadow-lg shadow-orange-500/20 flex-1 sm:flex-none">
              {isAuthenticated ? 'Continue to Booking →' : 'Login to Book'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
