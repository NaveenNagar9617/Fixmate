import { Badge } from '@/components/ui/Badge';
import { cn, getPriorityColor } from '@/lib/utils';

interface ComplaintPriorityBadgeProps {
  priority: string;
  className?: string;
}

export default function ComplaintPriorityBadge({ priority, className }: ComplaintPriorityBadgeProps) {
  return (
    <Badge variant="outline" className={cn(getPriorityColor(priority), 'font-medium', className)}>
      {priority}
    </Badge>
  );
}
