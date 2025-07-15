import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Star } from 'lucide-react';

interface StaffData {
  staffId: string;
  name: string;
  assigned: number;
  resolved: number;
  avgHours: number;
  reopenCount: number;
  avgRating: number;
}

interface StaffTableProps {
  data: StaffData[];
}

export default function StaffTable({ data }: StaffTableProps) {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">Staff Performance</CardTitle></CardHeader>
        <CardContent><p className="text-sm text-muted-foreground">No data available</p></CardContent>
      </Card>
    );
  }

  return (
    <Card className="hover-lift">
      <CardHeader><CardTitle className="text-base">Staff Performance</CardTitle></CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-2 font-medium text-muted-foreground">Name</th>
                <th className="text-center py-3 px-2 font-medium text-muted-foreground">Assigned</th>
                <th className="text-center py-3 px-2 font-medium text-muted-foreground">Resolved</th>
                <th className="text-center py-3 px-2 font-medium text-muted-foreground">Avg Time</th>
                <th className="text-center py-3 px-2 font-medium text-muted-foreground">Reopens</th>
                <th className="text-center py-3 px-2 font-medium text-muted-foreground">Rating</th>
              </tr>
            </thead>
            <tbody>
              {data.map((staff) => (
                <tr key={staff.staffId} className="border-b last:border-b-0 hover:bg-muted/50 transition-colors">
                  <td className="py-3 px-2 font-medium">{staff.name}</td>
                  <td className="py-3 px-2 text-center">{staff.assigned}</td>
                  <td className="py-3 px-2 text-center text-emerald-600 font-medium">{staff.resolved}</td>
                  <td className="py-3 px-2 text-center">{staff.avgHours}h</td>
                  <td className="py-3 px-2 text-center">{staff.reopenCount}</td>
                  <td className="py-3 px-2 text-center">
                    <div className="flex items-center justify-center gap-1">
                      <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                      <span className="font-medium">{staff.avgRating}</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}
