import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/axios';
import { queryKeys } from '@/lib/queryKeys';
import toast from 'react-hot-toast';

export interface ComplaintFilters {
  status?: string;
  category?: string;
  priority?: string;
  block?: string;
  floor?: number;
  fromDate?: string;
  toDate?: string;
  assignedStaffId?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export function useComplaints(filters: ComplaintFilters = {}) {
  return useQuery({
    queryKey: queryKeys.complaints.list(filters),
    queryFn: async () => {
      const params = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, String(value));
        }
      });
      const response = await api.get(`/complaints?${params.toString()}`);
      return response.data.data;
    },
  });
}

export function useComplaintDetail(id: string) {
  return useQuery({
    queryKey: queryKeys.complaints.detail(id),
    queryFn: async () => {
      const response = await api.get(`/complaints/${id}`);
      return response.data.data;
    },
    enabled: !!id,
  });
}

export function useComplaintTimeline(id: string) {
  return useQuery({
    queryKey: queryKeys.complaints.timeline(id),
    queryFn: async () => {
      const response = await api.get(`/complaints/${id}/timeline`);
      return response.data.data;
    },
    enabled: !!id,
  });
}

export function useCreateComplaint() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (formData: FormData) => {
      const response = await api.post('/complaints', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.all });
      toast.success('Complaint submitted successfully!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to submit complaint');
    },
  });
}

export function useUpdateStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, newStatus, note }: { id: string; newStatus: string; note?: string }) => {
      const response = await api.patch(`/complaints/${id}/status`, { newStatus, note });
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.all });
      toast.success('Status updated successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to update status');
    },
  });
}

export function useUpdatePriority() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, priority, reason }: { id: string; priority: string; reason: string }) => {
      const response = await api.patch(`/complaints/${id}/priority`, { priority, reason });
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.all });
      toast.success('Priority updated & SLA deadline recalculated');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to update priority');
    },
  });
}

export function useAssignStaff() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, staffId }: { id: string; staffId: string }) => {
      const response = await api.put(`/complaints/${id}/assign`, { staffId });
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.all });
      toast.success('Staff assigned successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to assign staff');
    },
  });
}

export function useUploadAfterPhoto() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, file }: { id: string; file: File }) => {
      const formData = new FormData();
      formData.append('photo', file);
      const response = await api.post(`/complaints/${id}/photo/after`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.detail(variables.id) });
      toast.success('After photo uploaded');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to upload photo');
    },
  });
}

export function useAddComment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, content, isInternal }: { id: string; content: string; isInternal: boolean }) => {
      const response = await api.post(`/complaints/${id}/comments`, { content, isInternal });
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.detail(variables.id) });
      toast.success('Comment added');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to add comment');
    },
  });
}

export function useUpvote() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/complaints/${id}/upvote`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.all });
    },
  });
}

export function useRemoveUpvote() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/complaints/${id}/upvote`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.all });
    },
  });
}

export function useCreateRating() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, stars, feedbackText }: { id: string; stars: number; feedbackText: string }) => {
      const response = await api.post(`/complaints/${id}/rating`, { stars, feedbackText });
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.complaints.detail(variables.id) });
      toast.success('Thank you for your rating!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to submit rating');
    },
  });
}
