import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateComplaint } from '@/hooks/useComplaints';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Card, CardContent } from '@/components/ui/Card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import PhotoUpload from '@/components/common/PhotoUpload';
import { ArrowLeft, Loader2, Lightbulb, AlertTriangle, ShieldAlert, Sparkles } from 'lucide-react';

const categories = [
  { value: 'ELECTRICAL', label: '⚡ Electrical' },
  { value: 'PLUMBING', label: '🚰 Plumbing' },
  { value: 'WIFI', label: '📶 WiFi / Internet' },
  { value: 'FURNITURE', label: '🪑 Furniture' },
  { value: 'CLEANING', label: '🧹 Cleaning' },
  { value: 'PEST_CONTROL', label: '🐜 Pest Control' },
  { value: 'SECURITY', label: '🔒 Security' },
  { value: 'OTHER', label: '📦 Other' },
];

const priorityHints: Record<string, string[]> = {
  CRITICAL: ['fire', 'flood', 'gas leak', 'electrocution', 'emergency', 'danger', 'collapse', 'sparking', 'shock'],
  HIGH: ['no water', 'no electricity', 'broken lock', 'water leak', 'sewage', 'no wifi', 'security breach', 'blackout'],
  MEDIUM: ['flickering', 'slow drain', 'crack', 'noise', 'intermittent', 'loose', 'damaged'],
  LOW: ['paint', 'cosmetic', 'minor', 'request', 'suggestion', 'improvement', 'chair', 'broom'],
};

const emergencyChecklistOptions = [
  { id: 'SPARKING', label: 'Active Sparking, Short Circuit, or Burning Smell' },
  { id: 'FLOOD_NEAR_ELECTRIC', label: 'Water Flooding near Electrical Outlets / Wiring' },
  { id: 'GAS_LEAK', label: 'Gas Leak or Toxic Chemical Smell' },
  { id: 'PERSON_TRAPPED', label: 'Person Trapped or Door Jammed from Inside' },
  { id: 'STRUCTURAL_DAMAGE', label: 'Structural Collapse or Falling Ceiling Plaster' },
];

function suggestPriority(description: string): { priority: string; reason: string } | null {
  const lower = description.toLowerCase();
  for (const [priority, keywords] of Object.entries(priorityHints)) {
    const matched = keywords.find((kw) => lower.includes(kw));
    if (matched) {
      return {
        priority,
        reason: `Detected "${matched}" in description`,
      };
    }
  }
  return null;
}

