import { useState, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { voyaraApi } from '../api/phase3Api';
import { useAuth } from '../context/AuthContext';
import {
  CreditCardIcon,
  TagIcon,
  FlightIcon,
  HotelIcon,
  CabIcon,
  TrainIcon,
  BusIcon,
  CheckIcon,
  UsersIcon,
  ArrowRightIcon,
} from '../components/common/Icons';

const EXPENSE_TYPES = [
  { value: 'FLIGHT', label: 'Flight', icon: FlightIcon },
  { value: 'HOTEL', label: 'Hotel', icon: HotelIcon },
  { value: 'CAB', label: 'Cab', icon: CabIcon },
  { value: 'TRAIN', label: 'Train', icon: TrainIcon },
  { value: 'BUS', label: 'Bus', icon: BusIcon },
  { value: 'FOOD', label: 'Dining', icon: TagIcon },
  { value: 'ACTIVITY', label: 'Activity', icon: TagIcon },
  { value: 'OTHER', label: 'General', icon: CreditCardIcon },
];

export default function GroupExpenses() {
  const { id } = useParams();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [showAddModal, setShowAddModal] = useState(false);
  const [newExpense, setNewExpense] = useState({ description: '', amount: '', expenseType: 'OTHER', splitMode: 'EQUAL' });
  const [toast, setToast] = useState(null);

  const { data: expenseData, isLoading } = useQuery({
    queryKey: ['expenses', id],
    queryFn: () => voyaraApi.getExpenses(id).then((r) => r.data),
  });

  const { data: settlementsData } = useQuery({
    queryKey: ['settlements', id],
    queryFn: () => voyaraApi.getSettlements(id).then((r) => r.data),
  });

  const addMutation = useMutation({
    mutationFn: (data) => voyaraApi.addExpense(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses', id] });
      queryClient.invalidateQueries({ queryKey: ['settlements', id] });
      setShowAddModal(false);
      setNewExpense({ description: '', amount: '', expenseType: 'OTHER', splitMode: 'EQUAL' });
      setToast({ type: 'success', message: 'Expense added to group pool!' });
    },
    onError: (err) => setToast({ type: 'error', message: err.response?.data?.message || 'Failed to add expense' }),
  });

  const settleMutation = useMutation({
    mutationFn: (settlementId) => voyaraApi.markSettled(settlementId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settlements', id] });
      setToast({ type: 'success', message: 'Payment marked as settled!' });
    },
  });

  const expenses = expenseData?.data || [];
  const summary = expenseData?.summary || {};
  const settlements = settlementsData?.data || [];

  // Calculate Personal Balance: You Owe vs You Are Owed vs Net Balance
  const { youOwe, youAreOwed, netBalance } = useMemo(() => {
    let owe = 0;
    let owed = 0;
    const currentUserId = user?.id;

    settlements.forEach((s) => {
      if (s.status === 'PENDING') {
        const amt = Number(s.amount) || 0;
        if (s.fromUser?.id === currentUserId) {
          owe += amt;
        }
        if (s.toUser?.id === currentUserId) {
          owed += amt;
        }
      }
    });

    return {
      youOwe: owe,
      youAreOwed: owed,
      netBalance: owed - owe,
    };
  }, [settlements, user]);

  return (
    <div className="section-shell py-8 max-w-4xl mx-auto">
      {toast && (
        <div
          className={`fixed top-20 right-4 z-[100] px-4 py-3 rounded-2xl border shadow-xl text-xs font-bold transition-all ${
            toast.type === 'success'
              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
              : 'bg-red-50 text-red-700 border-red-200'
          }`}
        >
          {toast.message}
        </div>
      )}

      {/* Breadcrumb & Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link to="/group-trips" className="text-xs font-bold text-slate-500 hover:text-primary transition mb-1 block">
            ← Back to Group Trips
          </Link>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900">Group Expense Split</h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Transparent group spending, automated split tallies, and one-click settlements
          </p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="travel-button-primary px-5 py-2.5 text-xs font-bold shadow-md shadow-primary/20"
        >
          + Add Expense
        </button>
      </div>

      {/* ═══ 4-BOX FINANCIAL HEALTH DASHBOARD ═══ */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3.5 mb-8">
        <div className="voyara-card p-4">
          <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block">Total Group Spend</span>
          <p className="text-lg sm:text-xl font-black text-slate-900 mt-1">
            ₹{Number(summary.totalExpenses || 0).toLocaleString()}
          </p>
          <span className="text-[11px] text-slate-500 font-medium">{summary.expenseCount || expenses.length} items logged</span>
        </div>

        <div className="voyara-card p-4">
          <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block">You Owe</span>
          <p className="text-lg sm:text-xl font-black text-red-600 mt-1">
            ₹{youOwe.toLocaleString()}
          </p>
          <span className="text-[11px] text-slate-500 font-medium">To companions</span>
        </div>

        <div className="voyara-card p-4">
          <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block">You Are Owed</span>
          <p className="text-lg sm:text-xl font-black text-emerald-600 mt-1">
            ₹{youAreOwed.toLocaleString()}
          </p>
          <span className="text-[11px] text-slate-500 font-medium">Receivable</span>
        </div>

        <div className={`voyara-card p-4 border-2 ${netBalance >= 0 ? 'border-emerald-200 bg-emerald-50/30' : 'border-amber-200 bg-amber-50/30'}`}>
          <span className="text-[10px] uppercase font-bold tracking-wider text-slate-500 block">Net Balance</span>
          <p className={`text-lg sm:text-xl font-black mt-1 ${netBalance >= 0 ? 'text-emerald-700' : 'text-amber-700'}`}>
            {netBalance >= 0 ? `+₹${netBalance.toLocaleString()}` : `-₹${Math.abs(netBalance).toLocaleString()}`}
          </p>
          <span className="text-[11px] font-bold text-slate-600">
            {netBalance >= 0 ? 'In the green' : 'Settlement needed'}
          </span>
        </div>
      </div>

      {/* ═══ SETTLEMENT RECOMMENDATIONS (Who Owes Whom) ═══ */}
      {settlements.length > 0 && (
        <div className="voyara-card p-5 sm:p-6 mb-8">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Settlement Ledger</h3>
              <p className="text-xs text-slate-500">Calculated peer-to-peer balance offsets</p>
            </div>
            <span className="text-xs font-bold text-slate-500 bg-slate-100 px-2.5 py-1 rounded-full">
              {settlements.filter((s) => s.status === 'PENDING').length} Pending
            </span>
          </div>

          <div className="space-y-2.5">
            {settlements.map((s) => {
              const isSettled = s.status === 'SETTLED';
              return (
                <div
                  key={s.id}
                  className={`flex flex-wrap items-center justify-between gap-3 p-3.5 rounded-2xl border transition-all ${
                    isSettled ? 'bg-slate-50 border-slate-100 opacity-65' : 'bg-white border-slate-200/80 shadow-sm'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-xs font-bold text-slate-700">
                      {(s.fromUser?.name || 'U').charAt(0).toUpperCase()}
                    </div>
                    <div className="text-xs font-semibold text-slate-800 flex items-center gap-1.5 truncate">
                      <span className="font-bold text-slate-900">{s.fromUser?.name || 'User'}</span>
                      <span className="text-slate-400">owes</span>
                      <span className="font-bold text-slate-900">{s.toUser?.name || 'Companion'}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-sm font-black text-slate-900">₹{Number(s.amount).toLocaleString()}</span>
                    {!isSettled ? (
                      <button
                        onClick={() => settleMutation.mutate(s.id)}
                        className="travel-button-secondary px-3 py-1.5 text-xs font-bold border-emerald-200 text-emerald-700 hover:bg-emerald-50"
                      >
                        Mark Settled ✓
                      </button>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                        <CheckIcon className="w-3 h-3" /> Settled
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ═══ EXPENSE LOG ═══ */}
      <div className="space-y-3">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-sm font-bold text-slate-900">Expense History</h3>
          <span className="text-xs text-slate-500 font-semibold">{expenses.length} Records</span>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="skeleton h-16 rounded-2xl" />
            ))}
          </div>
        ) : expenses.length === 0 ? (
          <div className="voyara-card p-10 text-center">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-primary flex items-center justify-center mx-auto mb-3">
              <CreditCardIcon className="w-6 h-6" />
            </div>
            <h4 className="text-sm font-bold text-slate-900">No Expenses Logged Yet</h4>
            <p className="text-xs text-slate-500 mt-1 max-w-xs mx-auto">
              Add shared flight tickets, accommodation, meals, or transfers to split costs equally.
            </p>
          </div>
        ) : (
          expenses.map((expense) => {
            const typeInfo =
              EXPENSE_TYPES.find((t) => t.value === expense.expenseType) || EXPENSE_TYPES[EXPENSE_TYPES.length - 1];
            const IconComponent = typeInfo.icon;

            return (
              <div
                key={expense.id}
                className="voyara-card p-4 flex items-center justify-between gap-3 hover:border-slate-300 transition"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-10 h-10 rounded-xl bg-blue-50 text-primary flex items-center justify-center shrink-0">
                    <IconComponent className="w-5 h-5" />
                  </div>
                  <div className="min-w-0">
                    <h5 className="text-sm font-bold text-slate-900 truncate">{expense.description}</h5>
                    <p className="text-xs text-slate-500">
                      Paid by <span className="font-semibold text-slate-700">{expense.paidBy?.name || 'Companion'}</span> ·{' '}
                      {typeInfo.label}
                    </p>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <span className="text-sm font-black text-slate-900">₹{Number(expense.amount).toLocaleString()}</span>
                  <span className="text-[10px] text-slate-400 block">Split {expense.splitMode?.toLowerCase() || 'equally'}</span>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Add Expense Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="voyara-card p-6 w-full max-w-md shadow-2xl animate-fade-in">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
              <h4 className="text-base font-bold text-slate-900">Log New Expense</h4>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600 text-lg">
                ✕
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                addMutation.mutate(newExpense);
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Description
                </label>
                <input
                  type="text"
                  value={newExpense.description}
                  onChange={(e) => setNewExpense({ ...newExpense, description: e.target.value })}
                  placeholder="e.g. Airport Transfer, Dinner at Baga"
                  className="voyara-input text-xs font-medium"
                  required
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Amount (₹)
                  </label>
                  <input
                    type="number"
                    value={newExpense.amount}
                    onChange={(e) => setNewExpense({ ...newExpense, amount: e.target.value })}
                    placeholder="2500"
                    min="1"
                    className="voyara-input text-xs font-bold"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Category
                  </label>
                  <select
                    value={newExpense.expenseType}
                    onChange={(e) => setNewExpense({ ...newExpense, expenseType: e.target.value })}
                    className="voyara-input text-xs font-medium"
                  >
                    {EXPENSE_TYPES.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="pt-2 flex gap-2.5">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="travel-button-secondary flex-1 py-2.5 text-xs font-bold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={addMutation.isPending}
                  className="travel-button-primary flex-1 py-2.5 text-xs font-bold"
                >
                  {addMutation.isPending ? 'Saving...' : 'Save Expense'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
