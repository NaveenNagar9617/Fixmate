import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

const COLORS = ['#6366f1', '#3b82f6', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#64748b'];

interface CategoryPieChartProps {
  data: Array<{ category: string; count: number; percentage: number }>;
}

export default function CategoryPieChart({ data }: CategoryPieChartProps) {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">Category Breakdown</CardTitle></CardHeader>
        <CardContent><p className="text-sm text-muted-foreground">No data available</p></CardContent>
      </Card>
    );
  }

  const chartData = data.map((d) => ({
    name: d.category.replace(/_/g, ' '),
    value: d.count,
    percentage: d.percentage,
  }));

  return (
    <Card className="hover-lift">
      <CardHeader><CardTitle className="text-base">Category Breakdown</CardTitle></CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={100}
              paddingAngle={3}
              dataKey="value"
            >
              {chartData.map((_, index) => (
                <Cell key={index} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{ borderRadius: '12px', border: '1px solid hsl(var(--border))', background: 'hsl(var(--card))' }}
              formatter={(value: number, name: string) => [`${value} complaints`, name]}
            />
            <Legend
              verticalAlign="bottom"
              height={36}
              formatter={(value: string) => <span className="text-xs">{value}</span>}
            />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
