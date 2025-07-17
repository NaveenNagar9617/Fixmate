import { useState } from 'react';
import { useComplaints, useAssignStaff, type ComplaintFilters as ComplaintFiltersType } from '@/hooks/useComplaints';
import { useStaffList } from '@/hooks/useAnalytics';
import ComplaintCard from '@/components/complaint/ComplaintCard';
import ComplaintFilters from '@/components/complaint/ComplaintFilters';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import EmptyState from '@/components/common/EmptyState';
import { Button } from '@/components/ui/Button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/Dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import { ChevronLeft, ChevronRight, Download } from 'lucide-react';
import api from '@/lib/axios';
import toast from 'react-hot-toast';

export default function AllComplaintsPage() {
  const [filters, setFilters] = useState<ComplaintFiltersType>({ page: 0, size: 10, sort: 'createdAt,desc' });
  const { data, isLoading } = useComplaints(filters);
  const { data: staffList } = useStaffList();
  const assignStaff = useAssignStaff();

  const [reassignDialogOpen, setReassignDialogOpen] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState<string | null>(null);
  const [selectedStaff, setSelectedStaff] = useState('');

  const complaints = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const handleExport = async () => {
    try {
      const response = await api.get('/admin/export/complaints', { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([response.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = `complaints_export_${new Date().toISOString().split('T')[0]}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      toast.success('CSV exported successfully');
    } catch {
      toast.error('Failed to export');
    }
  };

  const handleReassign = async () => {
    if (!selectedComplaint || !selectedStaff) return;
    await assignStaff.mutateAsync({ id: selectedComplaint, staffId: selectedStaff });
    setReassignDialogOpen(false);
    setSelectedComplaint(null);
    setSelectedStaff('');
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">All Complaints</h1>
          <p className="text-muted-foreground text-sm mt-1">Manage and oversee all maintenance complaints</p>
        </div>
        <Button variant="outline" onClick={handleExport}>
          <Download className="h-4 w-4 mr-2" /> Export CSV
        </Button>
      </div>

      <ComplaintFilters filters={filters} onFilterChange={setFilters} />

      {isLoading ? (
        <LoadingSpinner label="Loading complaints..." />
      ) : complaints.length === 0 ? (
        <EmptyState title="No complaints found" description="No complaints match your current filters." />
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
                onClick={() => setFilters((f) => ({ ...f, page: Math.max(0, (f.page ?? 0) - 1) }))}
                disabled={!(filters.page ?? 0)}>
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-muted-foreground">Page {(filters.page ?? 0) + 1} of {totalPages}</span>
              <Button variant="outline" size="sm"
                onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) + 1 }))}
                disabled={(filters.page ?? 0) + 1 >= totalPages}>
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}

      <Dialog open={reassignDialogOpen} onOpenChange={setReassignDialogOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Reassign Complaint</DialogTitle></DialogHeader>
          <Select value={selectedStaff} onValueChange={setSelectedStaff}>
            <SelectTrigger><SelectValue placeholder="Select staff member" /></SelectTrigger>
            <SelectContent>
              {(staffList || []).map((s: any) => (
                <SelectItem key={s.user.id} value={s.user.id}>{s.user.name} - {s.user.staffCategory}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <DialogFooter>
            <Button variant="outline" onClick={() => setReassignDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleReassign} disabled={!selectedStaff || assignStaff.isPending}>Assign</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
