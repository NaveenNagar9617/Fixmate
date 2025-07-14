import { useQuery } from '@tanstack/react-query';
import api from '@/lib/axios';
import { queryKeys } from '@/lib/queryKeys';

export function useDashboardStats() {
  return useQuery({
    queryKey: queryKeys.analytics.dashboard,
    queryFn: async () => {
      const response = await api.get('/analytics/dashboard');
      return response.data.data;
    },
  });
}

export function useCategoryBreakdown() {
  return useQuery({
    queryKey: queryKeys.analytics.categories,
    queryFn: async () => {
      const response = await api.get('/analytics/categories');
      return response.data.data;
    },
  });
}

export function useBlockStats() {
  return useQuery({
    queryKey: queryKeys.analytics.blocks,
    queryFn: async () => {
      const response = await api.get('/analytics/blocks');
      return response.data.data;
    },
  });
}

export function useStaffPerformance() {
  return useQuery({
    queryKey: queryKeys.analytics.staffPerformance,
    queryFn: async () => {
      const response = await api.get('/analytics/staff-performance');
      return response.data.data;
    },
  });
}

export function useDailyTrend(days: number = 30) {
  return useQuery({
    queryKey: queryKeys.analytics.trend(days),
    queryFn: async () => {
      const response = await api.get(`/analytics/trend?days=${days}`);
      return response.data.data;
    },
  });
}

export function useHeatmap() {
  return useQuery({
    queryKey: queryKeys.analytics.heatmap,
    queryFn: async () => {
      const response = await api.get('/analytics/heatmap');
      return response.data.data;
    },
  });
}

export function useStaffList() {
  return useQuery({
    queryKey: queryKeys.admin.staff,
    queryFn: async () => {
      const response = await api.get('/admin/staff');
      return response.data.data;
    },
  });
}

export function useAnnouncements() {
  return useQuery({
    queryKey: queryKeys.admin.announcements,
    queryFn: async () => {
      const response = await api.get('/admin/announcements');
      return response.data.data;
    },
  });
}

export function useMaintenanceSchedules() {
  return useQuery({
    queryKey: queryKeys.admin.maintenanceSchedules,
    queryFn: async () => {
      const response = await api.get('/admin/maintenance-schedules');
      return response.data.data;
    },
  });
}
