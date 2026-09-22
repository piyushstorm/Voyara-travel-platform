import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTodayDate } from '../utils/dateUtils';

const DESTINATIONS = [
  { name: 'Mumbai', region: 'Maharashtra', emoji: '🌊', hotels: '250+', from: '₹899' },
  { name: 'Delhi', region: 'NCR', emoji: '🏛️', hotels: '400+', from: '₹799' },
  { name: 'Goa', region: 'Goa', emoji: '🏖️', hotels: '180+', from: '₹1,199' },
  { name: 'Bangalore', region: 'Karnataka', emoji: '🌆', hotels: '200+', from: '₹999' },
  { name: 'Jaipur', region: 'Rajasthan', emoji: '🏰', hotels: '150+', from: '₹699' },
  { name: 'Chennai', region: 'Tamil Nadu', emoji: '🛕', hotels: '130+', from: '₹749' },
  { name: 'Hyderabad', region: 'Telangana', emoji: '💎', hotels: '160+', from: '₹849' },
  { name: 'Udaipur', region: 'Rajasthan', emoji: '🏰', hotels: '80+', from: '₹1,299' },
  { name: 'Manali', region: 'Himachal Pradesh', emoji: '🏔️', hotels: '90+', from: '₹999' },
  { name: 'Kolkata', region: 'West Bengal', emoji: '🌉', hotels: '120+', from: '₹699' },
];

const DEALS = [
  {
    title: 'Weekend Getaway Sale',
    desc: 'Flat 20% off on weekend hotel bookings across India',
    tag: 'HOTEL DEAL',
    code: 'WEEKEND20',
    color: 'from-blue-600 to-indigo-800',
  },
  {
    title: 'First Hotel Booking',
    desc: 'Get 500 loyalty points on your first hotel booking',
    tag: 'NEW USER',
    code: 'FIRST500',
    color: 'from-emerald-500 to-teal-700',
  },
  {
    title: 'Luxury Stay Offers',
    desc: 'Premium 5-star hotels at 4-star prices this month',
    tag: 'PREMIUM',
    code: 'LUXURY',
    color: 'from-purple-500 to-indigo-700',
  },
];

const CATEGORIES = [
  { icon: '💎', label: 'Luxury', desc: '5-star properties' },
  { icon: '💰', label: 'Budget', desc: 'Under ₹1,500/night' },
  { icon: '🏖️', label: 'Beachfront', desc: 'Steps from the shore' },
  { icon: '🏢', label: 'Business', desc: 'Work-friendly stays' },
  { icon: '🏡', label: 'Boutique', desc: 'Unique & curated' },
  { icon: '🌿', label: 'Resorts', desc: 'All-inclusive escapes' },
];

const FEATURES = [
  {
    title: 'Best Price Guarantee',
    desc: 'Find a lower price? We match it instantly.',
    color: 'bg-emerald-50 text-emerald-600 border-emerald-200',
  },
  {
    title: 'Free Cancellation',
    desc: 'Most bookings offer free cancellation up to 24h before check-in.',
    color: 'bg-blue-50 text-blue-600 border-blue-200',
  },
  {
    title: 'Verified Properties',
    desc: 'Every hotel is inspected for quality and safety standards.',
    color: 'bg-purple-50 text-purple-600 border-purple-200',
  },
  {
    title: 'Real Guest Reviews',
    desc: 'Authentic reviews from verified guests who actually stayed.',
    color: 'bg-amber-50 text-amber-600 border-amber-200',
  },
  {
    title: 'Secure Payments',
    desc: 'Razorpay-powered with bank-level encryption.',
    color: 'bg-cyan-50 text-cyan-600 border-cyan-200',
  },
  {
    title: '24/7 Support',
    desc: 'Help whenever you need it — before, during, or after your stay.',
    color: 'bg-rose-50 text-rose-600 border-rose-200',
  },
];

/* =========================================================
   POPOVER
========================================================= */

function Popover({
  open,
  onClose,
  triggerRef,
  children,
  align = 'left',
}) {
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return;

    const handler = (e) => {
      if (
        ref.current &&
        !ref.current.contains(e.target) &&
        triggerRef?.current &&
        !triggerRef.current.contains(e.target)
      ) {
        onClose();
      }
    };

    const esc = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('mousedown', handler);
    document.addEventListener('keydown', esc);

    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('keydown', esc);
    };
  }, [open, onClose, triggerRef]);

  if (!open) return null;

  return (
    <div
      ref={ref}
      className={`
        absolute
        top-full
        mt-2
        z-50
        w-[calc(100vw-2rem)]
        max-w-[360px]
        sm:w-[360px]
        bg-white
        rounded-2xl
        shadow-2xl
        border
        border-gray-200
        overflow-hidden

        ${align === 'right' ? 'right-0' : 'left-0'}

        max-[479px]:fixed
        max-[479px]:left-4
        max-[479px]:right-4
        max-[479px]:top-auto
        max-[479px]:mt-0
        max-[479px]:w-auto
        max-[479px]:max-w-none
      `}
      role="dialog"
    >
      {children}
    </div>
  );
}

/* =========================================================
   STEPPER
========================================================= */

