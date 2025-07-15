import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { cn } from '@/lib/utils';

interface HeatmapGridProps {
  data: Record<string, Record<number, number>>;
}

function getIntensity(count: number, maxCount: number): string {
  if (count === 0) return 'bg-muted';
  const ratio = count / maxCount;
  if (ratio > 0.75) return 'bg-red-500 text-white';
  if (ratio > 0.5) return 'bg-orange-400 text-white';
  if (ratio > 0.25) return 'bg-amber-300 text-amber-900';
  return 'bg-emerald-200 text-emerald-900';
}

export default function HeatmapGrid({ data }: HeatmapGridProps) {
  if (!data || Object.keys(data).length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">Complaint Heatmap</CardTitle></CardHeader>
        <CardContent><p className="text-sm text-muted-foreground">No data available</p></CardContent>
      </Card>
    );
  }

  const blocks = Object.keys(data).sort();
  const allFloors = new Set<number>();
  blocks.forEach((block) => {
    Object.keys(data[block]).forEach((floor) => allFloors.add(Number(floor)));
  });
  const floors = Array.from(allFloors).sort((a, b) => b - a);

  let maxCount = 0;
  blocks.forEach((block) => {
    Object.values(data[block]).forEach((count) => {
      if (count > maxCount) maxCount = count;
    });
  });

  return (
    <Card className="hover-lift">
      <CardHeader>
        <CardTitle className="text-base">Complaint Heatmap</CardTitle>
        <p className="text-xs text-muted-foreground">Block × Floor intensity map</p>
      </CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="text-left py-2 px-3 text-xs font-medium text-muted-foreground">Floor</th>
                {blocks.map((block) => (
                  <th key={block} className="py-2 px-3 text-xs font-medium text-muted-foreground text-center">
                    {block}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {floors.map((floor) => (
                <tr key={floor}>
                  <td className="py-1 px-3 text-xs font-medium text-muted-foreground">F{floor}</td>
                  {blocks.map((block) => {
                    const count = data[block]?.[floor] || 0;
                    return (
                      <td key={`${block}-${floor}`} className="py-1 px-3">
                        <div
                          className={cn(
                            'w-full h-10 rounded-lg flex items-center justify-center text-xs font-bold transition-all duration-200 hover:scale-105',
                            getIntensity(count, maxCount)
                          )}
                        >
                          {count}
                        </div>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex items-center gap-3 mt-4 justify-center">
          <span className="text-[10px] text-muted-foreground">Low</span>
          {['bg-emerald-200', 'bg-amber-300', 'bg-orange-400', 'bg-red-500'].map((c, i) => (
            <div key={i} className={cn('h-3 w-8 rounded', c)} />
          ))}
          <span className="text-[10px] text-muted-foreground">High</span>
        </div>
      </CardContent>
    </Card>
  );
}
