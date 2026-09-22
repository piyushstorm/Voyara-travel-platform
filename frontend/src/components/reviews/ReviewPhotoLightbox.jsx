import React, { useEffect } from 'react';

export default function ReviewPhotoLightbox({ photos = [], initialIndex = 0, isOpen, onClose }) {
  const [currentIndex, setCurrentIndex] = React.useState(initialIndex);

  useEffect(() => {
    setCurrentIndex(initialIndex);
  }, [initialIndex, isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowLeft') handlePrev();
      if (e.key === 'ArrowRight') handleNext();
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, currentIndex, photos.length]);

  if (!isOpen || !photos || photos.length === 0) return null;

  const currentPhoto = photos[currentIndex] || photos[0];

  const handlePrev = (e) => {
    if (e) e.stopPropagation();
    setCurrentIndex((prev) => (prev > 0 ? prev - 1 : photos.length - 1));
  };

  const handleNext = (e) => {
    if (e) e.stopPropagation();
    setCurrentIndex((prev) => (prev < photos.length - 1 ? prev + 1 : 0));
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Review photo lightbox"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 backdrop-blur-md p-4 transition-all animate-fadeIn"
      onClick={onClose}
    >
      {/* Close Button */}
      <button
        onClick={onClose}
        aria-label="Close photo view"
        className="absolute top-4 right-4 text-white/80 hover:text-white bg-black/40 hover:bg-black/60 p-2.5 rounded-full z-10 transition focus:outline-none focus:ring-2 focus:ring-white"
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>

      {/* Navigation Arrows */}
      {photos.length > 1 && (
        <>
          <button
            onClick={handlePrev}
            aria-label="Previous photo"
            className="absolute left-4 top-1/2 -translate-y-1/2 text-white/80 hover:text-white bg-black/50 hover:bg-black/80 p-3 rounded-full z-10 transition focus:outline-none focus:ring-2 focus:ring-white"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button
            onClick={handleNext}
            aria-label="Next photo"
            className="absolute right-4 top-1/2 -translate-y-1/2 text-white/80 hover:text-white bg-black/50 hover:bg-black/80 p-3 rounded-full z-10 transition focus:outline-none focus:ring-2 focus:ring-white"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </>
      )}

      {/* Photo Container */}
      <div
        className="max-w-4xl max-h-[85vh] flex flex-col items-center select-none"
        onClick={(e) => e.stopPropagation()}
      >
        <img
          src={currentPhoto.photoUrl}
          alt={currentPhoto.caption || `Review photo ${currentIndex + 1}`}
          className="max-h-[75vh] max-w-full rounded-lg object-contain shadow-2xl transition-all"
        />

        {/* Counter and Caption */}
        <div className="mt-3 text-center text-white/90">
          <span className="text-sm font-medium bg-black/60 px-3 py-1 rounded-full">
            {currentIndex + 1} / {photos.length}
          </span>
          {currentPhoto.caption && (
            <p className="text-sm mt-2 text-white/80 max-w-lg">{currentPhoto.caption}</p>
          )}
        </div>

        {/* Thumbnails strip */}
        {photos.length > 1 && (
          <div className="flex gap-2 mt-4 overflow-x-auto p-1 max-w-md">
            {photos.map((p, idx) => (
              <button
                key={p.id || idx}
                onClick={() => setCurrentIndex(idx)}
                className={`w-12 h-12 rounded-md overflow-hidden flex-shrink-0 border-2 transition ${
                  idx === currentIndex ? 'border-primary scale-105' : 'border-transparent opacity-60 hover:opacity-100'
                }`}
              >
                <img src={p.photoUrl} alt="" className="w-full h-full object-cover" />
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
