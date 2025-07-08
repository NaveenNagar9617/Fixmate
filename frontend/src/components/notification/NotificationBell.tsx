import { useState, useRef, useEffect } from 'react';
import { Bell } from 'lucide-react';
import { useNotificationStore } from '@/store/notificationStore';
import NotificationItem from './NotificationItem';
import api from '@/lib/axios';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import { Button } from '@/components/ui/Button';
import { useNavigate } from 'react-router-dom';

export default function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { notifications, unreadCount, setUnreadCount, setNotifications, markAllRead } = useNotificationStore();
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();

  useQuery({
    queryKey: queryKeys.notifications.unreadCount,
    queryFn: async () => {
      const response = await api.get('/notifications/unread-count');
      setUnreadCount(response.data.data);
      return response.data.data;
    },
    enabled: !!user,
  });


  useQuery({
    queryKey: queryKeys.notifications.list(0),
    queryFn: async () => {
      const response = await api.get('/notifications?page=0&size=10');
      setNotifications(response.data.data.content);
      return response.data.data;
    },
    enabled: !!user && isOpen,
  });

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleMarkAllRead = async () => {
    try {
      await api.patch('/notifications/read-all');
      markAllRead();
      setUnreadCount(0);
    } catch { /* ignore */ }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-lg hover:bg-muted transition-colors"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 h-5 w-5 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center animate-scale-in">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-12 w-80 sm:w-96 bg-card border rounded-2xl shadow-2xl z-50 animate-scale-in overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b">
            <h3 className="font-semibold text-sm">Notifications</h3>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="text-xs text-primary hover:underline font-medium"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="py-8 text-center text-sm text-muted-foreground">
                No notifications yet
              </div>
            ) : (
              notifications.slice(0, 10).map((n) => (
                <NotificationItem
                  key={n.id}
                  notification={n}
                  onClick={() => {
                    setIsOpen(false);
                    if (n.complaintId) navigate(`/complaints/${n.complaintId}`);
                  }}
                />
              ))
            )}
          </div>

          <div className="px-4 py-2 border-t">
            <Button
              variant="ghost"
              size="sm"
              className="w-full text-xs"
              onClick={() => { setIsOpen(false); navigate('/notifications'); }}
            >
              View all notifications
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