function StepperRow({
  label,
  sublabel,
  value,
  onDecrement,
  onIncrement,
  min,
  max,
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="min-w-0">
        <div className="font-medium text-gray-800 text-sm truncate">
          {label}
        </div>

        {sublabel && (
          <div className="text-[11px] text-gray-400">
            {sublabel}
          </div>
        )}
      </div>

      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        <button
          type="button"
          onClick={onDecrement}
          disabled={value <= min}
          className="
            w-9
            h-9
            sm:w-10
            sm:h-10
            rounded-full
            border
            border-gray-300
            flex
            items-center
            justify-center
            text-gray-600
            hover:bg-gray-100
            disabled:opacity-30
            disabled:cursor-not-allowed
            transition
            text-base
          "
          aria-label={`Decrease ${label.toLowerCase()}`}
        >
          −
        </button>

        <span className="w-6 text-center font-semibold text-gray-900 tabular-nums">
          {value}
        </span>

        <button
          type="button"
          onClick={onIncrement}
          disabled={value >= max}
          className="
            w-9
            h-9
            sm:w-10
            sm:h-10
            rounded-full
            border
            border-gray-300
            flex
            items-center
            justify-center
            text-gray-600
            hover:bg-gray-100
            disabled:opacity-30
            disabled:cursor-not-allowed
            transition
            text-base
          "
          aria-label={`Increase ${label.toLowerCase()}`}
        >
          +
        </button>
      </div>
    </div>
  );
}

/* =========================================================
   POPOVER HEADER
========================================================= */

function PopoverHeader({ title, onClose }) {
  return (
    <div className="px-4 sm:px-5 pt-4 sm:pt-5 pb-3 border-b border-gray-100 flex items-center justify-between gap-3">
      <h3 className="font-bold text-gray-900 text-base">
        {title}
      </h3>

      <button
        type="button"
        onClick={onClose}
        className="
          w-8
          h-8
          shrink-0
          flex
          items-center
          justify-center
          rounded-full
          hover:bg-gray-100
          text-gray-400
          hover:text-gray-600
          transition
        "
        aria-label="Close"
      >
        <svg
          className="w-4 h-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M6 18L18 6M6 6l12 12"
          />
        </svg>
      </button>
    </div>
  );
}

/* =========================================================
   POPOVER FOOTER
========================================================= */

function PopoverFooter({
  onClose,
  label = 'Done',
}) {
  return (
    <div className="px-4 sm:px-5 py-4 bg-gray-50 border-t border-gray-100">
      <button
        type="button"
        onClick={onClose}
        className="
          w-full
          bg-blue-600
          hover:bg-blue-700
          text-white
          py-2.5
          rounded-xl
          text-sm
          font-bold
          transition-colors
          active:scale-[0.98]
        "
      >
        {label}
      </button>
    </div>
  );
}

/* =========================================================
   GUEST POPOVER
========================================================= */

const AGE_OPTS = Array.from(
  { length: 10 },
  (_, i) => i + 2
);

function GuestPopoverContent({
  adults,
  setAdults,
  childrenCount,
  setChildrenCount,
  childAges,
  setChildAges,
  onClose,
}) {
  return (
    <>
      <PopoverHeader
        title="Guests"
        onClose={onClose}
      />

      <div className="px-4 sm:px-5 py-4 space-y-4">
        <StepperRow
          label="Adults"
          sublabel="12+ years"
          value={adults}
          min={1}
          max={9}
          onDecrement={() =>
            setAdults(Math.max(1, adults - 1))
          }
          onIncrement={() =>
            setAdults(Math.min(9, adults + 1))
          }
        />

        <StepperRow
          label="Children"
          sublabel="2–11 years"
          value={childrenCount}
          min={0}
          max={6}
          onDecrement={() => {
            const n = Math.max(
              0,
              childrenCount - 1
            );

            setChildrenCount(n);
            setChildAges(
              childAges.slice(0, n)
            );
          }}
          onIncrement={() => {
            if (childrenCount < 6) {
              setChildrenCount(
                childrenCount + 1
              );

              setChildAges([
                ...childAges,
                5,
              ]);
            }
          }}
        />

        {childrenCount > 0 && (
          <div className="pl-1 space-y-2 pt-3 border-t border-gray-100">
            <div className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
              Child ages
            </div>

            {Array.from(
              { length: childrenCount },
              (_, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between gap-3"
                >
                  <span className="text-xs text-gray-600">
                    Child {i + 1}
                  </span>

                  <select
                    value={
                      childAges[i] || 5
                    }
                    onChange={(e) => {
                      const ages = [
                        ...childAges,
                      ];

                      ages[i] = Number(
                        e.target.value
                      );

                      setChildAges(ages);
                    }}
                    className="
                      px-3
                      py-1.5
                      border
                      border-gray-200
                      rounded-lg
                      text-xs
                      text-gray-700
                      bg-white
                      focus:ring-2
                      focus:ring-blue-500/20
                      focus:border-blue-500
                      outline-none
                    "
                  >
                    {AGE_OPTS.map(
                      (age) => (
                        <option
                          key={age}
                          value={age}
                        >
                          {age} years
                        </option>
                      )
                    )}
                  </select>
                </div>
              )
            )}
          </div>
        )}
      </div>

      <PopoverFooter
        onClose={onClose}
      />
    </>
  );
}

/* =========================================================
   ROOM POPOVER
========================================================= */

