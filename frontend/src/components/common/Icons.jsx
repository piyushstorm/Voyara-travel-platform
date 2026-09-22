import React from 'react';

/**
 * Unified SVG Icon System for Voyara
 * Consistent 1.75 stroke-width, rounded caps & joins, clean geometric travel iconography.
 */

function BaseIcon({ className = 'w-5 h-5', children, strokeWidth = 1.75, ...props }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`shrink-0 ${className}`}
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  );
}

export function FlightIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3.5c-.5-.5-2.5 0-4 1.5L13.5 8.5 5.3 6.7c-.8-.2-1.6.1-2 .7l-.8 1.1c-.4.6-.2 1.4.4 1.8l6.3 4.1-3.2 3.2-3.1-.8c-.5-.1-1 .1-1.3.5l-.3.4c-.4.5-.2 1.2.3 1.5l3.8 2.3c.6.4 1.3.4 1.8 0l2.3 3.8c.3.5 1 .7 1.5.3l.4-.3c.4-.3.6-.8.5-1.3l-.8-3.1 3.2-3.2 4.1 6.3c.4.6 1.2.8 1.8.4l1.1-.8c.6-.4.9-1.2.7-2z" />
    </BaseIcon>
  );
}

export function HotelIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M18 2H6a2 2 0 0 0-2 2v18h16V4a2 2 0 0 0-2-2Z" />
      <path d="M9 16h6v6H9v-6Z" />
      <path d="M8 6h.01M12 6h.01M16 6h.01M8 10h.01M12 10h.01M16 10h.01" />
    </BaseIcon>
  );
}

export function HolidayIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M13 8c0-2.76-2.46-5-5.5-5S2 5.24 2 8h11Z" />
      <path d="M13 7.14A5.82 5.82 0 0 1 16.5 6c3.04 0 5.5 2.24 5.5 5h-9" />
      <path d="M5.8 11.3 2 22h3l3.2-9M12.5 13l2.5 9h3l-3.2-9M17 14l5 8" />
    </BaseIcon>
  );
}

export function TrainIcon(props) {
  return (
    <BaseIcon {...props}>
      <rect width="16" height="16" x="4" y="3" rx="2" />
      <path d="M4 11h16M12 3v8M8 19l-3 3M16 19l3 3M8 15h.01M16 15h.01" />
    </BaseIcon>
  );
}

export function BusIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M8 6v6M16 6v6M4 12h16" />
      <rect width="18" height="15" x="3" y="4" rx="2" />
      <path d="M6 19v2M18 19v2M7 16h.01M17 16h.01" />
    </BaseIcon>
  );
}

export function CabIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M14 2H10a1 1 0 0 0-1 1v1h6V3a1 1 0 0 0-1-1ZM5 9l2-4h10l2 4M3 9h18v8H3V9ZM5 17v3M19 17v3M7 13h.01M17 13h.01" />
    </BaseIcon>
  );
}

export function ShieldIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </BaseIcon>
  );
}

export function ShieldCheckIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      <path d="m9 12 2 2 4-4" />
    </BaseIcon>
  );
}

export function ClockIcon(props) {
  return (
    <BaseIcon {...props}>
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </BaseIcon>
  );
}

export function CalendarIcon(props) {
  return (
    <BaseIcon {...props}>
      <rect width="18" height="18" x="3" y="4" rx="2" />
      <line x1="16" x2="16" y1="2" y2="6" />
      <line x1="8" x2="8" y1="2" y2="6" />
      <line x1="3" x2="21" y1="10" y2="10" />
    </BaseIcon>
  );
}

export function MapPinIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </BaseIcon>
  );
}

export function UserIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </BaseIcon>
  );
}

export function UsersIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </BaseIcon>
  );
}

export function BellIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </BaseIcon>
  );
}

export function StarIcon({ filled = false, ...props }) {
  return (
    <BaseIcon {...props}>
      <polygon
        points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
        fill={filled ? 'currentColor' : 'none'}
      />
    </BaseIcon>
  );
}

