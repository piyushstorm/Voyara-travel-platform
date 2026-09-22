/**
 * Centralized Date Utilities for Flight & Travel Planning
 * Prevents UTC timezone drift and enforces local calendar date correctness.
 */

/**
 * Get today's calendar date in local timezone formatted as YYYY-MM-DD.
 * Does NOT use toISOString() which shifts backward in UTC+ timezones like IST (+05:30).
 */
export function getTodayDate() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Get tomorrow's calendar date in local timezone formatted as YYYY-MM-DD.
 */
export function getTomorrowDate() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Add N days to a YYYY-MM-DD date string (or today if omitted).
 */
export function addDays(dateStr, days = 1) {
  const base = dateStr ? parseLocalDate(dateStr) : new Date();
  base.setDate(base.getDate() + days);
  const year = base.getFullYear();
  const month = String(base.getMonth() + 1).padStart(2, '0');
  const day = String(base.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Safely parse a YYYY-MM-DD string into a local Date object.
 * Avoids new Date("YYYY-MM-DD") which standardizes to UTC midnight.
 */
export function parseLocalDate(dateStr) {
  if (!dateStr) return new Date();
  if (typeof dateStr !== 'string') return new Date(dateStr);
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    const year = parseInt(parts[0], 10);
    const monthIndex = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    return new Date(year, monthIndex, day);
  }
  return new Date(dateStr);
}

/**
 * Checks if a YYYY-MM-DD date string is in the past compared to local today.
 */
export function isPastDate(dateStr) {
  if (!dateStr) return true;
  const today = getTodayDate();
  return dateStr < today;
}

/**
 * Validates and normalizes a departure date.
 * If the provided date is valid and >= today, it is preserved.
 * Otherwise, resolves to a valid future default (tomorrow).
 */
export function normalizeFlightDate(dateStr, fallback = getTomorrowDate()) {
  if (dateStr && !isPastDate(dateStr)) {
    return dateStr;
  }
  return fallback;
}

/**
 * Formats a YYYY-MM-DD string into a display date (e.g., "Mon, 21 Sep").
 * Safe from UTC offset conversion errors.
 */
export function formatFlightDisplayDate(dateStr) {
  if (!dateStr) return '';
  const localDate = parseLocalDate(dateStr);
  return localDate.toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  });
}

/**
 * Formats a YYYY-MM-DD string into a full display date (e.g., "Monday, 21 September 2026").
 */
export function formatFlightDisplayDateLong(dateStr) {
  if (!dateStr) return '';
  const localDate = parseLocalDate(dateStr);
  return localDate.toLocaleDateString('en-IN', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

/**
 * Validates a trip date range.
 * Both dates must not be in the past, and endDate must be >= startDate.
 */
export function isValidTripDateRange(startDate, endDate) {
  const today = getTodayDate();
  if (startDate && startDate < today) {
    return { valid: false, error: 'Trip dates cannot be in the past.' };
  }
  if (endDate && endDate < today) {
    return { valid: false, error: 'Trip dates cannot be in the past.' };
  }
  if (startDate && endDate && endDate < startDate) {
    return { valid: false, error: 'End date must be on or after the start date.' };
  }
  return { valid: true, error: null };
}

