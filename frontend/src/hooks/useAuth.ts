import { useAuthStore, type User, type AuthResponse } from '@/store/authStore';
import api from '@/lib/axios';
import { useNavigate } from 'react-router-dom';
import { useCallback } from 'react';
import toast from 'react-hot-toast';

export function useAuth() {
  const { user, isAuthenticated, login: storeLogin, logout: storeLogout } = useAuthStore();
  const navigate = useNavigate();

  const login = useCallback(async (email: string, password: string) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const authData: AuthResponse = response.data.data;
      storeLogin(authData);
      toast.success(`Welcome back, ${authData.user.name}!`);

      switch (authData.user.role) {
        case 'ADMIN': navigate('/admin/dashboard'); break;
        case 'STAFF': navigate('/staff/dashboard'); break;
        case 'STUDENT': navigate('/student/dashboard'); break;
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Invalid credentials';
      toast.error(message);
      throw error;
    }
  }, [storeLogin, navigate]);

  const register = useCallback(async (data: {
    name: string; email: string; password: string; role: string;
    roomNumber?: string; block?: string; phone?: string; staffCategory?: string;
  }) => {
    try {
      const response = await api.post('/auth/register', data);
      const authData: AuthResponse = response.data.data;
      storeLogin(authData);
      toast.success('Account created successfully!');

      switch (authData.user.role) {
        case 'ADMIN': navigate('/admin/dashboard'); break;
        case 'STAFF': navigate('/staff/dashboard'); break;
        case 'STUDENT': navigate('/student/dashboard'); break;
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Registration failed';
      toast.error(message);
      throw error;
    }
  }, [storeLogin, navigate]);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Ignore logout errors
    }
    storeLogout();
    navigate('/login');
    toast.success('Logged out successfully');
  }, [storeLogout, navigate]);

  return { user, isAuthenticated, login, register, logout };
}
