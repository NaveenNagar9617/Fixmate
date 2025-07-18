import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import LoginPage from '@/pages/auth/LoginPage';

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    login: vi.fn().mockResolvedValue(undefined),
    register: vi.fn(),
    logout: vi.fn(),
    user: null,
    isAuthenticated: false,
  }),
}));

function renderLogin() {
  return render(
    <BrowserRouter>
      <LoginPage />
    </BrowserRouter>
  );
}

describe('LoginPage', () => {
  it('renders login form', () => {
    renderLogin();
    expect(screen.getByText('FixMate')).toBeDefined();
    expect(screen.getByText('Sign In')).toBeDefined();
    expect(screen.getByPlaceholderText('you@example.com')).toBeDefined();
  });

  it('shows validation errors for empty form', async () => {
    renderLogin();
    const signInBtn = screen.getByText('Sign In');
    fireEvent.click(signInBtn);

    await waitFor(() => {
      expect(screen.getByText('Email is required')).toBeDefined();
      expect(screen.getByText('Password is required')).toBeDefined();
    });
  });

  it('shows email validation error for invalid format', async () => {
    renderLogin();
    const emailInput = screen.getByPlaceholderText('you@example.com');
    fireEvent.change(emailInput, { target: { value: 'invalid' } });

    const passwordInput = screen.getByPlaceholderText('••••••••');
    fireEvent.change(passwordInput, { target: { value: 'password123' } });

    const signInBtn = screen.getByText('Sign In');
    fireEvent.click(signInBtn);

    await waitFor(() => {
      expect(screen.getByText('Invalid email format')).toBeDefined();
    });
  });

  it('shows link to register page', () => {
    renderLogin();
    expect(screen.getByText('Register')).toBeDefined();
  });

  it('shows demo credentials', () => {
    renderLogin();
    expect(screen.getByText(/admin@fixmate.com/)).toBeDefined();
  });
});
