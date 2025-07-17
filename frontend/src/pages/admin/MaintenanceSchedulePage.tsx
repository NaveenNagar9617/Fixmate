import { useState } from 'react';
import { useMaintenanceSchedules } from '@/hooks/useAnalytics';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import api from '@/lib/axios';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import EmptyState from '@/components/common/EmptyState';
import { Wrench, PlusCircle, Trash2, Loader2 } from 'lucide-react';
import { formatShortDate } from '@/lib/utils';
import toast from 'react-hot-toast';

const categories = ['ELECTRICAL', 'PLUMBING', 'WIFI', 'FURNITURE', 'CLEANING', 'PEST_CONTROL', 'SECURITY', 'OTHER'];

export default function MaintenanceSchedulePage() {
  const { data: schedules, isLoading } = useMaintenanceSchedules();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [form, setForm] = useState({
    title: '', category: 'CLEANING', block: 'Block A',
    recurrenceDays: '7', assignedStaffId: '',
  });

  const createMutation = useMutation({
    mutationFn: async (data: any) => { await api.post('/admin/maintenance-schedules', data); },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.maintenanceSchedules });
      setShowForm(false);
      toast.success('Schedule created');
    },
    onError: () => toast.error('Failed to create schedule'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/admin/maintenance-schedules/${id}`); },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.maintenanceSchedules });
      setDeleteTarget(null);
      toast.success('Schedule deleted');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title) return;
    createMutation.mutate({
      ...form,
      recurrenceDays: parseInt(form.recurrenceDays),
      assignedStaffId: form.assignedStaffId || null,
      nextDue: new Date(Date.now() + parseInt(form.recurrenceDays) * 86400000).toISOString().split('T')[0],
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Maintenance Schedules</h1>
          <p className="text-muted-foreground text-sm mt-1">Configure recurring maintenance tasks</p>
        </div>
        <Button variant="gradient" onClick={() => setShowForm(!showForm)}>
          <PlusCircle className="h-4 w-4 mr-2" /> New Schedule
        </Button>
      </div>

      {showForm && (
        <Card className="animate-scale-in">
          <CardContent className="pt-6">
            <form onSubmit={handleCreate} className="space-y-4">
              <Input value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                placeholder="Schedule title (e.g. Weekly corridor cleaning)" />
              <div className="grid grid-cols-3 gap-4">
                <Select value={form.category} onValueChange={(v) => setForm((f) => ({ ...f, category: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {categories.map((c) => <SelectItem key={c} value={c}>{c.replace(/_/g, ' ')}</SelectItem>)}
                  </SelectContent>
                </Select>
                <Select value={form.block} onValueChange={(v) => setForm((f) => ({ ...f, block: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {['Block A', 'Block B', 'Block C', 'Block D', 'Block E'].map((b) => (
                      <SelectItem key={b} value={b}>{b}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Input type="number" value={form.recurrenceDays} min="1"
                  onChange={(e) => setForm((f) => ({ ...f, recurrenceDays: e.target.value }))}
                  placeholder="Days between" />
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                  Create
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <LoadingSpinner label="Loading schedules..." />
      ) : !schedules || schedules.length === 0 ? (
        <EmptyState icon={<Wrench className="h-8 w-8 text-muted-foreground" />}
          title="No schedules" description="Create recurring maintenance schedules." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-3 font-medium text-muted-foreground">Title</th>
                <th className="text-left py-3 px-3 font-medium text-muted-foreground">Category</th>
                <th className="text-left py-3 px-3 font-medium text-muted-foreground">Block</th>
                <th className="text-center py-3 px-3 font-medium text-muted-foreground">Every</th>
                <th className="text-left py-3 px-3 font-medium text-muted-foreground">Next Due</th>
                <th className="text-center py-3 px-3 font-medium text-muted-foreground">Status</th>
                <th className="py-3 px-3"></th>
              </tr>
            </thead>
            <tbody>
              {schedules.map((s: any) => (
                <tr key={s.id} className="border-b hover:bg-muted/50 transition-colors">
                  <td className="py-3 px-3 font-medium">{s.title}</td>
                  <td className="py-3 px-3">{s.category?.replace(/_/g, ' ')}</td>
                  <td className="py-3 px-3">{s.block}</td>
                  <td className="py-3 px-3 text-center">{s.recurrenceDays}d</td>
                  <td className="py-3 px-3">{s.nextDue ? formatShortDate(s.nextDue) : 'N/A'}</td>
                  <td className="py-3 px-3 text-center">
                    <Badge variant={s.isActive ? 'success' : 'secondary'}>{s.isActive ? 'Active' : 'Inactive'}</Badge>
                  </td>
                  <td className="py-3 px-3">
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={() => setDeleteTarget(s.id)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog open={!!deleteTarget} onOpenChange={() => setDeleteTarget(null)}
        title="Delete Schedule" description="This maintenance schedule will be permanently removed."
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget)}
        confirmLabel="Delete" destructive loading={deleteMutation.isPending} />
    </div>
  );
}
