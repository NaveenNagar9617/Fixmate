import { useState } from 'react';
import { useAnnouncements } from '@/hooks/useAnalytics';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import api from '@/lib/axios';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import EmptyState from '@/components/common/EmptyState';
import { Megaphone, Trash2, PlusCircle, Loader2 } from 'lucide-react';
import { formatDate } from '@/lib/utils';
import toast from 'react-hot-toast';

export default function AnnouncementsPage() {
  const { data: announcements, isLoading } = useAnnouncements();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [form, setForm] = useState({ title: '', content: '', targetAudience: 'ALL', expiresAt: '' });

  const createMutation = useMutation({
    mutationFn: async (data: typeof form) => {
      await api.post('/admin/announcements', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.announcements });
      setShowForm(false);
      setForm({ title: '', content: '', targetAudience: 'ALL', expiresAt: '' });
      toast.success('Announcement created');
    },
    onError: () => toast.error('Failed to create announcement'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/admin/announcements/${id}`); },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.announcements });
      setDeleteTarget(null);
      toast.success('Announcement deleted');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title || !form.content) return;
    createMutation.mutate(form);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Announcements</h1>
          <p className="text-muted-foreground text-sm mt-1">Broadcast messages to students and staff</p>
        </div>
        <Button variant="gradient" onClick={() => setShowForm(!showForm)}>
          <PlusCircle className="h-4 w-4 mr-2" /> New Announcement
        </Button>
      </div>

      {showForm && (
        <Card className="animate-scale-in">
          <CardContent className="pt-6">
            <form onSubmit={handleCreate} className="space-y-4">
              <Input value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                placeholder="Announcement title" />
              <Textarea value={form.content} onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                placeholder="Announcement content..." className="min-h-[100px]" />
              <div className="grid grid-cols-2 gap-4">
                <Select value={form.targetAudience} onValueChange={(v) => setForm((f) => ({ ...f, targetAudience: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">Everyone</SelectItem>
                    <SelectItem value="STUDENTS">Students Only</SelectItem>
                    <SelectItem value="STAFF">Staff Only</SelectItem>
                  </SelectContent>
                </Select>
                <Input type="datetime-local" value={form.expiresAt}
                  onChange={(e) => setForm((f) => ({ ...f, expiresAt: e.target.value }))} />
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                  Publish
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <LoadingSpinner label="Loading announcements..." />
      ) : !announcements || announcements.length === 0 ? (
        <EmptyState icon={<Megaphone className="h-8 w-8 text-muted-foreground" />}
          title="No announcements" description="Create an announcement to broadcast to users." />
      ) : (
        <div className="space-y-4">
          {announcements.map((ann: any) => (
            <Card key={ann.id} className="hover-lift">
              <CardContent className="py-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold">{ann.title}</h3>
                      <Badge variant="secondary" className="text-[10px]">{ann.targetAudience}</Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">{ann.content}</p>
                    <p className="text-xs text-muted-foreground mt-2">{formatDate(ann.createdAt)}</p>
                  </div>
                  <Button variant="ghost" size="icon" className="text-destructive" onClick={() => setDeleteTarget(ann.id)}>
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <ConfirmDialog open={!!deleteTarget} onOpenChange={() => setDeleteTarget(null)}
        title="Delete Announcement" description="This announcement will be permanently removed."
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget)}
        confirmLabel="Delete" destructive loading={deleteMutation.isPending} />
    </div>
  );
}
