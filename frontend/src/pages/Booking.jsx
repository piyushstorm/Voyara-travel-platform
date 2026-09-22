import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { getFlightById, getFareOptions } from '../api/flightApi';
import { getHotelById, getHotelRooms } from '../api/hotelApi';
import { getSeatMap } from '../api/selectionApi';
import { createBooking } from '../api/bookingApi';
import { paymentApi, priceApi, couponApi, rewardsApi } from '../api/phase3Api';
import RoomSelector from '../components/RoomSelector';
import {
  ShieldCheckIcon,
  LockIcon,
  CheckIcon,
  CreditCardIcon,
  LuggageIcon,
  FlightIcon,
  HotelIcon,
  TrainIcon,
  BusIcon,
  CabIcon,
} from '../components/common/Icons';

const STEPS = ['Review', 'Passengers', 'Payment', 'Confirmation'];

function PassengerForm({ index, passenger, onChange, showPassport }) {
  const update = (field, value) => onChange({ ...passenger, [field]: value });
  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
      <h3 className="font-bold text-gray-900 mb-4 flex items-center gap-2">
        <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center text-sm font-bold">{index + 1}</span>
        Passenger {index + 1} {index === 0 && <span className="text-xs bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full font-medium">Contact Person</span>}
      </h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Title *</label>
          <select value={passenger.title || ''} onChange={e => update('title', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none">
            <option value="">Select</option>
            <option>Mr</option><option>Mrs</option><option>Ms</option><option>Dr</option>
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">First Name *</label>
          <input type="text" value={passenger.firstName || ''} onChange={e => update('firstName', e.target.value)} placeholder="First name"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Middle Name</label>
          <input type="text" value={passenger.middleName || ''} onChange={e => update('middleName', e.target.value)} placeholder="Middle name"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Last Name *</label>
          <input type="text" value={passenger.lastName || ''} onChange={e => update('lastName', e.target.value)} placeholder="Last name"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Gender *</label>
          <select value={passenger.gender || ''} onChange={e => update('gender', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none">
            <option value="">Select</option>
            <option>MALE</option><option>FEMALE</option><option>OTHER</option>
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Date of Birth</label>
          <input type="date" value={passenger.dateOfBirth || ''} onChange={e => update('dateOfBirth', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-gray-500 mb-1">Nationality</label>
          <input type="text" value={passenger.nationality || 'Indian'} onChange={e => update('nationality', e.target.value)} placeholder="Nationality"
            className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
        </div>
        {index === 0 && (
          <>
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Email *</label>
              <input type="email" value={passenger.email || ''} onChange={e => update('email', e.target.value)} placeholder="email@example.com"
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Phone *</label>
              <input type="tel" value={passenger.phone || ''} onChange={e => update('phone', e.target.value)} placeholder="+91 98765 43210"
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
            </div>
          </>
        )}
        {showPassport && (
          <>
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Passport Number</label>
              <input type="text" value={passenger.passportNumber || ''} onChange={e => update('passportNumber', e.target.value)} placeholder="A1234567"
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Passport Expiry</label>
              <input type="date" value={passenger.passportExpiry || ''} onChange={e => update('passportExpiry', e.target.value)}
                className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default function Booking() {
  const [sp] = useSearchParams();
  const navigate = useNavigate();
  const type = sp.get('type');
  const id = sp.get('id');
  const cabinClass = sp.get('cabinClass') || 'ECONOMY';
  const passengers = parseInt(sp.get('passengers') || '1');
  const roomId = sp.get('roomId');
  const nights = parseInt(sp.get('nights') || '1');
  const checkIn = sp.get('checkIn') || '';
  const checkOut = sp.get('checkOut') || '';
  const freezeId = sp.get('freezeId');
  const seatIds = sp.get('seatIds') || '';
  const seatNumbers = sp.get('seatNumbers') || '';
  const fareOptionId = sp.get('fareOptionId');
  const addOnIdsParam = sp.get('addOnIds') || '';
  // Train/Bus/Cab params
  const trainClass = sp.get('trainClass') || 'SL';
  const boardingPoint = sp.get('boardingPoint') || '';
  const droppingPoint = sp.get('droppingPoint') || '';
  const pickup = sp.get('pickup') || '';
  const drop = sp.get('drop') || '';
  const cabType = sp.get('cabType') || 'LOCAL';

  const [step, setStep] = useState(0);
  const [passengerData, setPassengerData] = useState(
    Array.from({ length: passengers }, (_, i) => ({
      title: '', firstName: '', middleName: '', lastName: '', gender: '',
      dateOfBirth: '', nationality: 'Indian', email: i === 0 ? '' : undefined,
      phone: i === 0 ? '' : undefined, passportNumber: '', passportExpiry: '',
    }))
  );
  const [paymentMethod, setPaymentMethod] = useState('CARD');
  const [couponCode, setCouponCode] = useState('');
  const [couponResult, setCouponResult] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponError, setCouponError] = useState('');
  const [rewardsBalance, setRewardsBalance] = useState(null);
  const [rewardsPointsToRedeem, setRewardsPointsToRedeem] = useState(0);
  const [rewardsRedemption, setRewardsRedemption] = useState(null);
  const [rewardsLoading, setRewardsLoading] = useState(false);
  const [rewardsError, setRewardsError] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [bookingResult, setBookingResult] = useState(null);
  const [errors, setErrors] = useState({});
  const [isProcessing, setIsProcessing] = useState(false);

  const { data: flight } = useQuery({
    queryKey: ['flight', id, cabinClass],
    queryFn: () => getFlightById(id, cabinClass),
    enabled: type === 'FLIGHT' && !!id,
  });

  const { data: fareOptions = [] } = useQuery({
    queryKey: ['fareOptions', id],
    queryFn: () => getFareOptions(id),
    enabled: type === 'FLIGHT' && !!id,
  });

  const { data: hotel } = useQuery({
    queryKey: ['hotel', id],
    queryFn: () => getHotelById(id),
    enabled: type === 'HOTEL' && !!id,
  });

  const { data: rooms } = useQuery({
    queryKey: ['hotelRooms', id],
    queryFn: () => getHotelRooms(id),
    enabled: type === 'HOTEL' && !!id,
  });

  const { data: train } = useQuery({
    queryKey: ['train', id],
    queryFn: () => import('../api/trainApi').then(m => m.trainApi.getById(id)),
    enabled: type === 'TRAIN' && !!id,
  });

  const { data: bus } = useQuery({
    queryKey: ['bus', id],
    queryFn: () => import('../api/busApi').then(m => m.busApi.getById(id)),
    enabled: type === 'BUS' && !!id,
  });

  const { data: cab } = useQuery({
    queryKey: ['cab', id],
    queryFn: () => import('../api/cabApi').then(m => m.cabApi.getById(id)),
    enabled: type === 'CAB' && !!id,
  });

  const { data: activeFreeze } = useQuery({
    queryKey: ['priceFreeze', type, id],
    queryFn: () => priceApi.getActiveFreeze(type, id),
    enabled: type === 'FLIGHT' && !!id,
  });

  // Fetch rewards balance
  const { data: rewardsData } = useQuery({
    queryKey: ['rewardsBalance'],
    queryFn: () => rewardsApi.getBalance(),
    enabled: true,
  });

  const { data: seatMapData } = useQuery({
    queryKey: ['seatMap', id, cabinClass],
    queryFn: () => getSeatMap(id, cabinClass),
    enabled: type === 'FLIGHT' && !!id && !!seatIds,
  });

  const selectedSeatsList = (seatMapData?.seats || []).filter(s =>
    seatIds ? seatIds.split(',').map(Number).includes(s.id) : false
  );
  const seatSurchargesTotal = selectedSeatsList.reduce(
    (sum, s) => sum + Number(s.premiumSurcharge || 0), 0
  );

  const [selectedRoomId, setSelectedRoomId] = useState(roomId || '');
  const selectedRoom = rooms?.find(r => r.id === Number(selectedRoomId));
  const selectedFare = fareOptions.find(f => String(f.id) === fareOptionId);

  const effectiveFreeze = (activeFreeze?.data?.active && activeFreeze?.data?.freeze) || null;
  const effectiveFreezeId = freezeId || effectiveFreeze?.id;
  const effectiveFrozenPrice = effectiveFreeze ? Number(effectiveFreeze.frozenPrice) : null;

  // Calculate prices
  let basePricePerPerson = 0;
  let quantity = 1;
  if (type === 'FLIGHT') {
    if (effectiveFrozenPrice) {
      basePricePerPerson = effectiveFrozenPrice;
    } else {
      basePricePerPerson = selectedFare ? Math.round((flight?.economyPrice || 0) * selectedFare.priceMultiplier) : (flight?.price || 0);
    }
    quantity = passengers;
  } else if (type === 'HOTEL') {
    basePricePerPerson = selectedRoom?.pricePerNight || 0;
    quantity = nights;
  } else if (type === 'TRAIN') {
    const cls = (train?.seats || []).find(s => s.classCode === trainClass);
    basePricePerPerson = cls?.fare || 500;
    quantity = passengers;
  } else if (type === 'BUS') {
    basePricePerPerson = bus?.basePrice || 800;
    quantity = passengers;
  } else if (type === 'CAB') {
    basePricePerPerson = cab?.baseFare || 500;
    quantity = 1;
  }
  const taxes = Math.round(basePricePerPerson * 0.12);
  const baseFareTotal = basePricePerPerson * quantity;
  const taxesTotal = taxes * quantity;
  const totalAmount = baseFareTotal + taxesTotal + seatSurchargesTotal;

  const bookingMutation = useMutation({
    mutationFn: createBooking,
    onSuccess: (data) => { setBookingResult(data); setStep(3); setIsProcessing(false); },
    onError: (err) => { setErrors({ payment: err.response?.data?.message || 'Booking failed. Please try again.' }); setIsProcessing(false); setStep(2); },
  });

  const validatePassengers = () => {
    const errs = {};
    passengerData.forEach((p, i) => {
      if (!p.firstName?.trim()) errs[`p${i}_first`] = 'First name required';
      if (!p.lastName?.trim()) errs[`p${i}_last`] = 'Last name required';
      if (!p.title) errs[`p${i}_title`] = 'Title required';
      if (!p.gender) errs[`p${i}_gender`] = 'Gender required';
      if (i === 0) {
        if (!p.email?.trim()) errs[`p${i}_email`] = 'Email required';
        if (!p.phone?.trim()) errs[`p${i}_phone`] = 'Phone required';
      }
    });
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) return;
    setCouponLoading(true);
    setCouponError('');
    setCouponResult(null);
    try {
      const res = await couponApi.validate(couponCode.trim(), totalAmount, type);
      if (res.data.valid) {
        setCouponResult(res.data);
      } else {
        setCouponError(res.data.message || 'Invalid coupon');
      }
    } catch (err) {
      setCouponError(err.response?.data?.message || 'Failed to validate coupon');
    } finally {
      setCouponLoading(false);
    }
  };

  const handleRemoveCoupon = () => {
    setCouponCode('');
    setCouponResult(null);
    setCouponError('');
  };

  const handleValidateRewards = async () => {
    if (!rewardsPointsToRedeem || rewardsPointsToRedeem <= 0) return;
    setRewardsLoading(true);
    setRewardsError('');
    setRewardsRedemption(null);
    try {
      const amountAfterCoupon = couponResult ? couponResult.finalAmount : totalAmount;
      const res = await rewardsApi.validateRedemption(rewardsPointsToRedeem, amountAfterCoupon);
      if (res.data.valid) {
        setRewardsRedemption(res.data);
      } else {
        setRewardsError(res.data.message || 'Cannot redeem points');
      }
    } catch (err) {
      setRewardsError(err.response?.data?.message || 'Failed to validate redemption');
    } finally {
      setRewardsLoading(false);
    }
  };

  const handleRemoveRewards = () => {
    setRewardsPointsToRedeem(0);
    setRewardsRedemption(null);
    setRewardsError('');
  };

  const couponDiscount = couponResult ? (couponResult.discountAmount || 0) : 0;
  const rewardsDiscount = rewardsRedemption ? (rewardsRedemption.discountAmount || 0) : 0;
  const finalAmount = totalAmount - couponDiscount - rewardsDiscount;

  // Build booking payload — single source of truth, extracted to avoid duplication.
  const buildBookingData = () => {
    const bookingData = {
      bookingType: type,
      paymentMethod,
      simulateFailure: false,
      couponCode: couponResult?.code ?? undefined,
    };
    if (type === 'FLIGHT') {
      bookingData.flightId = Number(id);
      bookingData.cabinClass = cabinClass;
      bookingData.passengerCount = passengers;
      bookingData.seatNumbers = seatNumbers || undefined;
      if (seatIds) bookingData.seatIds = seatIds.split(',').map(Number);
      if (fareOptionId) bookingData.fareOptionId = Number(fareOptionId);
      if (addOnIdsParam) bookingData.addOnIds = addOnIdsParam.split(',').map(Number);
      bookingData.passengers = passengerData.map(p => ({
        title: p.title, firstName: p.firstName, middleName: p.middleName || undefined,
        lastName: p.lastName, gender: p.gender, dateOfBirth: p.dateOfBirth || undefined,
        nationality: p.nationality, email: p.email, phone: p.phone,
        passportNumber: p.passportNumber || undefined, passportExpiry: p.passportExpiry || undefined,
      }));
      if (effectiveFreezeId) bookingData.freezeId = Number(effectiveFreezeId);
    } else if (type === 'HOTEL') {
      bookingData.hotelId = Number(id);
      bookingData.roomId = Number(selectedRoomId);
      bookingData.checkInDate = checkIn;
      bookingData.checkOutDate = checkOut;
      bookingData.numberOfNights = nights;
      const guest = passengerData[0] || {};
      bookingData.passengerCount = 1;
      bookingData.passengers = [{
        title: guest.title, firstName: guest.firstName, lastName: guest.lastName,
        email: guest.email, phone: guest.phone,
        gender: guest.gender || 'OTHER', nationality: guest.nationality || 'Indian',
      }];
    } else if (type === 'TRAIN') {
      bookingData.trainId = Number(id);
      bookingData.trainClass = trainClass;
      bookingData.passengerCount = passengers;
      if (boardingPoint) bookingData.boardingPoint = boardingPoint;
      if (droppingPoint) bookingData.droppingPoint = droppingPoint;
      bookingData.passengers = passengerData.map(p => ({
        title: p.title, firstName: p.firstName, lastName: p.lastName,
        gender: p.gender, email: p.email, phone: p.phone,
      }));
    } else if (type === 'BUS') {
      bookingData.busId = Number(id);
      bookingData.passengerCount = passengers;
      if (boardingPoint) bookingData.boardingPoint = boardingPoint;
      if (droppingPoint) bookingData.droppingPoint = droppingPoint;
      bookingData.passengers = passengerData.map(p => ({
        title: p.title, firstName: p.firstName, lastName: p.lastName,
        gender: p.gender, email: p.email, phone: p.phone,
      }));
    } else if (type === 'CAB') {
      bookingData.cabId = Number(id);
      bookingData.cabType = cabType;
      bookingData.pickup = pickup;
      bookingData.dropLocation = drop;
      const guest = passengerData[0] || {};
      bookingData.passengerCount = 1;
      bookingData.passengers = [{
        title: guest.title, firstName: guest.firstName, lastName: guest.lastName,
        email: guest.email, phone: guest.phone,
      }];
    }
    return bookingData;
  };

  const handlePayment = async () => {
    if (isProcessing) return; // double-click protection
    setIsProcessing(true);
    setStep(2);
    setErrors({});
    try {
      const orderRes = await paymentApi.createOrder(finalAmount, 'INR', paymentMethod);
      const orderData = orderRes.data;

      if (!window.Razorpay) {
        throw new Error('Razorpay Checkout SDK is not loaded. Please check your internet connection and refresh.');
      }

      const methodKey = paymentMethod === 'CARD' ? 'card' : paymentMethod === 'UPI' ? 'upi' : 'netbanking';

      const options = {
        key: orderData.keyId,
        amount: Math.round(orderData.amount * 100),
        currency: orderData.currency || 'INR',
        name: 'Voyara',
        description: `${type} Booking - ${paymentMethod}`,
        order_id: orderData.orderId,
        handler: async function (response) {
          try {
            const verifyRes = await paymentApi.verifyPayment(
              response.razorpay_order_id,
              response.razorpay_payment_id,
              response.razorpay_signature
            );
            if (verifyRes.data.success) {
              const bookingData = buildBookingData();
              bookingData.razorpayOrderId = response.razorpay_order_id;
              bookingData.razorpayPaymentId = response.razorpay_payment_id;
              bookingData.paymentId = verifyRes.data.paymentId || response.razorpay_payment_id;
              bookingData.paymentMethod = paymentMethod;
              bookingMutation.mutate(bookingData);
            } else {
              setErrors({ payment: 'Payment verification failed. Please contact customer support.' });
              setStep(2);
              setIsProcessing(false);
            }
          } catch (err) {
            setErrors({ payment: err.response?.data?.message || 'Payment signature verification error. Please retry.' });
            setStep(2);
            setIsProcessing(false);
          }
        },
        prefill: {
          name: passengerData[0] ? `${passengerData[0].firstName} ${passengerData[0].lastName}` : '',
          email: passengerData[0]?.email || '',
          contact: passengerData[0]?.phone || '',
          method: methodKey,
        },
        theme: { color: '#2563EB' },
        modal: {
          ondismiss: function () {
            setErrors({ payment: 'Payment was cancelled. You can retry with any payment method.' });
            setStep(2);
            setIsProcessing(false);
          },
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response) {
        const reason = response.error?.description || 'Payment was declined. Please try another payment method.';
        setErrors({ payment: reason });
        setStep(2);
        setIsProcessing(false);
      });
      rzp.open();
    } catch (err) {
      const status = err.response?.status;
      let msg = 'Unable to start payment. Please try again.';
      if (status === 401 || status === 403) {
        msg = 'Your session has expired. Please sign in again.';
      } else if (status === 404) {
        msg = 'This booking is no longer available.';
      } else if (!err.response) {
        msg = 'Connection problem. Check your internet connection and retry.';
      } else if (err.response?.data?.message) {
        msg = err.response.data.message;
      }
      setErrors({ payment: msg });
      setStep(2);
      setIsProcessing(false);
    }
  };

  if (!type || !id) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="text-5xl mb-4">🛒</div>
        <h2 className="text-xl font-semibold text-gray-900">Invalid booking request</h2>
        <p className="text-gray-500 text-sm mt-2">Please start from a flight or hotel search.</p>
        <Link to="/" className="text-blue-600 hover:underline mt-3 inline-block text-sm font-medium">Go to Home</Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6">
      {/* Progress */}
      <div className="flex items-center justify-center gap-1 sm:gap-2 mb-8">
        {STEPS.map((s, i) => (
          <div key={s} className="flex items-center gap-1 sm:gap-2">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all ${
              i <= step ? 'bg-blue-600 text-white shadow-sm' : 'bg-gray-200 text-gray-500'
            }`}>{i + 1}</div>
            <span className={`text-sm font-medium hidden sm:block ${i <= step ? 'text-gray-900' : 'text-gray-400'}`}>{s}</span>
            {i < 3 && <div className={`w-8 sm:w-12 h-0.5 ${i < step ? 'bg-blue-600' : 'bg-gray-200'}`} />}
          </div>
        ))}
      </div>

      {/* Step 0: Review */}
      {step === 0 && (
        <div className="space-y-5">
          <div className="bg-white rounded-2xl p-5 sm:p-6 shadow-sm border border-gray-100">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Trip Summary</h2>
            {type === 'FLIGHT' && flight && (
              <div className="space-y-3">
                <div className="flex items-center gap-3 pb-3 border-b border-gray-100">
                  <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600 font-bold text-xs">{flight.airlineCode}</div>
                  <div>
                    <div className="font-semibold text-gray-900">{flight.airlineName} · {flight.flightNumber}</div>
                    <div className="text-sm text-gray-500">{flight.originCode} → {flight.destinationCode} · {flight.departureDate}</div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div><span className="text-gray-500">Class</span><div className="font-medium">{cabinClass.replace('_', ' ')}</div></div>
                  <div><span className="text-gray-500">Passengers</span><div className="font-medium">{passengers}</div></div>
                  {selectedFare && <div><span className="text-gray-500">Fare</span><div className="font-medium">{selectedFare.name}</div></div>}
                  {seatNumbers && <div><span className="text-gray-500">Seats</span><div className="font-medium text-blue-600">{seatNumbers}</div></div>}
                </div>
              </div>
            )}
            {type === 'HOTEL' && hotel && (
              <div className="space-y-3">
                <div className="font-semibold text-gray-900">{hotel.name}</div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div><span className="text-gray-500">Check-in</span><div className="font-medium">{checkIn}</div></div>
                  <div><span className="text-gray-500">Check-out</span><div className="font-medium">{checkOut}</div></div>
                  <div><span className="text-gray-500">Nights</span><div className="font-medium">{nights}</div></div>
                  {selectedRoom && <div><span className="text-gray-500">Room</span><div className="font-medium">{selectedRoom.name || selectedRoom.roomType}</div></div>}
                </div>
              </div>
            )}
            {type === 'HOTEL' && rooms && rooms.length > 0 && (
              <div className="mt-4 pt-4 border-t border-gray-100">
                <RoomSelector
                  rooms={rooms}
                  selectedRoomId={selectedRoomId}
                  onSelectRoom={(room) => setSelectedRoomId(String(room.id))}
                  nights={nights}
                />
              </div>
            )}
            {type === 'TRAIN' && train && (
              <div className="space-y-3">
                <div className="flex items-center gap-3 pb-3 border-b border-gray-100">
                  <div className="w-10 h-10 rounded-lg bg-orange-50 flex items-center justify-center text-orange-600 font-bold text-xs">🚂</div>
                  <div>
                    <div className="font-semibold text-gray-900">{train.trainName} · {train.trainNumber}</div>
                    <div className="text-sm text-gray-500">{train.origin} → {train.destination} · {train.departureTime} - {train.arrivalTime}</div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div><span className="text-gray-500">Class</span><div className="font-medium">{trainClass}</div></div>
                  <div><span className="text-gray-500">Passengers</span><div className="font-medium">{passengers}</div></div>
                  {boardingPoint && <div><span className="text-gray-500">Boarding</span><div className="font-medium">{boardingPoint}</div></div>}
                  {droppingPoint && <div><span className="text-gray-500">Dropping</span><div className="font-medium">{droppingPoint}</div></div>}
                </div>
              </div>
            )}
            {type === 'BUS' && bus && (
              <div className="space-y-3">
                <div className="flex items-center gap-3 pb-3 border-b border-gray-100">
                  <div className="w-10 h-10 rounded-lg bg-green-50 flex items-center justify-center text-green-600 font-bold text-xs">🚌</div>
                  <div>
                    <div className="font-semibold text-gray-900">{bus.operatorName} · {bus.busType}</div>
                    <div className="text-sm text-gray-500">{bus.origin} → {bus.destination} · {bus.departureTime} - {bus.arrivalTime}</div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div><span className="text-gray-500">Bus Type</span><div className="font-medium">{bus.busType}</div></div>
                  <div><span className="text-gray-500">Passengers</span><div className="font-medium">{passengers}</div></div>
                </div>
              </div>
            )}
            {type === 'CAB' && cab && (
              <div className="space-y-3">
                <div className="flex items-center gap-3 pb-3 border-b border-gray-100">
                  <div className="w-10 h-10 rounded-lg bg-purple-50 flex items-center justify-center text-purple-600 font-bold text-xs">🚕</div>
                  <div>
                    <div className="font-semibold text-gray-900">{cab.vehicleName} · {cab.vehicleType}</div>
                    <div className="text-sm text-gray-500">{pickup || 'Pickup'} → {drop || 'Drop'} · {cabType}</div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div><span className="text-gray-500">Vehicle</span><div className="font-medium">{cab.vehicleType} {cab.ac ? '(AC)' : '(Non-AC)'}</div></div>
                  <div><span className="text-gray-500">Capacity</span><div className="font-medium">{cab.capacity} seats</div></div>
                </div>
              </div>
            )}
          </div>

          <div className="bg-white rounded-2xl p-5 sm:p-6 shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-900 mb-3">Price Breakdown</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-gray-500">Base fare × {quantity}</span><span>₹{baseFareTotal.toLocaleString()}</span></div>
              {seatNumbers && (
                <div className="flex justify-between text-blue-600">
                  <span>Seat selection ({seatNumbers})</span>
                  <span>{seatSurchargesTotal > 0 ? `+₹${seatSurchargesTotal.toLocaleString()}` : '₹0 (Included)'}</span>
                </div>
              )}
              <div className="flex justify-between"><span className="text-gray-500">Taxes & fees × {quantity}</span><span>₹{taxesTotal.toLocaleString()}</span></div>
              {activeFreeze?.data?.active && (
                <div className="bg-green-50 border border-green-200 rounded-xl p-2.5 text-xs text-green-700 font-medium">🔒 Price frozen at ₹{Number(activeFreeze.data.freeze.frozenPrice).toLocaleString()}/person</div>
              )}
              {/* Coupon UI */}
              {!couponResult ? (
                <div className="border-t pt-3">
                  <p className="text-xs text-gray-500 mb-2">Have a promo code?</p>
                  <div className="flex gap-2">
                    <input value={couponCode} onChange={e => setCouponCode(e.target.value.toUpperCase())} placeholder="Enter code"
                      className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 uppercase" onKeyDown={e => e.key === 'Enter' && handleApplyCoupon()} />
                    <button onClick={handleApplyCoupon} disabled={couponLoading || !couponCode.trim()}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors">
                      {couponLoading ? '...' : 'Apply'}
                    </button>
                  </div>
                  {couponError && <p className="text-xs text-red-600 mt-1.5">{couponError}</p>}
                </div>
              ) : (
                <div className="border-t pt-3">
                  <div className="flex items-center justify-between bg-green-50 border border-green-200 rounded-lg p-3">
                    <div>
                      <p className="text-sm font-medium text-green-700">✓ {couponResult.code}</p>
                      <p className="text-xs text-green-600">You save ₹{Number(couponResult.discountAmount).toLocaleString()}</p>
                    </div>
                    <button onClick={handleRemoveCoupon} className="text-xs text-red-500 hover:text-red-700 font-medium">Remove</button>
                  </div>
                </div>
              )}
              {couponResult && (
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Coupon discount</span><span className="text-green-600">-₹{Number(couponResult.discountAmount).toLocaleString()}</span>
                </div>
              )}

              {/* Rewards Redemption */}
              {rewardsData?.data?.pointsBalance > 0 ? (
                !rewardsRedemption ? (
                  <div className="border-t pt-3">
                    <p className="text-xs text-gray-500 mb-2">⭐ Use Voyara Rewards points</p>
                    <div className="flex items-center gap-2 mb-1">
                      <input type="number" min="0" max={rewardsData?.data?.pointsBalance || 0}
                        value={rewardsPointsToRedeem || ''}
                        onChange={e => setRewardsPointsToRedeem(Math.max(0, Number(e.target.value)))}
                        placeholder={`${rewardsData?.data?.pointsBalance || 0} pts available`}
                        className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500" />
                      <button onClick={handleValidateRewards}
                        disabled={rewardsLoading || !rewardsPointsToRedeem}
                        className="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50 transition-colors">
                        {rewardsLoading ? '...' : 'Use'}
                      </button>
                    </div>
                    {rewardsError && <p className="text-xs text-red-600 mt-1">{rewardsError}</p>}
                    <p className="text-[11px] text-gray-400 mt-1">10 points = ₹1 discount</p>
                  </div>
                ) : (
                  <div className="border-t pt-3">
                    <div className="flex items-center justify-between bg-purple-50 border border-purple-200 rounded-lg p-3">
                      <div>
                        <p className="text-sm font-medium text-purple-700">⭐ {rewardsRedemption.pointsToRedeem} points redeemed</p>
                        <p className="text-xs text-purple-600">You save ₹{Number(rewardsRedemption.discountAmount).toLocaleString()}</p>
                      </div>
                      <button onClick={handleRemoveRewards} className="text-xs text-red-500 hover:text-red-700 font-medium">Remove</button>
                    </div>
                  </div>
                )
              ) : null}
              {rewardsRedemption && (
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Rewards discount</span><span className="text-purple-600">-₹{Number(rewardsRedemption.discountAmount).toLocaleString()}</span>
                </div>
              )}

              <div className="border-t pt-2 flex justify-between font-bold text-lg">
                <span>Total</span><span className="text-blue-600">₹{finalAmount.toLocaleString()}</span>
              </div>
            </div>
          </div>

          <button onClick={() => setStep(1)}
            className="w-full bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white py-3.5 rounded-xl font-bold text-sm transition-all shadow-lg shadow-orange-500/20">
            Continue to Passenger Details →
          </button>
        </div>
      )}

      {/* Step 1: Passengers */}
      {step === 1 && (
        <div className="space-y-5">
          <h2 className="text-lg font-bold text-gray-900">Passenger Details</h2>
          {type === 'FLIGHT' && passengerData.map((p, i) => (
            <div key={i}>
              <PassengerForm index={i} passenger={p} onChange={(updated) => {
                const newData = [...passengerData]; newData[i] = updated; setPassengerData(newData);
              }} showPassport={false} />
              {errors[`p${i}_first`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_first`]}</p>}
              {errors[`p${i}_last`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_last`]}</p>}
              {errors[`p${i}_title`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_title`]}</p>}
              {errors[`p${i}_gender`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_gender`]}</p>}
              {errors[`p${i}_email`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_email`]}</p>}
              {errors[`p${i}_phone`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_phone`]}</p>}
            </div>
          ))}
          {type === 'HOTEL' && (
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-900 mb-4 flex items-center gap-2">
                <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center text-sm font-bold">1</span>
                Primary Guest <span className="text-xs bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full font-medium">Contact Person</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Title *</label>
                  <select value={passengerData[0]?.title || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], title: e.target.value}; setPassengerData(nd); }}
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none">
                    <option value="">Select</option>
                    <option>Mr</option><option>Mrs</option><option>Ms</option><option>Dr</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">First Name *</label>
                  <input type="text" value={passengerData[0]?.firstName || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], firstName: e.target.value}; setPassengerData(nd); }} placeholder="First name"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Last Name *</label>
                  <input type="text" value={passengerData[0]?.lastName || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], lastName: e.target.value}; setPassengerData(nd); }} placeholder="Last name"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Email *</label>
                  <input type="email" value={passengerData[0]?.email || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], email: e.target.value}; setPassengerData(nd); }} placeholder="email@example.com"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Phone *</label>
                  <input type="tel" value={passengerData[0]?.phone || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], phone: e.target.value}; setPassengerData(nd); }} placeholder="+91 98765 43210"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Special Requests</label>
                  <textarea value={passengerData[0]?.specialRequests || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], specialRequests: e.target.value}; setPassengerData(nd); }} placeholder="E.g., early check-in, extra towels, dietary needs..."
                    rows={3} className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none resize-none" />
                </div>
              </div>
            </div>
          )}
          {(type === 'TRAIN' || type === 'BUS') && passengerData.map((p, i) => (
            <div key={i}>
              <PassengerForm index={i} passenger={p} onChange={(updated) => {
                const newData = [...passengerData]; newData[i] = updated; setPassengerData(newData);
              }} showPassport={false} />
              {errors[`p${i}_first`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_first`]}</p>}
              {errors[`p${i}_last`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_last`]}</p>}
              {errors[`p${i}_title`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_title`]}</p>}
              {errors[`p${i}_gender`] && <p className="text-red-500 text-xs mt-1">{errors[`p${i}_gender`]}</p>}
            </div>
          ))}
          {type === 'CAB' && (
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
              <h3 className="font-bold text-gray-900 mb-4 flex items-center gap-2">
                <span className="w-7 h-7 rounded-full bg-purple-600 text-white flex items-center justify-center text-sm font-bold">1</span>
                Passenger <span className="text-xs bg-purple-100 text-purple-600 px-2 py-0.5 rounded-full font-medium">Contact Person</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Full Name *</label>
                  <input type="text" value={passengerData[0]?.firstName || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], firstName: e.target.value, lastName: e.target.value}; setPassengerData(nd); }} placeholder="Full name"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Phone *</label>
                  <input type="tel" value={passengerData[0]?.phone || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], phone: e.target.value}; setPassengerData(nd); }} placeholder="+91 98765 43210"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 mb-1">Email *</label>
                  <input type="email" value={passengerData[0]?.email || ''} onChange={e => { const nd = [...passengerData]; nd[0] = {...nd[0], email: e.target.value}; setPassengerData(nd); }} placeholder="email@example.com"
                    className="w-full px-3 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
                </div>
              </div>
            </div>
          )}
          <div className="flex gap-3">
            <button onClick={() => setStep(0)} className="px-6 py-3 border border-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 transition-colors">← Back</button>
            <button onClick={() => { if (validatePassengers()) setStep(2); }}
              className="flex-1 bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 text-white py-3 rounded-xl font-bold text-sm transition-all shadow-lg shadow-orange-500/20">
              Continue to Payment →
            </button>
          </div>
        </div>
      )}

      {/* Step 2: Payment */}
      {step === 2 && (
        <div className="space-y-5">
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-900 mb-4">Payment Method</h3>
            <div className="flex gap-2 mb-5 flex-wrap">
              {[{ key: 'CARD', label: '💳 Card' }, { key: 'UPI', label: '📱 UPI' }, { key: 'NET_BANKING', label: '🏦 Net Banking' }].map(m => (
                <button key={m.key} onClick={() => setPaymentMethod(m.key)}
                  className={`px-4 py-2 rounded-xl text-sm font-medium border transition-all ${
                    paymentMethod === m.key ? 'bg-blue-600 text-white border-blue-600' : 'bg-gray-50 text-gray-700 border-gray-200 hover:border-blue-300'
                  }`}>{m.label}</button>
              ))}
            </div>
            {paymentMethod === 'CARD' && (
              <div className="space-y-3">
                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">Credit & Debit Cards</span>
                    <div className="flex gap-1.5 text-xs font-bold text-slate-500">
                      <span className="px-2 py-0.5 bg-white border border-slate-200 rounded">Visa</span>
                      <span className="px-2 py-0.5 bg-white border border-slate-200 rounded">Mastercard</span>
                      <span className="px-2 py-0.5 bg-white border border-slate-200 rounded">RuPay</span>
                    </div>
                  </div>
                  <p className="text-xs text-slate-600 leading-relaxed">
                    Card payment will be securely processed by Razorpay. Clicking <strong>Continue to Secure Payment</strong> opens the encrypted gateway where you can enter your card details.
                  </p>
                  <div className="bg-amber-50 border border-amber-200/80 rounded-lg p-2.5 text-xs text-amber-800 space-y-1">
                    <div className="font-bold text-[11px] uppercase tracking-wider text-amber-900">⚡ Test Mode Domestic Cards:</div>
                    <div>• Visa: <code className="bg-amber-100/80 px-1.5 py-0.5 rounded font-mono font-bold text-amber-900">4718 6091 0820 4366</code> (Expiry: Any future date, CVV: 123)</div>
                    <div>• Mastercard: <code className="bg-amber-100/80 px-1.5 py-0.5 rounded font-mono font-bold text-amber-900">5555 5100 0000 1006</code></div>
                  </div>
                </div>
              </div>
            )}
            {paymentMethod === 'UPI' && (
              <div className="p-4 bg-blue-50/70 border border-blue-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-blue-900 uppercase tracking-wider">Instant UPI Payment</span>
                  <div className="flex gap-1.5 text-xs font-bold text-blue-700">
                    <span className="px-2 py-0.5 bg-white border border-blue-200 rounded">Google Pay</span>
                    <span className="px-2 py-0.5 bg-white border border-blue-200 rounded">PhonePe</span>
                    <span className="px-2 py-0.5 bg-white border border-blue-200 rounded">Paytm</span>
                  </div>
                </div>
                <p className="text-xs text-blue-800 leading-relaxed">
                  Proceed to Razorpay Checkout to scan QR code or enter your UPI ID for instant authentication.
                </p>
                <div className="bg-blue-100/70 border border-blue-300/80 rounded-lg p-2.5 text-xs text-blue-900">
                  <strong>⚡ Test Mode UPI:</strong> Enter <code className="bg-white px-1.5 py-0.5 rounded font-mono font-bold text-blue-900">success@razorpay</code> in the UPI prompt to simulate an authorized payment.
                </div>
              </div>
            )}
            {paymentMethod === 'NET_BANKING' && (
              <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-emerald-900 uppercase tracking-wider">All Major Indian Banks</span>
                  <div className="flex gap-1.5 text-xs font-bold text-emerald-700">
                    <span className="px-2 py-0.5 bg-white border border-emerald-200 rounded">HDFC</span>
                    <span className="px-2 py-0.5 bg-white border border-emerald-200 rounded">ICICI</span>
                    <span className="px-2 py-0.5 bg-white border border-emerald-200 rounded">SBI</span>
                    <span className="px-2 py-0.5 bg-white border border-emerald-200 rounded">Axis</span>
                  </div>
                </div>
                <p className="text-xs text-emerald-800 leading-relaxed">
                  Proceed to Razorpay Checkout to choose your bank and log in through your bank's secure net banking portal.
                </p>
                <div className="bg-emerald-100/70 border border-emerald-300/80 rounded-lg p-2.5 text-xs text-emerald-900">
                  <strong>⚡ Test Mode Net Banking:</strong> Select any bank and click <strong className="text-emerald-800">Success</strong> on the bank simulation page.
                </div>
              </div>
            )}
            {errors.payment && <p className="text-red-500 text-sm mt-3 bg-red-50 border border-red-200 rounded-xl p-3">{errors.payment}</p>}
          </div>

          {/* Price summary sidebar */}
          <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
            <h3 className="font-bold text-gray-900 mb-3">Order Summary</h3>
            <div className="space-y-2 text-sm">
              {type === 'FLIGHT' && <div className="flex justify-between"><span className="text-gray-500">{flight?.airlineName} {flight?.flightNumber}</span><span>{flight?.originCode} → {flight?.destinationCode}</span></div>}
              <div className="flex justify-between"><span className="text-gray-500">Base fare × {quantity}</span><span>₹{baseFareTotal.toLocaleString()}</span></div>
              {seatSurchargesTotal > 0 && (
                <div className="flex justify-between text-blue-600">
                  <span>Seat selection ({seatNumbers})</span>
                  <span>+₹{seatSurchargesTotal.toLocaleString()}</span>
                </div>
              )}
              <div className="flex justify-between"><span className="text-gray-500">Taxes × {quantity}</span><span>₹{taxesTotal.toLocaleString()}</span></div>
              {couponResult && (
                <div className="flex justify-between">
                  <span className="text-green-600">✓ {couponResult.code}</span>
                  <span className="text-green-600">-₹{Number(couponResult.discountAmount).toLocaleString()}</span>
                </div>
              )}
              <div className="border-t pt-2 flex justify-between font-bold text-lg">
                <span>Total</span><span className="text-blue-600">₹{finalAmount.toLocaleString()}</span>
              </div>
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button
              onClick={() => setStep(1)}
              disabled={isProcessing}
              className="travel-button-secondary px-6 py-3 text-xs font-bold disabled:opacity-50"
            >
              ← Back to Details
            </button>
            <button
              onClick={handlePayment}
              disabled={isProcessing}
              className="flex-1 travel-button-primary py-3.5 text-sm font-bold shadow-lg shadow-primary/25 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <LockIcon className="w-4 h-4" />
              <span>{isProcessing ? 'Connecting to Razorpay...' : `Continue to Secure Payment · ₹${finalAmount.toLocaleString()}`}</span>
            </button>
          </div>

          <div className="mt-4 flex items-center justify-center gap-4 text-xs text-slate-400">
            <span className="flex items-center gap-1">
              <LockIcon className="w-3.5 h-3.5 text-emerald-500" />
              256-bit SSL Encrypted
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <ShieldCheckIcon className="w-3.5 h-3.5 text-primary" />
              Razorpay Secured Gateway
            </span>
          </div>
        </div>
      )}

      {/* Step 2.5: Processing */}
      {step === 2 && isProcessing && (
        <div className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="voyara-card p-8 text-center max-w-sm mx-4 shadow-2xl">
            <div className="w-14 h-14 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <h2 className="text-lg font-black text-slate-900 mb-1">Verifying Payment...</h2>
            <p className="text-slate-500 text-xs">Communicating with banking gateway. Please do not refresh.</p>
          </div>
        </div>
      )}

      {/* Step 3: Confirmation */}
      {step === 3 && bookingResult && (
        <div className="voyara-card p-6 sm:p-10 text-center animate-fade-in max-w-xl mx-auto">
          <div className="w-20 h-20 rounded-3xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto mb-4 ring-8 ring-emerald-50/60 shadow-lg shadow-emerald-500/10">
            <CheckIcon className="w-10 h-10" />
          </div>

          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold mb-2">
            Reservation Confirmed & Secured
          </span>

          <h2 className="text-2xl sm:text-3xl font-black text-slate-900 mb-2">You're ready to travel!</h2>
          <p className="text-slate-500 text-xs sm:text-sm mb-6 max-w-md mx-auto">
            Your booking reference has been confirmed by the provider. Voyara Guardian is now monitoring your itinerary.
          </p>

          <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4 mb-6 inline-block w-full max-w-sm">
            <p className="text-[10px] uppercase font-bold tracking-wider text-slate-400 mb-1">Booking Reference</p>
            <p className="text-2xl font-mono font-black text-primary select-all tracking-wider">
              {bookingResult.bookingReference}
            </p>
          </div>

          <div className="bg-slate-50 rounded-2xl border border-slate-200/70 p-4 mb-6 text-left text-xs space-y-2.5 max-w-md mx-auto">
            {type === 'FLIGHT' && (
              <>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Airline</span><span className="font-bold text-slate-900">{bookingResult.airlineName} {bookingResult.flightNumber}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Route</span><span className="font-bold text-slate-900">{bookingResult.originCode} → {bookingResult.destinationCode}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Cabin Class</span><span className="font-bold text-slate-900">{bookingResult.cabinClass?.replace('_', ' ')}</span></div>
                {bookingResult.seatNumbers && <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Allocated Seats</span><span className="font-bold text-primary">{bookingResult.seatNumbers}</span></div>}
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Passengers</span><span className="font-bold text-slate-900">{bookingResult.passengerCount} Guest(s)</span></div>
              </>
            )}
            {type === 'HOTEL' && (
              <>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Hotel</span><span className="font-bold text-slate-900">{bookingResult.hotelName}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Room</span><span className="font-bold text-slate-900">{bookingResult.roomName}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Duration</span><span className="font-bold text-slate-900">{bookingResult.numberOfNights} Night(s)</span></div>
              </>
            )}
            {type === 'TRAIN' && (
              <>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Train</span><span className="font-bold text-slate-900">{bookingResult.trainName || train?.trainName} ({bookingResult.trainNumber || train?.trainNumber})</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Route</span><span className="font-bold text-slate-900">{bookingResult.origin || train?.origin} → {bookingResult.destination || train?.destination}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Class</span><span className="font-bold text-slate-900">{bookingResult.trainClass || trainClass}</span></div>
              </>
            )}
            {type === 'BUS' && (
              <>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Operator</span><span className="font-bold text-slate-900">{bookingResult.operatorName || bus?.operatorName}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Route</span><span className="font-bold text-slate-900">{bookingResult.origin || bus?.origin} → {bookingResult.destination || bus?.destination}</span></div>
              </>
            )}
            {type === 'CAB' && (
              <>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Vehicle</span><span className="font-bold text-slate-900">{bookingResult.vehicleName || cab?.vehicleName}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Pickup</span><span className="font-bold text-slate-900">{bookingResult.pickup || pickup}</span></div>
                <div className="flex justify-between pb-1.5 border-b border-slate-100"><span className="text-slate-500">Drop</span><span className="font-bold text-slate-900">{bookingResult.drop || drop}</span></div>
              </>
            )}
            <div className="flex justify-between font-black text-sm pt-2 text-slate-900 border-t border-slate-200">
              <span>Total Paid</span>
              <span className="text-primary">₹{bookingResult.totalAmount?.toLocaleString()}</span>
            </div>
          </div>

          <div className="flex gap-2.5 justify-center flex-wrap">
            <Link to="/dashboard" className="travel-button-primary px-6 py-2.5 text-xs font-bold">
              View in My Trips
            </Link>
            {type === 'FLIGHT' && (
              <Link to="/live-tracker" className="travel-button-secondary px-5 py-2.5 text-xs font-bold">
                Track Live Flight
              </Link>
            )}
            <Link to="/" className="travel-button-secondary px-5 py-2.5 text-xs font-bold">
              Home
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
