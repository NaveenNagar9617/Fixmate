import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

interface BlockBarChartProps {
  data: Array<{ block: string; count: number; avgResolutionHours: number }>;
}

export default function BlockBarChart({ data }: BlockBarChartProps) {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">Complaints by Block</CardTitle></CardHeader>
        <CardContent><p className="text-sm text-muted-foreground">No data available</p></CardContent>
      </Card>
    );
  }

  return (
    <Card className="hover-lift">
      <CardHeader><CardTitle className="text-base">Complaints by Block</CardTitle></CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={data} barSize={40}>
            <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
            <XAxis dataKey="block" tick={{ fontSize: 12 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip
              contentStyle={{ borderRadius: '12px', border: '1px solid hsl(var(--border))', background: 'hsl(var(--card))' }}
              formatter={(value: number) => [`${value} complaints`]}
            />
            <Bar dataKey="count" fill="url(#barGradient)" radius={[8, 8, 0, 0]} />
            <defs>
              <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#6366f1" />
                <stop offset="100%" stopColor="#3b82f6" />
              </linearGradient>
            </defs>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
