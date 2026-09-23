import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

/**
 * Resolves the WebSocket broker URL:
 * - If VITE_API_BASE_URL is defined (production on Vercel connecting to Railway):
 *   Converts https:// -> wss:// and http:// -> ws://, then appends /ws.
 * - Otherwise (local development):
 *   Falls back to window.location via Vite /ws proxy.
 */
function getBrokerUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL;
  if (apiBase) {
    const cleanUrl = apiBase.replace(/\/+$/, '');
    if (cleanUrl.startsWith('https://')) {
      return cleanUrl.replace(/^https:\/\//, 'wss://') + '/ws';
    }
    if (cleanUrl.startsWith('http://')) {
      return cleanUrl.replace(/^http:\/\//, 'ws://') + '/ws';
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${cleanUrl.replace(/^\/+/, '')}/ws`;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

/**
 * Custom hook for WebSocket/STOMP connection.
 * Subscribes to flight status and price update topics.
 */
export function useWebSocket() {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const subscriptionsRef = useRef(new Map());

  useEffect(() => {
    const brokerURL = getBrokerUrl();
    const client = new Client({
      brokerURL,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnected(true);
        // Re-subscribe to any active topics
        subscriptionsRef.current.forEach((callback, topic) => {
          client.subscribe(topic, (message) => {
            try {
              const data = JSON.parse(message.body);
              callback(data);
            } catch (e) {
              console.error('WebSocket message parse error:', e);
            }
          });
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  /** Subscribe to a topic and receive parsed messages */
  const subscribe = useCallback((topic, callback) => {
    subscriptionsRef.current.set(topic, callback);

    if (clientRef.current?.connected) {
      clientRef.current.subscribe(topic, (message) => {
        try {
          const data = JSON.parse(message.body);
          callback(data);
        } catch (e) {
          console.error('WebSocket message parse error:', e);
        }
      });
    }

    return () => {
      subscriptionsRef.current.delete(topic);
    };
  }, []);

  return { connected, subscribe };
}

/**
 * Hook for tracking multiple flights with live updates.
 * Returns a map of flightId -> latest status.
 */
export function useFlightTracking(subscribe) {
  const [flightStatuses, setFlightStatuses] = useState({});
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    if (!subscribe) return;

    // Subscribe to the "all flights" topic for dashboard-wide updates
    const unsub = subscribe('/topic/flight-status/all', (data) => {
      setFlightStatuses((prev) => ({
        ...prev,
        [data.flightId]: data,
      }));

      // Add to notifications list for the notification panel
      if (data.message) {
        const notification = {
          id: Date.now(),
          flightNumber: data.flightNumber,
          message: data.message,
          status: data.status,
          timestamp: new Date().toISOString(),
        };
        setNotifications((prev) => [notification, ...prev].slice(0, 50));

        // Trigger browser notification if permitted
        if ('Notification' in window && Notification.permission === 'granted') {
          new Notification('Flight Update', {
            body: data.message,
            icon: '/airplane-icon.png',
          });
        }
      }
    });

    return unsub;
  }, [subscribe]);

  const clearNotifications = useCallback(() => setNotifications([]), []);

  return { flightStatuses, notifications, clearNotifications };
}

/**
 * Hook for live price updates.
 */
export function usePriceUpdates(subscribe, entityType, entityId) {
  const [priceUpdate, setPriceUpdate] = useState(null);

  useEffect(() => {
    if (!subscribe || !entityType || !entityId) return;

    const topic = `/topic/price-updates/${entityType}/${entityId}`;
    const unsub = subscribe(topic, (data) => {
      setPriceUpdate(data);
    });

    return unsub;
  }, [subscribe, entityType, entityId]);

  return priceUpdate;
}

/**
 * Request browser notification permission.
 */
export function requestNotificationPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
  }
}
