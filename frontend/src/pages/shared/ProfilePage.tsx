import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryKeys';
import api from '@/lib/axios';
import { useAuthStore } from '@/store/authStore';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import PhotoUpload from '@/components/common/PhotoUpload';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { User, Mail, Phone, MapPin, Building, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

export default function ProfilePage() {
  const { user, updateUser } = useAuthStore();
  const queryClient = useQueryClient();

  const { data: profile, isLoading } = useQuery({
    queryKey: queryKeys.users.me,
    queryFn: async () => {
      const response = await api.get('/users/me');
      return response.data.data;
    },
  });

  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ name: '', phone: '', roomNumber: '', block: '' });
  const [photo, setPhoto] = useState<File | null>(null);

  const updateMutation = useMutation({
    mutationFn: async (data: typeof form) => {
      const response = await api.put('/users/me', data);
      return response.data.data;
    },
    onSuccess: (data) => {
      updateUser({ name: data.name });
      queryClient.invalidateQueries({ queryKey: queryKeys.users.me });
      setEditing(false);
      toast.success('Profile updated');
    },
    onError: () => toast.error('Failed to update profile'),
  });

  const photoMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('photo', file);
      const response = await api.post('/users/me/photo', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return response.data.data;
    },
    onSuccess: (url) => {
      updateUser({ profilePhotoUrl: url });
      queryClient.invalidateQueries({ queryKey: queryKeys.users.me });
      setPhoto(null);
      toast.success('Photo updated');
    },
    onError: () => toast.error('Failed to upload photo'),
  });

  const startEditing = () => {
    setForm({
      name: profile?.name || '',
      phone: profile?.phone || '',
      roomNumber: profile?.roomNumber || '',
      block: profile?.block || '',
    });
    setEditing(true);
  };

  if (isLoading) return <LoadingSpinner label="Loading profile..." />;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold">My Profile</h1>
        <p className="text-muted-foreground text-sm mt-1">Manage your account details</p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center gap-6 mb-6">
            <Avatar className="h-20 w-20">
              <AvatarImage src={profile?.profilePhotoUrl || undefined} />
              <AvatarFallback className="text-2xl">{profile?.name?.charAt(0)}</AvatarFallback>
            </Avatar>
            <div>
              <h2 className="text-xl font-bold">{profile?.name}</h2>
              <Badge variant="secondary" className="mt-1">{profile?.role}</Badge>
            </div>
          </div>

          {/* Photo Upload */}
          <div className="mb-6">
            <PhotoUpload file={photo} onFileChange={setPhoto} label="Upload new profile photo" />
            {photo && (
              <Button className="mt-2" size="sm" onClick={() => photoMutation.mutate(photo)}
                disabled={photoMutation.isPending}>
                {photoMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                Save Photo
              </Button>
            )}
          </div>

          {editing ? (
            <form onSubmit={(e) => { e.preventDefault(); updateMutation.mutate(form); }} className="space-y-4">
              <div>
                <label className="text-sm font-medium">Name</label>
                <Input value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
              </div>
              <div>
                <label className="text-sm font-medium">Phone</label>
                <Input value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
              </div>
              {profile?.role === 'STUDENT' && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium">Room Number</label>
                    <Input value={form.roomNumber} onChange={(e) => setForm((f) => ({ ...f, roomNumber: e.target.value }))} />
                  </div>
                  <div>
                    <label className="text-sm font-medium">Block</label>
                    <Input value={form.block} onChange={(e) => setForm((f) => ({ ...f, block: e.target.value }))} />
                  </div>
                </div>
              )}
              <div className="flex gap-2">
                <Button type="button" variant="outline" onClick={() => setEditing(false)}>Cancel</Button>
                <Button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                  Save Changes
                </Button>
              </div>
            </form>
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="flex items-center gap-3 p-3 rounded-xl bg-muted/50">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <div>
                    <p className="text-xs text-muted-foreground">Email</p>
                    <p className="text-sm font-medium">{profile?.email}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-3 rounded-xl bg-muted/50">
                  <Phone className="h-4 w-4 text-muted-foreground" />
                  <div>
                    <p className="text-xs text-muted-foreground">Phone</p>
                    <p className="text-sm font-medium">{profile?.phone || 'Not set'}</p>
                  </div>
                </div>
                {profile?.role === 'STUDENT' && (
                  <>
                    <div className="flex items-center gap-3 p-3 rounded-xl bg-muted/50">
                      <MapPin className="h-4 w-4 text-muted-foreground" />
                      <div>
                        <p className="text-xs text-muted-foreground">Room</p>
                        <p className="text-sm font-medium">{profile?.roomNumber || 'Not set'}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 p-3 rounded-xl bg-muted/50">
                      <Building className="h-4 w-4 text-muted-foreground" />
                      <div>
                        <p className="text-xs text-muted-foreground">Block</p>
                        <p className="text-sm font-medium">{profile?.block || 'Not set'}</p>
                      </div>
                    </div>
                  </>
                )}
              </div>
              <Button variant="outline" onClick={startEditing}>Edit Profile</Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
