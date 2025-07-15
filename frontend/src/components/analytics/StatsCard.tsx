import { Card } from '@/components/ui/Card';
import { cn } from '@/lib/utils';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface StatsCardProps {
  label: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: number;
  trendLabel?: string;
  color?: string;
}

export default function StatsCard({ label, value, icon, trend, trendLabel, color = 'from-blue-500 to-indigo-600' }: StatsCardProps) {
  const TrendIcon = trend && trend > 0 ? TrendingUp : trend && trend < 0 ? TrendingDown : Minus;
  const trendColor = trend && trend > 0 ? 'text-emerald-500' : trend && trend < 0 ? 'text-red-500' : 'text-muted-foreground';

  return (
    <Card className="overflow-hidden hover-lift">
      <div className="p-6">
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm font-medium text-muted-foreground">{label}</p>
          <div className={cn('h-10 w-10 rounded-xl bg-gradient-to-br flex items-center justify-center text-white', color)}>
            {icon}
          </div>
        </div>
        <div className="flex items-end justify-between">
          <div>
            <p className="text-3xl font-bold tracking-tight">{value}</p>
            {trend !== undefined && (
              <div className={cn('flex items-center gap-1 mt-1', trendColor)}>
                <TrendIcon className="h-3 w-3" />
                <span className="text-xs font-medium">
                  {Math.abs(trend)}% {trendLabel || ''}
                </span>
              </div>
            )}
          </div>
        </div>
      </div>
    </Card>
  );
}
