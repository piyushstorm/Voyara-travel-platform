/**
 * Curated destination image fallbacks and metadata for Voyara travel platform.
 * Provides multi-tier fallback protection:
 *   Tier 1: Package imageUrl from database
 *   Tier 2: Verified high-availability CDN destination photo
 *   Tier 3: Bundled, zero-latency local SVG destination asset
 */

export const DESTINATION_ASSETS = {
  kashmir: {
    label: 'Kashmir',
    emoji: '🏔️',
    localSvg: '/images/destinations/kashmir.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1598091383021-15ddea10925d?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-blue-600 via-indigo-700 to-slate-900',
  },
  goa: {
    label: 'Goa',
    emoji: '🏖️',
    localSvg: '/images/destinations/goa.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-amber-500 via-orange-600 to-rose-700',
  },
  kerala: {
    label: 'Kerala',
    emoji: '🌴',
    localSvg: '/images/destinations/kerala.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-emerald-600 via-teal-700 to-cyan-900',
  },
  rajasthan: {
    label: 'Rajasthan',
    emoji: '🏰',
    localSvg: '/images/destinations/rajasthan.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1477587458883-47145ed94245?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-amber-600 via-orange-700 to-amber-950',
  },
  manali: {
    label: 'Manali',
    emoji: '⛰️',
    localSvg: '/images/destinations/manali.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-sky-600 via-teal-700 to-slate-900',
  },
  himachal: {
    label: 'Himachal Pradesh',
    emoji: '⛰️',
    localSvg: '/images/destinations/himachal.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-sky-700 via-emerald-800 to-slate-900',
  },
  andaman: {
    label: 'Andaman',
    emoji: '🏝️',
    localSvg: '/images/destinations/andaman.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-cyan-500 via-teal-600 to-blue-900',
  },
  ladakh: {
    label: 'Ladakh',
    emoji: '🏍️',
    localSvg: '/images/destinations/ladakh.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-amber-600 via-blue-700 to-indigo-950',
  },
  dubai: {
    label: 'Dubai',
    emoji: '🏙️',
    localSvg: '/images/destinations/dubai.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-indigo-900 via-purple-900 to-amber-700',
  },
  singapore: {
    label: 'Singapore',
    emoji: '🌆',
    localSvg: '/images/destinations/singapore.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-purple-900 via-indigo-800 to-cyan-900',
  },
  bali: {
    label: 'Bali',
    emoji: '🌺',
    localSvg: '/images/destinations/bali.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-emerald-700 via-teal-800 to-amber-900',
  },
  maldives: {
    label: 'Maldives',
    emoji: '🐠',
    localSvg: '/images/destinations/maldives.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1514282401047-d79a71a590e8?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-sky-400 via-teal-500 to-cyan-800',
  },
  thailand: {
    label: 'Thailand',
    emoji: '🛕',
    localSvg: '/images/destinations/thailand.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1528181304800-259b08848526?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-teal-600 via-cyan-700 to-amber-800',
  },
  europe: {
    label: 'Europe',
    emoji: '🗼',
    localSvg: '/images/destinations/europe.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-blue-900 via-indigo-900 to-purple-900',
  },
  default: {
    label: 'Voyara Holiday',
    emoji: '✨',
    localSvg: '/images/destinations/default.svg',
    cdnFallback: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&auto=format&fit=crop&q=80',
    gradient: 'from-blue-600 via-indigo-700 to-violet-900',
  },
};

/**
 * Resolves the destination key based on name, title, or search terms.
 */
export function resolveDestinationKey(destination = '', title = '') {
  const text = `${destination} ${title}`.toLowerCase();

  if (/kashmir|srinagar|gulmarg|pahalgam|dal\s*lake/.test(text)) return 'kashmir';
  if (/goa|calangute|anjuna|panaji|baga/.test(text)) return 'goa';
  if (/kerala|munnar|alleppey|thekkady|cochin|kochi|backwaters/.test(text)) return 'kerala';
  if (/rajasthan|jaipur|udaipur|jodhpur|jaisalmer|pushkar/.test(text)) return 'rajasthan';
  if (/manali|solang|rohtang|kullu|hadimba/.test(text)) return 'manali';
  if (/himachal|shimla|dharamshala|kasol|spiti/.test(text)) return 'himachal';
  if (/andaman|havelock|port\s*blair|neil\s*island|swaraj/.test(text)) return 'andaman';
  if (/ladakh|leh|pangong|nubra/.test(text)) return 'ladakh';
  if (/dubai|uae|burj\s*khalifa|emirates|dhabi/.test(text)) return 'dubai';
  if (/singapore|sentosa|marina\s*bay|changi/.test(text)) return 'singapore';
  if (/bali|ubud|seminyak|denpasar|kuta/.test(text)) return 'bali';
  if (/maldives|male\b|atoll|bungalow/.test(text)) return 'maldives';
  if (/thailand|bangkok|phuket|pattaya|krabi|phi\s*phi/.test(text)) return 'thailand';
  if (/europe|paris|france|swiss|switzerland|amsterdam|italy|rome/.test(text)) return 'europe';

  return 'default';
}

/**
 * Returns complete fallback metadata for a given destination and title.
 */
export function getDestinationFallback(destination = '', title = '') {
  const key = resolveDestinationKey(destination, title);
  return DESTINATION_ASSETS[key] || DESTINATION_ASSETS.default;
}
