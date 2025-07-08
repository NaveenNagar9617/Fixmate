import { cn, formatTimeAgo } from '@/lib/utils';
import type { Notification } from '@/store/notificationStore';

interface NotificationItemProps {
  notification: Notification;
  onClick?: () => void;
}

const typeIcons: Record<string, string> = {
  COMPLAINT_ASSIGNED: '📋',
  STATUS_CHANGED: '🔄',
  ESCALATION: '⚠️',
  NEW_COMMENT: '💬',
  SLA_WARNING: '⏰',
  ANNOUNCEMENT: '📢',
};

export default function NotificationItem({ notification, onClick }: NotificationItemProps) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'w-full text-left px-4 py-3 hover:bg-muted/50 transition-colors border-b last:border-b-0 flex gap-3',
        !notification.isRead && 'bg-primary/5'
      )}
    >
      <span className="text-lg shrink-0 mt-0.5">{typeIcons[notification.type] || '🔔'}</span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <p className={cn('text-sm truncate', !notification.isRead ? 'font-semibold' : 'font-medium')}>
            {notification.title}
          </p>
          {!notification.isRead && (
            <span className="h-2 w-2 rounded-full bg-primary shrink-0" />
          )}
        </div>
        <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">{notification.message}</p>
        <p className="text-[10px] text-muted-foreground mt-1">{formatTimeAgo(notification.createdAt)}</p>
      </div>
    </button>
  );
}
