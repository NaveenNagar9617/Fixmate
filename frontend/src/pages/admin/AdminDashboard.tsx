import { useDashboardStats, useCategoryBreakdown, useBlockStats, useStaffPerformance, useDailyTrend, useHeatmap } from '@/hooks/useAnalytics';
import StatsCard from '@/components/analytics/StatsCard';
import CategoryPieChart from '@/components/analytics/CategoryPieChart';
import BlockBarChart from '@/components/analytics/BlockBarChart';
import TrendLineChart from '@/components/analytics/TrendLineChart';
import StaffTable from '@/components/analytics/StaffTable';
import HeatmapGrid from '@/components/analytics/HeatmapGrid';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { AlertTriangle, Clock, RotateCcw, FileText } from 'lucide-react';

export default function AdminDashboard() {
  const { data: stats, isLoading: statsLoading } = useDashboardStats();
  const { data: categories } = useCategoryBreakdown();
  const { data: blocks } = useBlockStats();
  const { data: staffPerf } = useStaffPerformance();
  const { data: trend } = useDailyTrend(30);
  const { data: heatmap } = useHeatmap();

  if (statsLoading) return <LoadingSpinner label="Loading analytics..." />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Admin Dashboard</h1>
        <p className="text-muted-foreground text-sm mt-1">Overview of hostel maintenance operations</p>
      </div>

      {/* Stats Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          label="Open Complaints"
          value={stats?.openComplaints ?? 0}
          icon={<FileText className="h-5 w-5" />}
          color="from-blue-500 to-indigo-600"
        />
        <StatsCard
          label="SLA Breach Rate"
          value={`${stats?.slaBreachRate ?? 0}%`}
          icon={<AlertTriangle className="h-5 w-5" />}
          color="from-red-500 to-rose-600"
        />
        <StatsCard
          label="Avg Fix Time"
          value={`${stats?.avgResolutionHours ?? 0}h`}
          icon={<Clock className="h-5 w-5" />}
          color="from-amber-500 to-orange-600"
        />
        <StatsCard
          label="Reopen Rate"
          value={`${stats?.reopenRate ?? 0}%`}
          icon={<RotateCcw className="h-5 w-5" />}
          color="from-purple-500 to-pink-600"
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <CategoryPieChart data={categories || []} />
        <BlockBarChart data={blocks || []} />
      </div>

      {/* Trend */}
      <TrendLineChart data={trend || []} />

      {/* Staff Performance */}
      <StaffTable data={staffPerf || []} />

      {/* Heatmap */}
      <HeatmapGrid data={heatmap || {}} />
    </div>
  );
}
