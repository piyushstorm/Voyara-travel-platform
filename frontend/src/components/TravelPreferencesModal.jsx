import { useState, useEffect } from 'react';
import { getTravelPreferences, updateTravelPreferences } from '../api/selectionApi';

const SEAT_POSITIONS = [
  { value: 'WINDOW', label: 'Window Seat', icon: '🪟', desc: 'Scenic view outside the aircraft' },
  { value: 'AISLE', label: 'Aisle Seat', icon: '🚶', desc: 'Easy access to walk & stretch' },
  { value: 'MIDDLE', label: 'Middle Seat', icon: '👥', desc: 'Cozy seating between travel companions' },
];

const SEAT_TYPES = [
  { value: 'STANDARD', label: 'Standard Seat', desc: 'Standard comfortable pitch' },
  { value: 'EXTRA_LEGROOM', label: 'Extra Legroom', desc: 'Extended leg space for comfort' },
  { value: 'EXIT_ROW', label: 'Exit Row', desc: 'Spacious exit row seating' },
  { value: 'PREMIUM', label: 'Premium Front Row', desc: 'Priority exit and fast service' },
];

const ROOM_TYPES = [
  { value: 'STANDARD', label: 'Standard Room' },
  { value: 'DELUXE', label: 'Deluxe Room' },
  { value: 'SUITE', label: 'Executive Suite' },
  { value: 'PRESIDENTIAL', label: 'Presidential Suite' },
];

const BED_TYPES = [
  { value: 'KING', label: 'King Bed (1 large bed)' },
  { value: 'QUEEN', label: 'Queen Bed (1 queen bed)' },
  { value: 'TWIN', label: 'Twin Beds (2 separate beds)' },
];

const ROOM_FEATURES = [
  'BALCONY',
  'CITY_VIEW',
  'OCEAN_VIEW',
  'HIGH_FLOOR',
  'BATHTUB',
  'QUIET_ROOM',
  'WORK_DESK',
];

const DESTINATION_STYLES = [
  { value: 'BEACH', label: 'Beach & Island', icon: '🏖️' },
  { value: 'MOUNTAIN', label: 'Mountain & Nature', icon: '🏔️' },
  { value: 'HERITAGE', label: 'Culture & Heritage', icon: '🏛️' },
  { value: 'LUXURY', label: 'Luxury & Wellness', icon: '✨' },
  { value: 'CITY', label: 'Metropolis & Shopping', icon: '🏙️' },
  { value: 'ADVENTURE', label: 'Adventure & Sports', icon: '🧗' },
];

const CABIN_CLASSES = [
  { value: 'ECONOMY', label: 'Economy' },
  { value: 'PREMIUM_ECONOMY', label: 'Premium Economy' },
  { value: 'BUSINESS', label: 'Business Class' },
  { value: 'FIRST', label: 'First Class' },
];

const BUDGET_LEVELS = [
  { value: 'BUDGET', label: 'Budget-Friendly', desc: 'Under ₹3,500/day' },
  { value: 'MID_RANGE', label: 'Comfort & Value', desc: '₹3,500 - ₹10,000/day' },
  { value: 'LUXURY', label: 'Luxury & Premium', desc: '₹10,000+/day' },
];

