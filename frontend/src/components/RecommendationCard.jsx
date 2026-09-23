import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTomorrowDate } from '../utils/dateUtils';
import DestinationImage from './common/DestinationImage';

export default function RecommendationCard({ recommendation, onFeedback }) {
  const navigate = useNavigate();
  const [showWhyModal, setShowWhyModal] = useState(false);
  const [feedbackGiven, setFeedbackGiven] = useState(recommendation.feedback || null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    id,
    entityType = 'DESTINATION',
    entityId,
    score = 85,
    algorithm = 'HYBRID',
    headline,
    why = {},
    details = {},
  } = recommendation;

  const handleFeedback = async (type) => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setFeedbackGiven(type);
    try {
      if (onFeedback) {
        await onFeedback(recommendation, type);
      }
    } catch (err) {
      console.error('Failed to submit feedback', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCardClick = () => {
    if (entityType === 'HOTEL') {
      navigate(`/hotels/${entityId}`);
    } else if (entityType === 'FLIGHT') {
      const origin = details.originCode || 'DEL';
      const dest = details.destinationCode || 'BOM';
      const departureDate = getTomorrowDate();
      navigate(`/flights/search?origin=${origin}&destination=${dest}&departureDate=${departureDate}`);
    } else if (entityType === 'DESTINATION') {
      const destCode = details.city || details.name;
      navigate(`/hotels?destination=${encodeURIComponent(destCode || '')}`);
    } else if (entityType === 'HOLIDAY_PACKAGE') {
      navigate(`/holidays/${entityId}`);
    }
  };

  // Algorithm badge styling
  const algorithmBadge = {
    COLLABORATIVE: { label: 'Traveler Match', bg: 'bg-purple-100 text-purple-700 border-purple-200' },
    CONTENT_BASED: { label: 'Style Match', bg: 'bg-blue-100 text-blue-700 border-blue-200' },
    HYBRID: { label: 'Tailored Match', bg: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
    POPULAR_COLD_START: { label: 'Trending', bg: 'bg-amber-100 text-amber-800 border-amber-200' },
  }[algorithm] || { label: 'Suggested', bg: 'bg-slate-100 text-slate-700 border-slate-200' };

  // Entity Type Icons & Badges
  const typeIcons = {
    DESTINATION: '🏖️',
    HOTEL: '🏨',
    FLIGHT: '✈️',
    HOLIDAY_PACKAGE: '🎒',
  };

  // Safe image fallback
  const fallbackImages = {
    DESTINATION: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80',
    HOTEL: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
    FLIGHT: 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=800&q=80',
    HOLIDAY_PACKAGE: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&q=80',
  };

  const imageUrl = details.imageUrl || fallbackImages[entityType];
  const roundedScore = Math.round(score);

  return (
    <div className="group relative flex flex-col justify-between overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition duration-200 hover:-translate-y-1 hover:border-primary/40 hover:shadow-lg">
      {/* Top Banner Image with Gradient Overlay */}
      <div className="relative h-44 w-full cursor-pointer overflow-hidden bg-slate-100" onClick={handleCardClick}>
        <DestinationImage
          src={details.imageUrl}
          alt={headline || 'Travel Recommendation'}
          destination={details.destination || details.city || details.name || ''}
          title={headline || details.title || ''}
          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/25 to-transparent pointer-events-none" />

        {/* Top Badges */}
        <div className="absolute left-3 top-3 flex flex-wrap items-center gap-1.5">
          <span className="flex items-center gap-1 rounded-full bg-black/60 px-2.5 py-0.5 text-xs font-semibold text-white backdrop-blur-md">
            <span>{typeIcons[entityType] || '✨'}</span>
            <span>{entityType === 'HOLIDAY_PACKAGE' ? 'HOLIDAY' : entityType}</span>
          </span>
          <span className={`rounded-full border px-2 py-0.5 text-[11px] font-bold backdrop-blur-md ${algorithmBadge.bg}`}>
            {algorithmBadge.label}
          </span>
        </div>

        {/* Match Score Badge */}
        <div className="absolute right-3 top-3">
          <div className="flex items-center gap-1 rounded-full bg-white/95 px-2.5 py-1 shadow-md backdrop-blur-md">
            <span className="text-xs">⚡</span>
            <span className="text-xs font-black text-slate-900">{roundedScore}%</span>
            <span className="text-[10px] font-semibold text-slate-500">fit</span>
          </div>
        </div>

        {/* Headline on Image */}
        <div className="absolute bottom-3 left-3 right-3">
          <p className="line-clamp-1 text-xs font-semibold uppercase tracking-wider text-cyan-300">
            {why.badge || (entityType === 'DESTINATION' ? details.category : entityType)}
          </p>
          <h4 className="line-clamp-1 text-base font-extrabold text-white">
            {entityType === 'HOTEL' && details.name}
            {entityType === 'DESTINATION' && `${details.name}, ${details.country}`}
            {entityType === 'FLIGHT' && `${details.originCode} → ${details.destinationCode} (${details.airlineName})`}
            {entityType === 'HOLIDAY_PACKAGE' && (details.title || details.name)}
            {!details.name && !details.flightNumber && !details.title && headline}
          </h4>
        </div>
      </div>

      {/* Card Body */}
      <div className="flex flex-1 flex-col justify-between p-4">
        {/* Entity Highlights */}
        <div>
          <div className="mb-2.5 flex flex-wrap items-center justify-between gap-1 text-xs text-slate-600">
            {entityType === 'HOTEL' && (
              <>
                <span className="flex items-center gap-1 font-medium text-amber-600">
                  ★ {details.guestRating ? Number(details.guestRating).toFixed(1) : details.starRating} ({details.starRating} Star)
                </span>
                <span className="text-sm font-bold text-slate-900">
                  ₹{Number(details.startingPrice || 3500).toLocaleString()} <span className="text-[10px] font-normal text-slate-500">/ night</span>
                </span>
              </>
            )}
            {entityType === 'DESTINATION' && (
              <>
                <span className="text-slate-500">
                  🗓️ Best: <strong className="text-slate-700">{details.bestSeason || 'Nov - Mar'}</strong>
                </span>
                <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                  ₹{Number(details.averageDailyBudget || 4000).toLocaleString()} avg / day
                </span>
              </>
            )}
            {entityType === 'FLIGHT' && (
              <>
                <span className="text-slate-500">
                  ⏱️ {details.durationMinutes ? `${Math.floor(details.durationMinutes / 60)}h ${details.durationMinutes % 60}m` : 'Direct Flight'}
                </span>
                <span className="text-sm font-bold text-slate-900">
                  ₹{Number(details.price || 4999).toLocaleString()}
                </span>
              </>
            )}
            {entityType === 'HOLIDAY_PACKAGE' && (
              <>
                <span className="text-slate-500">
                  ⏱️ {details.durationDays ? `${details.durationDays}D / ${details.durationNights || details.durationDays - 1}N` : 'Package Tour'}
                </span>
                <span className="text-sm font-bold text-slate-900">
                  ₹{Number(details.price || details.startingPrice || 12999).toLocaleString()} <span className="text-[10px] font-normal text-slate-500">/ person</span>
                </span>
              </>
            )}
          </div>

          {/* Structured Explanation Teaser (Why this recommendation?) */}
          <div className="mb-3 rounded-xl bg-blue-50/70 p-2.5 border border-blue-100">
            <div className="flex items-start justify-between gap-2">
              <p className="line-clamp-2 text-xs font-medium text-blue-900 leading-snug">
                💡 {why.title || headline || 'Tailored to your travel style and interactions.'}
              </p>
              <button
                type="button"
                onClick={() => setShowWhyModal(true)}
                className="shrink-0 text-[11px] font-bold text-primary hover:text-blue-700 hover:underline"
              >
                Why?
              </button>
            </div>
          </div>
        </div>

        {/* Feedback Section */}
        <div className="mt-2 pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs">
          <span className="text-slate-400 text-[11px]">Helpful for you?</span>

          {feedbackGiven ? (
            <div className="flex items-center gap-1.5">
              <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold ${
                feedbackGiven === 'HELPFUL'
                  ? 'bg-emerald-100 text-emerald-800'
                  : 'bg-rose-100 text-rose-800'
              }`}>
                {feedbackGiven === 'HELPFUL' ? '👍 Liked' : '👎 Disliked'}
              </span>
              <button
                type="button"
                onClick={() => handleFeedback(feedbackGiven === 'HELPFUL' ? 'IRRELEVANT' : 'HELPFUL')}
                className="text-[10px] text-slate-400 hover:text-slate-600 underline"
                title="Change feedback"
              >
                change
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={isSubmitting}
                onClick={() => handleFeedback('HELPFUL')}
                className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 font-semibold text-slate-700 transition hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-700"
              >
                <span>👍</span> Helpful
              </button>
              <button
                type="button"
                disabled={isSubmitting}
                onClick={() => handleFeedback('IRRELEVANT')}
                className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2 py-1 font-semibold text-slate-600 transition hover:border-rose-300 hover:bg-rose-50 hover:text-rose-700"
              >
                <span>👎</span> Not for me
              </button>
            </div>
          )}
        </div>
      </div>

      {/* "Why This Recommendation?" Transparent Modal */}
      {showWhyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm animate-fadeIn">
          <div className="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 animate-scaleUp">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-blue-100 text-lg">💡</span>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Why this recommendation?</h3>
                  <p className="text-xs text-slate-500">Transparent match breakdown</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowWhyModal(false)}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            {/* Modal Content */}
            <div className="mt-4 space-y-3.5">
              {/* Primary Reason Headline */}
              <div className="rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 p-3.5 border border-blue-200/60">
                <p className="text-[11px] font-bold uppercase tracking-wider text-blue-700">Primary Match</p>
                <p className="mt-1 text-sm font-semibold text-slate-900">{why.title || headline}</p>
                {why.badge && (
                  <span className="mt-2 inline-block rounded-md bg-blue-200/70 px-2 py-0.5 text-[11px] font-bold text-blue-900">
                    {why.badge}
                  </span>
                )}
              </div>

              {/* Contributing Signal Breakdown */}
              {why.reasons && why.reasons.length > 0 && (
                <div>
                  <p className="mb-2 text-xs font-bold uppercase tracking-wider text-slate-400">Decision Signals</p>
                  <div className="space-y-2">
                    {why.reasons.map((r, idx) => (
                      <div key={idx} className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50/80 p-2.5">
                        <div className="flex-1 pr-2">
                          <p className="text-xs font-semibold text-slate-800">{r.label}</p>
                          <span className="text-[10px] text-slate-400 font-mono">Signal: {r.reasonType?.replace(/_/g, ' ')}</span>
                        </div>
                        {r.weight && (
                          <div className="shrink-0 rounded-full bg-blue-100 px-2 py-0.5 text-[11px] font-bold text-blue-800">
                            {Math.round(r.weight * 100)}%
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Match Score & Algorithm */}
              <div className="grid grid-cols-2 gap-2 pt-2">
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-3 text-center">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Match Confidence</span>
                  <div className="mt-0.5 text-lg font-black text-slate-900">{roundedScore}%</div>
                </div>
                <div className="rounded-xl border border-slate-100 bg-slate-50 p-3 text-center">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Engine Type</span>
                  <div className="mt-0.5 text-xs font-bold text-purple-700">{algorithmBadge.label}</div>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="mt-6 flex items-center justify-end gap-2 border-t border-slate-100 pt-3">
              <button
                type="button"
                onClick={() => setShowWhyModal(false)}
                className="travel-button-secondary px-4 py-2 text-xs"
              >
                Got it
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowWhyModal(false);
                  handleCardClick();
                }}
                className="travel-button-primary px-4 py-2 text-xs"
              >
                Explore now →
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
