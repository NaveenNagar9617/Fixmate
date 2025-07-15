import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { formatShortDate } from '@/lib/utils';

interface TrendLineChartProps {
  data: Array<{ date: string; count: number }>;
}

export default function TrendLineChart({ data }: TrendLineChartProps) {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">Daily Complaint Trend</CardTitle></CardHeader>
        <CardContent><p className="text-sm text-muted-foreground">No data available</p></CardContent>
      </Card>
    );
  }

  const chartData = data.map((d) => ({
    ...d,
    label: formatShortDate(d.date),
  }));

  return (
    <Card className="hover-lift">
      <CardHeader><CardTitle className="text-base">Daily Complaint Trend (30 Days)</CardTitle></CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={chartData}>
            <defs>
              <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#6366f1" stopOpacity={0.3} />
                <stop offset="100%" stopColor="#6366f1" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
            <XAxis dataKey="label" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
            <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
            <Tooltip
              contentStyle={{ borderRadius: '12px', border: '1px solid hsl(var(--border))', background: 'hsl(var(--card))' }}
              formatter={(value: number) => [`${value} complaints`, 'Count']}
            />
            <Area type="monotone" dataKey="count" stroke="#6366f1" strokeWidth={2.5} fill="url(#trendGradient)" />
          </AreaChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
