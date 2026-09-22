import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import adminApi from '../api/adminApi';
import { rewardsApi } from '../api/phase3Api';
import ReviewModerationQueue from '../components/reviews/ReviewModerationQueue';

// ==================== SVG ICON ====================
function Icon({ d, className = "w-5 h-5" }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d={d} />
    </svg>
  );
}

// ==================== SIDEBAR SECTION GROUPS ====================
const SIDEBAR_SECTIONS = [
  {
    title: 'OVERVIEW',
    items: [
      { id: 'dashboard', label: 'Dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
    ],
  },
  {
    title: 'MANAGEMENT',
    items: [
      { id: 'users', label: 'Users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
      { id: 'flights', label: 'Flights', icon: 'M12 19l9 2-9-18-9 18 9-2zm0 0v-8' },
      { id: 'hotels', label: 'Hotels', icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4' },
      { id: 'holidays', label: 'Holidays', icon: 'M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064' },
      { id: 'trains', label: 'Trains', icon: 'M8 17l2 2 2-2m4-7v6m-4-3h8M4 7h4m4 0h4M4 7v10a2 2 0 002 2h12a2 2 0 002-2V7' },
      { id: 'buses', label: 'Buses', icon: 'M8 17l2 2 2-2m4-7v6m-4-3h8M4 7h4m4 0h4M4 7v10a2 2 0 002 2h12a2 2 0 002-2V7' },
      { id: 'cabs', label: 'Cabs', icon: 'M8 17l2 2 2-2m4-7v6m-4-3h8M4 7h4m4 0h4M4 7v10a2 2 0 002 2h12a2 2 0 002-2V7' },
    ],
  },
  {
    title: 'OPERATIONS',
    items: [
      { id: 'bookings', label: 'Bookings', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
      { id: 'payments', label: 'Payments', icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z' },
      { id: 'refunds', label: 'Refunds', icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z' },
      { id: 'reviews', label: 'Reviews', icon: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z' },
    ],
  },
  {
    title: 'BUSINESS',
    items: [
      { id: 'analytics', label: 'Analytics', icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' },
      { id: 'offers', label: 'Offers & Coupons', icon: 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z' },
      { id: 'rewards', label: 'Rewards', icon: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976-2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z' },
    ],
  },
  {
    title: 'SYSTEM',
    items: [
      { id: 'auditlogs', label: 'Audit Logs', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
    ],
  },
];

// ==================== SHARED COMPONENTS ====================
function StatCard({ label, value, icon, color = 'blue', subtitle, onClick }) {
  const colors = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    red: 'bg-red-50 text-red-600',
    purple: 'bg-purple-50 text-purple-600',
    indigo: 'bg-indigo-50 text-indigo-600',
    cyan: 'bg-cyan-50 text-cyan-600',
    rose: 'bg-rose-50 text-rose-600',
  };
  return (
    <button onClick={onClick} className="bg-white rounded-xl border border-gray-100 p-5 text-left hover:shadow-md transition-all duration-200 w-full group">
      <div className="flex items-center justify-between mb-3">
        <span className={`w-11 h-11 rounded-xl flex items-center justify-center transition-colors ${colors[color]}`}>
          <Icon d={icon} className="w-5 h-5" />
        </span>
      </div>
      <p className="text-2xl font-bold text-gray-900 group-hover:text-blue-600 transition-colors">{value ?? '—'}</p>
      <p className="text-sm text-gray-500 mt-1">{label}</p>
      {subtitle && <p className="text-xs text-gray-400 mt-0.5">{subtitle}</p>}
    </button>
  );
}

function StatusBadge({ status }) {
  const styles = {
    CONFIRMED: 'bg-emerald-100 text-emerald-700 border-emerald-200',
    COMPLETED: 'bg-emerald-100 text-emerald-700 border-emerald-200',
    PENDING: 'bg-amber-100 text-amber-700 border-amber-200',
    PROCESSING: 'bg-blue-100 text-blue-700 border-blue-200',
    CANCELLED: 'bg-red-100 text-red-700 border-red-200',
    FAILED: 'bg-red-100 text-red-700 border-red-200',
    REFUNDED: 'bg-purple-100 text-purple-700 border-purple-200',
    REJECTED: 'bg-red-100 text-red-700 border-red-200',
    ACTIVE: 'bg-emerald-100 text-emerald-700 border-emerald-200',
    ADMIN: 'bg-indigo-100 text-indigo-700 border-indigo-200',
    USER: 'bg-gray-100 text-gray-700 border-gray-200',
    ROLE_CHANGE: 'bg-purple-100 text-purple-700 border-purple-200',
  };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${styles[status] || 'bg-gray-100 text-gray-600 border-gray-200'}`}>
      {status}
    </span>
  );
}

function LoadingSkeleton({ rows = 5, className = '' }) {
  return (
    <div className={`space-y-3 ${className}`}>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-14 bg-gray-100 rounded-lg animate-pulse" style={{ animationDelay: `${i * 50}ms` }} />
      ))}
    </div>
  );
}

function EmptyState({ icon, title, description, action }) {
  return (
    <div className="text-center py-16">
      {icon && <div className="text-5xl mb-4">{icon}</div>}
      <h3 className="text-lg font-semibold text-gray-900 mb-1">{title}</h3>
      <p className="text-gray-500 text-sm mb-4">{description}</p>
      {action}
    </div>
  );
}

function Pagination({ page, totalPages, setPage }) {
  if (totalPages <= 1) return null;
  return (
    <div className="px-4 py-3 border-t border-gray-100 flex items-center justify-between text-sm">
      <span className="text-gray-500">Page {page + 1} of {totalPages}</span>
      <div className="flex gap-2">
        <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
          className="px-3 py-1.5 rounded-lg border border-gray-200 text-sm font-medium disabled:opacity-40 hover:bg-gray-50 transition-colors">Prev</button>
        <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
          className="px-3 py-1.5 rounded-lg border border-gray-200 text-sm font-medium disabled:opacity-40 hover:bg-gray-50 transition-colors">Next</button>
      </div>
    </div>
  );
}

function GenericTable({ title, data, loading, columns, emptyTitle, emptyDesc, searchPlaceholder, searchValue, onSearchChange, page, setPage, actions, headerExtra }) {
  const items = data?.content || data?.data || data || [];
  const totalPages = data?.totalPages || 1;
  const [mobileOpen, setMobileOpen] = useState(null);

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <h2 className="text-xl font-bold text-gray-900">{title}</h2>
        <div className="flex items-center gap-2">{headerExtra}</div>
        {searchPlaceholder && (
          <div className="relative">
            <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
            <input type="text" placeholder={searchPlaceholder} value={searchValue || ''} onChange={onSearchChange}
              className="pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-full sm:w-72" />
          </div>
        )}
      </div>
      {loading ? <LoadingSkeleton /> : items.length === 0 ? (
        <EmptyState title={emptyTitle || `No ${title.toLowerCase()} found`} description={emptyDesc || 'Data will appear here once available.'} />
      ) : (
        <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
          {/* Desktop table */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/80">
                  {columns.map((col, i) => (
                    <th key={i} className="px-4 py-3 text-left font-medium text-gray-500 text-xs uppercase tracking-wider">{col.label}</th>
                  ))}
                  {actions && <th className="px-4 py-3 text-right font-medium text-gray-500 text-xs uppercase tracking-wider">Actions</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {items.map((item, i) => (
                  <tr key={item.id || i} className="hover:bg-gray-50/50 transition-colors">
                    {columns.map((col, j) => (
                      <td key={j} className="px-4 py-3">
                        {col.render ? col.render(item) : (
                          <span className="text-gray-700">{item[col.key] ?? '—'}</span>
                        )}
                      </td>
                    ))}
                    {actions && <td className="px-4 py-3 text-right">{actions(item)}</td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {/* Mobile cards */}
          <div className="md:hidden divide-y divide-gray-50">
            {items.map((item, i) => (
              <div key={item.id || i} className="p-4">
                <div className="flex items-start justify-between mb-2">
                  <div className="flex-1 min-w-0">
                    {columns.slice(0, 3).map((col, j) => (
                      <div key={j} className={j === 0 ? 'font-medium text-gray-900 text-sm' : 'text-xs text-gray-500 mt-0.5'}>
                        {col.render ? col.render(item) : (item[col.key] ?? '—')}
                      </div>
                    ))}
                  </div>
                  {actions && <div className="ml-2 flex-shrink-0">{actions(item)}</div>}
                  {columns.length > 3 && (
                    <button onClick={() => setMobileOpen(mobileOpen === i ? null : i)}
                      className="ml-2 p-1 text-gray-400 hover:text-gray-600">
                      <Icon d={mobileOpen === i ? 'M5 15l7-7 7 7' : 'M19 9l-7 7-7-7'} className="w-4 h-4" />
                    </button>
                  )}
                </div>
                {mobileOpen === i && (
                  <div className="mt-2 pt-2 border-t border-gray-100 space-y-1">
                    {columns.slice(3).map((col, j) => (
                      <div key={j} className="flex justify-between text-xs">
                        <span className="text-gray-500">{col.label}</span>
                        <span className="text-gray-900">
                          {col.render ? col.render(item) : (item[col.key] ?? '—')}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} setPage={setPage} />
        </div>
      )}
    </div>
  );
}

// ==================== DASHBOARD ====================
function AdminDashboard({ stats, loading }) {
  const [activityTab, setActivityTab] = useState('bookings');

  if (loading) return (
    <div>
      <div className="mb-6">
        <div className="h-8 w-48 bg-gray-100 rounded animate-pulse mb-2" />
        <div className="h-4 w-64 bg-gray-100 rounded animate-pulse" />
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {[...Array(8)].map((_, i) => <div key={i} className="h-28 bg-gray-100 rounded-xl animate-pulse" />)}
      </div>
    </div>
  );

  if (!stats) return <EmptyState title="No data available" description="Dashboard data could not be loaded." />;

  const activityTabs = [
    { id: 'bookings', label: 'Bookings', data: stats.recentBookings, count: stats.recentBookings?.length },
    { id: 'payments', label: 'Payments', data: stats.recentPayments, count: stats.recentPayments?.length },
    { id: 'refunds', label: 'Refunds', data: stats.recentRefunds, count: stats.recentRefunds?.length },
    { id: 'users', label: 'Users', data: stats.recentUsers, count: stats.recentUsers?.length },
  ];

  const activeActivity = activityTabs.find(t => t.id === activityTab);

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-xl font-bold text-gray-900">Dashboard</h2>
        <p className="text-sm text-gray-500">TravelPlatform business overview and key metrics</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 lg:gap-4 mb-4">
        <StatCard label="Revenue (30d)" value={`₹${Number(stats.revenue30Days || stats.totalRevenue || 0).toLocaleString()}`} icon="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" color="purple" subtitle={`₹${Number(stats.revenue7Days || 0).toLocaleString()} last 7d`} />
        <StatCard label="Total Bookings" value={stats.totalBookings} icon="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" color="green" subtitle={`${stats.bookings30Days || 0} this month`} />
        <StatCard label="Avg Order Value" value={`₹${Number(stats.avgBookingValue || 0).toLocaleString()}`} icon="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" color="blue" subtitle="Per booking" />
        <StatCard label="Cancellation Rate" value={`${(stats.cancellationRate || 0).toFixed(1)}%`} icon="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" color="amber" subtitle={`${stats.totalRefunds || 0} refunds`} />
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 lg:gap-4 mb-8">
        <StatCard label="Users" value={stats.totalUsers} icon="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" color="blue" />
        <StatCard label="Flights" value={stats.totalFlights} icon="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" color="cyan" />
        <StatCard label="Hotels" value={stats.totalHotels} icon="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" color="indigo" />
        <StatCard label="Refunds" value={stats.totalRefunds} icon="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" color="rose" />
      </div>

      {/* Recent Activity */}
      <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100 flex items-center gap-1 overflow-x-auto">
          {activityTabs.map(tab => (
            <button key={tab.id} onClick={() => setActivityTab(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${activityTab === tab.id ? 'bg-blue-50 text-blue-700' : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'}`}>
              {tab.label}
              {tab.count > 0 && <span className="ml-1.5 text-xs bg-gray-200 text-gray-600 px-1.5 rounded-full">{tab.count}</span>}
            </button>
          ))}
        </div>
        <div className="divide-y divide-gray-50 max-h-96 overflow-y-auto">
          {activeActivity?.data?.length === 0 ? (
            <EmptyState title={`No recent ${activeActivity?.label?.toLowerCase()}`} description="Data will appear here as it becomes available." />
          ) : activeActivity?.data?.map((item, i) => {
            if (activityTab === 'bookings') return (
              <div key={i} className="px-5 py-3 flex items-center justify-between text-sm hover:bg-gray-50/50 transition-colors">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600 flex-shrink-0">
                    <Icon d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">{item.bookingReference || `#${item.id}`}</p>
                    <p className="text-xs text-gray-500 truncate">{item.userName || 'User'} · {item.bookingType}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <StatusBadge status={item.status} />
                  <span className="font-medium text-gray-900 hidden sm:inline">₹{Number(item.totalAmount || 0).toLocaleString()}</span>
                </div>
              </div>
            );
            if (activityTab === 'payments') return (
              <div key={i} className="px-5 py-3 flex items-center justify-between text-sm hover:bg-gray-50/50 transition-colors">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600 flex-shrink-0">
                    <Icon d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">{item.paymentId || `#${item.id}`}</p>
                    <p className="text-xs text-gray-500 truncate">{item.userName || 'User'} · {item.paymentMethod || '—'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <StatusBadge status={item.status || 'PENDING'} />
                  <span className="font-medium text-gray-900">₹{Number(item.amount || 0).toLocaleString()}</span>
                </div>
              </div>
            );
            if (activityTab === 'refunds') return (
              <div key={i} className="px-5 py-3 flex items-center justify-between text-sm hover:bg-gray-50/50 transition-colors">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-rose-50 flex items-center justify-center text-rose-600 flex-shrink-0">
                    <Icon d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">{item.bookingReference || `Refund #${item.id}`}</p>
                    <p className="text-xs text-gray-500 truncate">{item.userName || 'User'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <StatusBadge status={item.status || 'PENDING'} />
                  <span className="font-medium text-gray-900">₹{Number(item.refundAmount || 0).toLocaleString()}</span>
                </div>
              </div>
            );
            return (
              <div key={i} className="px-5 py-3 flex items-center justify-between text-sm hover:bg-gray-50/50 transition-colors">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-indigo-50 flex items-center justify-center text-indigo-700 flex-shrink-0 text-xs font-bold">
                    {(item.name || 'U')[0].toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-gray-900 truncate">{item.name}</p>
                    <p className="text-xs text-gray-500 truncate">{item.email}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <StatusBadge status={item.role} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="mt-6">
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Quick Actions</h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          {[
            { label: 'Users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z', color: 'blue', tab: 'users' },
            { label: 'Flights', icon: 'M12 19l9 2-9-18-9 18 9-2zm0 0v-8', color: 'cyan', tab: 'flights' },
            { label: 'Hotels', icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5', color: 'indigo', tab: 'hotels' },
            { label: 'Bookings', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2', color: 'green', tab: 'bookings' },
            { label: 'Refunds', icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z', color: 'rose', tab: 'refunds' },
            { label: 'Audit Logs', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z', color: 'purple', tab: 'auditlogs' },
          ].map(qa => (
            <button key={qa.tab} className={`bg-white border border-gray-100 rounded-xl p-3 text-center hover:shadow-md transition-all group`}>
              <div className={`w-10 h-10 rounded-lg mx-auto mb-2 flex items-center justify-center bg-${qa.color}-50 text-${qa.color}-600`}>
                <Icon d={qa.icon} className="w-5 h-5" />
              </div>
              <p className="text-xs font-medium text-gray-700 group-hover:text-blue-600 transition-colors">{qa.label}</p>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ==================== BOOKINGS PAGE ====================
function BookingsPage({ data, loading, filters, setFilters, page, setPage }) {
  const items = data?.content || data?.data || data || [];
  const totalPages = data?.totalPages || 1;
  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <h2 className="text-xl font-bold text-gray-900">Bookings</h2>
        <div className="relative">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" /></svg>
          <input type="text" placeholder="Search reference, email..." value={filters.search || ''} onChange={e => setFilters(f => ({ ...f, search: e.target.value }))}
            className="pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-full sm:w-72" />
        </div>
      </div>
      <div className="flex flex-wrap gap-2 mb-6">
        {['', 'CONFIRMED', 'PENDING', 'CANCELLED', 'COMPLETED'].map(s => (
          <button key={s} onClick={() => setFilters(f => ({ ...f, status: s }))}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${filters.status === s ? 'bg-blue-600 text-white shadow-sm' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            {s || 'All'}
          </button>
        ))}
      </div>
      {loading ? <LoadingSkeleton /> : items.length === 0 ? (
        <EmptyState title="No bookings found" description="Try adjusting your filters or search terms." />
      ) : (
        <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/80">
                  {['Reference', 'Customer', 'Type', 'Travel', 'Amount', 'Status', 'Created'].map(h => (
                    <th key={h} className="px-4 py-3 text-left font-medium text-gray-500 text-xs uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {items.map((b, i) => (
                  <tr key={b.id || i} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-4 py-3 font-medium text-gray-900 font-mono text-xs">{b.bookingReference || `#${b.id}`}</td>
                    <td className="px-4 py-3">
                      <div className="text-gray-900 text-sm">{b.userName || '—'}</div>
                      <div className="text-gray-400 text-xs">{b.userEmail || ''}</div>
                    </td>
                    <td className="px-4 py-3"><span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700">{b.bookingType}</span></td>
                    <td className="px-4 py-3 text-gray-600">
                      {b.originCode && b.destinationCode ? `${b.originCode} → ${b.destinationCode}` : b.hotelName || '—'}
                      {b.travelDate && <div className="text-xs text-gray-400">{String(b.travelDate).slice(0, 10)}</div>}
                    </td>
                    <td className="px-4 py-3 font-medium">₹{Number(b.totalAmount || 0).toLocaleString()}</td>
                    <td className="px-4 py-3"><StatusBadge status={b.status} /></td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{b.createdAt ? String(b.createdAt).slice(0, 10) : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {/* Mobile cards */}
          <div className="md:hidden divide-y divide-gray-50">
            {items.map((b, i) => (
              <div key={b.id || i} className="p-4">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <p className="font-mono text-xs font-medium text-gray-900">{b.bookingReference || `#${b.id}`}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{b.userName || 'User'} · {b.bookingType}</p>
                  </div>
                  <StatusBadge status={b.status} />
                </div>
                <div className="flex items-center justify-between mt-2">
                  <span className="text-xs text-gray-500">
                    {b.originCode && b.destinationCode ? `${b.originCode} → ${b.destinationCode}` : b.hotelName || '—'}
                    {b.travelDate ? ` · ${String(b.travelDate).slice(0, 10)}` : ''}
                  </span>
                  <span className="text-sm font-semibold">₹{Number(b.totalAmount || 0).toLocaleString()}</span>
                </div>
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} setPage={setPage} />
        </div>
      )}
    </div>
  );
}

// ==================== USERS PAGE ====================
function UsersPage({ data, loading, onRoleChange, currentUserEmail }) {
  const items = data?.content || data?.data || data || [];
  const [confirmModal, setConfirmModal] = useState(null);
  const [changing, setChanging] = useState(false);
  const [toast, setToast] = useState(null);

  const handleRoleChange = async (user, newRole) => {
    setChanging(true);
    try {
      const result = await adminApi.changeUserRole(user.id, newRole);
      if (result.success) {
        setToast({ type: 'success', message: `${user.name} is now ${newRole === 'ADMIN' ? 'an Admin' : 'a User'}` });
        onRoleChange && onRoleChange();
      } else {
        setToast({ type: 'error', message: result.message || 'Failed to change role' });
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to change role';
      setToast({ type: 'error', message: msg });
    } finally {
      setChanging(false);
      setConfirmModal(null);
      setTimeout(() => setToast(null), 4000);
    }
  };

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <h2 className="text-xl font-bold text-gray-900">User Management</h2>
        <div className="text-sm text-gray-500">{items.length} user{items.length !== 1 ? 's' : ''}</div>
      </div>
      {toast && (
        <div className={`mb-4 px-4 py-3 rounded-xl text-sm font-medium flex items-center gap-2 ${toast.type === 'success' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-red-50 text-red-700 border border-red-200'}`}
          style={{ animation: 'slideIn 0.3s ease-out' }}>
          {toast.type === 'success' ? (
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" /></svg>
          ) : (
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          )}
          {toast.message}
        </div>
      )}
      {loading ? <LoadingSkeleton /> : items.length === 0 ? (
        <EmptyState title="No users found" description="Registered users will appear here." />
      ) : (
        <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
          {/* Desktop table */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/80">
                  {['User', 'Email', 'Role', 'Status', 'Joined', 'Actions'].map(h => (
                    <th key={h} className="px-4 py-3 text-left font-medium text-gray-500 text-xs uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {items.map((u, i) => {
                  const isSelf = u.email === currentUserEmail;
                  return (
                    <tr key={u.id || i} className="hover:bg-gray-50/50 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-xs font-semibold flex-shrink-0">
                            {(u.name || 'U')[0].toUpperCase()}
                          </div>
                          <span className="font-medium text-gray-900">{u.name}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-500">{u.email}</td>
                      <td className="px-4 py-3"><StatusBadge status={u.role} /></td>
                      <td className="px-4 py-3"><StatusBadge status={u.enabled ? 'ACTIVE' : 'DISABLED'} /></td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{u.createdAt ? String(u.createdAt).slice(0, 10) : '—'}</td>
                      <td className="px-4 py-3">
                        {isSelf ? (
                          <span className="text-xs text-gray-400 italic">You</span>
                        ) : u.role === 'ADMIN' ? (
                          <button onClick={() => setConfirmModal({ user: u, action: 'demote', newRole: 'USER' })}
                            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100 transition-colors">
                            Demote to User
                          </button>
                        ) : (
                          <button onClick={() => setConfirmModal({ user: u, action: 'promote', newRole: 'ADMIN' })}
                            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-blue-200 text-blue-700 bg-blue-50 hover:bg-blue-100 transition-colors">
                            Promote to Admin
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {/* Mobile cards */}
          <div className="md:hidden divide-y divide-gray-50">
            {items.map((u, i) => {
              const isSelf = u.email === currentUserEmail;
              return (
                <div key={u.id || i} className="p-4">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                        {(u.name || 'U')[0].toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <div className="font-medium text-gray-900 text-sm">{u.name}</div>
                        <div className="text-xs text-gray-500 truncate">{u.email}</div>
                      </div>
                    </div>
                    <StatusBadge status={u.role} />
                  </div>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-xs text-gray-400">{u.createdAt ? String(u.createdAt).slice(0, 10) : '—'}</span>
                    {!isSelf && (
                      u.role === 'ADMIN' ? (
                        <button onClick={() => setConfirmModal({ user: u, action: 'demote', newRole: 'USER' })}
                          className="px-3 py-1.5 text-xs font-medium rounded-lg border border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100 transition-colors">
                          Demote
                        </button>
                      ) : (
                        <button onClick={() => setConfirmModal({ user: u, action: 'promote', newRole: 'ADMIN' })}
                          className="px-3 py-1.5 text-xs font-medium rounded-lg border border-blue-200 text-blue-700 bg-blue-50 hover:bg-blue-100 transition-colors">
                          Promote
                        </button>
                      )
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
      {/* Confirmation Modal */}
      {confirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={() => !changing && setConfirmModal(null)}>
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
          <div className="relative bg-white rounded-2xl shadow-2xl max-w-md w-full p-6" onClick={e => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center ${confirmModal.action === 'promote' ? 'bg-blue-100' : 'bg-amber-100'}`}>
                {confirmModal.action === 'promote' ? (
                  <svg className="w-5 h-5 text-blue-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v6m3-3H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                ) : (
                  <svg className="w-5 h-5 text-amber-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" /></svg>
                )}
              </div>
              <div>
                <h3 className="font-semibold text-gray-900">{confirmModal.action === 'promote' ? 'Promote to Admin' : 'Demote to User'}</h3>
                <p className="text-sm text-gray-500">This action requires confirmation</p>
              </div>
            </div>
            <div className="bg-gray-50 rounded-xl p-4 mb-4">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-xs font-semibold">
                  {(confirmModal.user.name || 'U')[0].toUpperCase()}
                </div>
                <div>
                  <div className="font-medium text-gray-900 text-sm">{confirmModal.user.name}</div>
                  <div className="text-xs text-gray-500">{confirmModal.user.email}</div>
                </div>
              </div>
            </div>
            <div className="mb-6">
              {confirmModal.action === 'promote' ? (
                <div className="text-sm text-gray-600 space-y-1">
                  <p>This user will gain access to:</p>
                  <ul className="list-disc list-inside text-gray-500 ml-2 space-y-0.5">
                    <li>Admin Dashboard</li>
                    <li>User Management</li>
                    <li>Booking Management</li>
                    <li>All admin APIs</li>
                  </ul>
                </div>
              ) : (
                <div className="text-sm text-gray-600 space-y-1">
                  <p>This user will lose access to:</p>
                  <ul className="list-disc list-inside text-gray-500 ml-2 space-y-0.5">
                    <li>Admin Dashboard</li>
                    <li>All admin functionality</li>
                  </ul>
                </div>
              )}
            </div>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setConfirmModal(null)} disabled={changing}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors disabled:opacity-50">
                Cancel
              </button>
              <button onClick={() => handleRoleChange(confirmModal.user, confirmModal.newRole)} disabled={changing}
                className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors disabled:opacity-50 ${confirmModal.action === 'promote' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-amber-600 hover:bg-amber-700'}`}>
                {changing ? 'Processing...' : confirmModal.action === 'promote' ? 'Promote User' : 'Demote Admin'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ==================== AUDIT LOGS PAGE ====================
function AuditLogsPage({ data, loading }) {
  const items = data?.content || data?.data || data || [];
  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <h2 className="text-xl font-bold text-gray-900">Audit Logs</h2>
        <div className="text-sm text-gray-500">{items.length} log{items.length !== 1 ? 's' : ''}</div>
      </div>
      {loading ? <LoadingSkeleton /> : items.length === 0 ? (
        <EmptyState title="No audit logs" description="Role changes and admin actions will be recorded here." />
      ) : (
        <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/80">
                  {['Timestamp', 'Action', 'Actor', 'Target', 'Old Role', 'New Role', 'Details'].map(h => (
                    <th key={h} className="px-4 py-3 text-left font-medium text-gray-500 text-xs uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {items.map((log, i) => (
                  <tr key={log.id || i} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-4 py-3 text-gray-500 text-xs whitespace-nowrap">{log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'}</td>
                    <td className="px-4 py-3"><StatusBadge status={log.action} /></td>
                    <td className="px-4 py-3 font-medium text-gray-900">{log.actorEmail}</td>
                    <td className="px-4 py-3 text-gray-600">{log.targetUserEmail}</td>
                    <td className="px-4 py-3"><StatusBadge status={log.oldRole} /></td>
                    <td className="px-4 py-3"><StatusBadge status={log.newRole} /></td>
                    <td className="px-4 py-3 text-gray-500 text-xs max-w-[200px] truncate">{log.details || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {/* Mobile cards */}
          <div className="md:hidden divide-y divide-gray-50">
            {items.map((log, i) => (
              <div key={log.id || i} className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <StatusBadge status={log.action} />
                  <span className="text-xs text-gray-400">{log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'}</span>
                </div>
                <div className="flex items-center gap-2 text-sm mt-2">
                  <span className="text-gray-900 font-medium truncate">{log.actorEmail}</span>
                  <svg className="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" /></svg>
                  <span className="text-gray-600 truncate">{log.targetUserEmail}</span>
                </div>
                <div className="flex items-center gap-2 mt-1">
                  <StatusBadge status={log.oldRole} />
                  <span className="text-gray-400">→</span>
                  <StatusBadge status={log.newRole} />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ==================== ANALYTICS PAGE ====================
function AnalyticsPage({ data, loading }) {
  const [cancelData, setCancelData] = useState(null);

  useEffect(() => {
    adminApi.getCancellationAnalytics()
      .then(res => setCancelData(res))
      .catch(err => console.error("Failed to load cancellation analytics", err));
  }, []);

  if (loading) return <LoadingSkeleton rows={6} />;
  if (!data) return <EmptyState title="No analytics data" description="Analytics will be available once booking data exists." />;
  const byType = data.bookingsByType || {};
  const byStatus = data.bookingsByStatus || {};
  const revenueByType = data.revenueByType || {};
  const popularRoutes = data.popularRoutes || [];
  const bookingsOverTime = data.bookingsOverTime || {};

  return (
    <div>
      <h2 className="text-xl font-bold text-gray-900 mb-6">Analytics</h2>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 lg:gap-4 mb-8">
        <StatCard label="Total Revenue" value={`₹${Number(data.totalRevenue || 0).toLocaleString()}`} icon="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" color="green" />
        <StatCard label="Avg Booking" value={`₹${Number(data.averageBookingValue || 0).toLocaleString()}`} icon="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" color="blue" />
        <StatCard label="Cancellation Rate" value={`${(data.cancellationRate || 0).toFixed(1)}%`} icon="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" color="amber" />
        <StatCard label="Total Bookings" value={data.totalBookings} icon="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" color="purple" />
      </div>
      <div className="grid md:grid-cols-2 gap-6">
        {[
          { title: 'Bookings by Type', data: byType, color: 'bg-blue-500' },
          { title: 'Bookings by Status', data: byStatus, colorFn: (k) => ({ CONFIRMED: 'bg-emerald-500', COMPLETED: 'bg-emerald-500', PENDING: 'bg-amber-500', CANCELLED: 'bg-red-500', FAILED: 'bg-red-500' }[k] || 'bg-gray-500') },
        ].map(chart => (
          <div key={chart.title} className="bg-white rounded-xl border border-gray-100 p-5">
            <h3 className="font-semibold text-gray-900 mb-4 text-sm">{chart.title}</h3>
            {Object.keys(chart.data).length === 0 ? <p className="text-gray-400 text-sm">No data yet</p> : (
              <div className="space-y-2.5">
                {Object.entries(chart.data).sort((a, b) => b[1] - a[1]).map(([key, count]) => {
                  const max = Math.max(...Object.values(chart.data));
                  const barColor = chart.colorFn ? chart.colorFn(key) : chart.color;
                  return (
                    <div key={key} className="flex items-center gap-3">
                      <span className="w-24 text-xs text-gray-600 truncate">{key}</span>
                      <div className="flex-1 bg-gray-100 rounded-full h-4 overflow-hidden">
                        <div className={`${barColor} h-4 rounded-full transition-all duration-500`} style={{ width: `${(count / max) * 100}%` }} />
                      </div>
                      <span className="text-xs font-medium text-gray-900 w-8 text-right">{count}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ))}
        {[
          { title: 'Revenue by Type', data: revenueByType, format: (v) => `₹${Number(v).toLocaleString()}`, color: 'bg-emerald-500' },
          { title: 'Popular Routes', data: Object.fromEntries(popularRoutes.map(r => [r.key || r.route || r, r.value || r])), color: 'bg-indigo-500' },
        ].map(chart => (
          <div key={chart.title} className="bg-white rounded-xl border border-gray-100 p-5">
            <h3 className="font-semibold text-gray-900 mb-4 text-sm">{chart.title}</h3>
            {Object.keys(chart.data).length === 0 ? <p className="text-gray-400 text-sm">No data yet</p> : (
              <div className="space-y-2.5">
                {Object.entries(chart.data).sort((a, b) => Number(b[1]) - Number(a[1])).map(([key, value]) => {
                  const max = Math.max(...Object.values(chart.data).map(Number));
                  return (
                    <div key={key} className="flex items-center gap-3">
                      <span className="w-28 text-xs text-gray-600 truncate">{key}</span>
                      <div className="flex-1 bg-gray-100 rounded-full h-4 overflow-hidden">
                        <div className={`${chart.color} h-4 rounded-full transition-all duration-500`} style={{ width: `${(Number(value) / (max || 1)) * 100}%` }} />
                      </div>
                      <span className="text-xs font-medium text-gray-900 w-20 text-right">{chart.format ? chart.format(value) : value}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Cancellation & Refund Reason Analytics */}
      {cancelData && (
        <div className="mt-8">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Cancellation & Refund Analytics</h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
            <div className="bg-white rounded-xl border border-gray-100 p-4">
              <p className="text-xs text-gray-500">Total Cancellations</p>
              <p className="text-xl font-bold text-rose-600 mt-1">{cancelData.totalCancellations || 0}</p>
            </div>
            <div className="bg-white rounded-xl border border-gray-100 p-4">
              <p className="text-xs text-gray-500">Total Refunded Value</p>
              <p className="text-xl font-bold text-gray-900 mt-1">₹{Number(cancelData.totalRefundedAmount || 0).toLocaleString()}</p>
            </div>
            <div className="bg-white rounded-xl border border-gray-100 p-4">
              <p className="text-xs text-gray-500">Refund Records</p>
              <p className="text-xl font-bold text-purple-600 mt-1">{cancelData.totalRefunds || 0}</p>
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            {/* Predefined Reasons Chart */}
            <div className="bg-white rounded-xl border border-gray-100 p-5">
              <h4 className="font-semibold text-gray-900 mb-3 text-sm">Cancellation Reasons Breakdown</h4>
              {(!cancelData.reasonDistribution || Object.keys(cancelData.reasonDistribution).length === 0) ? (
                <p className="text-gray-400 text-sm">No cancellation reasons recorded yet</p>
              ) : (
                <div className="space-y-2.5">
                  {Object.entries(cancelData.reasonDistribution).sort((a, b) => b[1] - a[1]).map(([reason, count]) => {
                    const max = Math.max(...Object.values(cancelData.reasonDistribution));
                    const readableName = reason.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
                    return (
                      <div key={reason} className="flex items-center gap-3">
                        <span className="w-36 text-xs text-gray-600 truncate" title={readableName}>{readableName}</span>
                        <div className="flex-1 bg-gray-100 rounded-full h-4 overflow-hidden">
                          <div className="bg-rose-500 h-4 rounded-full transition-all duration-500" style={{ width: `${(count / max) * 100}%` }} />
                        </div>
                        <span className="text-xs font-semibold text-gray-900 w-8 text-right">{count}</span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Refund Type Breakdown */}
            <div className="bg-white rounded-xl border border-gray-100 p-5">
              <h4 className="font-semibold text-gray-900 mb-3 text-sm">Refund Outcomes</h4>
              <div className="space-y-3">
                <div className="flex items-center justify-between p-3 rounded-lg bg-emerald-50 border border-emerald-100">
                  <div>
                    <p className="text-xs font-semibold text-emerald-800">Full Refund</p>
                    <p className="text-[10px] text-emerald-600">100% of booking amount returned</p>
                  </div>
                  <span className="text-lg font-bold text-emerald-900">{cancelData.fullRefundCount || 0}</span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-blue-50 border border-blue-100">
                  <div>
                    <p className="text-xs font-semibold text-blue-800">Partial Refund (e.g. 50% within 24h)</p>
                    <p className="text-[10px] text-blue-600">Partial percentage refunded per policy</p>
                  </div>
                  <span className="text-lg font-bold text-blue-900">{cancelData.partialRefundCount || 0}</span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-gray-50 border border-gray-100">
                  <div>
                    <p className="text-xs font-semibold text-gray-800">No Refund</p>
                    <p className="text-[10px] text-gray-500">Non-refundable window expired</p>
                  </div>
                  <span className="text-lg font-bold text-gray-900">{cancelData.noRefundCount || 0}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {Object.keys(bookingsOverTime).length > 0 && (
        <div className="bg-white rounded-xl border border-gray-100 p-5 mt-6">
          <h3 className="font-semibold text-gray-900 mb-4 text-sm">Bookings Over Time (Last 30 Days)</h3>
          <div className="flex items-end gap-1 h-40 overflow-x-auto">
            {Object.entries(bookingsOverTime).sort((a, b) => a[0].localeCompare(b[0])).map(([date, count], i) => {
              const max = Math.max(...Object.values(bookingsOverTime));
              return (
                <div key={i} className="flex-1 min-w-[24px] flex flex-col items-center gap-1">
                  <span className="text-[10px] text-gray-500 font-medium">{count}</span>
                  <div className="w-full bg-blue-500 rounded-t transition-all duration-500" style={{ height: `${(count / max) * 120}px` }} />
                  <span className="text-[9px] text-gray-400" style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)', height: '40px' }}>
                    {date.slice(5)}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

// ==================== REWARDS PAGE ====================
function AdminRewardsPage() {
  const [userId, setUserId] = useState('');
  const [userRewards, setUserRewards] = useState(null);
  const [adjPoints, setAdjPoints] = useState('');
  const [adjReason, setAdjReason] = useState('');
  const [adjLoading, setAdjLoading] = useState(false);
  const [config, setConfig] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [msg, setMsg] = useState('');

  useEffect(() => {
    rewardsApi.getConfig().then(r => setConfig(r.data)).catch(() => {});
  }, []);

  const searchUser = async () => {
    if (!userId) return;
    setSearchLoading(true);
    try {
      const res = await rewardsApi.adminGetUserRewards(userId);
      setUserRewards(res.data);
      setMsg('');
    } catch {
      setMsg('User not found or no rewards account');
      setUserRewards(null);
    } finally {
      setSearchLoading(false);
    }
  };

  const adjustPoints = async (isAdd) => {
    if (!userId || !adjPoints || !adjReason) return;
    setAdjLoading(true);
    try {
      await rewardsApi.adminAdjustPoints(userId, Number(adjPoints), adjReason, isAdd);
      setMsg(isAdd ? 'Points added successfully' : 'Points deducted successfully');
      setAdjPoints('');
      setAdjReason('');
      searchUser();
    } catch (err) {
      setMsg(err.response?.data?.message || 'Adjustment failed');
    } finally {
      setAdjLoading(false);
    }
  };

  return (
    <div>
      <h2 className="text-xl font-bold text-gray-900 mb-6">Rewards Management</h2>

      {/* Config */}
      {config && (
        <div className="bg-white rounded-xl border border-gray-100 p-5 mb-6">
          <h3 className="font-semibold text-gray-900 mb-3">Rewards Configuration</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div><span className="text-gray-500">Points/₹100:</span> <span className="font-medium ml-1">{config.pointsPerHundredRupees}</span></div>
            <div><span className="text-gray-500">Gold threshold:</span> <span className="font-medium ml-1">{config.goldTierThreshold?.toLocaleString()}</span></div>
            <div><span className="text-gray-500">Platinum threshold:</span> <span className="font-medium ml-1">{config.platinumTierThreshold?.toLocaleString()}</span></div>
            <div><span className="text-gray-500">Expiry days:</span> <span className="font-medium ml-1">{config.pointsExpiryDays}</span></div>
            <div><span className="text-gray-500">Redemption rate:</span> <span className="font-medium ml-1">{config.pointsPerRupeeRedemption} pts = ₹1</span></div>
            <div><span className="text-gray-500">Min redemption:</span> <span className="font-medium ml-1">{config.minRedemptionPoints} pts</span></div>
            <div><span className="text-gray-500">Max redemption %:</span> <span className="font-medium ml-1">{config.maxRedemptionPercent}%</span></div>
          </div>
        </div>
      )}

      {/* User search */}
      <div className="bg-white rounded-xl border border-gray-100 p-5 mb-6">
        <h3 className="font-semibold text-gray-900 mb-3">User Rewards Lookup</h3>
        <div className="flex gap-2 mb-4">
          <input value={userId} onChange={e => setUserId(e.target.value)} placeholder="User ID"
            className="px-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-48" onKeyDown={e => e.key === 'Enter' && searchUser()} />
          <button onClick={searchUser} disabled={searchLoading || !userId}
            className="px-5 py-2.5 bg-blue-600 text-white rounded-xl text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors">
            {searchLoading ? 'Searching...' : 'Search'}
          </button>
        </div>
        {msg && <p className="text-sm text-blue-600 mb-3">{msg}</p>}

        {userRewards && (
          <div className="border-t border-gray-100 pt-4">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{userRewards.pointsBalance?.toLocaleString()}</p>
                <p className="text-xs text-gray-500">Balance</p>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{userRewards.tier}</p>
                <p className="text-xs text-gray-500">Tier</p>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{userRewards.lifetimePointsEarned?.toLocaleString()}</p>
                <p className="text-xs text-gray-500">Lifetime Earned</p>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{userRewards.lifetimePointsRedeemed?.toLocaleString()}</p>
                <p className="text-xs text-gray-500">Redeemed</p>
              </div>
            </div>

            {/* Admin adjustment */}
            <div className="border-t border-gray-100 pt-4">
              <h4 className="font-medium text-gray-900 mb-2">Adjust Points</h4>
              <div className="flex flex-col sm:flex-row gap-2">
                <input type="number" value={adjPoints} onChange={e => setAdjPoints(e.target.value)} placeholder="Points"
                  className="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 w-32" />
                <input value={adjReason} onChange={e => setAdjReason(e.target.value)} placeholder="Reason (required)"
                  className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500" />
                <button onClick={() => adjustPoints(true)} disabled={adjLoading || !adjPoints || !adjReason}
                  className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 disabled:opacity-50 transition-colors">
                  {adjLoading ? '...' : 'Add'}
                </button>
                <button onClick={() => adjustPoints(false)} disabled={adjLoading || !adjPoints || !adjReason}
                  className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors">
                  {adjLoading ? '...' : 'Deduct'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ==================== COUPONS PAGE ====================
function CouponsPage({ data, loading, page, setPage, setOffers }) {
  const items = data?.content || data?.data || data || [];
  const totalPages = data?.totalPages || 1;
  const [showForm, setShowForm] = useState(false);
  const [editingCoupon, setEditingCoupon] = useState(null);
  const [form, setForm] = useState({ code: '', description: '', discountType: 'PERCENTAGE', discountValue: '', maxDiscount: '', minBookingAmount: '', startDate: '', expiryDate: '', usageLimit: 0, perUserLimit: 1, active: true, applicableModule: 'ALL' });
  const [toast, setToast] = useState(null);

  const handleSave = async () => {
    try {
      const payload = { ...form, discountValue: parseFloat(form.discountValue), maxDiscount: form.maxDiscount ? parseFloat(form.maxDiscount) : null, minBookingAmount: form.minBookingAmount ? parseFloat(form.minBookingAmount) : null, usageLimit: parseInt(form.usageLimit) || 0, perUserLimit: parseInt(form.perUserLimit) || 1, startDate: form.startDate ? new Date(form.startDate).toISOString() : new Date().toISOString(), expiryDate: form.expiryDate ? new Date(form.expiryDate).toISOString() : new Date(Date.now() + 30*86400000).toISOString() };
      if (editingCoupon) {
        await adminApi.updateCoupon(editingCoupon.id, payload);
        setToast({ type: 'success', message: 'Coupon updated' });
      } else {
        await adminApi.createCoupon(payload);
        setToast({ type: 'success', message: 'Coupon created' });
      }
      setShowForm(false); setEditingCoupon(null);
      setForm({ code: '', description: '', discountType: 'PERCENTAGE', discountValue: '', maxDiscount: '', minBookingAmount: '', startDate: '', expiryDate: '', usageLimit: 0, perUserLimit: 1, active: true, applicableModule: 'ALL' });
      setOffers(null);
    } catch (err) {
      setToast({ type: 'error', message: err.response?.data?.message || 'Failed' });
    }
    setTimeout(() => setToast(null), 4000);
  };

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <h2 className="text-xl font-bold text-gray-900">Offers & Coupons</h2>
        <button onClick={() => { setEditingCoupon(null); setShowForm(true); }} className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
          Create Coupon
        </button>
      </div>
      {toast && (
        <div className={`mb-4 px-4 py-3 rounded-xl text-sm font-medium flex items-center gap-2 ${toast.type === 'success' ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-red-50 text-red-700 border border-red-200'}`} style={{ animation: 'slideIn 0.3s ease-out' }}>
          {toast.type === 'success' ? '✓' : '✕'} {toast.message}
        </div>
      )}
      {/* Form Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={() => setShowForm(false)}>
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
          <div className="relative bg-white rounded-2xl shadow-2xl max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">{editingCoupon ? 'Edit Coupon' : 'Create Coupon'}</h3>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Code *</label><input value={form.code} onChange={e => setForm({...form, code: e.target.value.toUpperCase()})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" placeholder="WEEKEND20" /></div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Type *</label><select value={form.discountType} onChange={e => setForm({...form, discountType: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm"><option value="PERCENTAGE">Percentage</option><option value="FIXED">Fixed Amount</option></select></div>
              </div>
              <div><label className="block text-xs font-medium text-gray-500 mb-1">Description</label><input value={form.description} onChange={e => setForm({...form, description: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" placeholder="Weekend sale discount" /></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Discount Value *</label><input type="number" value={form.discountValue} onChange={e => setForm({...form, discountValue: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" placeholder={form.discountType === 'PERCENTAGE' ? '20' : '500'} /></div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Max Discount</label><input type="number" value={form.maxDiscount} onChange={e => setForm({...form, maxDiscount: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" placeholder="1000" /></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Min Booking ₹</label><input type="number" value={form.minBookingAmount} onChange={e => setForm({...form, minBookingAmount: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" placeholder="1000" /></div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Module</label><select value={form.applicableModule} onChange={e => setForm({...form, applicableModule: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm"><option value="ALL">All</option><option value="FLIGHT">Flights</option><option value="HOTEL">Hotels</option><option value="HOLIDAY">Holidays</option><option value="TRAIN">Trains</option><option value="BUS">Buses</option><option value="CAB">Cabs</option></select></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Start Date *</label><input type="datetime-local" value={form.startDate} onChange={e => setForm({...form, startDate: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" /></div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Expiry Date *</label><input type="datetime-local" value={form.expiryDate} onChange={e => setForm({...form, expiryDate: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" /></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Usage Limit (0=unlimited)</label><input type="number" value={form.usageLimit} onChange={e => setForm({...form, usageLimit: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" /></div>
                <div><label className="block text-xs font-medium text-gray-500 mb-1">Per User Limit</label><input type="number" value={form.perUserLimit} onChange={e => setForm({...form, perUserLimit: e.target.value})} className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" /></div>
              </div>
            </div>
            <div className="flex gap-3 justify-end mt-6">
              <button onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg">Cancel</button>
              <button onClick={handleSave} disabled={!form.code || !form.discountValue} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg disabled:opacity-50">{editingCoupon ? 'Update' : 'Create'}</button>
            </div>
          </div>
        </div>
      )}
      {/* Table */}
      {loading ? <LoadingSkeleton /> : items.length === 0 ? (
        <EmptyState title="No coupons yet" description="Create your first coupon to offer discounts." action={<button onClick={() => setShowForm(true)} className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium">Create Coupon</button>} />
      ) : (
        <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-gray-100 bg-gray-50/80">
                {['Code', 'Description', 'Discount', 'Min Amount', 'Module', 'Usage', 'Expiry', 'Status', 'Actions'].map(h => (
                  <th key={h} className="px-3 py-3 text-left font-medium text-gray-500 text-xs uppercase tracking-wider">{h}</th>
                ))}
              </tr></thead>
              <tbody className="divide-y divide-gray-50">
                {items.map((c, i) => {
                  const isExpired = new Date(c.expiryDate) < new Date();
                  return (
                    <tr key={c.id || i} className="hover:bg-gray-50/50">
                      <td className="px-3 py-3 font-mono font-medium text-gray-900 text-xs">{c.code}</td>
                      <td className="px-3 py-3 text-gray-600 text-xs max-w-[200px] truncate">{c.description || '—'}</td>
                      <td className="px-3 py-3"><span className="text-xs font-medium text-blue-700 bg-blue-50 px-2 py-0.5 rounded">{c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `₹${c.discountValue}`}</span></td>
                      <td className="px-3 py-3 text-gray-600 text-xs">{c.minBookingAmount ? `₹${c.minBookingAmount}` : '—'}</td>
                      <td className="px-3 py-3"><span className="text-xs font-medium text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">{c.applicableModule}</span></td>
                      <td className="px-3 py-3 text-gray-600 text-xs">{c.usedCount}{c.usageLimit > 0 ? `/${c.usageLimit}` : ''}</td>
                      <td className="px-3 py-3 text-xs">
                        {isExpired ? <span className="text-red-600 font-medium">Expired</span> : <span className="text-gray-500">{c.expiryDate ? String(c.expiryDate).slice(0, 10) : '—'}</span>}
                      </td>
                      <td className="px-3 py-3"><StatusBadge status={c.active ? 'ACTIVE' : 'CANCELLED'} /></td>
                      <td className="px-3 py-3">
                        <div className="flex items-center gap-1.5">
                          <button onClick={() => { setEditingCoupon(c); setForm({ code: c.code, description: c.description || '', discountType: c.discountType, discountValue: String(c.discountValue), maxDiscount: c.maxDiscount ? String(c.maxDiscount) : '', minBookingAmount: c.minBookingAmount ? String(c.minBookingAmount) : '', startDate: c.startDate ? String(c.startDate).slice(0, 16) : '', expiryDate: c.expiryDate ? String(c.expiryDate).slice(0, 16) : '', usageLimit: c.usageLimit || 0, perUserLimit: c.perUserLimit || 1, active: c.active, applicableModule: c.applicableModule || 'ALL' }); setShowForm(true); }} className="px-2 py-1 text-xs text-blue-700 bg-blue-50 rounded hover:bg-blue-100">Edit</button>
                          <button onClick={async () => { if (confirm('Delete this coupon?')) { await adminApi.deleteCoupon(c.id); setOffers(null); } }} className="px-2 py-1 text-xs text-red-700 bg-red-50 rounded hover:bg-red-100">Delete</button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {/* Mobile cards */}
          <div className="md:hidden divide-y divide-gray-50">
            {items.map((c, i) => (
              <div key={c.id || i} className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-mono font-medium text-gray-900 text-sm">{c.code}</span>
                  <StatusBadge status={c.active ? 'ACTIVE' : 'CANCELLED'} />
                </div>
                <div className="text-xs text-gray-500 mb-1">{c.discountType === 'PERCENTAGE' ? `${c.discountValue}% off` : `₹${c.discountValue} off`} · {c.applicableModule}</div>
                <div className="text-xs text-gray-400 mb-2">Used: {c.usedCount}{c.usageLimit > 0 ? `/${c.usageLimit}` : ''} · Expires: {c.expiryDate ? String(c.expiryDate).slice(0, 10) : '—'}</div>
                <div className="flex items-center gap-2">
                  <button onClick={() => { setEditingCoupon(c); setForm({ code: c.code, description: c.description || '', discountType: c.discountType, discountValue: String(c.discountValue), maxDiscount: c.maxDiscount ? String(c.maxDiscount) : '', minBookingAmount: c.minBookingAmount ? String(c.minBookingAmount) : '', startDate: c.startDate ? String(c.startDate).slice(0, 16) : '', expiryDate: c.expiryDate ? String(c.expiryDate).slice(0, 16) : '', usageLimit: c.usageLimit || 0, perUserLimit: c.perUserLimit || 1, active: c.active, applicableModule: c.applicableModule || 'ALL' }); setShowForm(true); }} className="px-3 py-1.5 text-xs font-medium rounded-lg border border-blue-200 text-blue-700 bg-blue-50">Edit</button>
                  <button onClick={async () => { if (confirm('Delete this coupon?')) { await adminApi.deleteCoupon(c.id); setOffers(null); } }} className="px-3 py-1.5 text-xs font-medium rounded-lg border border-red-200 text-red-700 bg-red-50">Delete</button>
                </div>
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={totalPages} setPage={setPage} />
        </div>
      )}
    </div>
  );
}

// ==================== COMING SOON PAGE ====================
function ComingSoonPage({ title, description }) {
  return (
    <div>
      <h2 className="text-xl font-bold text-gray-900 mb-6">{title}</h2>
      <EmptyState
        title={title}
        description={description || 'This section is coming soon. Functionality will be available in a future update.'}
        action={
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-blue-50 text-blue-700 rounded-lg text-sm font-medium">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            Coming Soon
          </div>
        }
      />
    </div>
  );
}

// ==================== MAIN ADMIN PANEL ====================
export default function AdminPanel() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [stats, setStats] = useState(null);
  const [bookings, setBookings] = useState(null);
  const [users, setUsers] = useState(null);
  const [flights, setFlights] = useState(null);
  const [hotels, setHotels] = useState(null);
  const [holidays, setHolidays] = useState(null);
  const [trains, setTrains] = useState(null);
  const [buses, setBuses] = useState(null);
  const [cabs, setCabs] = useState(null);
  const [payments, setPayments] = useState(null);
  const [refunds, setRefunds] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [auditLogs, setAuditLogs] = useState(null);
  const [offers, setOffers] = useState(null);
  const [rewardsData, setRewardsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ status: '', search: '' });
  const [page, setPage] = useState(0);
  const sidebarRef = useRef(null);

  // Get current user from localStorage
  const currentUser = (() => { try { return JSON.parse(localStorage.getItem('user') || '{}'); } catch { return {}; } })();

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        switch (activeTab) {
          case 'dashboard': {
            const [d, summary] = await Promise.all([adminApi.getDashboard(), adminApi.getAnalyticsSummary()]);
            if (!cancelled) setStats({ ...d, ...summary });
            break;
          }
          case 'bookings': {
            const d = await adminApi.getBookings({ page, status: filters.status, search: filters.search });
            if (!cancelled) setBookings(d);
            break;
          }
          case 'users': {
            const d = await adminApi.getUsers({ page });
            if (!cancelled) setUsers(d);
            break;
          }
          case 'flights': {
            const d = await adminApi.getFlights({ page });
            if (!cancelled) setFlights(d);
            break;
          }
          case 'hotels': {
            const d = await adminApi.getHotels({ page });
            if (!cancelled) setHotels(d);
            break;
          }
          case 'holidays': {
            const d = await adminApi.getHolidays({ page });
            if (!cancelled) setHolidays(d);
            break;
          }
          case 'trains': {
            const d = await adminApi.getTrains({ page });
            if (!cancelled) setTrains(d);
            break;
          }
          case 'buses': {
            const d = await adminApi.getBuses({ page });
            if (!cancelled) setBuses(d);
            break;
          }
          case 'cabs': {
            const d = await adminApi.getCabs({ page });
            if (!cancelled) setCabs(d);
            break;
          }
          case 'payments': {
            const d = await adminApi.getPayments({ page });
            if (!cancelled) setPayments(d);
            break;
          }
          case 'refunds': {
            const d = await adminApi.getRefunds({ page });
            if (!cancelled) setRefunds(d);
            break;
          }
          case 'reviews': {
            break;
          }
          case 'analytics': {
            const d = await adminApi.getAnalytics();
            if (!cancelled) setAnalytics(d);
            break;
          }
          case 'auditlogs': {
            const d = await adminApi.getAuditLogs({ page });
            if (!cancelled) setAuditLogs(d);
            break;
          }
          case 'offers': {
            const d = await adminApi.getCoupons({ page });
            if (!cancelled) setOffers(d);
            break;
          }
          case 'rewards': {
            // Rewards config loaded separately
            break;
          }
        }
      } catch (err) {
        console.error('Admin load error:', err);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [activeTab, page, filters]);

  // Click outside to close sidebar
  useEffect(() => {
    const handler = (e) => {
      if (sidebarRef.current && !sidebarRef.current.contains(e.target)) setSidebarOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Escape key to close sidebar
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') setSidebarOpen(false); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, []);

  const switchTab = (tab) => {
    setActiveTab(tab);
    setSidebarOpen(false);
    setPage(0);
    setFilters({ status: '', search: '' });
  };

  const pageTitle = SIDEBAR_SECTIONS.flatMap(s => s.items).find(i => i.id === activeTab)?.label || 'Dashboard';

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard': return <AdminDashboard stats={stats} loading={loading} />;
      case 'bookings': return <BookingsPage data={bookings} loading={loading} filters={filters} setFilters={setFilters} page={page} setPage={setPage} />;
      case 'users': return <UsersPage data={users} loading={loading} currentUserEmail={currentUser.email} onRoleChange={() => { setUsers(null); }} />;
      case 'flights': return <GenericTable title="Flights" data={flights} loading={loading} page={page} setPage={setPage}
        headerExtra={<button onClick={async () => { if (confirm('Export flights to CSV?')) { const r = await adminApi.exportData('flights'); if (r.data) { const blob = new Blob([r.data], {type:'text/csv'}); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href=url; a.download='flights.csv'; a.click(); }}}} className="px-3 py-1.5 rounded-lg text-sm font-medium bg-gray-100 text-gray-600 hover:bg-gray-200 flex items-center gap-1.5"><svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>Export</button>}
        actions={(f) => (
          <div className="flex items-center gap-1.5">
            <button onClick={async () => { await adminApi.toggleFlight(f.id); setFlights(null); }} className={`px-2.5 py-1 text-xs font-medium rounded-lg transition-colors ${f.active ? 'bg-amber-50 text-amber-700 hover:bg-amber-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'}`}>{f.active ? 'Deactivate' : 'Activate'}</button>
          </div>
        )}
        columns={[
          { label: 'Flight', render: (f) => <span className="font-medium text-gray-900">{f.flightNumber}</span> },
          { label: 'Airline', render: (f) => f.airlineName || '—' },
          { label: 'Route', render: (f) => `${f.originCode} → ${f.destinationCode}` },
          { label: 'Departure', render: (f) => f.departureTime ? String(f.departureTime).slice(11, 16) : '—' },
          { label: 'Price', render: (f) => `₹${Number(f.basePrice || 0).toLocaleString()}` },
          { label: 'Status', render: (f) => <StatusBadge status={f.active ? 'ACTIVE' : 'CANCELLED'} /> },
        ]} />;
      case 'hotels': return <GenericTable title="Hotels" data={hotels} loading={loading} page={page} setPage={setPage}
        actions={(h) => (
          <button onClick={async () => { await adminApi.toggleHotel(h.id); setHotels(null); }} className={`px-2.5 py-1 text-xs font-medium rounded-lg transition-colors ${h.active !== false ? 'bg-amber-50 text-amber-700 hover:bg-amber-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'}`}>{h.active !== false ? 'Deactivate' : 'Activate'}</button>
        )}
        columns={[
          { label: 'Hotel', render: (h) => <span className="font-medium text-gray-900">{h.name}</span> },
          { label: 'City', key: 'city' },
          { label: 'Rating', render: (h) => h.starRating ? `${'★'.repeat(h.starRating)} ${h.starRating}` : '—' },
          { label: 'Status', render: (h) => <StatusBadge status={h.active !== false ? 'ACTIVE' : 'CANCELLED'} /> },
        ]} />;
      case 'holidays': return <GenericTable title="Holiday Packages" data={holidays} loading={loading} page={page} setPage={setPage}
        columns={[
          { label: 'Package', render: (h) => <span className="font-medium text-gray-900">{h.title || h.name}</span> },
          { label: 'Destination', key: 'destination' },
          { label: 'Duration', render: (h) => h.durationDays ? `${h.durationDays} days` : '—' },
          { label: 'Price', render: (h) => `₹${Number(h.price || 0).toLocaleString()}` },
          { label: 'Type', render: (h) => h.packageType || h.tripType || '—' },
        ]} />;
      case 'trains': return <GenericTable title="Trains" data={trains} loading={loading} page={page} setPage={setPage}
        actions={(t) => (
          <button onClick={async () => { await adminApi.updateTrain(t.id, {...t, active: t.active === false}); setTrains(null); }} className={`px-2.5 py-1 text-xs font-medium rounded-lg transition-colors ${t.active !== false ? 'bg-amber-50 text-amber-700 hover:bg-amber-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'}`}>{t.active !== false ? 'Deactivate' : 'Activate'}</button>
        )}
        columns={[
          { label: 'Train', render: (t) => <span className="font-medium text-gray-900">{t.trainName}</span> },
          { label: 'Number', key: 'trainNumber' },
          { label: 'Route', render: (t) => `${t.originStation} → ${t.destinationStation}` },
          { label: 'Departure', key: 'departureTime' },
          { label: 'Status', render: (t) => <StatusBadge status={t.active !== false ? 'ACTIVE' : 'CANCELLED'} /> },
        ]} />;
      case 'buses': return <GenericTable title="Buses" data={buses} loading={loading} page={page} setPage={setPage}
        actions={(b) => (
          <button onClick={async () => { await adminApi.updateBus(b.id, {...b, active: b.active === false}); setBuses(null); }} className={`px-2.5 py-1 text-xs font-medium rounded-lg transition-colors ${b.active !== false ? 'bg-amber-50 text-amber-700 hover:bg-amber-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'}`}>{b.active !== false ? 'Deactivate' : 'Activate'}</button>
        )}
        columns={[
          { label: 'Bus', render: (b) => <span className="font-medium text-gray-900">{b.busName || b.busNumber}</span> },
          { label: 'Operator', key: 'operator' },
          { label: 'Type', key: 'busType' },
          { label: 'Route', render: (b) => `${b.origin} → ${b.destination}` },
          { label: 'Price', render: (b) => `₹${Number(b.basePrice || 0).toLocaleString()}` },
          { label: 'Status', render: (b) => <StatusBadge status={b.active !== false ? 'ACTIVE' : 'CANCELLED'} /> },
        ]} />;
      case 'cabs': return <GenericTable title="Cabs" data={cabs} loading={loading} page={page} setPage={setPage}
        actions={(c) => (
          <button onClick={async () => { await adminApi.updateCab(c.id, {...c, active: c.active === false}); setCabs(null); }} className={`px-2.5 py-1 text-xs font-medium rounded-lg transition-colors ${c.active !== false ? 'bg-amber-50 text-amber-700 hover:bg-amber-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'}`}>{c.active !== false ? 'Deactivate' : 'Activate'}</button>
        )}
        columns={[
          { label: 'Vehicle', render: (c) => <span className="font-medium text-gray-900">{c.vehicleName || c.vehicleType}</span> },
          { label: 'Type', key: 'vehicleType' },
          { label: 'Capacity', render: (c) => `${c.capacity || '—'} seats` },
          { label: 'Price/km', render: (c) => `₹${Number(c.pricePerKm || 0).toLocaleString()}` },
          { label: 'Status', render: (c) => <StatusBadge status={c.active !== false ? 'ACTIVE' : 'CANCELLED'} /> },
        ]} />;
      case 'payments': return <GenericTable title="Payments" data={payments} loading={loading} page={page} setPage={setPage}
        columns={[
          { label: 'Payment ID', render: (p) => <span className="font-mono text-xs text-gray-700">{p.paymentId || '—'}</span> },
          { label: 'Booking', render: (p) => p.bookingReference || '—' },
          { label: 'Customer', render: (p) => <span className="text-gray-900">{p.userName || '—'}</span> },
          { label: 'Amount', render: (p) => `₹${Number(p.amount || 0).toLocaleString()}` },
          { label: 'Method', key: 'paymentMethod' },
          { label: 'Status', render: (p) => <StatusBadge status={p.status || 'PENDING'} /> },
          { label: 'Date', render: (p) => p.createdAt ? String(p.createdAt).slice(0, 10) : '—' },
        ]} />;
      case 'refunds': return <GenericTable title="Refunds" data={refunds} loading={loading} page={page} setPage={setPage}
        columns={[
          { label: 'Booking', render: (r) => r.bookingReference || r.booking?.reference || r.bookingId || '—' },
          { label: 'Customer', render: (r) => <span className="text-gray-900">{r.userName || r.user?.name || '—'}</span> },
          { label: 'Amount', render: (r) => `₹${Number(r.refundAmount || 0).toLocaleString()}` },
          { label: 'Reason', render: (r) => <span className="line-clamp-1 max-w-[200px]">{r.cancellationReason || r.reason || '—'}</span> },
          { label: 'Status', render: (r) => <StatusBadge status={r.status || 'PENDING'} /> },
          { label: 'Date', render: (r) => r.createdAt ? String(r.createdAt).slice(0, 10) : '—' },
        ]} />;
      case 'reviews': return <ReviewModerationQueue />;
      case 'analytics': return <AnalyticsPage data={analytics} loading={loading} />;
      case 'auditlogs': return <AuditLogsPage data={auditLogs} loading={loading} />;
      case 'offers': return <CouponsPage data={offers} loading={loading} page={page} setPage={setPage} setOffers={setOffers} />;
      case 'rewards': return <AdminRewardsPage />;
      default: return <EmptyState title="Page not found" />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Mobile overlay */}
      {sidebarOpen && <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-40 lg:hidden transition-opacity" onClick={() => setSidebarOpen(false)} />}

      {/* Sidebar */}
      <aside ref={sidebarRef}
        className={`fixed lg:sticky top-0 left-0 z-50 h-screen w-64 bg-white border-r border-gray-200 flex flex-col transition-transform duration-300 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        {/* Logo */}
        <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center shadow-sm">
              <span className="text-white font-bold text-sm">V</span>
            </div>
            <div>
              <span className="font-bold text-gray-900 text-sm">Voyara</span>
              <span className="text-[10px] text-gray-400 block -mt-0.5">Admin Console</span>
            </div>
          </div>
          <button onClick={() => setSidebarOpen(false)} className="lg:hidden p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
            <Icon d="M6 18L18 6M6 6l12 12" className="w-5 h-5 text-gray-500" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-3 px-3">
          {SIDEBAR_SECTIONS.map((section, si) => (
            <div key={si} className="mb-3">
              <p className="px-3 py-1.5 text-[10px] font-bold text-gray-400 uppercase tracking-widest">{section.title}</p>
              {section.items.map(item => {
                const isActive = activeTab === item.id;
                return (
                  <button key={item.id} onClick={() => switchTab(item.id)}
                    className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 mb-0.5 group ${isActive ? 'bg-blue-50 text-blue-700 shadow-sm' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'}`}>
                    <Icon d={item.icon} className={`w-[18px] h-[18px] flex-shrink-0 ${isActive ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
                    <span className="flex-1 text-left">{item.label}</span>
                    {item.comingSoon && (
                      <span className="text-[9px] font-medium text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">Soon</span>
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        {/* Admin User */}
        <div className="px-4 py-3 border-t border-gray-100">
          <div className="flex items-center gap-2.5 mb-3">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center text-white text-xs font-bold shadow-sm">
              {(currentUser.name || 'A')[0].toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-gray-900 truncate">{currentUser.name || 'Admin'}</p>
              <p className="text-[10px] text-gray-400 truncate">{currentUser.email || ''}</p>
            </div>
          </div>
          <button onClick={() => navigate('/')} className="w-full text-sm text-gray-500 hover:text-gray-700 flex items-center gap-2 py-1.5 transition-colors">
            <Icon d="M10 19l-7-7m0 0l7-7m-7 7h18" className="w-4 h-4" />
            Back to Site
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 min-w-0">
        {/* Top Header */}
        <header className="sticky top-0 z-30 bg-white/80 backdrop-blur-md border-b border-gray-200 px-4 sm:px-6 py-3 flex items-center gap-4">
          <button onClick={() => setSidebarOpen(true)} className="lg:hidden p-2 rounded-lg hover:bg-gray-100 transition-colors -ml-1">
            <Icon d="M4 6h16M4 12h16M4 18h16" className="w-5 h-5 text-gray-600" />
          </button>
          <div className="flex-1">
            <h1 className="text-lg font-semibold text-gray-900">{pageTitle}</h1>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2 text-xs text-gray-500">
              <div className="w-2 h-2 rounded-full bg-emerald-500" />
              <span>Online</span>
            </div>
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center text-white text-xs font-bold shadow-sm">
              {(currentUser.name || 'A')[0].toUpperCase()}
            </div>
          </div>
        </header>

        {/* Content */}
        <main className="p-4 sm:p-6">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}
