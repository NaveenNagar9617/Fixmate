import { useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import ComplaintStatusBadge from './ComplaintStatusBadge';
import ComplaintPriorityBadge from './ComplaintPriorityBadge';
import SLACountdown from './SLACountdown';
import { MapPin, ThumbsUp, User } from 'lucide-react';
import { cn, getCategoryIcon, formatTimeAgo } from '@/lib/utils';

interface ComplaintCardProps {
  complaint: {
    id: string;
    title: string;
    category: string;
    priority: string;
    status: string;
    locationBlock: string;
    locationFloor: number;
    roomNumber: string;
    upvoteCount: number;
    slaDeadline: string | null;
    assignedStaff?: { name: string } | null;
    createdAt: string;
  };
}

export default function ComplaintCard({ complaint }: ComplaintCardProps) {
  const navigate = useNavigate();

  return (
    <Card
      className="cursor-pointer hover-lift hover:border-primary/30 group"
      onClick={() => navigate(`/complaints/${complaint.id}`)}
    >
      <div className="p-5">
        <div className="flex items-start justify-between gap-3 mb-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-lg">{getCategoryIcon(complaint.category)}</span>
              <h3 className="font-semibold text-sm truncate group-hover:text-primary transition-colors">
                {complaint.title}
              </h3>
            </div>
            <p className="text-xs text-muted-foreground">
              {complaint.category.replace(/_/g, ' ')} • {formatTimeAgo(complaint.createdAt)}
            </p>
          </div>
          <ComplaintPriorityBadge priority={complaint.priority} />
        </div>

        <div className="flex items-center gap-4 mb-3 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <MapPin className="h-3 w-3" />
            {complaint.locationBlock} • F{complaint.locationFloor} • {complaint.roomNumber}
          </span>
          {complaint.assignedStaff && (
            <span className="flex items-center gap-1">
              <User className="h-3 w-3" />
              {complaint.assignedStaff.name}
            </span>
          )}
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ComplaintStatusBadge status={complaint.status} />
            <SLACountdown slaDeadline={complaint.slaDeadline} status={complaint.status} compact />
          </div>
          <div className="flex items-center gap-1 text-muted-foreground">
            <ThumbsUp className="h-3.5 w-3.5" />
            <span className="text-xs font-medium">{complaint.upvoteCount}</span>
          </div>
        </div>
      </div>
    </Card>
  );
}
