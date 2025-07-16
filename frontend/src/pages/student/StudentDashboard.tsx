import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useComplaints, type ComplaintFilters as ComplaintFiltersType } from '@/hooks/useComplaints';
import { useAuthStore } from '@/store/authStore';
import ComplaintCard from '@/components/complaint/ComplaintCard';
import ComplaintFilters from '@/components/complaint/ComplaintFilters';
import StatsCard from '@/components/analytics/StatsCard';
import EmptyState from '@/components/common/EmptyState';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { Button } from '@/components/ui/Button';
import { PlusCircle, FileText, CheckCircle, Star, ChevronLeft, ChevronRight } from 'lucide-react';

export default function StudentDashboard() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const [filters, setFilters] = useState<ComplaintFiltersType>({ page: 0, size: 10, sort: 'createdAt,desc' });
  const { data, isLoading } = useComplaints(filters);

  const complaints = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const openCount = complaints.filter((c: any) =>
    ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'REOPENED'].includes(c.status)
  ).length;
  const resolvedCount = complaints.filter((c: any) =>
    ['RESOLVED', 'CLOSED'].includes(c.status)
  ).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">My Dashboard</h1>
          <p className="text-muted-foreground text-sm mt-1">Track and manage your complaints</p>
        </div>
        <Button variant="gradient" onClick={() => navigate('/student/submit')}>
          <PlusCircle className="h-4 w-4 mr-2" />
          New Complaint
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatsCard
          label="Open Complaints"
          value={openCount}
          icon={<FileText className="h-5 w-5" />}
          color="from-blue-500 to-indigo-600"
        />
        <StatsCard
          label="Resolved"
          value={resolvedCount}
          icon={<CheckCircle className="h-5 w-5" />}
          color="from-emerald-500 to-teal-600"
        />
        <StatsCard
          label="Total Filed"
          value={data?.totalElements || 0}
          icon={<Star className="h-5 w-5" />}
          color="from-purple-500 to-pink-600"
        />
      </div>

      {/* Filters */}
      <ComplaintFilters filters={filters} onFilterChange={setFilters} showBlock={false} />

      {/* Complaints List */}
      {isLoading ? (
        <LoadingSpinner label="Loading complaints..." />
      ) : complaints.length === 0 ? (
        <EmptyState
          title="No complaints yet"
          description="You haven't submitted any complaints. Click the button above to create one."
          actionLabel="Submit Complaint"
          onAction={() => navigate('/student/submit')}
        />
      ) : (
        <>
          <div className="grid gap-4">
            {complaints.map((complaint: any) => (
              <ComplaintCard key={complaint.id} complaint={complaint} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setFilters((f) => ({ ...f, page: Math.max(0, (f.page || 0) - 1) }))}
                disabled={!filters.page}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {(filters.page || 0) + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setFilters((f) => ({ ...f, page: (f.page || 0) + 1 }))}
                disabled={(filters.page || 0) + 1 >= totalPages}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
