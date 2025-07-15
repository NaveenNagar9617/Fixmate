import { Badge } from '@/components/ui/Badge';
import { cn, getStatusColor, getStatusLabel } from '@/lib/utils';

interface ComplaintStatusBadgeProps {
  status: string;
  className?: string;
}

export default function ComplaintStatusBadge({ status, className }: ComplaintStatusBadgeProps) {
  return (
    <Badge variant="outline" className={cn(getStatusColor(status), 'font-medium', className)}>
      {getStatusLabel(status)}
    </Badge>
  );
}
