import { useState, useEffect } from 'react';
import { rewardsApi } from '../api/phase3Api';
import {
  AwardIcon,
  SparklesIcon,
  TagIcon,
  ClockIcon,
  CheckIcon,
  FlightIcon,
  UserIcon,
} from '../components/common/Icons';

const TIER_THEMES = {
  SILVER: {
    label: 'Silver Voyager',
    bg: 'from-[#1e293b] via-[#334155] to-[#475569]',
    badgeBg: 'bg-slate-700 text-slate-100 border-slate-600',
    iconColor: 'text-slate-300',
    ringColor: 'ring-slate-400',
  },
  GOLD: {
    label: 'Gold Prestige',
    bg: 'from-[#78350f] via-[#b45309] to-[#d97706]',
    badgeBg: 'bg-amber-800 text-amber-100 border-amber-600',
    iconColor: 'text-amber-300',
    ringColor: 'ring-amber-400',
  },
  PLATINUM: {
    label: 'Platinum Elite',
    bg: 'from-[#1e1b4b] via-[#4338ca] to-[#6366f1]',
    badgeBg: 'bg-indigo-900 text-indigo-100 border-indigo-500',
    iconColor: 'text-indigo-300',
    ringColor: 'ring-indigo-400',
  },
};

const TXN_LABELS = {
  BOOKING_EARN: { label: 'Flight / Stay Booking', color: 'text-emerald-700 bg-emerald-50 border-emerald-200' },
  REDEMPTION: { label: 'Points Redeemed', color: 'text-blue-700 bg-blue-50 border-blue-200' },
  EXPIRY: { label: 'Points Expired', color: 'text-red-700 bg-red-50 border-red-200' },
  ADMIN_ADJUSTMENT: { label: 'Loyalty Credit', color: 'text-purple-700 bg-purple-50 border-purple-200' },
  REFERRAL_BONUS: { label: 'Companion Referral', color: 'text-pink-700 bg-pink-50 border-pink-200' },
  REVERSAL: { label: 'Credit Reversal', color: 'text-orange-700 bg-orange-50 border-orange-200' },
};

