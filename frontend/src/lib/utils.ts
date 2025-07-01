import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { format, formatDistanceToNow, differenceInHours, differenceInMinutes } from 'date-fns';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(date: string | Date): string {
  return format(new Date(date), 'MMM dd, yyyy HH:mm');
}

export function formatShortDate(date: string | Date): string {
  return format(new Date(date), 'MMM dd, yyyy');
}

export function formatTimeAgo(date: string | Date): string {
  return formatDistanceToNow(new Date(date), { addSuffix: true });
}

export function formatDuration(hours: number): string {
  if (hours < 1) return `${Math.round(hours * 60)}m`;
  if (hours < 24) return `${Math.round(hours)}h`;
  return `${Math.round(hours / 24)}d ${Math.round(hours % 24)}h`;
}

export function getPriorityColor(priority: string): string {
  const colors: Record<string, string> = {
    CRITICAL: 'bg-red-500/15 text-red-600 border-red-500/30',
    HIGH: 'bg-orange-500/15 text-orange-600 border-orange-500/30',
    MEDIUM: 'bg-amber-500/15 text-amber-600 border-amber-500/30',
    LOW: 'bg-emerald-500/15 text-emerald-600 border-emerald-500/30',
  };
  return colors[priority] || 'bg-gray-500/15 text-gray-600 border-gray-500/30';
}

export function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    SUBMITTED: 'bg-blue-500/15 text-blue-600 border-blue-500/30',
    ASSIGNED: 'bg-indigo-500/15 text-indigo-600 border-indigo-500/30',
    IN_PROGRESS: 'bg-yellow-500/15 text-yellow-700 border-yellow-500/30',
    RESOLVED: 'bg-emerald-500/15 text-emerald-600 border-emerald-500/30',
    CLOSED: 'bg-gray-500/15 text-gray-600 border-gray-500/30',
    ESCALATED: 'bg-red-500/15 text-red-600 border-red-500/30',
    REOPENED: 'bg-purple-500/15 text-purple-600 border-purple-500/30',
  };
  return colors[status] || 'bg-gray-500/15 text-gray-600 border-gray-500/30';
}

export function getStatusDot(status: string): string {
  const colors: Record<string, string> = {
    SUBMITTED: 'bg-blue-500',
    ASSIGNED: 'bg-indigo-500',
    IN_PROGRESS: 'bg-yellow-500',
    RESOLVED: 'bg-emerald-500',
    CLOSED: 'bg-gray-500',
    ESCALATED: 'bg-red-500',
    REOPENED: 'bg-purple-500',
  };
  return colors[status] || 'bg-gray-500';
}

export type SLAInfo = {
  label: string;
  color: 'green' | 'amber' | 'red';
  isOverdue: boolean;
  hoursRemaining: number;
};

export function getSLAStatus(slaDeadline: string | null, status: string): SLAInfo {
  if (!slaDeadline || ['RESOLVED', 'CLOSED'].includes(status)) {
    return { label: 'N/A', color: 'green', isOverdue: false, hoursRemaining: 0 };
  }

  const deadline = new Date(slaDeadline);
  const now = new Date();
  const hoursLeft = differenceInHours(deadline, now);
  const minutesLeft = differenceInMinutes(deadline, now);

  if (minutesLeft <= 0) {
    const overdueHours = Math.abs(hoursLeft);
    const overdueMinutes = Math.abs(minutesLeft) % 60;
    return {
      label: `OVERDUE by ${overdueHours}h ${overdueMinutes}m`,
      color: 'red',
      isOverdue: true,
      hoursRemaining: hoursLeft,
    };
  }

  const label = hoursLeft >= 1
    ? `${hoursLeft}h ${minutesLeft % 60}m remaining`
    : `${minutesLeft}m remaining`;

  if (hoursLeft <= 1) {
    return { label, color: 'red', isOverdue: false, hoursRemaining: hoursLeft };
  }
  if (hoursLeft <= 4) {
    return { label, color: 'amber', isOverdue: false, hoursRemaining: hoursLeft };
  }
  return { label, color: 'green', isOverdue: false, hoursRemaining: hoursLeft };
}

export function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    ELECTRICAL: '⚡', PLUMBING: '🔧', WIFI: '📶', FURNITURE: '🪑',
    CLEANING: '🧹', PEST_CONTROL: '🐛', SECURITY: '🔒', OTHER: '📋',
  };
  return icons[category] || '📋';
}

export function getCategoryLabel(category: string): string {
  return category.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

export function getStatusLabel(status: string): string {
  return status.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}
