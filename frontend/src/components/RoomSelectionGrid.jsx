import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import {
  getHotelRoomsSelection,
  getRoomUpgrades,
  holdRoom,
  releaseRoomHold,
} from '../api/selectionApi';
import TravelPreferencesModal from './TravelPreferencesModal';

export default function RoomSelectionGrid({
  hotelId,
  checkIn,
  checkOut,
  nights = 1,
  selectedRoomId,
  onRoomSelect,
  onHoldChanged,
}) {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rooms, setRooms] = useState([]);
  const [upgrades, setUpgrades] = useState([]);
  const [activeHold, setActiveHold] = useState(null); // { roomId, expiresAt }
  const [holdSecondsLeft, setHoldSecondsLeft] = useState(0);
  const [showPreferencesModal, setShowPreferencesModal] = useState(false);
  const [galleryModalRoom, setGalleryModalRoom] = useState(null);
  const [activeImageIndex, setActiveImageIndex] = useState(0);

  // Fetch rooms selection & upgrades data
  const fetchData = useCallback(async () => {
    if (!hotelId) return;
    setLoading(true);
    setError('');
    try {
      const [selectionRes, upgradeRes] = await Promise.all([
        getHotelRoomsSelection(hotelId),
        getRoomUpgrades(hotelId).catch(() => []),
      ]);

      setRooms(selectionRes.rooms || []);
      setUpgrades(upgradeRes || []);
    } catch (err) {
      console.error('Error fetching room selection:', err);
      setError(err.response?.data?.message || 'Failed to load rooms');
    } finally {
      setLoading(false);
    }
  }, [hotelId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Hold Countdown Timer
  useEffect(() => {
    if (!activeHold?.expiresAt) {
      setHoldSecondsLeft(0);
      return;
    }

    const updateTimer = () => {
      const remaining = Math.max(0, Math.ceil((new Date(activeHold.expiresAt).getTime() - Date.now()) / 1000));
      setHoldSecondsLeft(remaining);
      if (remaining <= 0) {
        setActiveHold(null);
        if (onHoldChanged) onHoldChanged(null);
        fetchData(); // Refresh availability
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [activeHold, fetchData, onHoldChanged]);

  // Handle Hold / Select Room
  const handleSelectRoom = async (room) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/hotels/${hotelId}` } } });
      return;
    }

    if (room.availableRooms <= 0) return;

    // If already holding this room, release it
    if (activeHold?.roomId === room.id) {
      try {
        await releaseRoomHold(room.id);
        setActiveHold(null);
        if (onRoomSelect) onRoomSelect(null);
        if (onHoldChanged) onHoldChanged(null);
        fetchData();
      } catch (err) {
        console.warn('Failed to release room hold:', err);
      }
      return;
    }

    // Try to acquire room hold
    try {
      const hold = await holdRoom(room.id);
      setActiveHold({
        roomId: room.id,
        holdId: hold.holdId,
        expiresAt: hold.expiresAt,
      });
      if (onRoomSelect) onRoomSelect(room);
      if (onHoldChanged) onHoldChanged(hold);
      setError('');
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to hold room. It may have just sold out.');
    }
  };

  // Open Room Gallery Lightbox
  const openGallery = (room, initialIndex = 0) => {
    setGalleryModalRoom(room);
    setActiveImageIndex(initialIndex);
  };

  if (loading) {
    return (
      <div className="space-y-4 animate-pulse">
        <div className="h-8 bg-gray-200 rounded-lg w-1/4" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-64 bg-gray-100 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Controls & Preferences Banner */}
      <div className="bg-gradient-to-r from-blue-50/70 via-indigo-50/50 to-white p-4 rounded-2xl border border-blue-100/80 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-600 text-white flex items-center justify-center text-lg shadow-sm">
            🛏️
          </div>
          <div>
            <h3 className="font-bold text-gray-900 text-sm">Interactive Room Selection & Previews</h3>
            <p className="text-xs text-gray-500">
              Browse photo galleries, compare upgrades, and lock your room rate.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setShowPreferencesModal(true)}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white hover:bg-gray-50 text-blue-700 text-xs font-semibold rounded-xl border border-blue-200 shadow-2xs transition"
        >
          <span>⚙️</span> Travel Preferences
        </button>
      </div>

      {/* Active Hold Alert Banner */}
      {activeHold && holdSecondsLeft > 0 && (
        <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4 flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-amber-500 text-white flex items-center justify-center font-bold text-sm shadow-sm animate-pulse">
              ⏱
            </div>
            <div>
              <p className="text-xs font-bold text-amber-900">
                Room Temporarily Reserved Just For You!
              </p>
              <p className="text-xs text-amber-700">
                Complete your booking before your guaranteed hold expires.
              </p>
            </div>
          </div>
          <div className="text-right">
            <span className="font-mono text-base font-bold text-amber-800 bg-amber-100/80 px-3 py-1 rounded-lg border border-amber-200">
              {Math.floor(holdSecondsLeft / 60)}:{(holdSecondsLeft % 60).toString().padStart(2, '0')}
            </span>
          </div>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium">
          {error}
        </div>
      )}

      {/* Upgrade Opportunities / Upsell Callout */}
      {upgrades.length > 0 && (
        <div className="bg-white rounded-2xl p-5 border border-purple-100 shadow-2xs">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <span className="text-base">✨</span>
              <h4 className="font-bold text-sm text-gray-900">Featured Room Upgrades</h4>
            </div>
            <span className="text-[11px] font-semibold text-purple-700 bg-purple-50 px-2.5 py-0.5 rounded-full border border-purple-200">
              Exclusive Value
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {upgrades.slice(0, 3).map((up) => (
              <div
                key={up.upgradeRoomId}
                className="bg-purple-50/30 hover:bg-purple-50/60 p-3.5 rounded-xl border border-purple-100 transition flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-xs text-gray-900">{up.upgradeRoomName}</span>
                    <span className="text-xs font-bold text-purple-700">
                      +₹{up.priceDifferencePerNight?.toLocaleString()}/nt
                    </span>
                  </div>
                  <div className="text-[11px] text-gray-500 mt-1">
                    +{up.sizeDifferenceSqm} sqm extra space · {up.upgradeRoomType}
                  </div>
                  {up.exclusiveAmenities?.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-2">
                      {up.exclusiveAmenities.slice(0, 2).map((a, i) => (
                        <span
                          key={i}
                          className="text-[10px] bg-white text-purple-800 px-1.5 py-0.5 rounded border border-purple-100"
                        >
                          + {a.replace(/_/g, ' ')}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <button
                  type="button"
                  onClick={() => {
                    const targetRoom = rooms.find(r => r.id === up.upgradeRoomId);
                    if (targetRoom) handleSelectRoom(targetRoom);
                  }}
                  className="mt-3 w-full py-1.5 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-xs font-bold transition shadow-xs"
                >
                  Choose {up.upgradeRoomType}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Main Room Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {rooms.map((room) => {
          const isSelected = selectedRoomId === room.id || activeHold?.roomId === room.id;
          const images = room.images && room.images.length > 0 ? room.images : [
            'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80',
            'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
          ];

          return (
            <div
              key={room.id}
              className={`bg-white rounded-2xl overflow-hidden border-2 transition-all flex flex-col justify-between ${
                isSelected
                  ? 'border-blue-600 shadow-md ring-2 ring-blue-500/20'
                  : 'border-gray-100 hover:border-gray-200 shadow-xs'
              }`}
            >
              <div>
                {/* Photo Gallery & Previews */}
                <div className="relative h-48 bg-gray-100 group">
                  <img
                    src={images[0]}
                    alt={room.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent pointer-events-none" />

                  {/* Preference match badge */}
                  {room.matchesUserPreference && (
                    <div className="absolute top-3 left-3 bg-amber-400 text-gray-900 text-[11px] font-bold px-2.5 py-1 rounded-full shadow-md flex items-center gap-1 animate-bounce">
                      <span>⭐</span> Best Match For You
                    </div>
                  )}

                  {/* Room Type Tag */}
                  <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-xs text-white text-[11px] font-semibold px-2.5 py-0.5 rounded-full">
                    {room.roomType}
                  </div>

                  {/* Multi-Photo trigger */}
                  <button
                    type="button"
                    onClick={() => openGallery(room, 0)}
                    className="absolute bottom-3 right-3 bg-white/90 hover:bg-white text-gray-900 text-xs font-semibold px-2.5 py-1 rounded-lg shadow-sm flex items-center gap-1.5 transition"
                  >
                    <span>📷</span> View {images.length} Photos
                  </button>

                  {/* Urgency Badge */}
                  {room.availableRooms <= 3 && room.availableRooms > 0 && (
                    <div className="absolute bottom-3 left-3 bg-red-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-md shadow-xs">
                      Only {room.availableRooms} rooms left!
                    </div>
                  )}
                </div>

                {/* Content */}
                <div className="p-4 space-y-3">
                  <div>
                    <h4 className="font-bold text-gray-900 text-base">{room.name}</h4>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {room.bedType} Bed · {room.sizeSqm} m² · Up to {room.maxGuests} guests
                    </p>
                  </div>

                  {/* Amenities Pills */}
                  <div className="flex flex-wrap gap-1.5">
                    {(room.amenities || []).slice(0, 5).map((amenity, i) => (
                      <span
                        key={i}
                        className="text-[10px] font-medium bg-gray-50 text-gray-600 px-2 py-0.5 rounded-md border border-gray-100"
                      >
                        ✓ {amenity.replace(/_/g, ' ')}
                      </span>
                    ))}
                    {(room.amenities || []).length > 5 && (
                      <span className="text-[10px] text-gray-400 font-medium px-1 py-0.5">
                        +{(room.amenities.length - 5)} more
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Price and Select Button */}
              <div className="p-4 pt-3 border-t border-gray-100 bg-gray-50/50 flex items-center justify-between">
                <div>
                  <div className="text-lg font-extrabold text-gray-900">
                    ₹{room.pricePerNight?.toLocaleString()}
                    <span className="text-xs font-normal text-gray-400">/night</span>
                  </div>
                  {nights > 1 && (
                    <div className="text-[11px] text-gray-500">
                      ₹{(room.pricePerNight * nights).toLocaleString()} total for {nights} nights
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleSelectRoom(room)}
                    disabled={room.availableRooms <= 0}
                    className={`px-5 py-2.5 rounded-xl text-xs font-bold transition-all shadow-xs ${
                      isSelected
                        ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                        : room.availableRooms <= 0
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        : 'bg-blue-600 hover:bg-blue-700 text-white'
                    }`}
                  >
                    {isSelected
                      ? '✓ Held & Selected'
                      : room.availableRooms <= 0
                      ? 'Sold Out'
                      : 'Select Room'}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Lightbox / Room Photo Preview Modal */}
      {galleryModalRoom && (
        <div className="fixed inset-0 bg-black/90 z-50 flex flex-col items-center justify-center p-4 backdrop-blur-sm animate-in fade-in">
          {/* Modal Header */}
          <div className="w-full max-w-4xl flex items-center justify-between text-white mb-3 px-2">
            <div>
              <h3 className="font-bold text-base">{galleryModalRoom.name} — Visual Tour</h3>
              <p className="text-xs text-gray-400">
                Photo {activeImageIndex + 1} of {galleryModalRoom.images?.length || 1}
              </p>
            </div>
            <button
              onClick={() => setGalleryModalRoom(null)}
              className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white text-lg transition"
            >
              ✕
            </button>
          </div>

          {/* Active Image Container */}
          <div className="w-full max-w-4xl h-[65vh] relative bg-black/40 rounded-2xl overflow-hidden flex items-center justify-center">
            <img
              src={galleryModalRoom.images?.[activeImageIndex] || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80'}
              alt={galleryModalRoom.name}
              className="w-full h-full object-contain"
            />

            {/* Prev / Next buttons */}
            {galleryModalRoom.images?.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={() =>
                    setActiveImageIndex((prev) =>
                      prev === 0 ? galleryModalRoom.images.length - 1 : prev - 1
                    )
                  }
                  className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center text-xl transition"
                >
                  ‹
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setActiveImageIndex((prev) =>
                      prev === galleryModalRoom.images.length - 1 ? 0 : prev + 1
                    )
                  }
                  className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center text-xl transition"
                >
                  ›
                </button>
              </>
            )}
          </div>

          {/* Thumbnails */}
          {galleryModalRoom.images?.length > 1 && (
            <div className="w-full max-w-4xl flex items-center gap-2 mt-4 overflow-x-auto py-2 px-2">
              {galleryModalRoom.images.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setActiveImageIndex(idx)}
                  className={`w-20 h-14 rounded-lg overflow-hidden shrink-0 border-2 transition ${
                    activeImageIndex === idx
                      ? 'border-blue-500 scale-105 ring-2 ring-blue-500/40'
                      : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img} alt="thumb" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Travel Preferences Modal */}
      <TravelPreferencesModal
        isOpen={showPreferencesModal}
        onClose={() => setShowPreferencesModal(false)}
        onPreferencesUpdated={() => fetchData()}
      />
    </div>
  );
}
