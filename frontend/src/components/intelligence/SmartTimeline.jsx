import { FlightIcon, HotelIcon, ClockIcon, MapPinIcon, AlertTriangleIcon, CheckIcon } from '../common/Icons';

export default function SmartTimeline({ events = [], className = '' }) {
  if (!events || events.length === 0) {
    return (
      <div className={`voyara-card p-8 text-center ${className}`}>
        <div className="w-12 h-12 rounded-2xl bg-blue-50 text-primary flex items-center justify-center mx-auto mb-3">
          <ClockIcon className="w-6 h-6" />
        </div>
        <h4 className="text-sm font-bold text-slate-900">Timeline Generating</h4>
        <p className="text-xs text-slate-500 mt-1 max-w-xs mx-auto">
          Your itinerary milestones and transfer checkpoints will appear here once flight schedules lock in.
        </p>
      </div>
    );
  }

  const getEventIcon = (event) => {
    const title = (event.eventTitle || event.title || '').toLowerCase();
    if (title.includes('flight') || title.includes('depart') || title.includes('arriv')) {
      return FlightIcon;
    }
    if (title.includes('hotel') || title.includes('check-in')) {
      return HotelIcon;
    }
    return ClockIcon;
  };

  return (
    <div className={`voyara-card p-5 sm:p-6 ${className}`}>
      <div className="flex items-center justify-between mb-6 pb-3 border-b border-slate-100">
        <div>
          <h4 className="text-sm font-bold text-slate-900">Smart Trip Timeline</h4>
          <p className="text-xs text-slate-500">Autonomous step-by-step itinerary progression</p>
        </div>
        <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700">
          {events.length} Milestones
        </span>
      </div>

      <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-200">
        {events.map((event, index) => {
          const IconComponent = getEventIcon(event);
          const isAffected = event.affected || event.hasDelay;
          const isLast = index === events.length - 1;

          const dateObj = event.eventTime ? new Date(event.eventTime) : null;
          const timeStr = dateObj
            ? dateObj.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })
            : '—';
          const dateStr = dateObj
            ? dateObj.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
            : '';

          return (
            <div key={event.id || index} className="relative group">
              {/* Timeline Bullet Node */}
              <div
                className={`absolute -left-6 top-1 w-5 h-5 rounded-full border-2 bg-white flex items-center justify-center transition-all ${
                  isAffected
                    ? 'border-amber-500 text-amber-500 shadow-sm shadow-amber-200'
                    : 'border-primary text-primary shadow-sm shadow-primary/20 group-hover:scale-110'
                }`}
              >
                <div
                  className={`w-2 h-2 rounded-full ${isAffected ? 'bg-amber-500' : 'bg-primary'}`}
                />
              </div>

              {/* Event Card Content */}
              <div
                className={`rounded-2xl border p-4 transition-all duration-200 ${
                  isAffected
                    ? 'border-amber-200 bg-amber-50/40 shadow-sm'
                    : 'border-slate-100 bg-slate-50/70 hover:bg-white hover:border-slate-200 hover:shadow-sm'
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-2 mb-1.5">
                  <div className="flex items-center gap-2">
                    <IconComponent className={`w-4 h-4 ${isAffected ? 'text-amber-600' : 'text-primary'}`} />
                    <h5 className="text-sm font-bold text-slate-900">{event.eventTitle || event.title}</h5>
                  </div>

                  <div className="flex items-center gap-2">
                    {isAffected && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                        <AlertTriangleIcon className="w-3 h-3" />
                        Disruption
                      </span>
                    )}
                    <span className="text-xs font-mono font-bold text-slate-700 bg-white px-2 py-0.5 rounded-md border border-slate-200">
                      {timeStr}
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 mb-1">
                  {dateStr && <span>{dateStr}</span>}
                  {event.eventLocation && (
                    <span className="flex items-center gap-1 text-slate-600">
                      <MapPinIcon className="w-3 h-3 text-slate-400" />
                      {event.eventLocation}
                    </span>
                  )}
                </div>

                {event.eventDescription && (
                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">{event.eventDescription}</p>
                )}

                {isAffected && event.affectedReason && (
                  <div className="mt-2 text-xs font-medium text-amber-800 bg-amber-100/70 rounded-xl p-2.5 border border-amber-200">
                    ⚠ {event.affectedReason}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
