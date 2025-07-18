import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ComplaintCard from '@/components/complaint/ComplaintCard';

const mockComplaint = {
  id: '1',
  title: 'Broken AC in Room 301',
  category: 'ELECTRICAL',
  priority: 'HIGH',
  status: 'ASSIGNED',
  locationBlock: 'Block A',
  locationFloor: 3,
  roomNumber: '301',
  upvoteCount: 5,
  slaDeadline: new Date(Date.now() + 3600000).toISOString(),
  assignedStaff: { name: 'Ravi Kumar' },
  createdAt: new Date().toISOString(),
};

function renderCard() {
  return render(
    <BrowserRouter>
      <ComplaintCard complaint={mockComplaint} />
    </BrowserRouter>
  );
}

describe('ComplaintCard', () => {
  it('renders the complaint title', () => {
    renderCard();
    expect(screen.getByText('Broken AC in Room 301')).toBeDefined();
  });

  it('renders priority badge', () => {
    renderCard();
    expect(screen.getByText('HIGH')).toBeDefined();
  });

  it('renders status badge', () => {
    renderCard();
    expect(screen.getByText('ASSIGNED')).toBeDefined();
  });

  it('renders location info', () => {
    renderCard();
    expect(screen.getByText(/Block A/)).toBeDefined();
  });

  it('renders upvote count', () => {
    renderCard();
    expect(screen.getByText('5')).toBeDefined();
  });

  it('renders assigned staff name', () => {
    renderCard();
    expect(screen.getByText('Ravi Kumar')).toBeDefined();
  });
});
