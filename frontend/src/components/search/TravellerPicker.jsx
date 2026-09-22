import { useState, useRef, useEffect } from 'react';

const CABIN_CLASSES = [
  { key: 'Economy', desc: 'Standard seating' },
  { key: 'Premium Economy', desc: 'Extra legroom' },
  { key: 'Business', desc: 'Lie-flat seats' },
  { key: 'First Class', desc: 'Private suite' },
];

export default function TravellerPicker({ adults, setAdults, children: childCount, setChildren, infants, setInfants, cabinClass, setCabinClass }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);

  const total = adults + childCount + infants;

  const Counter = ({ label, sublabel, value, setValue, min = 0, max = 9 }) => (
    <div className="flex items-center justify-between py-3">
      <div>
        <div className="text-sm font-semibold text-gray-900">{label}</div>
        {sublabel && <div className="text-xs text-gray-500">{sublabel}</div>}
      </div>
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => setValue(Math.max(min, value - 1))}
          disabled={value <= min}
          className="w-8 h-8 rounded-full border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed font-bold transition-colors"
        >
          −
        </button>
        <span className="w-6 text-center font-bold text-gray-900">{value}</span>
        <button
          type="button"
          onClick={() => setValue(Math.min(max, value + 1))}
          disabled={value >= max}
          className="w-8 h-8 rounded-full border border-gray-200 flex items-center justify-center text-gray-500 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed font-bold transition-colors"
        >
          +
        </button>
      </div>
    </div>
  );

  return (
    <div className="relative" ref={ref}>
      <label className="block text-xs font-semibold text-gray-500 mb-1.5 uppercase tracking-wider">Travellers & Class</label>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="w-full text-left px-4 py-3.5 bg-white border border-gray-200 rounded-xl hover:border-primary focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all outline-none"
      >
        <div className="text-sm font-semibold text-gray-900">{total} Traveller{total > 1 ? 's' : ''}</div>
        <div className="text-xs text-gray-500">{cabinClass}</div>
      </button>

      {open && (
        <div className="absolute top-full left-0 right-0 sm:right-auto sm:w-[360px] mt-2 bg-white rounded-2xl shadow-xl border border-gray-100 z-50 p-4 animate-[slideDown_0.15s_ease-out]">
          <Counter label="Adults" sublabel="12+ years" value={adults} setValue={setAdults} min={1} />
          <Counter label="Children" sublabel="2–12 years" value={childCount} setValue={setChildren} min={0} />
          <Counter label="Infants" sublabel="Under 2 years" value={infants} setValue={setInfants} min={0} max={Math.min(adults, 9)} />

          <div className="border-t border-gray-100 mt-2 pt-3">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Cabin Class</div>
            <div className="grid grid-cols-2 gap-2">
              {CABIN_CLASSES.map(cls => (
                <button
                  key={cls.key}
                  type="button"
                  onClick={() => setCabinClass(cls.key)}
                  className={`px-3 py-2 rounded-xl text-left text-sm font-medium border-2 transition-all ${
                    cabinClass === cls.key
                      ? 'border-primary bg-primary-light text-primary'
                      : 'border-gray-100 hover:border-gray-200 text-gray-700'
                  }`}
                >
                  <div>{cls.key}</div>
                  <div className="text-xs text-gray-400 font-normal">{cls.desc}</div>
                </button>
              ))}
            </div>
          </div>

          <button
            type="button"
            onClick={() => setOpen(false)}
            className="w-full mt-4 bg-primary text-white py-2.5 rounded-xl text-sm font-semibold hover:bg-primary-dark transition-colors"
          >
            Done
          </button>
        </div>
      )}
    </div>
  );
}
