import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import { Wrench, Loader2 } from 'lucide-react';

export default function RegisterPage() {
  const { register } = useAuth();
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    roomNumber: '',
    block: '',
    phone: '',
  });
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!form.name.trim()) errs.name = 'Name is required';
    if (!form.email) errs.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(form.email)) errs.email = 'Invalid email';
    if (!form.password) errs.password = 'Password is required';
    else if (form.password.length < 10) errs.password = 'Min 10 characters';
    if (form.password !== form.confirmPassword) errs.confirmPassword = 'Passwords do not match';
    if (!form.roomNumber) errs.roomNumber = 'Room number is required';
    if (!form.block) errs.block = 'Block is required';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      await register({
        name: form.name,
        email: form.email,
        password: form.password,
        role: 'STUDENT',
        roomNumber: form.roomNumber || undefined,
        block: form.block || undefined,
        phone: form.phone || undefined,
      });
    } catch { /* handled */ } finally { setLoading(false); }
  };

  const update = (key: string, value: string) => setForm((f) => ({ ...f, [key]: value }));

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 -left-20 w-72 h-72 bg-indigo-500/20 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-purple-500/15 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-lg relative z-10 animate-fade-in">
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center h-14 w-14 rounded-2xl gradient-primary shadow-lg shadow-indigo-500/30 mb-3">
            <Wrench className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">Student Registration</h1>
          <p className="text-slate-400 text-sm mt-1">Join FixMate to submit and track hostel complaints</p>
        </div>

        <Card className="glass-dark border-white/10">
          <CardContent className="pt-6">
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Full Name</label>
                <Input value={form.name} onChange={(e) => update('name', e.target.value)}
                  placeholder="John Doe" className="bg-white/5 border-white/10 text-white placeholder:text-slate-500" />
                {errors.name && <p className="text-xs text-red-400 mt-1">{errors.name}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Email</label>
                <Input type="email" value={form.email} onChange={(e) => update('email', e.target.value)}
                  placeholder="you@example.com" className="bg-white/5 border-white/10 text-white placeholder:text-slate-500" />
                {errors.email && <p className="text-xs text-red-400 mt-1">{errors.email}</p>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Password</label>
                  <Input type="password" value={form.password} onChange={(e) => update('password', e.target.value)}
                    placeholder="••••••••••" className="bg-white/5 border-white/10 text-white placeholder:text-slate-500" />
                  {errors.password && <p className="text-xs text-red-400 mt-1">{errors.password}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Confirm</label>
                  <Input type="password" value={form.confirmPassword} onChange={(e) => update('confirmPassword', e.target.value)}
                    placeholder="••••••••••" className="bg-white/5 border-white/10 text-white placeholder:text-slate-500" />
                  {errors.confirmPassword && <p className="text-xs text-red-400 mt-1">{errors.confirmPassword}</p>}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 animate-scale-in">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Room Number</label>
                  <Input value={form.roomNumber} onChange={(e) => update('roomNumber', e.target.value)}
                    placeholder="301" className="bg-white/5 border-white/10 text-white placeholder:text-slate-500" />
                  {errors.roomNumber && <p className="text-xs text-red-400 mt-1">{errors.roomNumber}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Block</label>
                  <Select value={form.block} onValueChange={(v) => update('block', v)}>
                    <SelectTrigger className="bg-white/5 border-white/10 text-white">
                      <SelectValue placeholder="Select block" />
                    </SelectTrigger>
                    <SelectContent>
                      {['Block A', 'Block B', 'Block C', 'Block D', 'Block E'].map((b) => (
                        <SelectItem key={b} value={b}>{b}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {errors.block && <p className="text-xs text-red-400 mt-1">{errors.block}</p>}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Phone (optional)</label>
                <Input value={form.phone} onChange={(e) => update('phone', e.target.value)}
                  placeholder="+91 98765 43210" className="bg-white/5 border-white/10 text-white placeholder:text-slate-500" />
              </div>

              <Button type="submit" className="w-full" variant="gradient" size="lg" disabled={loading}>
                {loading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                {loading ? 'Creating...' : 'Create Account'}
              </Button>
            </form>

            <div className="mt-4 text-center">
              <p className="text-sm text-slate-400">
                Already have an account?{' '}
                <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium">Sign in</Link>
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
