import { useState } from 'react';

const ROOM_IMAGES = {
  'STANDARD': '🛏️',
  'DELUXE': '🛏️',
  'SUITE': '🏨',
  'EXECUTIVE': '💼',
  'PREMIUM': '👑',
  'FAMILY': '👨‍👩‍👧‍👦',
};

const AMENITY_ICONS = {
  'WIFI': '📶',
  'AC': '❄️',
  'BREAKFAST': '🍳',
  'POOL': '🏊',
  'SPA': '🧖',
  'GYM': '💪',
  'PARKING': '🅿️',
  'LAUNDRY': '👔',
  'ROOM_SERVICE': '🛎️',
  'BALCONY': '🌅',
  'SEA_VIEW': '🌊',
  'GARDEN_VIEW': '🌿',
};

export default function RoomSelector({ rooms, selectedRoomId, onSelectRoom, nights = 1 }) {
  const [hoveredRoom, setHoveredRoom] = useState(null);

  if (!rooms || rooms.length === 0) return null;

  const getRoomIcon = (roomType) => {
    const type = (roomType || '').toUpperCase();
    for (const [key, icon] of Object.entries(ROOM_IMAGES)) {
      if (type.includes(key)) return icon;
    }
    return '🛏️';
  };

  const parseAmenities = (amenities) => {
    if (!amenities) return [];
    if (Array.isArray(amenities)) return amenities;
    if (typeof amenities === 'string') return amenities.split(',').map(a => a.trim());
    return [];
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="font-bold text-gray-900">Select Your Room</h3>
        {selectedRoomId && (
          <span className="text-xs bg-blue-100 text-blue-700 px-2.5 py-1 rounded-full font-medium">
            {nights} night{nights !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {rooms.map(room => {
          const isSelected = room.id === Number(selectedRoomId);
          const isUnavailable = room.availableRooms <= 0;
          const isHovered = hoveredRoom === room.id;
          const amenities = parseAmenities(room.amenities);
          const totalPrice = Number(room.pricePerNight) * nights;

          return (
            <div
              key={room.id}
              onMouseEnter={() => setHoveredRoom(room.id)}
              onMouseLeave={() => setHoveredRoom(null)}
              onClick={() => !isUnavailable && onSelectRoom(room)}
              className={`
                relative rounded-xl border-2 p-4 transition-all duration-200
                ${isUnavailable
                  ? 'border-gray-100 bg-gray-50 opacity-60 cursor-not-allowed'
                  : isSelected
                    ? 'border-blue-500 bg-blue-50 shadow-md ring-2 ring-blue-200 cursor-pointer'
                    : isHovered
                      ? 'border-blue-300 bg-white shadow-sm cursor-pointer'
                      : 'border-gray-200 bg-white hover:border-gray-300 cursor-pointer'
                }
              `}
            >
              {/* Selected indicator */}
              {isSelected && (
                <div className="absolute -top-2 -right-2 w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center">
                  <span className="text-white text-xs font-bold">✓</span>
                </div>
              )}

              {/* Unavailable badge */}
              {isUnavailable && (
                <div className="absolute top-3 right-3 bg-red-100 text-red-600 text-[10px] font-bold px-2 py-0.5 rounded-full">
                  SOLD OUT
                </div>
              )}

              {/* Room header */}
              <div className="flex items-start gap-3 mb-3">
                <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center text-lg flex-shrink-0">
                  {getRoomIcon(room.roomType)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className={`font-semibold text-sm ${isSelected ? 'text-blue-800' : 'text-gray-900'} truncate`}>
                    {room.name || room.roomType}
                  </p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {room.bedType || 'King Bed'} · {room.maxGuests || 2} Guest{(room.maxGuests || 2) !== 1 ? 's' : ''}
                  </p>
                </div>
              </div>

              {/* Room description */}
              {room.description && (
                <p className="text-xs text-gray-500 mb-3 line-clamp-2">{room.description}</p>
              )}

              {/* Amenities */}
              {amenities.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mb-3">
                  {amenities.slice(0, 5).map((amenity, i) => (
                    <span key={i} className="text-[10px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full flex items-center gap-0.5">
                      <span>{AMENITY_ICONS[amenity.toUpperCase()] || '✓'}</span>
                      <span>{amenity}</span>
                    </span>
                  ))}
                  {amenities.length > 5 && (
                    <span className="text-[10px] text-gray-400">+{amenities.length - 5} more</span>
                  )}
                </div>
              )}

              {/* Availability */}
              <div className="flex items-center justify-between text-xs mb-2">
                <span className={`font-medium ${room.availableRooms <= 3 ? 'text-amber-600' : 'text-green-600'}`}>
                  {isUnavailable
                    ? 'No rooms available'
                    : room.availableRooms <= 3
                      ? `Only ${room.availableRooms} left!`
                      : `${room.availableRooms} rooms available`
                  }
                </span>
              </div>

              {/* Price */}
              <div className="flex items-end justify-between pt-2 border-t border-gray-100">
                <div>
                  <p className="text-xs text-gray-400">
                    ₹{Number(room.pricePerNight).toLocaleString()} / night
                  </p>
                  {nights > 1 && (
                    <p className="text-xs text-gray-500 font-medium">
                      ₹{totalPrice.toLocaleString()} total
                    </p>
                  )}
                </div>
                <div className="text-right">
                  {!isUnavailable && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectRoom(room);
                      }}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                        isSelected
                          ? 'bg-blue-600 text-white'
                          : 'bg-blue-50 text-blue-600 hover:bg-blue-100'
                      }`}
                    >
                      {isSelected ? 'Selected' : 'Select'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
