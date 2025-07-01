export const queryKeys = {
  complaints: {
    all: ['complaints'] as const,
    list: (filters: object) => ['complaints', 'list', filters] as const,
    detail: (id: string) => ['complaints', 'detail', id] as const,
    timeline: (id: string) => ['complaints', 'timeline', id] as const,
    nearby: (block: string) => ['complaints', 'nearby', block] as const,
  },
  notifications: {
    all: ['notifications'] as const,
    list: (page: number) => ['notifications', 'list', page] as const,
    unreadCount: ['notifications', 'unread-count'] as const,
  },
  analytics: {
    dashboard: ['analytics', 'dashboard'] as const,
    categories: ['analytics', 'categories'] as const,
    blocks: ['analytics', 'blocks'] as const,
    staffPerformance: ['analytics', 'staff-performance'] as const,
    trend: (days: number) => ['analytics', 'trend', days] as const,
    heatmap: ['analytics', 'heatmap'] as const,
  },
  admin: {
    staff: ['admin', 'staff'] as const,
    staffWorkload: (id: string) => ['admin', 'staff', id, 'workload'] as const,
    announcements: ['admin', 'announcements'] as const,
    auditLog: (page: number) => ['admin', 'audit-log', page] as const,
    maintenanceSchedules: ['admin', 'maintenance-schedules'] as const,
  },
  users: {
    me: ['users', 'me'] as const,
  },
  ratings: {
    staff: (id: string) => ['ratings', 'staff', id] as const,
  },
};
