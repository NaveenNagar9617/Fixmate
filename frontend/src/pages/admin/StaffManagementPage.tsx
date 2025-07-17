import { useState } from 'react';
import { useStaffList } from '@/hooks/useAnalytics';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import api from '@/lib/axios';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Progress } from '@/components/ui/Progress';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/Dialog';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import EmptyState from '@/components/common/EmptyState';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { User, ToggleLeft, ToggleRight, Star, UserPlus, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { cn } from '@/lib/utils';

export default function StaffManagementPage() {
  const { data: staff, isLoading } = useStaffList();
  const queryClient = useQueryClient();
  const [openAddModal, setOpenAddModal] = useState(false);
  const [newStaff, setNewStaff] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
    category: 'PLUMBER',
  });

  const staffCategories = [
    { value: 'ELECTRICIAN', label: 'Electrician' },
    { value: 'PLUMBER', label: 'Plumber' },
    { value: 'IT_TECHNICIAN', label: 'IT Technician' },
    { value: 'CARPENTER', label: 'Carpenter' },
    { value: 'CLEANER', label: 'Cleaner' },
    { value: 'SECURITY', label: 'Security' },
    { value: 'GENERAL', label: 'General Maintenance' },
  ];

  const createStaff = useMutation({
    mutationFn: async (payload: typeof newStaff) => {
      await api.post('/admin/staff', payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.staff });
      toast.success('Staff member provisioned successfully');
      setOpenAddModal(false);
      setNewStaff({ name: '', email: '', password: '', phone: '', category: 'PLUMBER' });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to create staff member');
    },
  });

  const toggleDuty = useMutation({
    mutationFn: async (staffId: string) => {
      await api.put(`/admin/staff/${staffId}/duty`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.staff });
      toast.success('Duty status updated');
    },
    onError: () => toast.error('Failed to update duty status'),
  });

  if (isLoading) return <LoadingSpinner label="Loading staff..." />;

  const chartData = (staff || []).map((s: any) => ({
    name: s.user?.name?.split(' ')[0] || 'N/A',
    workload: s.activeComplaints || 0,
  }));

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newStaff.name || !newStaff.email || !newStaff.password) {
      toast.error('Please fill in all required fields');
      return;
    }
    if (newStaff.password.length < 10) {
      toast.error('Password must be at least 10 characters long');
      return;
    }
    createStaff.mutate(newStaff);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Staff Management</h1>
          <p className="text-muted-foreground text-sm mt-1">Monitor staff workload and manage duty status</p>
        </div>

        <Dialog open={openAddModal} onOpenChange={setOpenAddModal}>
          <DialogTrigger asChild>
            <Button variant="gradient">
              <UserPlus className="h-4 w-4 mr-2" /> Add Staff Member
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-[425px]">
            <DialogHeader>
              <DialogTitle>Add New Staff Member</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleAddSubmit} className="space-y-4 mt-2">
              <div>
                <label className="block text-sm font-medium mb-1">Full Name</label>
                <Input
                  required
                  value={newStaff.name}
                  onChange={(e) => setNewStaff({ ...newStaff, name: e.target.value })}
                  placeholder="Ramesh Kumar"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Email</label>
                <Input
                  required
                  type="email"
                  value={newStaff.email}
                  onChange={(e) => setNewStaff({ ...newStaff, email: e.target.value })}
                  placeholder="ramesh@hostel.com"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Temporary Password</label>
                <Input
                  required
                  type="password"
                  value={newStaff.password}
                  onChange={(e) => setNewStaff({ ...newStaff, password: e.target.value })}
                  placeholder="At least 10 characters"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Staff Specialization</label>
                <Select
                  value={newStaff.category}
                  onValueChange={(val) => setNewStaff({ ...newStaff, category: val })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {staffCategories.map((cat) => (
                      <SelectItem key={cat.value} value={cat.value}>
                        {cat.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Phone (optional)</label>
                <Input
                  value={newStaff.phone}
                  onChange={(e) => setNewStaff({ ...newStaff, phone: e.target.value })}
                  placeholder="+91 98765 43210"
                />
              </div>

              <Button type="submit" variant="gradient" className="w-full" disabled={createStaff.isPending}>
                {createStaff.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                {createStaff.isPending ? 'Provisioning...' : 'Provision Staff'}
              </Button>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Workload Chart */}
      <Card className="hover-lift">
        <CardContent className="pt-6">
          <h3 className="text-base font-semibold mb-4">Workload Distribution</h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={chartData} barSize={40}>
              <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip contentStyle={{ borderRadius: '12px', border: '1px solid hsl(var(--border))', background: 'hsl(var(--card))' }} />
              <Bar dataKey="workload" fill="url(#staffGradient)" radius={[8, 8, 0, 0]} />
              <defs>
                <linearGradient id="staffGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#8b5cf6" />
                  <stop offset="100%" stopColor="#6366f1" />
                </linearGradient>
              </defs>
            </BarChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Staff Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {staff.map((member: any) => (
          <Card key={member.user.id} className="hover-lift">
            <CardContent className="pt-6">
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold text-sm">
                    {member.user.name?.charAt(0)}
                  </div>
                  <div>
                    <p className="font-semibold">{member.user.name}</p>
                    <p className="text-xs text-muted-foreground">{member.user.staffCategory?.replace(/_/g, ' ')}</p>
                  </div>
                </div>
                <Badge variant={member.user.isOnDuty ? 'success' : 'secondary'}>
                  {member.user.isOnDuty ? 'On Duty' : 'Off Duty'}
                </Badge>
              </div>

              <div className="space-y-3">
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground">Active Complaints</span>
                    <span className="font-medium">{member.activeComplaints || 0}</span>
                  </div>
                  <Progress value={Math.min((member.activeComplaints || 0) * 20, 100)} />
                </div>

                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground flex items-center gap-1">
                    <Star className="h-3 w-3 fill-amber-400 text-amber-400" /> Rating
                  </span>
                  <span className="font-medium">{member.user.avgRating || 'N/A'}</span>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => toggleDuty.mutate(member.user.id)}
                >
                  {member.user.isOnDuty ? (
                    <><ToggleRight className="h-4 w-4 mr-2 text-emerald-500" /> Set Off Duty</>
                  ) : (
                    <><ToggleLeft className="h-4 w-4 mr-2" /> Set On Duty</>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