function RoomPopoverContent({
  rooms,
  setRooms,
  onClose,
}) {
  const addRoom = () => {
    if (rooms.length < 5) {
      setRooms([
        ...rooms,
        {
          adults: 2,
          children: 0,
          childAges: [],
        },
      ]);
    }
  };

  const removeRoom = (i) => {
    if (rooms.length > 1) {
      setRooms(
        rooms.filter(
          (_, index) => index !== i
        )
      );
    }
  };

  const updateRoom = (i, patch) => {
    const next = [...rooms];

    next[i] = {
      ...next[i],
      ...patch,
    };

    setRooms(next);
  };

  return (
    <>
      <PopoverHeader
        title="Rooms"
        onClose={onClose}
      />

      <div
        className="
          px-4
          sm:px-5
          py-4
          max-h-[55dvh]
          overflow-y-auto
          overscroll-contain
          space-y-4
        "
      >
        {rooms.map((room, i) => (
          <div
            key={i}
            className={
              i > 0
                ? 'border-t border-gray-100 pt-4'
                : ''
            }
          >
            <div className="flex items-center justify-between gap-3 mb-3">
              <h4 className="font-semibold text-gray-900 text-sm">
                Room {i + 1}
              </h4>

              {rooms.length > 1 && (
                <button
                  type="button"
                  onClick={() =>
                    removeRoom(i)
                  }
                  className="
                    text-red-500
                    hover:text-red-600
                    text-xs
                    font-medium
                    shrink-0
                  "
                >
                  Remove
                </button>
              )}
            </div>

            <div className="space-y-3">
              <StepperRow
                label="Adults"
                sublabel="12+ years"
                value={room.adults}
                min={1}
                max={4}
                onDecrement={() =>
                  updateRoom(i, {
                    adults: Math.max(
                      1,
                      room.adults - 1
                    ),
                  })
                }
                onIncrement={() =>
                  updateRoom(i, {
                    adults: Math.min(
                      4,
                      room.adults + 1
                    ),
                  })
                }
              />

              <StepperRow
                label="Children"
                sublabel="2–11 years"
                value={room.children}
                min={0}
                max={4}
                onDecrement={() => {
                  const n = Math.max(
                    0,
                    room.children - 1
                  );

                  updateRoom(i, {
                    children: n,
                    childAges:
                      room.childAges.slice(
                        0,
                        n
                      ),
                  });
                }}
                onIncrement={() => {
                  if (room.children < 4) {
                    updateRoom(i, {
                      children:
                        room.children + 1,
                      childAges: [
                        ...room.childAges,
                        5,
                      ],
                    });
                  }
                }}
              />

              {room.children > 0 && (
                <div className="pl-1 space-y-2">
                  <div className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                    Child ages
                  </div>

                  {Array.from(
                    {
                      length:
                        room.children,
                    },
                    (_, ci) => (
                      <div
                        key={ci}
                        className="flex items-center justify-between gap-3"
                      >
                        <span className="text-xs text-gray-600">
                          Child {ci + 1}
                        </span>

                        <select
                          value={
                            room.childAges[
                              ci
                            ] || 5
                          }
                          onChange={(e) => {
                            const ages =
                              [
                                ...room.childAges,
                              ];

                            ages[ci] =
                              Number(
                                e.target.value
                              );

                            updateRoom(i, {
                              childAges:
                                ages,
                            });
                          }}
                          className="
                            px-3
                            py-1.5
                            border
                            border-gray-200
                            rounded-lg
                            text-xs
                            text-gray-700
                            bg-white
                            focus:ring-2
                            focus:ring-blue-500/20
                            focus:border-blue-500
                            outline-none
                          "
                        >
                          {AGE_OPTS.map(
                            (age) => (
                              <option
                                key={age}
                                value={age}
                              >
                                {age} years
                              </option>
                            )
                          )}
                        </select>
                      </div>
                    )
                  )}
                </div>
              )}
            </div>
          </div>
        ))}

        {rooms.length < 5 && (
          <button
            type="button"
            onClick={addRoom}
            className="
              flex
              items-center
              gap-2
              text-blue-600
              hover:text-blue-700
              text-sm
              font-semibold
              transition-colors
              py-1
            "
          >
            <svg
              className="w-4 h-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth="2"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 4.5v15m7.5-7.5h-15"
              />
            </svg>

            Add another room
          </button>
        )}
      </div>

      <PopoverFooter
        onClose={onClose}
      />
    </>
  );
}

/* =========================================================
   MAIN HOMEPAGE
========================================================= */

