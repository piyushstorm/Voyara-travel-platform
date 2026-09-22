import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts';
import { priceApi } from '../api/phase3Api';

export default function PriceHistoryChart({ entityType, entityId, cabinClass, currentPrice }) {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!entityType || !entityId) return;
    setLoading(true);
    priceApi.getHistory(entityType, entityId)
      .then((res) => {
        const data = (res.data || [])
          .filter(h => !cabinClass || h.cabinClass === cabinClass)
          .map(h => ({
            time: new Date(h.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            price: Number(h.newPrice),
            reason: h.reason,
            fullTime: new Date(h.createdAt).toLocaleString(),
          }))
          .reverse();
        setHistory(data);
      })
      .catch(() => setError('Failed to load price history'))
      .finally(() => setLoading(false));
  }, [entityType, entityId, cabinClass]);

  if (loading) {
    return (
      <div className="h-64 flex items-center justify-center">
        <div className="animate-pulse text-gray-400">Loading price history...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-64 flex items-center justify-center text-red-500 text-sm">
        {error}
      </div>
    );
  }

  if (history.length === 0) {
    return (
      <div className="h-64 flex items-center justify-center text-gray-400 text-sm">
        No price history available yet. Prices update every 60 seconds.
      </div>
    );
  }

  const minPrice = Math.min(...history.map(h => h.price));
  const maxPrice = Math.max(...history.map(h => h.price));

  const CustomTooltip = ({ active, payload }) => {
    if (!active || !payload?.length) return null;
    const data = payload[0].payload;
    return (
      <div className="bg-white shadow-lg rounded-lg p-3 border text-sm">
        <p className="font-semibold text-gray-800">₹{data.price.toLocaleString()}</p>
        <p className="text-gray-500 text-xs">{data.fullTime}</p>
        <p className="text-xs mt-1 text-blue-600">{data.reason?.replace('_', ' ')}</p>
      </div>
    );
  };

  return (
    <div className="bg-white rounded-xl border p-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold text-gray-800">Price History</h3>
        <div className="flex items-center gap-3 text-xs">
          <span className="text-green-600">Low: ₹{minPrice.toLocaleString()}</span>
          <span className="text-red-600">High: ₹{maxPrice.toLocaleString()}</span>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={250}>
        <LineChart data={history}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="time"
            tick={{ fontSize: 11 }}
            stroke="#9ca3af"
          />
          <YAxis
            domain={['dataMin - 200', 'dataMax + 200']}
            tick={{ fontSize: 11 }}
            stroke="#9ca3af"
            tickFormatter={(v) => `₹${(v / 1000).toFixed(1)}k`}
          />
          <Tooltip content={<CustomTooltip />} />
          {currentPrice && (
            <ReferenceLine
              y={currentPrice}
              stroke="#3b82f6"
              strokeDasharray="5 5"
              label={{ value: 'Current', position: 'right', fontSize: 11, fill: '#3b82f6' }}
            />
          )}
          <Line
            type="monotone"
            dataKey="price"
            stroke="#6366f1"
            strokeWidth={2}
            dot={{ r: 3, fill: '#6366f1' }}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
