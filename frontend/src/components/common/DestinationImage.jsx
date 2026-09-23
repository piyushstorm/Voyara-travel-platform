import { useState, useEffect } from 'react';
import { getDestinationFallback } from '../../utils/destinationImages';

/**
 * Self-contained visual placeholder when external images fail or are loading.
 * Requires ZERO network requests. 100% responsive, preserves aspect ratio.
 */
function ThemedPlaceholder({ fallback, containerClassName = '' }) {
  return (
    <div
      className={`w-full h-full bg-gradient-to-br ${fallback.gradient} flex flex-col items-center justify-center text-white relative overflow-hidden select-none ${containerClassName}`}
    >
      {/* Decorative landscape geometry */}
      <svg
        className="absolute inset-0 w-full h-full opacity-25 pointer-events-none"
        preserveAspectRatio="xMidYMid slice"
        viewBox="0 0 400 200"
      >
        <polygon points="0,200 120,70 240,200" fill="#ffffff" />
        <polygon points="160,200 290,50 400,200" fill="#ffffff" />
        <ellipse cx="200" cy="190" rx="250" ry="30" fill="#000000" opacity="0.3" />
      </svg>

      {/* Destination Icon & Badge */}
      <div className="relative z-10 flex flex-col items-center text-center p-4">
        <span className="text-5xl sm:text-6xl drop-shadow-lg mb-2">
          {fallback.emoji}
        </span>
        <span className="text-xs sm:text-sm font-black uppercase tracking-widest bg-black/40 backdrop-blur-md px-3.5 py-1 rounded-lg border border-white/20 shadow-sm">
          {fallback.label}
        </span>
      </div>
    </div>
  );
}

/**
 * Resilient image component for Holiday Packages and Destinations.
 * Automatically transitions through 4 fallback stages:
 *   Stage 0: primary external URL (e.g. database imageUrl)
 *       ↓ failure
 *   Stage 1: verified high-availability CDN destination photo
 *       ↓ failure
 *   Stage 2: local bundled SVG asset (/images/destinations/{name}.svg)
 *       ↓ failure
 *   Stage 3: guaranteed themed gradient visual (zero network required)
 *
 * Guarantees that:
 *   - No broken-image icon is ever visible to the user
 *   - No alt text replaces the visual area
 *   - Container dimensions and aspect ratio are 100% preserved
 */
export default function DestinationImage({
  src,
  alt = '',
  destination = '',
  title = '',
  className = 'w-full h-full object-cover',
  containerClassName = '',
  ...props
}) {
  const fallback = getDestinationFallback(destination, title);

  // Determine starting stage
  const getInitialStage = () => {
    if (!src || typeof src !== 'string' || !src.trim()) {
      return fallback.cdnFallback ? 1 : 2;
    }
    const clean = src.trim();
    // Known dead/deleted Unsplash photos skip immediately to verified CDN fallback
    if (clean.includes('photo-1597074866923-dc0589150a32')) {
      return 1;
    }
    return 0;
  };

  const [stage, setStage] = useState(getInitialStage);
  const [isLoaded, setIsLoaded] = useState(false);

  // Sync state if props change
  useEffect(() => {
    setStage(getInitialStage());
    setIsLoaded(false);
  }, [src, destination, title]);

  // Determine active URL for current stage
  const getStageUrl = () => {
    switch (stage) {
      case 0:
        return src?.trim();
      case 1:
        return fallback.cdnFallback;
      case 2:
        return fallback.localSvg;
      default:
        return null;
    }
  };

  const currentUrl = getStageUrl();

  const handleError = () => {
    setIsLoaded(false);
    setStage((prev) => {
      if (prev === 0) {
        if (fallback.cdnFallback && fallback.cdnFallback !== src?.trim()) {
          return 1;
        }
        return 2; // Go to local SVG
      }
      if (prev === 1) {
        return 2; // Go to local SVG
      }
      return 3; // Go to guaranteed placeholder
    });
  };

  const handleLoad = () => {
    setIsLoaded(true);
  };

  // Stage 3 or missing URL: render guaranteed visual with zero network dependency
  if (stage >= 3 || !currentUrl) {
    return (
      <ThemedPlaceholder
        fallback={fallback}
        containerClassName={containerClassName}
      />
    );
  }

  return (
    <div className={`relative w-full h-full overflow-hidden ${containerClassName}`}>
      {/* Underlying guaranteed placeholder while loading or during stage transitions */}
      {!isLoaded && (
        <ThemedPlaceholder
          fallback={fallback}
          containerClassName="absolute inset-0 z-0"
        />
      )}

      {/* Active Image - hidden until fully loaded so no broken icon is ever displayed */}
      <img
        key={`${stage}-${currentUrl}`}
        src={currentUrl}
        alt={alt || fallback.label}
        loading="lazy"
        onLoad={handleLoad}
        onError={handleError}
        className={`${className} relative z-10 transition-opacity duration-300 ${
          isLoaded ? 'opacity-100' : 'opacity-0'
        }`}
        {...props}
      />
    </div>
  );
}