export default function VoyaraRewards() {
  const [account, setAccount] = useState(null);
  const [benefits, setBenefits] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [txnLoading, setTxnLoading] = useState(false);
  const [txnPage, setTxnPage] = useState(0);
  const [txnTotalPages, setTxnTotalPages] = useState(1);
  const [txnFilter, setTxnFilter] = useState('');

  useEffect(() => {
    Promise.all([rewardsApi.getAccount(), rewardsApi.getBenefits()])
      .then(([accRes, benRes]) => {
        setAccount(accRes.data);
        setBenefits(benRes.data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  useEffect(() => {
    setTxnLoading(true);
    rewardsApi
      .getTransactions(txnFilter || null, txnPage, 10)
      .then((res) => {
        const data = res.data;
        setTransactions(data.content || []);
        setTxnTotalPages(data.totalPages || 1);
        setTxnLoading(false);
      })
      .catch(() => setTxnLoading(false));
  }, [txnPage, txnFilter]);

  if (loading) {
    return (
      <div className="section-shell py-12 max-w-4xl mx-auto space-y-4">
        <div className="skeleton h-8 w-64 rounded-xl" />
        <div className="skeleton h-56 w-full rounded-2xl" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="skeleton h-24 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  const tier = account?.tier || 'SILVER';
  const theme = TIER_THEMES[tier] || TIER_THEMES.SILVER;
  const progress =
    account?.nextTierThreshold > 0
      ? Math.min(100, Math.round((account.qualifyingPoints / account.nextTierThreshold) * 100))
      : 100;

  return (
    <div className="section-shell py-8 max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">Loyalty & Privileges</p>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 mt-1">Voyara Elite Rewards</h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
          Earn points on flights, stays, and transit. Redeem for checkout discounts.
        </p>
      </div>

      {/* ═══ LUXURY DIGITAL PASSPORT / MEMBERSHIP CARD ═══ */}
      <div
        className={`rounded-[26px] bg-gradient-to-br ${theme.bg} p-6 sm:p-8 text-white shadow-2xl relative overflow-hidden mb-8 border border-white/20`}
      >
        {/* Subtle holographic foil circle background */}
        <div className="absolute top-0 right-0 w-80 h-80 rounded-full bg-white/10 blur-3xl -translate-y-1/3 translate-x-1/3 pointer-events-none" />

        <div className="relative z-10 flex flex-wrap items-start justify-between gap-4 mb-8">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center text-white border border-white/20 shadow-inner">
              <AwardIcon className="w-7 h-7" />
            </div>
            <div>
              <span className="text-[10px] uppercase font-bold tracking-[0.24em] text-white/70 block">
                Voyara Loyalty Club
              </span>
              <h2 className="text-xl sm:text-2xl font-black">{theme.label}</h2>
            </div>
          </div>

          <span className={`px-3 py-1 rounded-full text-xs font-bold border backdrop-blur-md ${theme.badgeBg}`}>
            Tier Multiplier: {account?.tierMultiplier || 1}x
          </span>
        </div>

        <div className="relative z-10 grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-white/15 text-xs">
          <div>
            <span className="text-white/70 uppercase text-[10px] font-bold tracking-wider block">Available Balance</span>
            <span className="text-2xl sm:text-3xl font-black text-white block mt-0.5">
              {account?.pointsBalance?.toLocaleString() || 0}
            </span>
            <span className="text-white/60 text-[10px]">Points ready to redeem</span>
          </div>

          <div>
            <span className="text-white/70 uppercase text-[10px] font-bold tracking-wider block">Lifetime Earned</span>
            <span className="text-xl sm:text-2xl font-bold text-white block mt-1">
              {account?.lifetimePointsEarned?.toLocaleString() || 0}
            </span>
          </div>

          <div>
            <span className="text-white/70 uppercase text-[10px] font-bold tracking-wider block">Points Redeemed</span>
            <span className="text-xl sm:text-2xl font-bold text-white block mt-1">
              {account?.lifetimePointsRedeemed?.toLocaleString() || 0}
            </span>
          </div>

          <div>
            <span className="text-white/70 uppercase text-[10px] font-bold tracking-wider block">Membership ID</span>
            <span className="text-base sm:text-lg font-mono font-bold text-white/90 block mt-1">
              VOY-{(account?.id || 1000).toString().padStart(6, '0')}
            </span>
          </div>
        </div>
      </div>

      {/* ═══ TIER MILESTONE PROGRESS ═══ */}
      {tier !== 'PLATINUM' && account?.nextTier && (
        <div className="voyara-card p-5 sm:p-6 mb-8">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Next Milestone: {account.nextTier}</h3>
              <p className="text-xs text-slate-500">
                Earn {account.pointsToNextTier?.toLocaleString()} more qualifying points to unlock {account.nextTier} privileges
              </p>
            </div>
            <span className="text-xs font-black text-primary bg-blue-50 px-2.5 py-1 rounded-full">
              {progress}%
            </span>
          </div>

          <div className="h-3 w-full bg-slate-100 rounded-full overflow-hidden border border-slate-200">
            <div
              className="h-full rounded-full bg-gradient-to-r from-primary to-cyan-500 transition-all duration-700 ease-out"
              style={{ width: `${progress}%` }}
            />
          </div>

          <div className="flex justify-between text-[11px] text-slate-400 font-bold mt-2">
            <span>{account.qualifyingPoints?.toLocaleString()} qualifying pts</span>
            <span>{account.nextTierThreshold?.toLocaleString()} pts required</span>
          </div>
        </div>
      )}

      {/* ═══ TIER PRIVILEGES ═══ */}
      {benefits && (
        <div className="voyara-card p-5 sm:p-6 mb-8">
          <h3 className="text-sm font-bold text-slate-900 mb-4 pb-2 border-b border-slate-100">
            Voyara Tier Benefits Matrix
          </h3>

          <div className="grid sm:grid-cols-3 gap-4">
            {Object.entries(benefits).map(([tierKey, info]) => {
              const active = tier === tierKey;
              return (
                <div
                  key={tierKey}
                  className={`p-4 rounded-2xl border transition-all ${
                    active ? 'border-primary bg-blue-50/40 shadow-sm' : 'border-slate-200 bg-white'
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-700">{info.name}</span>
                    {active && (
                      <span className="text-[10px] font-bold bg-primary text-white px-2 py-0.5 rounded-full">Current</span>
                    )}
                  </div>

                  <p className="text-xs font-bold text-primary mb-3">Multiplier: {info.multiplier}</p>

                  <ul className="space-y-1.5 text-xs text-slate-600">
                    {info.benefits?.map((b, i) => (
                      <li key={i} className="flex items-start gap-1.5">
                        <CheckIcon className="w-3.5 h-3.5 text-emerald-500 shrink-0 mt-0.5" />
                        <span>{b}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ═══ RECENT REWARDS ACTIVITY ═══ */}
      <div className="voyara-card p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-slate-100">
          <div>
            <h3 className="text-sm font-bold text-slate-900">Points Activity Ledger</h3>
            <p className="text-xs text-slate-500">Track all loyalty earnings and redemptions</p>
          </div>

          <select
            value={txnFilter}
            onChange={(e) => {
              setTxnFilter(e.target.value);
              setTxnPage(0);
            }}
            className="voyara-input text-xs font-semibold py-1.5 h-auto w-auto"
          >
            <option value="">All Transactions</option>
            <option value="BOOKING_EARN">Bookings Earned</option>
            <option value="REDEMPTION">Redemptions</option>
            <option value="EXPIRY">Expired</option>
          </select>
        </div>

        {txnLoading ? (
          <div className="space-y-2">
            {[1, 2, 3].map((i) => (
              <div key={i} className="skeleton h-12 rounded-xl" />
            ))}
          </div>
        ) : transactions.length === 0 ? (
          <div className="py-8 text-center text-xs text-slate-400">No rewards transactions found.</div>
        ) : (
          <div className="space-y-2">
            {transactions.map((txn) => {
              const isCredit = (txn.points || 0) > 0;
              const typeCfg = TXN_LABELS[txn.type] || { label: txn.type, color: 'text-slate-700 bg-slate-50' };
              return (
                <div
                  key={txn.id}
                  className="p-3 rounded-xl border border-slate-100 bg-slate-50/60 flex items-center justify-between gap-3 text-xs"
                >
                  <div className="min-w-0">
                    <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold border ${typeCfg.color}`}>
                      {typeCfg.label}
                    </span>
                    <p className="font-semibold text-slate-800 mt-1 truncate">{txn.description || 'Loyalty activity'}</p>
                    <span className="text-[10px] text-slate-400">
                      {txn.createdAt ? new Date(txn.createdAt).toLocaleDateString('en-IN') : ''}
                    </span>
                  </div>

                  <span className={`text-sm font-black shrink-0 ${isCredit ? 'text-emerald-600' : 'text-slate-700'}`}>
                    {isCredit ? `+${txn.points}` : txn.points} pts
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
