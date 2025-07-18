import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '@/store/authStore';
import { act } from '@testing-library/react';

describe('authStore', () => {
  beforeEach(() => {
    act(() => {
      useAuthStore.getState().logout();
    });
  });

  it('starts with default state', () => {
    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.user).toBeNull();
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
  });

  it('login sets user and tokens', () => {
    act(() => {
      useAuthStore.getState().login({
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        user: {
          id: '1',
          name: 'Test User',
          email: 'test@test.com',
          role: 'STUDENT',
          profilePhotoUrl: null,
        },
      });
    });

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.user?.name).toBe('Test User');
    expect(state.user?.role).toBe('STUDENT');
    expect(state.accessToken).toBe('test-access');
    expect(state.refreshToken).toBe('test-refresh');
  });

  it('logout clears all state', () => {
    act(() => {
      useAuthStore.getState().login({
        accessToken: 'token',
        refreshToken: 'refresh',
        user: { id: '1', name: 'User', email: 'u@t.com', role: 'ADMIN', profilePhotoUrl: null },
      });
    });

    act(() => {
      useAuthStore.getState().logout();
    });

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.user).toBeNull();
    expect(state.accessToken).toBeNull();
  });

  it('updateUser merges user data', () => {
    act(() => {
      useAuthStore.getState().login({
        accessToken: 'token',
        refreshToken: 'refresh',
        user: { id: '1', name: 'Old Name', email: 'u@t.com', role: 'STUDENT', profilePhotoUrl: null },
      });
    });

    act(() => {
      useAuthStore.getState().updateUser({ name: 'New Name' });
    });

    expect(useAuthStore.getState().user?.name).toBe('New Name');
    expect(useAuthStore.getState().user?.email).toBe('u@t.com');
  });
});
