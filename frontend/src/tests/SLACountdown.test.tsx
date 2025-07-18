import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import SLACountdown from '@/components/complaint/SLACountdown';

describe('SLACountdown', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows nothing for resolved complaints', () => {
    const { container } = render(
      <SLACountdown slaDeadline={new Date().toISOString()} status="RESOLVED" />
    );
    expect(container.innerHTML).toBe('');
  });

  it('shows remaining time for active complaints', () => {
    const future = new Date(Date.now() + 2 * 3600000).toISOString();
    render(<SLACountdown slaDeadline={future} status="ASSIGNED" />);
    expect(screen.getByText(/remaining/i)).toBeDefined();
  });

  it('shows overdue state when deadline has passed', () => {
    const past = new Date(Date.now() - 3600000).toISOString();
    render(<SLACountdown slaDeadline={past} status="IN_PROGRESS" />);
    expect(screen.getByText(/OVERDUE/i)).toBeDefined();
  });

  it('shows nothing when slaDeadline is null', () => {
    const { container } = render(
      <SLACountdown slaDeadline={null} status="ASSIGNED" />
    );
    expect(container.innerHTML).toBe('');
  });

  it('shows compact mode correctly', () => {
    const future = new Date(Date.now() + 5 * 3600000).toISOString();
    render(<SLACountdown slaDeadline={future} status="ASSIGNED" compact />);
    expect(screen.getByText(/remaining/i)).toBeDefined();
  });
});
