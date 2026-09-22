import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import NotificationBell from '../NotificationBell';
import {
  FlightIcon,
  HotelIcon,
  HolidayIcon,
  TrainIcon,
  BusIcon,
  CabIcon,
  UserIcon,
  LuggageIcon,
  AwardIcon,
  UsersIcon,
  ShieldIcon,
  LogOutIcon,
  CompassIcon,
  BellIcon,
} from '../common/Icons';

const NAV_ITEMS = [
  { label: 'Flights', to: '/', icon: FlightIcon },
  { label: 'Hotels', to: '/hotels', icon: HotelIcon },
  { label: 'Holidays', to: '/holidays', icon: HolidayIcon },
  { label: 'Trains', to: '/trains', icon: TrainIcon },
  { label: 'Buses', to: '/buses', icon: BusIcon },
  { label: 'Cabs', to: '/cabs', icon: CabIcon },
];

export default function Navbar() {
  const { user, isAuthenticated, logout, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const dropdownRef = useRef(null);
  const buttonRef = useRef(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => setMobileMenuOpen(false), [location.pathname]);
  useEffect(() => setDropdownOpen(false), [location.pathname]);

  useEffect(() => {
    if (!dropdownOpen) return;
    const handler = (e) => {
      if (e.key === 'Escape') {
        setDropdownOpen(false);
        buttonRef.current?.focus();
      }
      if (
        e.target instanceof HTMLElement &&
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target) &&
        !buttonRef.current?.contains(e.target)
      ) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    document.addEventListener('keydown', handler);
    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('keydown', handler);
    };
  }, [dropdownOpen]);

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/' || location.pathname.startsWith('/flights');
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  const handleLogout = useCallback(async () => {
    setDropdownOpen(false);
    await logout();
    navigate('/');
  }, [logout, navigate]);

  const handleNav = useCallback((path) => {
    setDropdownOpen(false);
    setMobileMenuOpen(false);
    navigate(path);
  }, [navigate]);

  const displayName = user?.name || 'Traveler';
  const displayEmail = user?.email || '';
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <>
      <nav
        className={`sticky top-0 z-50 transition-all duration-200 ${
          scrolled
            ? 'bg-white/95 backdrop-blur-md shadow-[0_4px_20px_-4px_rgba(9,19,34,0.08)] border-b border-slate-200/80'
            : 'bg-white/90 backdrop-blur-sm border-b border-slate-200/60'
        }`}
      >
        <div className="section-shell">
          <div className="flex h-16 items-center justify-between gap-3">
            {/* Logo */}
            <Link to="/" className="flex shrink-0 items-center gap-2.5 group touch-target">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-primary-dark text-white font-black text-lg shadow-md shadow-primary/25 transition-transform duration-200 group-hover:scale-105">
                V
              </div>
              <div className="flex flex-col">
                <span className="text-xl font-black tracking-tight text-slate-900 leading-none">Voyara</span>
                <span className="text-[9px] uppercase font-bold tracking-[0.24em] text-primary">Travel Tech</span>
              </div>
            </Link>

            {/* Desktop Navigation Pills */}
            <div className="hidden lg:flex items-center gap-1 rounded-full bg-slate-100/90 p-1 border border-slate-200/60 shadow-inner">
              {NAV_ITEMS.map((item) => {
                const IconComponent = item.icon;
                const active = isActive(item.to);
                return (
                  <Link
                    key={item.label}
                    to={item.to}
                    className={`flex items-center gap-2 rounded-full px-3.5 py-1.5 text-xs font-semibold transition-all duration-200 ${
                      active
                        ? 'bg-white text-primary shadow-sm ring-1 ring-slate-200/80'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
                    }`}
                  >
                    <IconComponent className={`w-4 h-4 ${active ? 'text-primary' : 'text-slate-500'}`} />
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>

            {/* Right Controls */}
            <div className="flex items-center gap-2">
              {isAuthenticated ? (
                <>
                  <div className="hidden sm:block">
                    <NotificationBell />
                  </div>

                  <div className="relative" ref={dropdownRef}>
                    <button
                      ref={buttonRef}
                      onClick={() => setDropdownOpen(!dropdownOpen)}
                      className="flex items-center gap-2 rounded-full border border-slate-200 bg-white p-1 pr-2.5 shadow-sm transition hover:border-slate-300 focus:outline-none"
                      aria-expanded={dropdownOpen}
                      aria-haspopup="true"
                    >
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-primary to-blue-600 text-xs font-bold text-white shadow-sm">
                        {initial}
                      </div>
                      <span className="hidden lg:inline text-xs font-semibold text-slate-800 max-w-[100px] truncate">
                        {displayName}
                      </span>
                      <svg
                        className={`h-3.5 w-3.5 text-slate-400 transition-transform duration-200 ${
                          dropdownOpen ? 'rotate-180' : ''
                        }`}
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>

                    {/* Dropdown Menu */}
                    <div
                      className={`absolute right-0 top-full mt-2 w-72 origin-top-right rounded-2xl border border-slate-200 bg-white p-2 shadow-2xl transition-all duration-200 ${
                        dropdownOpen
                          ? 'scale-100 opacity-100'
                          : 'pointer-events-none scale-95 opacity-0'
                      }`}
                    >
                      <div className="border-b border-slate-100 px-3 py-3 bg-slate-50/70 rounded-xl mb-1.5">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary text-sm font-bold text-white shadow-sm">
                            {initial}
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-bold text-slate-900">{displayName}</p>
                            <p className="truncate text-xs text-slate-500">{displayEmail}</p>
                          </div>
                        </div>
                      </div>

                      <div className="space-y-0.5 py-1">
                        <button
                          onClick={() => handleNav('/dashboard')}
                          className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-primary transition-colors"
                        >
                          <LuggageIcon className="w-4 h-4 text-slate-400" />
                          <span>My Trips</span>
                        </button>
                        <button
                          onClick={() => handleNav('/profile')}
                          className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-primary transition-colors"
                        >
                          <UserIcon className="w-4 h-4 text-slate-400" />
                          <span>Profile & Preferences</span>
                        </button>
                        <button
                          onClick={() => handleNav('/rewards')}
                          className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-primary transition-colors"
                        >
                          <AwardIcon className="w-4 h-4 text-slate-400" />
                          <span>Voyara Rewards</span>
                        </button>
                        <button
                          onClick={() => handleNav('/group-trips')}
                          className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-primary transition-colors"
                        >
                          <UsersIcon className="w-4 h-4 text-slate-400" />
                          <span>Group Trips & Expenses</span>
                        </button>
                        <button
                          onClick={() => handleNav('/live-tracker')}
                          className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-primary transition-colors"
                        >
                          <CompassIcon className="w-4 h-4 text-slate-400" />
                          <span>Live Flight Tracker</span>
                        </button>
                      </div>

                      {isAdmin && (
                        <div className="border-t border-slate-100 pt-1.5 mt-1">
                          <button
                            onClick={() => handleNav('/admin')}
                            className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-bold text-primary hover:bg-blue-50 transition-colors"
                          >
                            <ShieldIcon className="w-4 h-4 text-primary" />
                            <span>Admin Portal</span>
                          </button>
                        </div>
                      )}

                      <div className="border-t border-slate-100 pt-1.5 mt-1">
                        <button
                          onClick={handleLogout}
                          className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-left text-xs font-semibold text-red-600 hover:bg-red-50 transition-colors"
                        >
                          <LogOutIcon className="w-4 h-4 text-red-500" />
                          <span>Sign out</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <div className="hidden sm:flex items-center gap-2">
                  <Link
                    to="/login"
                    className="rounded-full px-4 py-2 text-xs font-bold text-slate-700 hover:text-slate-900 transition hover:bg-slate-100/70"
                  >
                    Log in
                  </Link>
                  <Link to="/register" className="travel-button-primary px-4 py-2 text-xs">
                    Sign up
                  </Link>
                </div>
              )}

              {/* Hamburger Button for Mobile */}
              <button
                className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 lg:hidden touch-target"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                aria-label="Toggle navigation menu"
              >
                <div className="flex w-4 flex-col gap-1">
                  <span
                    className={`block h-0.5 rounded-full bg-current transition-all duration-200 ${
                      mobileMenuOpen ? 'translate-y-1.5 rotate-45' : ''
                    }`}
                  />
                  <span
                    className={`block h-0.5 rounded-full bg-current transition-all duration-200 ${
                      mobileMenuOpen ? 'opacity-0' : ''
                    }`}
                  />
                  <span
                    className={`block h-0.5 rounded-full bg-current transition-all duration-200 ${
                      mobileMenuOpen ? '-translate-y-1.5 -rotate-45' : ''
                    }`}
                  />
                </div>
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Mobile Backdrop */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/60 backdrop-blur-sm lg:hidden transition-opacity"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Mobile Drawer */}
      <aside
        className={`fixed right-0 top-0 z-50 h-full w-[84vw] max-w-[340px] overflow-y-auto bg-white shadow-2xl transition-transform duration-300 ease-out lg:hidden ${
          mobileMenuOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="p-5 flex flex-col min-h-full">
          <div className="mb-5 flex items-center justify-between pb-4 border-b border-slate-100">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary text-white font-black text-sm">
                V
              </div>
              <span className="text-base font-black text-slate-900">Voyara</span>
            </div>
            <button
              onClick={() => setMobileMenuOpen(false)}
              className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200"
              aria-label="Close menu"
            >
              ✕
            </button>
          </div>

          {isAuthenticated && (
            <div className="mb-4 rounded-2xl border border-slate-200 bg-slate-50 p-3.5">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-white font-bold text-sm">
                  {initial}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-bold text-slate-900">{displayName}</div>
                  <div className="truncate text-xs text-slate-500">{displayEmail}</div>
                </div>
              </div>
            </div>
          )}

          {/* Travel Navigation Links */}
          <div className="mb-5 space-y-1">
            <p className="px-3 text-[10px] uppercase font-bold tracking-[0.2em] text-slate-400 mb-2">Bookings</p>
            {NAV_ITEMS.map((item) => {
              const IconComponent = item.icon;
              const active = isActive(item.to);
              return (
                <button
                  key={item.label}
                  onClick={() => handleNav(item.to)}
                  className={`flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold transition-colors ${
                    active ? 'bg-blue-50 text-primary' : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <IconComponent className={`w-5 h-5 ${active ? 'text-primary' : 'text-slate-400'}`} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>

          {/* Authenticated Links */}
          {isAuthenticated ? (
            <div className="space-y-1 border-t border-slate-100 pt-4 mb-6">
              <p className="px-3 text-[10px] uppercase font-bold tracking-[0.2em] text-slate-400 mb-2">My Account</p>
              <button
                onClick={() => handleNav('/dashboard')}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <LuggageIcon className="w-5 h-5 text-slate-400" />
                <span>My Trips</span>
              </button>
              <button
                onClick={() => handleNav('/live-tracker')}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <CompassIcon className="w-5 h-5 text-slate-400" />
                <span>Live Flight Tracker</span>
              </button>
              <button
                onClick={() => handleNav('/rewards')}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <AwardIcon className="w-5 h-5 text-slate-400" />
                <span>Voyara Rewards</span>
              </button>
              <button
                onClick={() => handleNav('/group-trips')}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <UsersIcon className="w-5 h-5 text-slate-400" />
                <span>Group Trips</span>
              </button>
              <button
                onClick={() => handleNav('/notifications')}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <BellIcon className="w-5 h-5 text-slate-400" />
                <span>Notifications</span>
              </button>
              <button
                onClick={() => handleNav('/profile')}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 hover:bg-slate-50"
              >
                <UserIcon className="w-5 h-5 text-slate-400" />
                <span>Profile & Settings</span>
              </button>
              {isAdmin && (
                <button
                  onClick={() => handleNav('/admin')}
                  className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-bold text-primary hover:bg-blue-50"
                >
                  <ShieldIcon className="w-5 h-5 text-primary" />
                  <span>Admin Panel</span>
                </button>
              )}
            </div>
          ) : (
            <div className="space-y-2 border-t border-slate-100 pt-4 mt-auto">
              <button
                onClick={() => handleNav('/login')}
                className="travel-button-secondary w-full py-3 text-sm"
              >
                Log in
              </button>
              <button
                onClick={() => handleNav('/register')}
                className="travel-button-primary w-full py-3 text-sm"
              >
                Create an account
              </button>
            </div>
          )}

          {isAuthenticated && (
            <div className="mt-auto border-t border-slate-100 pt-3">
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-red-600 hover:bg-red-50"
              >
                <LogOutIcon className="w-5 h-5 text-red-500" />
                <span>Sign out</span>
              </button>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}
