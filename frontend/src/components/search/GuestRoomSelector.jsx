import { useState, useRef, useEffect } from 'react';

const AGE_OPTIONS = Array.from({ length: 10 }, (_, i) => ({ value: i + 2, label: `${i + 2} years` }));

function Counter({ value, min, max, onChange, label }) {
  return (
    <div className="flex items-center gap-3">
      <button type="button" onClick={() => onChange(Math.max(min, value - 1))} disabled={value <= min}
        className="w-9 h-9 rounded-full border border-gray-300 flex items-center justify-center text-gray-600 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-30 disabled:cursor-not-allowed transition-all text-base font-medium"
        aria-label={`Decrease ${label}`}>
        −
      </button>
      <span className="w-8 text-center font-semibold text-gray-900 text-base">{value}</span>
      <button type="button" onClick={() => onChange(Math.min(max, value + 1))} disabled={value >= max}
        className="w-9 h-9 rounded-full border border-gray-300 flex items-center justify-center text-gray-600 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-30 disabled:cursor-not-allowed transition-all text-base font-medium"
        aria-label={`Increase ${label}`}>
        +
      </button>
    </div>
  );
}

function RoomPanel({ room, index, totalRooms, onUpdate, onRemove, canRemove }) {
  return (
    <div className={`py-4 ${index > 0 ? 'border-t border-gray-100' : ''}`}>
      <div className="flex items-center justify-between mb-3">
        <h4 className="font-semibold text-gray-900 text-sm">Room {index + 1}</h4>
        {canRemove && (
          <button type="button" onClick={onRemove}
            className="text-red-500 hover:text-red-600 text-xs font-medium transition-colors">
            Remove
          </button>
        )}
      </div>
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <div className="font-medium text-gray-800 text-sm">Adults</div>
            <div className="text-[10px] text-gray-400">12+ years</div>
          </div>
          <Counter value={room.adults} min={1} max={4} onChange={v => onUpdate({ ...room, adults: v })} label="adults" />
        </div>
        <div className="flex items-center justify-between">
          <div>
            <div className="font-medium text-gray-800 text-sm">Children</div>
            <div className="text-[10px] text-gray-400">2–11 years</div>
          </div>
          <Counter value={room.children} min={0} max={4} onChange={v => {
            const newAges = [...room.childAges];
            if (v > room.children) newAges.push(5); // default age for new child
            else newAges.pop();
            onUpdate({ ...room, children: v, childAges: newAges });
          }} label="children" />
        </div>
        {room.children > 0 && (
          <div className="pl-2 space-y-2">
            <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Child Ages</div>
            {Array.from({ length: room.children }, (_, i) => (
              <div key={i} className="flex items-center justify-between">
                <span className="text-xs text-gray-600">Child {i + 1}</span>
                <select value={room.childAges[i] || 5}
                  onChange={e => {
                    const newAges = [...room.childAges];
                    newAges[i] = Number(e.target.value);
                    onUpdate({ ...room, childAges: newAges });
                  }}
                  className="px-3 py-1.5 border border-gray-200 rounded-lg text-xs text-gray-700 bg-white focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none">
                  {AGE_OPTIONS.map(a => <option key={a.value} value={a.value}>{a.label}</option>)}
                </select>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default function GuestRoomSelector({ rooms, setRooms, triggerRef, onClose }) {
  const [localRooms, setLocalRooms] = useState(() => {
    if (rooms && rooms.length > 0) return rooms;
    return [{ adults: 2, children: 0, childAges: [] }];
  });
  const panelRef = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (panelRef.current && !panelRef.current.contains(e.target) && triggerRef?.current && !triggerRef.current.contains(e.target)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [onClose, triggerRef]);

  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  const updateRoom = (index, updated) => {
    const next = [...localRooms];
    next[index] = updated;
    setLocalRooms(next);
  };

  const removeRoom = (index) => {
    setLocalRooms(localRooms.filter((_, i) => i !== index));
  };

  const addRoom = () => {
    if (localRooms.length < 5) {
      setLocalRooms([...localRooms, { adults: 2, children: 0, childAges: [] }]);
    }
  };

  const totalGuests = localRooms.reduce((sum, r) => sum + r.adults + r.children, 0);
  const totalAdults = localRooms.reduce((sum, r) => sum + r.adults, 0);
  const totalChildren = localRooms.reduce((sum, r) => sum + r.children, 0);

  const handleDone = () => {
    setRooms(localRooms);
    onClose();
  };

  return (
    <div ref={panelRef} className="absolute top-full left-0 right-0 md:left-auto md:right-auto md:w-[420px] mt-2 bg-white rounded-2xl shadow-2xl border border-gray-200 z-50 overflow-hidden"
      role="dialog" aria-label="Guests and rooms selector">
      {/* Header */}
      <div className="px-5 pt-5 pb-3 border-b border-gray-100">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-gray-900">Guests & Rooms</h3>
          <button onClick={onClose} className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition" aria-label="Close">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>
      </div>

      {/* Rooms */}
      <div className="px-5 max-h-[50vh] overflow-y-auto">
        {localRooms.map((room, i) => (
          <RoomPanel key={i} room={room} index={i} totalRooms={localRooms.length}
            onUpdate={(updated) => updateRoom(i, updated)}
            onRemove={() => removeRoom(i)}
            canRemove={localRooms.length > 1} />
        ))}
        {localRooms.length < 5 && (
          <div className="py-3 border-t border-gray-100">
            <button type="button" onClick={addRoom}
              className="flex items-center gap-2 text-blue-600 hover:text-blue-700 text-sm font-semibold transition-colors">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
              Add another room
            </button>
          </div>
        )}
      </div>

      {/* Footer Summary + Done */}
      <div className="px-5 py-4 bg-gray-50 border-t border-gray-100">
        <div className="flex items-center justify-between mb-3">
          <div className="text-sm text-gray-600">
            <span className="font-semibold text-gray-900">{totalGuests} Guest{totalGuests !== 1 ? 's' : ''}</span>
            <span className="mx-1.5 text-gray-300">·</span>
            <span className="font-semibold text-gray-900">{localRooms.length} Room{localRooms.length !== 1 ? 's' : ''}</span>
          </div>
          {totalChildren > 0 && (
            <div className="text-[10px] text-gray-400">
              {totalAdults} adult{totalAdults !== 1 ? 's' : ''}, {totalChildren} child{totalChildren !== 1 ? 'ren' : ''}
            </div>
          )}
        </div>
        <button type="button" onClick={handleDone}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-xl text-sm font-bold transition-colors active:scale-[0.98]">
          Done
        </button>
      </div>
    </div>
  );
}
