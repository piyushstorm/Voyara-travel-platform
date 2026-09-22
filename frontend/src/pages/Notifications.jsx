import { useState, useEffect } from 'react';
import { notificationsApi } from '../api/phase3Api';
import {
  BellIcon,
  CheckIcon,
  CreditCardIcon,
  FlightIcon,
  AlertTriangleIcon,
  LockIcon,
  SparklesIcon,
  ShieldCheckIcon,
} from '../components/common/Icons';

const TYPE_CONFIG = {
  BOOKING_CONFIRMED: {
    icon: CheckIcon,
    bg: 'bg-emerald-50 text-emerald-600 border-emerald-200',
    label: 'Booking Confirmed',
  },
  PAYMENT_SUCCESSFUL: {
    icon: CreditCardIcon,
    bg: 'bg-blue-50 text-blue-600 border-blue-200',
    label: 'Payment Successful',
  },
  BOOKING_CANCELLED: {
    icon: AlertTriangleIcon,
    bg: 'bg-rose-50 text-rose-600 border-rose-200',
    label: 'Booking Cancelled',
  },
  REFUND_PROCESSED: {
    icon: CreditCardIcon,
    bg: 'bg-amber-50 text-amber-600 border-amber-200',
    label: 'Refund Processed',
  },
  FLIGHT_STATUS_CHANGE: {
    icon: FlightIcon,
    bg: 'bg-sky-50 text-sky-600 border-sky-200',
    label: 'Flight Update',
  },
  FLIGHT_DELAY: {
    icon: AlertTriangleIcon,
    bg: 'bg-amber-50 text-amber-600 border-amber-200',
    label: 'Flight Delay',
  },
  PRICE_ALERT: {
    icon: SparklesIcon,
    bg: 'bg-purple-50 text-purple-600 border-purple-200',
    label: 'Price Alert',
  },
  ACCOUNT_SECURITY: {
    icon: LockIcon,
    bg: 'bg-indigo-50 text-indigo-600 border-indigo-200',
    label: 'Security Alert',
  },
  WELCOME: {
    icon: SparklesIcon,
    bg: 'bg-sky-50 text-sky-600 border-sky-200',
    label: 'Welcome',
  },
  PASSWORD_RESET: {
    icon: ShieldCheckIcon,
    bg: 'bg-emerald-50 text-emerald-600 border-emerald-200',
    label: 'Password Security',
  },
};

function timeAgo(dateStr) {
  if (!dateStr) return '';
  const now = new Date();
  const date = new Date(dateStr);
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [markingAll, setMarkingAll] = useState(false);

  const fetchNotifications = () => {
    setLoading(true);
    notificationsApi
      .getNotifications({ type: filter || undefined, page, size: 20 })
      .then((res) => {
        const data = res.data;
        setNotifications(data.content || []);
        setTotalPages(data.totalPages || 1);
        setUnreadCount(data.unreadCount || 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchNotifications();
  }, [page, filter]);

  const handleMarkRead = async (id) => {
    try {
      await notificationsApi.markAsRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)));
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {}
  };

  const handleMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await notificationsApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch {}
    setMarkingAll(false);
  };

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 pb-20 md:pb-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Notifications</h1>
            {unreadCount > 0 && (
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-bold text-primary">
                {unreadCount} new
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? 's' : ''}` : 'All caught up!'}
          </p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllRead}
            disabled={markingAll}
            className="travel-button-secondary h-9 px-4 text-xs font-bold whitespace-nowrap self-start sm:self-auto"
          >
            {markingAll ? 'Marking...' : 'Mark all as read'}
          </button>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-1.5 mb-6 overflow-x-auto no-scrollbar pb-1">
        {[
          { key: '', label: 'All' },
          { key: 'BOOKING_CONFIRMED', label: 'Bookings' },
          { key: 'PAYMENT_SUCCESSFUL', label: 'Payments' },
          { key: 'FLIGHT_STATUS_CHANGE', label: 'Flights' },
          { key: 'REFUND_PROCESSED', label: 'Refunds' },
          { key: 'ACCOUNT_SECURITY', label: 'Security' },
        ].map((f) => {
          const active = filter === f.key;
          return (
            <button
              key={f.key}
              onClick={() => {
                setFilter(f.key);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all whitespace-nowrap border ${
                active
                  ? 'bg-slate-900 text-white border-slate-900 shadow-sm'
                  : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
              }`}
            >
              {f.label}
            </button>
          );
        })}
      </div>

      {/* Notification List */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-20 bg-slate-100 rounded-2xl animate-pulse" />
          ))}
        </div>
      ) : notifications.length === 0 ? (
        <div className="voyara-card text-center py-16 px-6">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
            <BellIcon className="h-7 w-7" />
          </div>
          <h3 className="text-base font-bold text-slate-900 mb-1">No notifications</h3>
          <p className="text-xs text-slate-500 max-w-xs mx-auto">
            {filter ? 'No notifications match this filter.' : "You're completely up to date with your travels."}
          </p>
        </div>
      ) : (
        <div className="space-y-2.5">
          {notifications.map((n) => {
            const config = TYPE_CONFIG[n.type] || {
              icon: BellIcon,
              bg: 'bg-slate-100 text-slate-600 border-slate-200',
              label: 'Alert',
            };
            const IconComponent = config.icon;

            return (
              <div
                key={n.id}
                className={`flex items-start gap-3.5 p-4 rounded-2xl border transition-all ${
                  n.isRead
                    ? 'bg-white border-slate-200/80 shadow-sm'
                    : 'bg-sky-50/40 border-sky-200 shadow-sm'
                }`}
              >
                <div
                  className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border ${config.bg}`}
                >
                  <IconComponent className="h-4 w-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <p className={`text-xs font-bold ${n.isRead ? 'text-slate-800' : 'text-slate-950'}`}>
                      {n.title}
                    </p>
                    {!n.isRead && (
                      <button
                        onClick={() => handleMarkRead(n.id)}
                        className="text-[11px] text-primary hover:underline whitespace-nowrap font-bold"
                      >
                        Mark read
                      </button>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed line-clamp-2">{n.message}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <span className="text-[10px] font-medium text-slate-400">{timeAgo(n.createdAt)}</span>
                    {n.relatedEntityRef && (
                      <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-mono text-slate-600">
                        {n.relatedEntityRef}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 pt-4 border-t border-slate-200">
          <span className="text-xs text-slate-500 font-medium">
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-bold disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-bold disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
