import { useState } from 'react';
import { useComplaints, useUpdateStatus } from '@/hooks/useComplaints';
import ComplaintCard from '@/components/complaint/ComplaintCard';
import StatsCard from '@/components/analytics/StatsCard';
import EmptyState from '@/components/common/EmptyState';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { Button } from '@/components/ui/Button';
import { ClipboardList, Clock, CheckCircle, ChevronLeft, ChevronRight } from 'lucide-react';

export default function StaffDashboard() {
  const [filters, setFilters] = useState({ page: 0, size: 10, sort: 'slaDeadline,asc' });
  const { data, isLoading } = useComplaints(filters);
  const complaints = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const assigned = complaints.filter((c: any) => c.status === 'ASSIGNED').length;
  const inProgress = complaints.filter((c: any) => c.status === 'IN_PROGRESS').length;
  const resolved = complaints.filter((c: any) => ['RESOLVED', 'CLOSED'].includes(c.status)).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Staff Dashboard</h1>
        <p className="text-muted-foreground text-sm mt-1">Your assigned complaints sorted by SLA deadline</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatsCard label="Assigned" value={assigned} icon={<ClipboardList className="h-5 w-5" />} color="from-indigo-500 to-blue-600" />
        <StatsCard label="In Progress" value={inProgress} icon={<Clock className="h-5 w-5" />} color="from-amber-500 to-orange-600" />
        <StatsCard label="Resolved" value={resolved} icon={<CheckCircle className="h-5 w-5" />} color="from-emerald-500 to-teal-600" />
      </div>

      {isLoading ? (
        <LoadingSpinner label="Loading assignments..." />
      ) : complaints.length === 0 ? (
        <EmptyState title="No assignments" description="You don't have any complaints assigned yet." />
      ) : (
        <>
          <div className="grid gap-4">
            {complaints.map((complaint: any) => (
              <ComplaintCard key={complaint.id} complaint={complaint} />
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-4">
              <Button variant="outline" size="sm"
                onClick={() => setFilters((f) => ({ ...f, page: Math.max(0, f.page - 1) }))}
                disabled={!filters.page}>
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">Page {filters.page + 1} of {totalPages}</span>
              <Button variant="outline" size="sm"
                onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}
                disabled={filters.page + 1 >= totalPages}>
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
