import { formatDate, getStatusDot, cn } from '@/lib/utils';
import { User, ArrowRight, MessageSquare, Camera, AlertTriangle, CheckCircle } from 'lucide-react';

interface TimelineEntry {
  id: string;
  actorName: string;
  action: string;
  oldStatus: string | null;
  newStatus: string | null;
  note: string | null;
  createdAt: string;
}

interface ComplaintTimelineProps {
  timeline: TimelineEntry[];
}

function getTimelineIcon(action: string) {
  if (action.toLowerCase().includes('escalat')) return <AlertTriangle className="h-4 w-4" />;
  if (action.toLowerCase().includes('photo')) return <Camera className="h-4 w-4" />;
  if (action.toLowerCase().includes('comment')) return <MessageSquare className="h-4 w-4" />;
  if (action.toLowerCase().includes('resolv') || action.toLowerCase().includes('closed')) return <CheckCircle className="h-4 w-4" />;
  return <ArrowRight className="h-4 w-4" />;
}

export default function ComplaintTimeline({ timeline }: ComplaintTimelineProps) {
  if (!timeline || timeline.length === 0) {
    return <p className="text-sm text-muted-foreground py-4">No timeline entries yet.</p>;
  }

  return (
    <div className="relative space-y-0">
      <div className="absolute left-[18px] top-2 bottom-2 w-px bg-border" />

      {timeline.map((entry, index) => (
        <div key={entry.id || index} className="relative flex gap-4 pb-6 last:pb-0">
          <div className={cn(
            'relative z-10 flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 bg-background',
            entry.newStatus ? getStatusDot(entry.newStatus).replace('bg-', 'border-') : 'border-muted-foreground/30'
          )}>
            {getTimelineIcon(entry.action)}
          </div>

          <div className="flex-1 min-w-0 pt-0.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm font-medium">{entry.action}</span>
              {entry.oldStatus && entry.newStatus && (
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <span className="px-1.5 py-0.5 rounded bg-muted">{entry.oldStatus.replace(/_/g, ' ')}</span>
                  <ArrowRight className="h-3 w-3" />
                  <span className="px-1.5 py-0.5 rounded bg-muted">{entry.newStatus.replace(/_/g, ' ')}</span>
                </div>
              )}
            </div>

            <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
              <User className="h-3 w-3" />
              <span>{entry.actorName}</span>
              <span>•</span>
              <span>{formatDate(entry.createdAt)}</span>
            </div>

            {entry.note && (
              <p className="mt-1.5 text-sm text-muted-foreground bg-muted/50 rounded-lg px-3 py-2">
                {entry.note}
              </p>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
