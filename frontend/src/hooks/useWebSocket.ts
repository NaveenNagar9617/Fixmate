import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store/authStore';
import { useNotificationStore } from '@/store/notificationStore';
import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import toast from 'react-hot-toast';

export function useWebSocket() {
  const clientRef = useRef<Client | null>(null);
  const { user, accessToken, isAuthenticated } = useAuthStore();
  const { addNotification, setConnected } = useNotificationStore();
  const queryClient = useQueryClient();

  // Keep latest accessToken in ref to prevent socket teardown/thrashing on token refresh
  const accessTokenRef = useRef<string | null>(accessToken);
  accessTokenRef.current = accessToken;

  useEffect(() => {
    if (!isAuthenticated || !user) {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
        setConnected(false);
      }
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      beforeConnect: () => {
        // Dynamically inject latest token before every handshake/reconnect
        if (accessTokenRef.current) {
          client.connectHeaders = {
            Authorization: `Bearer ${accessTokenRef.current}`,
          };
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,

      onConnect: () => {
        setConnected(true);

        // 1. Private User Queue: /user/queue/notifications
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const notification = JSON.parse(message.body);
            handleIncomingNotification(notification);
          } catch (err) {
            console.error('Failed to parse private notification:', err);
          }
        });

        // 2. Public Broadcast Topic: /topic/announcements
        client.subscribe('/topic/announcements', (message) => {
          try {
            const notification = JSON.parse(message.body);
            handleIncomingNotification(notification);
          } catch (err) {
            console.error('Failed to parse broadcast announcement:', err);
          }
        });
      },

      onDisconnect: () => {
        setConnected(false);
      },

      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message'] || 'Connection error');
        setConnected(false);
      },
    });

    const handleIncomingNotification = (notification: any) => {
      // 1. Add to local notification state
      addNotification(notification);

      // 2. Trigger rich Toast notification
      toast(notification.title, {
        icon: getNotificationIcon(notification.type),
        duration: 5000,
        style: {
          background: '#1e293b',
          color: '#f8fafc',
          borderRadius: '12px',
          border: '1px solid rgba(99, 102, 241, 0.3)',
        },
      });

      // 3. 🚀 REAL-TIME BOARD & STATE SYNC:
      // Invalidate React Query caches so tables, status boards, charts, and stats update immediately without page refresh!
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['complaints'] });
      queryClient.invalidateQueries({ queryKey: ['analytics'] });
      queryClient.invalidateQueries({ queryKey: ['admin'] });

      if (notification.complaintId) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.complaints.detail(notification.complaintId),
        });
        queryClient.invalidateQueries({
          queryKey: queryKeys.complaints.timeline(notification.complaintId),
        });
      }
    };

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
        setConnected(false);
      }
    };
  }, [isAuthenticated, user?.id]);
}

function getNotificationIcon(type: string): string {
  const icons: Record<string, string> = {
    COMPLAINT_ASSIGNED: '📋',
    STATUS_CHANGED: '🔄',
    ESCALATION: '⚠️',
    NEW_COMMENT: '💬',
    SLA_WARNING: '⏰',
    ANNOUNCEMENT: '📢',
  };
  return icons[type] || '🔔';
}