export default function TravelPreferencesModal({ isOpen, onClose, onPreferencesUpdated }) {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [preferredSeatPosition, setPreferredSeatPosition] = useState('WINDOW');
  const [preferredSeatType, setPreferredSeatType] = useState('EXTRA_LEGROOM');
  const [preferredRoomType, setPreferredRoomType] = useState('DELUXE');
  const [preferredBedType, setPreferredBedType] = useState('KING');
  const [preferredRoomFeatures, setPreferredRoomFeatures] = useState(['CITY_VIEW', 'BALCONY']);
  const [preferredDestinations, setPreferredDestinations] = useState(['BEACH', 'RELAXATION']);
  const [preferredCabinClass, setPreferredCabinClass] = useState('ECONOMY');
  const [budgetLevel, setBudgetLevel] = useState('MID_RANGE');

  useEffect(() => {
    if (!isOpen) return;
    setLoading(true);
    setError('');
    setSuccess('');
    getTravelPreferences()
      .then(data => {
        if (data) {
          setPreferredSeatPosition(data.preferredSeatPosition || 'WINDOW');
          setPreferredSeatType(data.preferredSeatType || 'EXTRA_LEGROOM');
          setPreferredRoomType(data.preferredRoomType || 'DELUXE');
          setPreferredBedType(data.preferredBedType || 'KING');
          setPreferredRoomFeatures(data.preferredRoomFeatures || ['CITY_VIEW']);
          if (data.preferredDestinations) {
            setPreferredDestinations(data.preferredDestinations.split(',').map(s => s.trim().toUpperCase()));
          }
          setPreferredCabinClass(data.preferredCabinClass || 'ECONOMY');
          setBudgetLevel(data.budgetLevel || 'MID_RANGE');
        }
      })
      .catch(err => {
        console.warn('Failed to load travel preferences:', err);
      })
      .finally(() => setLoading(false));
  }, [isOpen]);

  const toggleFeature = (feat) => {
    setPreferredRoomFeatures(prev =>
      prev.includes(feat) ? prev.filter(f => f !== feat) : [...prev, feat]
    );
  };

  const toggleDestinationStyle = (style) => {
    setPreferredDestinations(prev =>
      prev.includes(style)
        ? (prev.length > 1 ? prev.filter(s => s !== style) : prev)
        : [...prev, style]
    );
  };

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const payload = {
        preferredSeatPosition,
        preferredSeatType,
        preferredRoomType,
        preferredBedType,
        preferredRoomFeatures,
        preferredDestinations: preferredDestinations.join(','),
        preferredCabinClass,
        budgetLevel,
      };
      const updated = await updateTravelPreferences(payload);
      setSuccess('Travel preferences saved!');
      if (onPreferencesUpdated) onPreferencesUpdated(updated);
      setTimeout(() => {
        onClose();
      }, 700);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save travel preferences');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-xs z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 border-b border-gray-100 flex items-center justify-between sticky top-0 bg-white z-10">
          <div>
            <h3 className="font-bold text-gray-900 text-lg flex items-center gap-2">
              <span>✈️🏨</span> Travel Personalization
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">
              Save your seating & hotel room choices for smart recommendations
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-400 hover:text-gray-600 transition"
          >
            ✕
          </button>
        </div>

        <div className="p-5 space-y-6">
          {error && (
            <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 bg-green-50 text-green-700 text-xs rounded-xl border border-green-200 flex items-center gap-2">
              <span>✓</span> {success}
            </div>
          )}

          {loading ? (
            <div className="py-12 text-center text-gray-400 text-sm animate-pulse">
              Loading preferences...
            </div>
          ) : (
            <>
              {/* Seat Preferences */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Flight Seat Preference
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {SEAT_POSITIONS.map(pos => {
                    const isSelected = preferredSeatPosition === pos.value;
                    return (
                      <button
                        key={pos.value}
                        type="button"
                        onClick={() => setPreferredSeatPosition(pos.value)}
                        className={`p-3 rounded-xl border text-center transition-all ${
                          isSelected
                            ? 'border-blue-600 bg-blue-50/50 ring-2 ring-blue-500/20'
                            : 'border-gray-200 hover:border-gray-300'
                        }`}
                      >
                        <div className="text-2xl mb-1">{pos.icon}</div>
                        <div className="text-xs font-bold text-gray-900">{pos.label}</div>
                        <div className="text-[10px] text-gray-500 mt-0.5 leading-tight">{pos.desc}</div>
                      </button>
                    );
                  })}
                </div>

                <div className="mt-3">
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    Seat Category / Pitch
                  </label>
                  <select
                    value={preferredSeatType}
                    onChange={e => setPreferredSeatType(e.target.value)}
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                  >
                    {SEAT_TYPES.map(st => (
                      <option key={st.value} value={st.value}>
                        {st.label} — {st.desc}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Room Preferences */}
              <div className="border-t border-gray-100 pt-5">
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Hotel Room Preference
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {ROOM_TYPES.map(rt => {
                    const isSelected = preferredRoomType === rt.value;
                    return (
                      <button
                        key={rt.value}
                        type="button"
                        onClick={() => setPreferredRoomType(rt.value)}
                        className={`p-2.5 rounded-xl border text-left transition-all ${
                          isSelected
                            ? 'border-blue-600 bg-blue-50/50 ring-2 ring-blue-500/20'
                            : 'border-gray-200 hover:border-gray-300'
                        }`}
                      >
                        <div className="text-xs font-bold text-gray-900">{rt.label}</div>
                      </button>
                    );
                  })}
                </div>

                <div className="mt-3">
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    Bed Type
                  </label>
                  <select
                    value={preferredBedType}
                    onChange={e => setPreferredBedType(e.target.value)}
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                  >
                    {BED_TYPES.map(bt => (
                      <option key={bt.value} value={bt.value}>{bt.label}</option>
                    ))}
                  </select>
                </div>

                <div className="mt-3">
                  <label className="block text-xs font-medium text-gray-600 mb-1.5">
                    Desired Features
                  </label>
                  <div className="flex flex-wrap gap-1.5">
                    {ROOM_FEATURES.map(feat => {
                      const active = preferredRoomFeatures.includes(feat);
                      return (
                        <button
                          key={feat}
                          type="button"
                          onClick={() => toggleFeature(feat)}
                          className={`px-2.5 py-1 rounded-full text-xs font-medium transition ${
                            active
                              ? 'bg-blue-600 text-white shadow-xs'
                              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                          }`}
                        >
                          {feat.replace(/_/g, ' ')} {active && '✓'}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* Destination Style & Travel Interests */}
              <div className="border-t border-gray-100 pt-5">
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Destination Style & Interests
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {DESTINATION_STYLES.map(style => {
                    const isSelected = preferredDestinations.includes(style.value);
                    return (
                      <button
                        key={style.value}
                        type="button"
                        onClick={() => toggleDestinationStyle(style.value)}
                        className={`p-2.5 rounded-xl border text-center transition-all ${
                          isSelected
                            ? 'border-blue-600 bg-blue-50/60 ring-2 ring-blue-500/20'
                            : 'border-gray-200 hover:border-gray-300 bg-white'
                        }`}
                      >
                        <div className="text-xl mb-0.5">{style.icon}</div>
                        <div className="text-xs font-bold text-gray-900">{style.label}</div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Cabin & Budget Preferences */}
              <div className="border-t border-gray-100 pt-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                      Preferred Cabin Class
                    </label>
                    <select
                      value={preferredCabinClass}
                      onChange={e => setPreferredCabinClass(e.target.value)}
                      className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      {CABIN_CLASSES.map(cls => (
                        <option key={cls.value} value={cls.value}>{cls.label}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                      Travel Budget Level
                    </label>
                    <select
                      value={budgetLevel}
                      onChange={e => setBudgetLevel(e.target.value)}
                      className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-blue-500 outline-none"
                    >
                      {BUDGET_LEVELS.map(b => (
                        <option key={b.value} value={b.value}>{b.label} ({b.desc})</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-gray-100 bg-gray-50/50 flex items-center justify-end gap-2 sticky bottom-0">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-200 rounded-xl transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={saving || loading}
            className="px-5 py-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-md shadow-blue-500/20 disabled:opacity-50 transition"
          >
            {saving ? 'Saving...' : 'Save Preferences'}
          </button>
        </div>
      </div>
    </div>
  );
}