export default function HotelsHomepage() {
  const navigate = useNavigate();

  const [city, setCity] =
    useState('');

  const [checkIn, setCheckIn] =
    useState('');

  const [checkOut, setCheckOut] =
    useState('');

  const [adults, setAdults] =
    useState(2);

  const [
    childrenCount,
    setChildrenCount,
  ] = useState(0);

  const [childAges, setChildAges] =
    useState([]);

  const [rooms, setRooms] =
    useState([
      {
        adults: 2,
        children: 0,
        childAges: [],
      },
    ]);

  const [
    showDestDropdown,
    setShowDestDropdown,
  ] = useState(false);

  const [showGuests, setShowGuests] =
    useState(false);

  const [showRooms, setShowRooms] =
    useState(false);

  const [
    recentSearches,
    setRecentSearches,
  ] = useState(() => {
    try {
      return JSON.parse(
        localStorage.getItem(
          'hotelRecentSearches'
        ) || '[]'
      );
    } catch {
      return [];
    }
  });

  const destRef = useRef(null);
  const guestRef = useRef(null);
  const roomRef = useRef(null);

  /* =========================================================
     DATE HELPERS
  ========================================================= */

  const today = getTodayDate();

  const minCheckOut =
    checkIn || today;

  /* =========================================================
     CLOSE DROPDOWNS WHEN CLICKING OUTSIDE
  ========================================================= */

  useEffect(() => {
    const handler = (e) => {
      if (
        destRef.current &&
        !destRef.current.contains(
          e.target
        )
      ) {
        setShowDestDropdown(false);
      }

      if (
        guestRef.current &&
        !guestRef.current.contains(
          e.target
        )
      ) {
        setShowGuests(false);
      }

      if (
        roomRef.current &&
        !roomRef.current.contains(
          e.target
        )
      ) {
        setShowRooms(false);
      }
    };

    document.addEventListener(
      'mousedown',
      handler
    );

    return () => {
      document.removeEventListener(
        'mousedown',
        handler
      );
    };
  }, []);

  /* =========================================================
     FILTER DESTINATIONS
  ========================================================= */

  const filteredDests = city.trim()
    ? DESTINATIONS.filter(
        (d) =>
          d.name
            .toLowerCase()
            .includes(
              city.toLowerCase()
            ) ||
          d.region
            .toLowerCase()
            .includes(
              city.toLowerCase()
            )
      )
    : DESTINATIONS;

  /* =========================================================
     TOTALS
  ========================================================= */

  const totalGuests =
    adults + childrenCount;

  const totalRooms =
    rooms.length;

  const nights =
    checkIn && checkOut
      ? Math.max(
          1,
          Math.round(
            (new Date(checkOut) -
              new Date(checkIn)) /
              (1000 * 60 * 60 * 24)
          )
        )
      : 0;

  /* =========================================================
     SEARCH
  ========================================================= */

  const handleSearch = (e) => {
    e.preventDefault();

    if (!city.trim()) {
      return;
    }

    const params =
      new URLSearchParams({
        city: city.trim(),
        checkIn: checkIn || '',
        checkOut: checkOut || '',
        guests: String(
          totalGuests
        ),
        rooms: String(
          totalRooms
        ),
      });

    const newRecent = [
      {
        city: city.trim(),
        checkIn,
        checkOut,
        guests: totalGuests,
        rooms: totalRooms,
        ts: Date.now(),
      },
      ...recentSearches.filter(
        (r) =>
          r.city !== city.trim()
      ),
    ].slice(0, 5);

    setRecentSearches(
      newRecent
    );

    localStorage.setItem(
      'hotelRecentSearches',
      JSON.stringify(
        newRecent
      )
    );

    navigate(
      `/hotels/search?${params.toString()}`
    );
  };

  const selectDestination = (
    name
  ) => {
    setCity(name);
    setShowDestDropdown(false);
  };

  /* =========================================================
     SHARED FORM STYLES
  ========================================================= */

  const fieldClass = `
    w-full
    h-14
    px-3.5
    bg-white
    border
    border-gray-200
    rounded-xl
    text-sm
    text-gray-900
    focus:border-blue-500
    focus:ring-2
    focus:ring-blue-500/10
    outline-none
    transition
    [color-scheme:light]
  `;

  const labelClass = `
    block
    text-[11px]
    font-bold
    text-gray-400
    uppercase
    tracking-wider
    mb-1.5
  `;

  /* =========================================================
     RENDER
  ========================================================= */

  return (
    <div className="min-h-screen w-full max-w-full overflow-x-hidden bg-gray-50">

      {/* =====================================================
          HERO
      ===================================================== */}

      <section className="relative bg-gradient-to-br from-[#0a1628] via-[#152238] to-[#0d3150] overflow-visible">

        {/* Decorative background */}
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
          <div className="
            absolute
            top-0
            right-0
            w-56
            h-56
            sm:w-[500px]
            sm:h-[500px]
            bg-blue-500/8
            rounded-full
            blur-3xl
            translate-x-1/4
            -translate-y-1/4
          " />

          <div className="
            absolute
            bottom-0
            left-0
            w-48
            h-48
            sm:w-[350px]
            sm:h-[350px]
            bg-cyan-400/6
            rounded-full
            blur-3xl
            -translate-x-1/4
            translate-y-1/4
          " />
        </div>

        <div className="
          relative
          w-full
          max-w-6xl
          mx-auto
          px-4
          sm:px-6
          lg:px-8
          pt-9
          pb-10
          sm:pt-14
          sm:pb-14
          md:pt-16
          md:pb-20
        ">

          {/* Heading */}
          <div className="text-center mb-7 sm:mb-9">

            <h1 className="
              text-[1.6rem]
              min-[380px]:text-[1.75rem]
              sm:text-4xl
              md:text-5xl
              leading-tight
              font-bold
              text-white
              mb-3
              px-2
            ">
              Find Your Perfect Stay
            </h1>

            <p className="
              text-blue-200/60
              text-sm
              sm:text-base
              max-w-md
              mx-auto
              px-3
              leading-relaxed
            ">
              Hotels, resorts, and homes at the best prices with free cancellation
            </p>

          </div>

          {/* Search container */}
          <div className="w-full max-w-[1120px] mx-auto">

            <div className="
              bg-white
              rounded-2xl
              shadow-xl
              overflow-visible
              border
              border-gray-100
            ">

              <form
                onSubmit={handleSearch}
                className="
                  p-3.5
                  sm:p-5
                  md:p-6
                "
              >

                {/* Mobile: section heading */}
                <div className="
                  lg:hidden
                  text-center
                  mb-4
                ">
                  <span className="
                    text-xs
                    font-bold
                    text-gray-400
                    uppercase
                    tracking-widest
                  ">
                    Search Hotels
                  </span>
                </div>

                {/* =================================================
                    SEARCH FIELDS
                ================================================= */}

                <div className="
                  grid
                  grid-cols-1
                  min-[480px]:grid-cols-2
                  lg:grid-cols-5
                  gap-3
                  sm:gap-4
                ">

                  {/* DESTINATION */}

                  <div
                    className="
                      min-[480px]:col-span-2
                      lg:col-span-1
                      relative
                      min-w-0
                    "
                    ref={destRef}
                  >

                    <label
                      className={
                        labelClass
                      }
                    >
                      Destination
                    </label>

                    <button
                      type="button"
                      onClick={() =>
                        setShowDestDropdown(
                          !showDestDropdown
                        )
                      }
                      className={`
                        ${fieldClass}
                        text-left
                        flex
                        items-center
                        justify-between
                        ${
                          showDestDropdown
                            ? 'border-blue-500 ring-2 ring-blue-500/10'
                            : ''
                        }
                      `}
                    >

                      {city ? (
                        <span className="font-medium truncate">
                          {city}
                        </span>
                      ) : (
                        <span className="text-gray-400 truncate">
                          City or hotel name
                        </span>
                      )}

                      <svg
                        className="w-4 h-4 text-gray-400 shrink-0 ml-2"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
                        />
                      </svg>

                    </button>

                    {/* Destination dropdown */}

                    {showDestDropdown && (
                      <div className="
                        absolute
                        top-full
                        left-0
                        right-0
                        mt-1
                        bg-white
                        rounded-xl
                        shadow-xl
                        border
                        border-gray-200
                        z-50
                        max-h-72
                        overflow-y-auto
                      ">

                        {/* Recent */}

                        {recentSearches.length > 0 && (
                          <div className="p-2 border-b border-gray-100">

                            <div className="
                              text-[11px]
                              font-bold
                              text-gray-400
                              uppercase
                              tracking-wider
                              px-2
                              py-1
                            ">
                              Recent
                            </div>

                            {recentSearches.map(
                              (r, i) => (
                                <button
                                  key={i}
                                  type="button"
                                  onClick={() =>
                                    selectDestination(
                                      r.city
                                    )
                                  }
                                  className="
                                    w-full
                                    text-left
                                    px-3
                                    py-2
                                    text-sm
                                    text-gray-700
                                    hover:bg-blue-50
                                    rounded-lg
                                    flex
                                    items-center
                                    gap-2
                                  "
                                >
                                  <span className="text-gray-400">
                                    🕐
                                  </span>

                                  <span className="truncate">
                                    {r.city}
                                  </span>
                                </button>
                              )
                            )}

                          </div>
                        )}

                        {/* Popular / Results */}

                        <div className="p-2">

                          <div className="
                            text-[11px]
                            font-bold
                            text-gray-400
                            uppercase
                            tracking-wider
                            px-2
                            py-1
                          ">
                            {city.trim()
                              ? 'Results'
                              : 'Popular destinations'}
                          </div>

                          {filteredDests.map(
                            (d) => (
                              <button
                                key={d.name}
                                type="button"
                                onClick={() =>
                                  selectDestination(
                                    d.name
                                  )
                                }
                                className="
                                  w-full
                                  text-left
                                  px-3
                                  py-2.5
                                  text-sm
                                  hover:bg-blue-50
                                  rounded-lg
                                  flex
                                  items-center
                                  justify-between
                                  group
                                  gap-3
                                "
                              >

                                <div className="
                                  flex
                                  items-center
                                  gap-2
                                  min-w-0
                                ">

                                  <span className="text-base shrink-0">
                                    {d.emoji}
                                  </span>

                                  <span className="truncate">

                                    <span className="
                                      text-gray-900
                                      font-medium
                                      group-hover:text-blue-600
                                    ">
                                      {d.name}
                                    </span>

                                    <span className="
                                      text-gray-400
                                      text-xs
                                      ml-1.5
                                      hidden
                                      min-[380px]:inline
                                    ">
                                      {d.region}
                                    </span>

                                  </span>

                                </div>

                                <span className="
                                  text-xs
                                  text-gray-400
                                  shrink-0
                                ">
                                  {d.hotels}
                                </span>

                              </button>
                            )
                          )}

                          {filteredDests.length === 0 && (
                            <div className="
                              px-3
                              py-4
                              text-center
                              text-sm
                              text-gray-400
                            ">
                              No destinations found
                            </div>
                          )}

                        </div>

                      </div>
                    )}

                  </div>

                  {/* CHECK-IN */}

                  <div className="
                    min-w-0
                    min-[480px]:col-span-1
                    lg:col-span-1
                  ">

                    <label
                      className={
                        labelClass
                      }
                    >
                      Check-in
                    </label>

                    <input
                      type="date"
                      value={checkIn}
                      onChange={(e) => {
                        const value =
                          e.target.value;

                        setCheckIn(value);

                        if (
                          checkOut &&
                          value >= checkOut
                        ) {
                          setCheckOut('');
                        }
                      }}
                      min={today}
                      required
                      className={
                        fieldClass
                      }
                    />

                  </div>

                  {/* CHECK-OUT */}

                  <div className="
                    min-w-0
                    min-[480px]:col-span-1
                    lg:col-span-1
                  ">

                    <label
                      className={
                        labelClass
                      }
                    >
                      Check-out
                    </label>

                    <input
                      type="date"
                      value={checkOut}
                      onChange={(e) =>
                        setCheckOut(
                          e.target.value
                        )
                      }
                      min={minCheckOut}
                      required
                      className={
                        fieldClass
                      }
                    />

                  </div>

                  {/* GUESTS */}

                  <div
                    className="
                      min-w-0
                      relative
                      min-[480px]:col-span-1
                      lg:col-span-1
                    "
                    ref={guestRef}
                  >

                    <label
                      className={
                        labelClass
                      }
                    >
                      Guests
                    </label>

                    <button
                      type="button"
                      onClick={() => {
                        setShowGuests(
                          !showGuests
                        );

                        setShowRooms(false);
                        setShowDestDropdown(
                          false
                        );
                      }}
                      className={`
                        ${fieldClass}
                        text-left
                        flex
                        items-center
                        justify-between
                        ${
                          showGuests
                            ? 'border-blue-500 ring-2 ring-blue-500/10'
                            : ''
                        }
                      `}
                    >

                      <div className="
                        flex
                        items-center
                        gap-2
                        min-w-0
                      ">

                        <svg
                          className="w-4 h-4 text-gray-400 shrink-0"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                          strokeWidth="1.5"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"
                          />
                        </svg>

                        <span className="font-medium truncate">
                          {totalGuests}{' '}
                          Guest
                          {totalGuests !== 1
                            ? 's'
                            : ''}
                        </span>

                      </div>

                      <svg
                        className={`
                          w-4
                          h-4
                          text-gray-400
                          shrink-0
                          transition-transform
                          ${
                            showGuests
                              ? 'rotate-180'
                              : ''
                          }
                        `}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M19.5 8.25l-7.5 7.5-7.5-7.5"
                        />
                      </svg>

                    </button>

                    <Popover
                      open={showGuests}
                      onClose={() =>
                        setShowGuests(
                          false
                        )
                      }
                      triggerRef={guestRef}
                      align="left"
                    >
                      <GuestPopoverContent
                        adults={adults}
                        setAdults={
                          setAdults
                        }
                        childrenCount={
                          childrenCount
                        }
                        setChildrenCount={
                          setChildrenCount
                        }
                        childAges={
                          childAges
                        }
                        setChildAges={
                          setChildAges
                        }
                        onClose={() =>
                          setShowGuests(
                            false
                          )
                        }
                      />
                    </Popover>

                  </div>

                  {/* ROOMS */}

                  <div
                    className="
                      min-w-0
                      relative
                      min-[480px]:col-span-2
                      lg:col-span-1
                    "
                    ref={roomRef}
                  >

                    <label
                      className={
                        labelClass
                      }
                    >
                      Rooms
                    </label>

                    <button
                      type="button"
                      onClick={() => {
                        setShowRooms(
                          !showRooms
                        );

                        setShowGuests(false);
                        setShowDestDropdown(
                          false
                        );
                      }}
                      className={`
                        ${fieldClass}
                        text-left
                        flex
                        items-center
                        justify-between
                        ${
                          showRooms
                            ? 'border-blue-500 ring-2 ring-blue-500/10'
                            : ''
                        }
                      `}
                    >

                      <div className="
                        flex
                        items-center
                        gap-2
                        min-w-0
                      ">

                        <svg
                          className="w-4 h-4 text-gray-400 shrink-0"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                          strokeWidth="1.5"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M2.25 12l8.954-8.955a1.126 1.126 0 011.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25"
                          />
                        </svg>

                        <span className="font-medium truncate">
                          {totalRooms}{' '}
                          Room
                          {totalRooms !== 1
                            ? 's'
                            : ''}
                        </span>

                      </div>

                      <svg
                        className={`
                          w-4
                          h-4
                          text-gray-400
                          shrink-0
                          transition-transform
                          ${
                            showRooms
                              ? 'rotate-180'
                              : ''
                          }
                        `}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M19.5 8.25l-7.5 7.5-7.5-7.5"
                        />
                      </svg>

                    </button>

                    <Popover
                      open={showRooms}
                      onClose={() =>
                        setShowRooms(
                          false
                        )
                      }
                      triggerRef={roomRef}
                      align="right"
                    >
                      <RoomPopoverContent
                        rooms={rooms}
                        setRooms={
                          setRooms
                        }
                        onClose={() =>
                          setShowRooms(
                            false
                          )
                        }
                      />
                    </Popover>

                  </div>

                </div>

                {/* SEARCH BUTTON */}

                <button
                  type="submit"
                  className="
                    mt-3
                    sm:mt-4
                    w-full
                    h-14
                    bg-gradient-to-r
                    from-orange-500
                    to-red-500
                    hover:from-orange-600
                    hover:to-red-600
                    text-white
                    rounded-xl
                    font-bold
                    text-sm
                    sm:text-base
                    transition-all
                    shadow-lg
                    shadow-orange-500/20
                    active:scale-[0.98]
                  "
                >
                  Search Hotels
                </button>

                {/* SUMMARY */}

                {nights > 0 && (
                  <div className="
                    mt-2.5
                    text-xs
                    text-gray-500
                    text-center
                    leading-relaxed
                  ">
                    {nights}{' '}
                    night
                    {nights > 1
                      ? 's'
                      : ''}{' '}
                    · {totalGuests}{' '}
                    guest
                    {totalGuests > 1
                      ? 's'
                      : ''}{' '}
                    · {totalRooms}{' '}
                    room
                    {totalRooms > 1
                      ? 's'
                      : ''}
                  </div>
                )}

              </form>

            </div>

          </div>

        </div>
      </section>

      {/* =====================================================
          TRUST FEATURES
      ===================================================== */}

      <section className="bg-white border-b border-gray-100">

        <div className="
          w-full
          max-w-6xl
          mx-auto
          px-4
          sm:px-6
          lg:px-8
          py-4
        ">

          <div className="
            grid
            grid-cols-1
            min-[420px]:grid-cols-2
            sm:flex
            sm:flex-wrap
            sm:items-center
            sm:justify-center
            gap-x-4
            gap-y-3
            sm:gap-6
            lg:gap-8
            text-xs
            sm:text-sm
          ">

            {[
              'Best Price Guarantee',
              'Free Cancellation',
              'Verified Properties',
              'Secure Payments',
              '24/7 Support',
            ].map((label) => (
              <span
                key={label}
                className="
                  flex
                  items-center
                  gap-1.5
                  text-gray-600
                  min-w-0
                "
              >

                <span className="
                  w-4
                  h-4
                  rounded-full
                  bg-emerald-100
                  flex
                  items-center
                  justify-center
                  shrink-0
                ">
                  <svg
                    className="w-2.5 h-2.5 text-emerald-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth="3"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                </span>

                <span className="font-medium truncate">
                  {label}
                </span>

              </span>
            ))}

          </div>

        </div>

      </section>

      {/* =====================================================
          HOTEL OFFERS
      ===================================================== */}

      <section className="
        w-full
        max-w-6xl
        mx-auto
        px-4
        sm:px-6
        lg:px-8
        py-8
        sm:py-10
        md:py-12
      ">

        <h2 className="
          text-xl
          sm:text-2xl
          font-bold
          text-gray-900
          mb-5
        ">
          Hotel Offers
        </h2>

        <div className="
          grid
          grid-cols-1
          sm:grid-cols-2
          lg:grid-cols-3
          gap-4
        ">

          {DEALS.map((deal) => (
            <div
              key={deal.title}
              className={`
                bg-gradient-to-r
                ${deal.color}
                rounded-xl
                p-4
                sm:p-5
                text-white
                shadow-md
                hover:shadow-lg
                transition-all
                cursor-pointer
                group
                relative
                overflow-hidden
                min-w-0
              `}
            >

              <div className="
                absolute
                top-0
                right-0
                w-24
                h-24
                bg-white/5
                rounded-full
                -translate-y-1/2
                translate-x-1/2
                group-hover:scale-110
                transition-transform
              " />

              <span className="
                inline-block
                bg-white/20
                px-2
                py-0.5
                rounded
                text-[10px]
                font-bold
                tracking-wider
                mb-2.5
              ">
                {deal.tag}
              </span>

              <h3 className="
                text-base
                font-bold
                mb-1
                relative
              ">
                {deal.title}
              </h3>

              <p className="
                text-white/75
                text-xs
                mb-3
                relative
                leading-relaxed
              ">
                {deal.desc}
              </p>

              <span className="
                inline-block
                bg-white/20
                px-2.5
                py-1
                rounded-md
                text-xs
                font-mono
                font-bold
                relative
              ">
                USE: {deal.code}
              </span>

            </div>
          ))}

        </div>

      </section>

      {/* =====================================================
          POPULAR DESTINATIONS
      ===================================================== */}

      <section className="
        w-full
        max-w-6xl
        mx-auto
        px-4
        sm:px-6
        lg:px-8
        py-7
        sm:py-8
      ">

        <h2 className="
          text-xl
          sm:text-2xl
          font-bold
          text-gray-900
          mb-1
        ">
          Popular Destinations
        </h2>

        <p className="
          text-sm
          text-gray-500
          mb-5
        ">
          Where travelers love to stay
        </p>

        <div className="
          grid
          grid-cols-2
          min-[520px]:grid-cols-3
          lg:grid-cols-5
          gap-3
          sm:gap-4
        ">

          {DESTINATIONS.slice(
            0,
            10
          ).map((d) => (
            <button
              key={d.name}
              type="button"
              onClick={() =>
                navigate(
                  `/hotels/search?city=${encodeURIComponent(
                    d.name
                  )}`
                )
              }
              className="
                bg-white
                rounded-xl
                overflow-hidden
                shadow-sm
                border
                border-gray-100
                hover:shadow-md
                hover:border-blue-200
                transition-all
                text-left
                group
                min-w-0
              "
            >

              <div className="
                h-20
                sm:h-24
                bg-gradient-to-br
                from-blue-50
                to-indigo-50
                flex
                items-center
                justify-center
              ">
                <span className="
                  text-3xl
                  group-hover:scale-110
                  transition-transform
                ">
                  {d.emoji}
                </span>
              </div>

              <div className="
                p-3
                min-w-0
              ">

                <h3 className="
                  font-semibold
                  text-gray-900
                  text-sm
                  group-hover:text-blue-600
                  transition-colors
                  truncate
                ">
                  {d.name}
                </h3>

                <div className="
                  flex
                  items-center
                  justify-between
                  gap-2
                  mt-1
                ">

                  <span className="
                    text-[11px]
                    text-gray-400
                  ">
                    {d.hotels}
                  </span>

                  <span className="
                    text-[11px]
                    font-bold
                    text-blue-600
                    whitespace-nowrap
                  ">
                    From {d.from}
                  </span>

                </div>

              </div>

            </button>
          ))}

        </div>

      </section>

      {/* =====================================================
          CATEGORIES
      ===================================================== */}

      <section className="
        w-full
        max-w-6xl
        mx-auto
        px-4
        sm:px-6
        lg:px-8
        py-7
        sm:py-8
      ">

        <h2 className="
          text-xl
          sm:text-2xl
          font-bold
          text-gray-900
          mb-5
        ">
          Browse by Category
        </h2>

        <div className="
          grid
          grid-cols-2
          min-[420px]:grid-cols-3
          sm:grid-cols-6
          gap-3
        ">

          {CATEGORIES.map((cat) => (
            <button
              key={cat.label}
              type="button"
              onClick={() =>
                navigate('/hotels')
              }
              className="
                bg-white
                rounded-xl
                p-3
                sm:p-4
                text-center
                shadow-sm
                border
                border-gray-100
                hover:shadow-md
                hover:border-blue-200
                transition-all
                group
                min-w-0
              "
            >

              <div className="
                text-2xl
                mb-2
                group-hover:scale-110
                transition-transform
              ">
                {cat.icon}
              </div>

              <div className="
                font-semibold
                text-gray-900
                text-xs
                truncate
              ">
                {cat.label}
              </div>

              <div className="
                text-[10px]
                text-gray-400
                mt-0.5
                hidden
                sm:block
                leading-relaxed
              ">
                {cat.desc}
              </div>

            </button>
          ))}

        </div>

      </section>

      {/* =====================================================
          WHY BOOK
      ===================================================== */}

      <section className="
        w-full
        max-w-6xl
        mx-auto
        px-4
        sm:px-6
        lg:px-8
        py-8
        sm:py-10
        md:py-12
      ">

        <h2 className="
          text-xl
          sm:text-2xl
          font-bold
          text-gray-900
          mb-1
          text-center
        ">
          Why Book Hotels on TravelPlatform?
        </h2>

        <p className="
          text-sm
          text-gray-500
          mb-7
          text-center
          px-2
        ">
          Verified properties, best prices, and real guest reviews
        </p>

        <div className="
          grid
          grid-cols-1
          sm:grid-cols-2
          lg:grid-cols-3
          gap-3
          sm:gap-4
        ">

          {FEATURES.map((f) => (
            <div
              key={f.title}
              className={`
                rounded-xl
                p-4
                border
                ${f.color}
                hover:shadow-sm
                transition-all
                min-w-0
              `}
            >

              <div className="
                flex
                items-start
                gap-3
              ">

                <div className="
                  w-8
                  h-8
                  rounded-lg
                  bg-white/80
                  flex
                  items-center
                  justify-center
                  shrink-0
                  mt-0.5
                ">

                  <svg
                    className="w-4 h-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth="2.5"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M5 13l4 4L19 7"
                    />
                  </svg>

                </div>

                <div className="min-w-0">

                  <h3 className="
                    font-bold
                    text-sm
                  ">
                    {f.title}
                  </h3>

                  <p className="
                    text-xs
                    opacity-75
                    mt-1
                    leading-relaxed
                  ">
                    {f.desc}
                  </p>

                </div>

              </div>

            </div>
          ))}

        </div>

      </section>

      {/* =====================================================
          FINAL CTA
      ===================================================== */}

      <section className="
        bg-gradient-to-r
        from-[#0a1628]
        to-[#1a3a5c]
        py-12
        sm:py-16
      ">

        <div className="
          w-full
          max-w-3xl
          mx-auto
          text-center
          px-4
          sm:px-6
        ">

          <h2 className="
            text-2xl
            sm:text-3xl
            font-bold
            text-white
            mb-2
          ">
            Find your next stay
          </h2>

          <p className="
            text-blue-200/60
            mb-6
            text-sm
            sm:text-base
          ">
            Thousands of verified hotels at the best prices
          </p>

          <button
            type="button"
            onClick={() =>
              window.scrollTo({
                top: 0,
                behavior: 'smooth',
              })
            }
            className="
              bg-white
              text-blue-600
              px-7
              sm:px-8
              py-3
              rounded-xl
              font-bold
              text-sm
              sm:text-base
              hover:shadow-lg
              transition-all
            "
          >
            Search Hotels
          </button>

        </div>

      </section>

    </div>
  );
}