export function SparklesIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z" />
    </BaseIcon>
  );
}

export function CheckIcon(props) {
  return (
    <BaseIcon {...props}>
      <polyline points="20 6 9 17 4 12" />
    </BaseIcon>
  );
}

export function AlertCircleIcon(props) {
  return (
    <BaseIcon {...props}>
      <circle cx="12" cy="12" r="10" />
      <line x1="12" x2="12" y1="8" y2="12" />
      <line x1="12" x2="12.01" y1="16" y2="16" />
    </BaseIcon>
  );
}

export function AlertTriangleIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
      <line x1="12" x2="12" y1="9" y2="13" />
      <line x1="12" x2="12.01" y1="17" y2="17" />
    </BaseIcon>
  );
}

export function SearchIcon(props) {
  return (
    <BaseIcon {...props}>
      <circle cx="11" cy="11" r="8" />
      <line x1="21" x2="16.65" y1="21" y2="16.65" />
    </BaseIcon>
  );
}

export function ArrowRightIcon(props) {
  return (
    <BaseIcon {...props}>
      <line x1="5" x2="19" y1="12" y2="12" />
      <polyline points="12 5 19 12 12 19" />
    </BaseIcon>
  );
}

export function ChevronDownIcon(props) {
  return (
    <BaseIcon {...props}>
      <polyline points="6 9 12 15 18 9" />
    </BaseIcon>
  );
}

export function ChevronRightIcon(props) {
  return (
    <BaseIcon {...props}>
      <polyline points="9 18 15 12 9 6" />
    </BaseIcon>
  );
}

export function ArrowLeftRightIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M8 3 4 7l4 4" />
      <path d="M4 7h16" />
      <path d="m16 21 4-4-4-4" />
      <path d="M20 17H4" />
    </BaseIcon>
  );
}

export function CreditCardIcon(props) {
  return (
    <BaseIcon {...props}>
      <rect width="20" height="14" x="2" y="5" rx="2" />
      <line x1="2" x2="22" y1="10" y2="10" />
    </BaseIcon>
  );
}

export function LockIcon(props) {
  return (
    <BaseIcon {...props}>
      <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </BaseIcon>
  );
}

export function LuggageIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M6 20h0a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h0" />
      <path d="M8 18V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v14" />
      <circle cx="8" cy="20" r="2" />
      <circle cx="16" cy="20" r="2" />
    </BaseIcon>
  );
}

export function CompassIcon(props) {
  return (
    <BaseIcon {...props}>
      <circle cx="12" cy="12" r="10" />
      <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" />
    </BaseIcon>
  );
}

export function TagIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M12 2H2v10l9.29 9.29c.94.94 2.48.94 3.42 0l6.58-6.58c.94-.94.94-2.48 0-3.42L12 2Z" />
      <path d="M7 7h.01" />
    </BaseIcon>
  );
}

export function HeartIcon({ filled = false, ...props }) {
  return (
    <BaseIcon {...props}>
      <path
        d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
        fill={filled ? 'currentColor' : 'none'}
      />
    </BaseIcon>
  );
}

export function RefreshCwIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
      <path d="M21 3v5h-5" />
      <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
      <path d="M8 16H3v5" />
    </BaseIcon>
  );
}

export function AwardIcon(props) {
  return (
    <BaseIcon {...props}>
      <circle cx="12" cy="8" r="6" />
      <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11" />
    </BaseIcon>
  );
}

export function FilterIcon(props) {
  return (
    <BaseIcon {...props}>
      <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
    </BaseIcon>
  );
}

export function CloseIcon(props) {
  return (
    <BaseIcon {...props}>
      <line x1="18" x2="6" y1="6" y2="18" />
      <line x1="6" x2="18" y1="6" y2="18" />
    </BaseIcon>
  );
}

export function LogOutIcon(props) {
  return (
    <BaseIcon {...props}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" x2="9" y1="12" y2="12" />
    </BaseIcon>
  );
}