export default function SubmitComplaintPage() {
  const navigate = useNavigate();
  const createComplaint = useCreateComplaint();
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: '',
    urgencyLevel: 'INDIVIDUAL',
    locationBlock: '',
    locationFloor: '1',
    roomNumber: '',
  });

  const [selectedSafetyFlags, setSelectedSafetyFlags] = useState<string[]>([]);
  const [photo, setPhoto] = useState<File | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [aiSuggestion, setAiSuggestion] = useState<{ priority: string; reason: string } | null>(null);

  useEffect(() => {
    if (form.description.length > 10) {
      const suggested = suggestPriority(form.description);
      setAiSuggestion(suggested);
    } else {
      setAiSuggestion(null);
    }
  }, [form.description]);

  const update = (key: string, value: string) => setForm((f) => ({ ...f, [key]: value }));

  const toggleSafetyFlag = (flagId: string) => {
    setSelectedSafetyFlags((prev) =>
      prev.includes(flagId) ? prev.filter((f) => f !== flagId) : [...prev, flagId]
    );
  };

  const validate = () => {
    const errs: Record<string, string> = {};
    if (!form.title.trim()) errs.title = 'Title is required';
    if (!form.description.trim()) errs.description = 'Description is required';
    if (form.description.length > 1000) errs.description = 'Max 1000 characters';
    if (!form.category) errs.category = 'Category is required';
    if (!form.locationBlock) errs.locationBlock = 'Block is required';
    if (!form.roomNumber) errs.roomNumber = 'Room number is required';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  // Map urgency level to requested priority for backend evaluation
  const getRequestedPriority = (): string => {
    if (form.urgencyLevel === 'EMERGENCY' || selectedSafetyFlags.length > 0) return 'CRITICAL';
    if (form.urgencyLevel === 'FLOOR') return 'HIGH';
    return 'MEDIUM';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const formData = new FormData();
    const complaintJson = JSON.stringify({
      title: form.title,
      description: form.description,
      category: form.category,
      priority: getRequestedPriority(),
      locationBlock: form.locationBlock,
      locationFloor: parseInt(form.locationFloor),
      roomNumber: form.roomNumber,
      safetyFlags: selectedSafetyFlags,
      urgencyLevel: form.urgencyLevel,
    });

    formData.append('complaint', new Blob([complaintJson], { type: 'application/json' }));
    if (photo) formData.append('photo', photo);

    try {
      await createComplaint.mutateAsync(formData);
      navigate('/student/dashboard');
    } catch {
      /* handled by mutation hook */
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-12">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">Submit Complaint</h1>
          <p className="text-muted-foreground text-sm">Smart hostel maintenance & fast tracking</p>
        </div>
      </div>

      <Card>
        <CardContent className="pt-6">
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Title */}
            <div>
              <label className="block text-sm font-medium mb-1.5">Title</label>
              <Input
                value={form.title}
                onChange={(e) => update('title', e.target.value)}
                placeholder="e.g., Switchboard sparking or Bathroom tap leaking"
              />
              {errors.title && <p className="text-xs text-destructive mt-1">{errors.title}</p>}
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium mb-1.5">Description</label>
              <Textarea
                value={form.description}
                onChange={(e) => update('description', e.target.value)}
                placeholder="Provide detailed description of the issue..."
                className="min-h-[120px]"
              />
              <div className="flex justify-between mt-1">
                {errors.description ? (
                  <p className="text-xs text-destructive">{errors.description}</p>
                ) : (
                  <span />
                )}
                <span className="text-xs text-muted-foreground">{form.description.length}/1000</span>
              </div>
            </div>

            {/* AI / Smart Analysis Note */}
            {aiSuggestion && (
              <div className="flex items-start gap-3 p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 animate-scale-in">
                <Sparkles className="h-5 w-5 text-indigo-500 shrink-0 mt-0.5" />
                <div className="text-xs">
                  <p className="font-semibold text-indigo-600 dark:text-indigo-400">
                    Smart Priority System: {aiSuggestion.priority} suggested
                  </p>
                  <p className="text-muted-foreground mt-0.5">{aiSuggestion.reason}. FixMate will automatically evaluate and assign based on safety rules.</p>
                </div>
              </div>
            )}

            {/* Category & Impact / Urgency Assessment */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1.5">Category</label>
                <Select value={form.category} onValueChange={(v) => update('category', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select issue category" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((c) => (
                      <SelectItem key={c.value} value={c.value}>
                        {c.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.category && <p className="text-xs text-destructive mt-1">{errors.category}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium mb-1.5">Impact Assessment</label>
                <Select value={form.urgencyLevel} onValueChange={(v) => update('urgencyLevel', v)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="INDIVIDUAL">🟢 Room / Individual (Standard SLA)</SelectItem>
                    <SelectItem value="FLOOR">🟡 Shared Floor / Major (Elevated SLA)</SelectItem>
                    <SelectItem value="EMERGENCY">🔴 Safety Hazard / Emergency (2h SLA)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Emergency Checklist and Warning */}
            {form.urgencyLevel === 'EMERGENCY' && (
              <div className="p-4 rounded-xl bg-destructive/10 border border-destructive/30 space-y-3 animate-scale-in">
                <div className="flex items-center gap-2 text-destructive font-semibold text-sm">
                  <ShieldAlert className="h-5 w-5 shrink-0" />
                  <span>Safety Hazard Verification Checklist</span>
                </div>
                <p className="text-xs text-muted-foreground">
                  Emergency priority is reserved for physical hazards. Please check any active hazards below to verify:
                </p>

                <div className="space-y-2 pt-1">
                  {emergencyChecklistOptions.map((opt) => (
                    <label
                      key={opt.id}
                      className="flex items-start gap-2.5 text-xs text-foreground cursor-pointer hover:bg-destructive/5 p-1.5 rounded-lg transition-colors"
                    >
                      <input
                        type="checkbox"
                        checked={selectedSafetyFlags.includes(opt.id)}
                        onChange={() => toggleSafetyFlag(opt.id)}
                        className="mt-0.5 rounded border-destructive text-destructive focus:ring-destructive"
                      />
                      <span>{opt.label}</span>
                    </label>
                  ))}
                </div>

                <div className="flex items-center gap-2 text-[11px] text-amber-600 dark:text-amber-400 bg-amber-500/10 p-2 rounded-lg">
                  <AlertTriangle className="h-4 w-4 shrink-0" />
                  <span>False emergency claims without verified hazards will be auto-normalized to standard priority.</span>
                </div>
              </div>
            )}

            {/* Location Fields */}
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1.5">Block</label>
                <Select value={form.locationBlock} onValueChange={(v) => update('locationBlock', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Block" />
                  </SelectTrigger>
                  <SelectContent>
                    {['Block A', 'Block B', 'Block C', 'Block D', 'Block E'].map((b) => (
                      <SelectItem key={b} value={b}>
                        {b}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.locationBlock && <p className="text-xs text-destructive mt-1">{errors.locationBlock}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium mb-1.5">Floor</label>
                <Select value={form.locationFloor} onValueChange={(v) => update('locationFloor', v)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[1, 2, 3, 4, 5].map((f) => (
                      <SelectItem key={f} value={String(f)}>
                        Floor {f}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1.5">Room</label>
                <Input
                  value={form.roomNumber}
                  onChange={(e) => update('roomNumber', e.target.value)}
                  placeholder="e.g., 301"
                />
                {errors.roomNumber && <p className="text-xs text-destructive mt-1">{errors.roomNumber}</p>}
              </div>
            </div>

            {/* Photo Upload */}
            <div>
              <label className="block text-sm font-medium mb-1.5">Photo (optional)</label>
              <PhotoUpload file={photo} onFileChange={setPhoto} label="Upload issue photo" />
            </div>

            {/* Submit Buttons */}
            <div className="flex gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate(-1)} className="flex-1">
                Cancel
              </Button>
              <Button
                type="submit"
                variant="gradient"
                className="flex-1"
                disabled={createComplaint.isPending}
              >
                {createComplaint.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                {createComplaint.isPending ? 'Evaluating & Submitting...' : 'Submit Complaint'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
