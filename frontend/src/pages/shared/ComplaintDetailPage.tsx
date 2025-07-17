import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useComplaintDetail,
  useComplaintTimeline,
  useUpdateStatus,
  useUpdatePriority,
  useAssignStaff,
  useUploadAfterPhoto,
  useAddComment,
  useCreateRating,
} from '@/hooks/useComplaints';
import { useStaffList } from '@/hooks/useAnalytics';
import { useAuthStore } from '@/store/authStore';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Textarea } from '@/components/ui/Textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import ComplaintStatusBadge from '@/components/complaint/ComplaintStatusBadge';
import ComplaintPriorityBadge from '@/components/complaint/ComplaintPriorityBadge';
import SLACountdown from '@/components/complaint/SLACountdown';
import ComplaintTimeline from '@/components/complaint/ComplaintTimeline';
import PhotoComparison from '@/components/complaint/PhotoComparison';
import StarRating from '@/components/common/StarRating';
import PhotoUpload from '@/components/common/PhotoUpload';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { ArrowLeft, MapPin, User, Calendar, Send, Upload, Loader2, ShieldCheck, AlertCircle } from 'lucide-react';
import { formatDate, getCategoryIcon } from '@/lib/utils';

export default function ComplaintDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const { data: complaint, isLoading } = useComplaintDetail(id!);
  const { data: timeline } = useComplaintTimeline(id!);
  const { data: staffList } = useStaffList();
  const updateStatus = useUpdateStatus();
  const updatePriority = useUpdatePriority();
  const assignStaff = useAssignStaff();
  const uploadAfterPhoto = useUploadAfterPhoto();
  const addComment = useAddComment();
  const createRating = useCreateRating();

  const [comment, setComment] = useState('');
  const [isInternal, setIsInternal] = useState(false);
  const [statusNote, setStatusNote] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedStaff, setSelectedStaff] = useState('');
  const [newPriority, setNewPriority] = useState('');
  const [priorityChangeReason, setPriorityChangeReason] = useState('');
  const [showPriorityModal, setShowPriorityModal] = useState(false);
  const [afterPhoto, setAfterPhoto] = useState<File | null>(null);
  const [rating, setRating] = useState(0);
  const [feedback, setFeedback] = useState('');

  if (isLoading) return <LoadingSpinner label="Loading complaint..." />;
  if (!complaint) return <div className="text-center py-8 text-muted-foreground">Complaint not found</div>;

  const isStudent = user?.role === 'STUDENT';
  const isStaff = user?.role === 'STAFF';
  const isAdmin = user?.role === 'ADMIN';
  const isCompleted = ['CLOSED', 'RESOLVED'].includes(complaint.status);
  const canManagePriority = (isAdmin || (isStaff && complaint.assignedStaff?.id === user?.id)) && !isCompleted;

  const handleStatusUpdate = async () => {
    if (!selectedStatus) return;
    await updateStatus.mutateAsync({ id: id!, newStatus: selectedStatus, note: statusNote });
    setSelectedStatus('');
    setStatusNote('');
  };

  const handlePriorityUpdate = async () => {
    if (!newPriority || !priorityChangeReason.trim()) return;
    await updatePriority.mutateAsync({
      id: id!,
      priority: newPriority,
      reason: priorityChangeReason.trim(),
    });
    setShowPriorityModal(false);
    setNewPriority('');
    setPriorityChangeReason('');
  };

  const handleAssign = async () => {
    if (!selectedStaff) return;
    await assignStaff.mutateAsync({ id: id!, staffId: selectedStaff });
    setSelectedStaff('');
  };

  const handleUploadAfter = async () => {
    if (!afterPhoto) return;
    await uploadAfterPhoto.mutateAsync({ id: id!, file: afterPhoto });
    setAfterPhoto(null);
  };

  const handleComment = async () => {
    if (!comment.trim()) return;
    await addComment.mutateAsync({ id: id!, content: comment, isInternal });
    setComment('');
  };

  const handleRating = async () => {
    if (rating === 0) return;
    await createRating.mutateAsync({ id: id!, stars: rating, feedbackText: feedback });
    setRating(0);
    setFeedback('');
  };

  const statusOptions: Record<string, string[]> = {
    STUDENT: complaint.status === 'RESOLVED' ? ['CLOSED', 'REOPENED'] : [],
    STAFF:
      complaint.status === 'ASSIGNED'
        ? ['IN_PROGRESS']
        : complaint.status === 'IN_PROGRESS'
        ? ['RESOLVED']
        : [],
    ADMIN: ['ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'],
  };

  const availableStatuses = statusOptions[user?.role || ''] || [];

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex items-start gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="mt-1">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-2 flex-wrap mb-2">
            <span className="text-xl">{getCategoryIcon(complaint.category)}</span>
            <h1 className="text-xl font-bold">{complaint.title}</h1>
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            <ComplaintStatusBadge status={complaint.status} />
            <ComplaintPriorityBadge priority={complaint.priority} />
            <SLACountdown slaDeadline={complaint.slaDeadline} status={complaint.status} compact />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Description & Priority Evaluation Notice */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Description & Assessment</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-muted-foreground whitespace-pre-wrap">{complaint.description}</p>

              {/* Priority Metadata Box */}
              {complaint.priorityReason && (
                <div className="p-3.5 rounded-xl bg-muted/60 border text-xs space-y-1">
                  <div className="flex items-center gap-1.5 font-semibold text-foreground">
                    <ShieldCheck className="h-4 w-4 text-indigo-500" />
                    <span>Priority Evaluation Note ({complaint.prioritySource || 'SYSTEM'})</span>
                  </div>
                  <p className="text-muted-foreground">{complaint.priorityReason}</p>
                  {complaint.requestedPriority && complaint.requestedPriority !== complaint.priority && (
                    <p className="text-[11px] text-amber-600 dark:text-amber-400 font-medium">
                      Student requested: {complaint.requestedPriority} • System normalized to: {complaint.priority}
                    </p>
                  )}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Photos */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Photos</CardTitle>
            </CardHeader>
            <CardContent>
              <PhotoComparison photos={complaint.photos || []} />
            </CardContent>
          </Card>

          {/* Timeline */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              <ComplaintTimeline timeline={timeline || []} />
            </CardContent>
          </Card>

          {/* Comments */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Comments</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {complaint.comments?.length > 0 ? (
                <div className="space-y-3">
                  {complaint.comments.map((c: any) => (
                    <div key={c.id} className="flex gap-3 p-3 rounded-xl bg-muted/50">
                      <div className="h-8 w-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold shrink-0">
                        {c.authorName?.charAt(0)}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">{c.authorName}</span>
                          {c.isInternal && <Badge variant="secondary" className="text-[10px]">Internal</Badge>}
                          <span className="text-xs text-muted-foreground">{formatDate(c.createdAt)}</span>
                        </div>
                        <p className="text-sm text-muted-foreground mt-1">{c.content}</p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground py-2">No comments yet</p>
              )}

              <div className="flex gap-2 pt-2 border-t">
                <Textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Add a comment..."
                  className="min-h-[60px]"
                />
                <div className="flex flex-col gap-2">
                  {(isStaff || isAdmin) && (
                    <label className="flex items-center gap-1.5 text-xs cursor-pointer">
                      <input
                        type="checkbox"
                        checked={isInternal}
                        onChange={(e) => setIsInternal(e.target.checked)}
                        className="rounded"
                      />
                      Internal
                    </label>
                  )}
                  <Button size="icon" onClick={handleComment} disabled={!comment.trim() || addComment.isPending}>
                    <Send className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Rating */}
          {isStudent && complaint.status === 'CLOSED' && !complaint.rating && (
            <Card className="border-primary/30 animate-scale-in">
              <CardHeader>
                <CardTitle className="text-base">Rate this resolution</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <StarRating value={rating} onChange={setRating} size="lg" />
                <Textarea
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                  placeholder="Share your feedback (optional)"
                  className="min-h-[60px]"
                />
                <Button onClick={handleRating} disabled={rating === 0 || createRating.isPending}>
                  {createRating.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                  Submit Rating
                </Button>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Right Column - Actions */}
        <div className="space-y-4">
          {/* Metadata */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex items-center gap-2 text-muted-foreground">
                <MapPin className="h-4 w-4" />
                <span>
                  {complaint.locationBlock} • Floor {complaint.locationFloor} • Room {complaint.roomNumber}
                </span>
              </div>
              <div className="flex items-center gap-2 text-muted-foreground">
                <User className="h-4 w-4" />
                <span>Filed by: {complaint.student?.name || 'Unknown'}</span>
              </div>
              {complaint.assignedStaff && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <User className="h-4 w-4" />
                  <span>Assigned to: {complaint.assignedStaff.name}</span>
                </div>
              )}
              <div className="flex items-center gap-2 text-muted-foreground">
                <Calendar className="h-4 w-4" />
                <span>Created: {formatDate(complaint.createdAt)}</span>
              </div>
              {complaint.resolvedAt && (
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Calendar className="h-4 w-4" />
                  <span>Resolved: {formatDate(complaint.resolvedAt)}</span>
                </div>
              )}
              <div className="pt-2">
                <SLACountdown slaDeadline={complaint.slaDeadline} status={complaint.status} />
              </div>
            </CardContent>
          </Card>

          {/* Change Priority (Staff / Admin) */}
          {canManagePriority && (
            <Card className="border-indigo-500/20">
              <CardHeader className="pb-3">
                <CardTitle className="text-base flex items-center justify-between">
                  <span>Priority & SLA</span>
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => setShowPriorityModal(!showPriorityModal)}
                  >
                    {showPriorityModal ? 'Cancel' : 'Change Priority'}
                  </Button>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">Active SLA Priority:</span>
                  <ComplaintPriorityBadge priority={complaint.priority} />
                </div>

                {showPriorityModal && (
                  <div className="pt-3 border-t space-y-3 animate-scale-in">
                    <div>
                      <label className="block text-xs font-medium mb-1">New Priority</label>
                      <Select value={newPriority} onValueChange={setNewPriority}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select priority" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="CRITICAL">🔴 Critical (2h SLA - Immediate hazard)</SelectItem>
                          <SelectItem value="HIGH">🟠 High (4h SLA - Urgent disruption)</SelectItem>
                          <SelectItem value="MEDIUM">🟡 Medium (24h SLA - Standard maintenance)</SelectItem>
                          <SelectItem value="LOW">🟢 Low (72h SLA - Minor / cosmetic)</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    <div>
                      <label className="block text-xs font-medium mb-1">Reason for override</label>
                      <Textarea
                        value={priorityChangeReason}
                        onChange={(e) => setPriorityChangeReason(e.target.value)}
                        placeholder="e.g., Issue affects whole floor or Verified routine request"
                        className="min-h-[60px] text-xs"
                      />
                    </div>

                    <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground bg-muted p-2 rounded-lg">
                      <AlertCircle className="h-4 w-4 shrink-0 text-indigo-500" />
                      <span>SLA timer will recalculate from the current time.</span>
                    </div>

                    <Button
                      className="w-full"
                      size="sm"
                      onClick={handlePriorityUpdate}
                      disabled={!newPriority || !priorityChangeReason.trim() || updatePriority.isPending}
                    >
                      {updatePriority.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                      Update Priority & Reset SLA
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* Status Update */}
          {availableStatuses.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Update Status</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Select value={selectedStatus} onValueChange={setSelectedStatus}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select new status" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableStatuses.map((s) => (
                      <SelectItem key={s} value={s}>
                        {s.replace(/_/g, ' ')}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Textarea
                  value={statusNote}
                  onChange={(e) => setStatusNote(e.target.value)}
                  placeholder="Add a note (optional)"
                  className="min-h-[60px]"
                />
                <Button
                  className="w-full"
                  onClick={handleStatusUpdate}
                  disabled={!selectedStatus || updateStatus.isPending}
                >
                  {updateStatus.isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                  Update Status
                </Button>
              </CardContent>
            </Card>
          )}

          {/* Upload After Photo */}
          {isStaff && ['IN_PROGRESS', 'ASSIGNED'].includes(complaint.status) && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Upload After Photo</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <PhotoUpload file={afterPhoto} onFileChange={setAfterPhoto} label="Upload after repair photo" />
                {afterPhoto && (
                  <Button
                    className="w-full"
                    onClick={handleUploadAfter}
                    disabled={uploadAfterPhoto.isPending}
                  >
                    {uploadAfterPhoto.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                    ) : (
                      <Upload className="h-4 w-4 mr-2" />
                    )}
                    Upload Photo
                  </Button>
                )}
              </CardContent>
            </Card>
          )}

          {/* Reassign (Admin) */}
          {isAdmin && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Reassign Staff</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Select value={selectedStaff} onValueChange={setSelectedStaff}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select staff" />
                  </SelectTrigger>
                  <SelectContent>
                    {(staffList || []).map((s: any) => (
                      <SelectItem key={s.user.id} value={s.user.id}>
                        {s.user.name} ({s.user.staffCategory || 'Staff'})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Button
                  className="w-full"
                  onClick={handleAssign}
                  disabled={!selectedStaff || assignStaff.isPending}
                >
                  Reassign
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
