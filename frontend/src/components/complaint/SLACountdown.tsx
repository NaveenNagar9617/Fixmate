import { useState, useEffect } from 'react';
import { getSLAStatus, cn } from '@/lib/utils';
import { Clock, AlertTriangle } from 'lucide-react';

interface SLACountdownProps {
  slaDeadline: string | null;
  status: string;
  compact?: boolean;
}

export default function SLACountdown({ slaDeadline, status, compact = false }: SLACountdownProps) {
  const [sla, setSla] = useState(getSLAStatus(slaDeadline, status));

  useEffect(() => {
    setSla(getSLAStatus(slaDeadline, status));
    const interval = setInterval(() => {
      setSla(getSLAStatus(slaDeadline, status));
    }, 60000);
    return () => clearInterval(interval);
  }, [slaDeadline, status]);

  if (sla.label === 'N/A') return null;

  const colorClasses = {
    green: 'text-emerald-600 bg-emerald-500/10',
    amber: 'text-amber-600 bg-amber-500/10',
    red: 'text-red-600 bg-red-500/10',
  };

  if (compact) {
    return (
      <span className={cn('inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium', colorClasses[sla.color])}>
        {sla.isOverdue ? <AlertTriangle className="h-3 w-3" /> : <Clock className="h-3 w-3" />}
        {sla.label}
      </span>
    );
  }

  return (
    <div className={cn('flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium', colorClasses[sla.color])}>
      {sla.isOverdue ? <AlertTriangle className="h-4 w-4 animate-pulse" /> : <Clock className="h-4 w-4" />}
      <span>{sla.label}</span>
    </div>
  );
}
