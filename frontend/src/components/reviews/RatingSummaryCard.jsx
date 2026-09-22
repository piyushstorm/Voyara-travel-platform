import React from 'react';

export default function RatingSummaryCard({
  summary = {},
  selectedRating = null,
  onSelectRating,
  verifiedOnly = false,
  onToggleVerified,
  hasPhotos = false,
  onTogglePhotos,
  sortBy = 'NEWEST',
  onSortChange,
  onOpenComposer,
  canReview = true,
}) {
  const avg = Number(summary.averageRating || 0).toFixed(1);
  const total = summary.totalReviews ?? summary.reviewCount ?? 0;
  const distribution = summary.ratingDistribution || { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 mb-6">
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
        {/* Left column: Big Score & Stars */}
        <div className="md:col-span-4 flex flex-col items-center justify-center text-center md:border-r border-gray-100 md:pr-6">
          <div className="text-5xl font-black text-gray-900 tracking-tight flex items-baseline gap-1">
            {avg}
            <span className="text-xl font-medium text-gray-400">/ 5</span>
          </div>
          
          <div className="flex items-center gap-1 my-2" aria-label={`Rating: ${avg} out of 5 stars`}>
            {[1, 2, 3, 4, 5].map((star) => (
              <svg
                key={star}
                className={`w-6 h-6 ${
                  star <= Math.round(Number(avg)) ? 'text-amber-400 fill-amber-400' : 'text-gray-200 fill-gray-200'
                }`}
                viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
            ))}
          </div>

          <p className="text-sm text-gray-500 font-medium">
            Based on <span className="font-semibold text-gray-900">{total}</span> verified reviews
          </p>

          {canReview && (
            <button
              onClick={onOpenComposer}
              className="mt-4 w-full md:w-auto px-5 py-2.5 bg-primary hover:bg-primary-dark text-white font-medium text-sm rounded-xl shadow-sm hover:shadow transition flex items-center justify-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              Write a Review
            </button>
          )}
        </div>

        {/* Middle column: Rating Breakdown Bars */}
        <div className="md:col-span-8 flex flex-col justify-center space-y-2">
          {[5, 4, 3, 2, 1].map((star) => {
            const count = distribution[star] || 0;
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            const isSelected = selectedRating === star;

            return (
              <button
                key={star}
                type="button"
                onClick={() => onSelectRating(isSelected ? null : star)}
                className={`flex items-center gap-3 w-full text-left py-1 px-2 rounded-lg transition group ${
                  isSelected ? 'bg-amber-50 ring-1 ring-amber-300' : 'hover:bg-gray-50'
                }`}
                title={`Filter by ${star} star reviews`}
              >
                <div className="flex items-center gap-1 w-12 text-xs font-semibold text-gray-700">
                  <span>{star}</span>
                  <span className="text-amber-400">★</span>
                </div>

                <div className="flex-1 bg-gray-100 rounded-full h-2.5 overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      isSelected ? 'bg-amber-500' : 'bg-amber-400 group-hover:bg-amber-500'
                    }`}
                    style={{ width: `${pct}%` }}
                  />
                </div>

                <div className="w-16 text-right text-xs text-gray-500 group-hover:text-gray-900 font-medium">
                  {count} <span className="text-gray-400 text-[10px]">({pct}%)</span>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Filter and Sorting Controls */}
      <div className="mt-6 pt-5 border-t border-gray-100 flex flex-wrap items-center justify-between gap-3">
        {/* Filter Pills */}
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider mr-1">Filters:</span>
          
          <button
            onClick={() => onSelectRating(null)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition ${
              selectedRating === null
                ? 'bg-gray-900 text-white shadow-sm'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            All Ratings
          </button>

          {[5, 4, 3, 2, 1].map((s) => (
            <button
              key={s}
              onClick={() => onSelectRating(selectedRating === s ? null : s)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold flex items-center gap-1 transition ${
                selectedRating === s
                  ? 'bg-amber-500 text-white shadow-sm'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              <span>{s}</span>
              <span>★</span>
            </button>
          ))}

          <button
            onClick={onToggleVerified}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition flex items-center gap-1 ${
              verifiedOnly
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            <span>✓</span>
            Verified Only
          </button>

          <button
            onClick={onTogglePhotos}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition flex items-center gap-1 ${
              hasPhotos
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            With Photos
          </button>
        </div>

        {/* Sort Select */}
        <div className="flex items-center gap-2">
          <label htmlFor="review-sort" className="text-xs text-gray-500 font-medium">
            Sort by:
          </label>
          <select
            id="review-sort"
            value={sortBy}
            onChange={(e) => onSortChange(e.target.value)}
            className="text-xs font-semibold bg-gray-50 border border-gray-200 rounded-lg px-2.5 py-1.5 text-gray-700 outline-none focus:ring-1 focus:ring-primary focus:border-primary"
          >
            <option value="NEWEST">Newest First</option>
            <option value="HIGHEST_RATED">Highest Rated</option>
            <option value="MOST_HELPFUL">Most Helpful</option>
          </select>
        </div>
      </div>
    </div>
  );
}
