import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { recommendationApi } from '../api/phase3Api';
import { useAuth } from '../context/AuthContext';
import RecommendationCard from './RecommendationCard';

const TABS = [
  { id: 'ALL', label: 'All Picks', icon: '✨' },
  { id: 'HOTEL', label: 'Stays', icon: '🏨' },
  { id: 'FLIGHT', label: 'Flights', icon: '✈️' },
  { id: 'DESTINATION', label: 'Destinations', icon: '🏖️' },
  { id: 'HOLIDAY_PACKAGE', label: 'Holidays', icon: '🎒' },
];

export default function RecommendationsSection({ title = 'Tailored For Your Travel Style', subtitle = 'Personalized suggestions driven by your searches, bookings, and traveler affinity' }) {
  const { isAuthenticated, user } = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('ALL');

  const queryKey = ['recommendations', activeTab, isAuthenticated ? user?.id : 'anon'];

  const { data: recommendations = [], isLoading, isError, isFetching } = useQuery({
    queryKey,
    queryFn: async () => {
      const typeParam = activeTab === 'ALL' ? undefined : activeTab;
      const res = await recommendationApi.get(typeParam, 8);
      return res.data || [];
    },
    staleTime: 1000 * 60 * 3, // 3 minutes
  });

  const recomputeMutation = useMutation({
    mutationFn: () => recommendationApi.recompute(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recommendations'] });
    },
  });

  const handleFeedback = async (rec, type) => {
    try {
      if (rec.id) {
        await recommendationApi.feedback(rec.id, type);
      } else {
        await recommendationApi.entityFeedback(rec.entityType, rec.entityId, type);
      }
      // Optimistically update query cache or refetch
      queryClient.setQueryData(queryKey, (old) => {
        if (!old) return old;
        return old.map((item) => (item.id === rec.id ? { ...item, feedback: type } : item));
      });
    } catch (err) {
      console.error('Failed to submit feedback', err);
    }
  };

  return (
    <section className="section-shell py-8 sm:py-10 lg:py-12">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-100 text-xs text-primary">✨</span>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">Personalized Discovery</p>
          </div>
          <h2 className="mt-1.5 text-2xl font-black text-slate-900 sm:text-3xl">{title}</h2>
          <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
        </div>

        {/* Controls: Tabs & Recompute */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex rounded-xl bg-slate-100 p-1 border border-slate-200/80">
            {TABS.map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  activeTab === tab.id
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>{tab.icon}</span>
                <span>{tab.label}</span>
              </button>
            ))}
          </div>

          {isAuthenticated && (
            <button
              type="button"
              disabled={recomputeMutation.isPending || isFetching}
              onClick={() => recomputeMutation.mutate()}
              className="flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 disabled:opacity-50"
              title="Refresh recommendations based on latest activity"
            >
              <span className={`inline-block ${recomputeMutation.isPending || isFetching ? 'animate-spin' : ''}`}>🔄</span>
              <span className="hidden sm:inline">Refresh</span>
            </button>
          )}
        </div>
      </div>

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="animate-pulse rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="h-40 w-full rounded-xl bg-slate-200" />
              <div className="mt-4 h-4 w-2/3 rounded bg-slate-200" />
              <div className="mt-2 h-3 w-1/2 rounded bg-slate-100" />
              <div className="mt-4 h-8 w-full rounded-lg bg-slate-100" />
            </div>
          ))}
        </div>
      )}

      {/* Error / Empty State */}
      {!isLoading && (isError || recommendations.length === 0) && (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 p-8 text-center">
          <span className="text-3xl">🏖️</span>
          <h3 className="mt-2 text-base font-bold text-slate-900">Start exploring to unlock personalized picks</h3>
          <p className="mx-auto mt-1 max-w-md text-xs text-slate-500">
            Search flights, explore boutique hotels, or save your travel preferences to receive transparent, personalized recommendations.
          </p>
        </div>
      )}

      {/* Recommendations Grid */}
      {!isLoading && recommendations.length > 0 && (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {recommendations.map((rec) => (
            <RecommendationCard
              key={rec.id || `${rec.entityType}-${rec.entityId}`}
              recommendation={rec}
              onFeedback={handleFeedback}
            />
          ))}
        </div>
      )}
    </section>
  );
}
