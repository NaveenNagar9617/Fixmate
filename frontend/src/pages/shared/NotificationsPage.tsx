import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import api from '@/lib/axios';
import NotificationItem from '@/components/notification/NotificationItem';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import EmptyState from '@/components/common/EmptyState';
import { Button } from '@/components/ui/Button';
import { useNavigate } from 'react-router-dom';
import { useNotificationStore } from '@/store/notificationStore';
import { Bell, ChevronLeft, ChevronRight, CheckCheck } from 'lucide-react';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const { setUnreadCount } = useNotificationStore();

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.notifications.list(page),
    queryFn: async () => {
      const response = await api.get(`/notifications?page=${page}&size=20`);
      return response.data.data;
    },
  });

  const markRead = useMutation({
    mutationFn: async (id: string) => { await api.patch(`/notifications/${id}/read`); },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.list(page) });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.unreadCount });
    },
  });

  const markAllRead = useMutation({
    mutationFn: async () => { await api.patch('/notifications/read-all'); },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications.list(page) });
      setUnreadCount(0);
    },
  });

  const notifications = data?.content || [];
  const totalPages = data?.totalPages || 0;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Notifications</h1>
          <p className="text-muted-foreground text-sm mt-1">Stay updated on your complaints</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => markAllRead.mutate()}>
          <CheckCheck className="h-4 w-4 mr-2" /> Mark all read
        </Button>
      </div>

      {isLoading ? (
        <LoadingSpinner label="Loading notifications..." />
      ) : notifications.length === 0 ? (
        <EmptyState icon={<Bell className="h-8 w-8 text-muted-foreground" />}
          title="No notifications" description="You're all caught up!" />
      ) : (
        <>
          <div className="bg-card rounded-xl border overflow-hidden divide-y">
            {notifications.map((n: any) => (
              <div key={n.id} onClick={() => {
                if (!n.isRead) markRead.mutate(n.id);
              }}>
                <NotificationItem
                  notification={n}
                  onClick={() => {
                    if (n.complaintId) navigate(`/complaints/${n.complaintId}`);
                  }}
                />
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <Button variant="outline" size="sm" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">Page {page + 1} of {totalPages}</span>
              <Button variant="outline" size="sm" onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= totalPages}>
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
