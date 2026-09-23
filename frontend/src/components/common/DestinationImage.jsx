import { useState, useEffect } from 'react';
import { getDestinationFallback } from '../../utils/destinationImages';

/**
 * Resilient image component for Holiday Packages and Destinations.
 * Automatically handles missing URLs, 404s, network timeouts, and CORS errors:
 *   1. Tries primary src (e.g. database imageUrl)
 *   2. On failure, falls back to destination-specific verified CDN photo
 *   3. On second failure or offline, falls back to bundled local SVG asset
 *   4. Guarantees no broken-image icons and preserves container aspect ratio.
 */
export default function DestinationImage({
  src,
  alt = '',
  destination = '',
  title = '',
  className = 'w-full h-full object-cover',
  fallbackClassName = 'w-full h-full',
  ...props
}) {
  const fallback = getDestinationFallback(destination, title);

  // Determine starting source: primary src -> CDN fallback -> local SVG
  const initialSrc = src?.trim() ? src.trim() : (fallback.cdnFallback || fallback.localSvg);
  const [currentSrc, setCurrentSrc] = useState(initialSrc);
  const [errorStage, setErrorStage] = useState(0); // 0 = initial, 1 = tried cdn, 2 = tried localSvg, 3 = render css placeholder

  // Sync if src changes dynamically
  useEffect(() => {
    const nextSrc = src?.trim() ? src.trim() : (fallback.cdnFallback || fallback.localSvg);
    setCurrentSrc(nextSrc);
    setErrorStage(0);
  }, [src, destination, title]);

  const handleError = (e) => {
    e.currentTarget.onerror = null; // Prevent cyclic triggers

    if (errorStage === 0) {
      // Primary src failed -> try destination CDN fallback (if different)
      if (fallback.cdnFallback && fallback.cdnFallback !== currentSrc) {
        setErrorStage(1);
        setCurrentSrc(fallback.cdnFallback);
      } else if (fallback.localSvg && fallback.localSvg !== currentSrc) {
        setErrorStage(2);
        setCurrentSrc(fallback.localSvg);
      } else {
        setErrorStage(3);
      }
    } else if (errorStage === 1) {
      // CDN fallback failed -> try local bundled SVG
      if (fallback.localSvg && fallback.localSvg !== currentSrc) {
        setErrorStage(2);
        setCurrentSrc(fallback.localSvg);
      } else {
        setErrorStage(3);
      }
    } else {
      // Local SVG failed -> show rich CSS gradient placeholder
      setErrorStage(3);
    }
  };

  if (errorStage === 3 || !currentSrc) {
    return (
      <div
        className={`w-full h-full bg-gradient-to-br ${fallback.gradient} flex flex-col items-center justify-center text-white relative overflow-hidden select-none ${fallbackClassName}`}
      >
        <span className="text-5xl sm:text-6xl drop-shadow-md mb-1 animate-pulse">
          {fallback.emoji}
        </span>
        <span className="text-xs font-bold uppercase tracking-widest bg-black/30 backdrop-blur-sm px-2.5 py-1 rounded-md">
          {fallback.label}
        </span>
      </div>
    );
  }

  return (
    <img
      src={currentSrc}
      alt={alt || fallback.label}
      loading="lazy"
      onError={handleError}
      className={className}
      {...props}
    />
  );
}
