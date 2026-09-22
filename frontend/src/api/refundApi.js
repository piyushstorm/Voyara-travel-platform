import api from './axios';

export async function previewRefund(bookingId) {
  const res = await api.get(`/refunds/preview/${bookingId}`);
  return res.data;
}

export async function getMyRefunds() {
  const res = await api.get('/refunds');
  return res.data;
}

export async function getRefundStatus(refundId) {
  const res = await api.get(`/refunds/${refundId}`);
  return res.data;
}

export async function getCancellationReasons() {
  const res = await api.get('/refunds/reasons');
  return res.data;
}
