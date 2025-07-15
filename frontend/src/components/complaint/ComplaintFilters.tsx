import { useState, useCallback, useEffect, useRef } from 'react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/Select';
import { Search, X, SlidersHorizontal } from 'lucide-react';
import type { ComplaintFilters as ComplaintFiltersType } from '@/hooks/useComplaints';

interface FiltersProps {
  filters: ComplaintFiltersType;
  onFilterChange: (filters: ComplaintFiltersType) => void;
  showBlock?: boolean;
  showStaff?: boolean;
}

const statuses = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ESCALATED', 'REOPENED'];
const categories = ['ELECTRICAL', 'PLUMBING', 'WIFI', 'FURNITURE', 'CLEANING', 'PEST_CONTROL', 'SECURITY', 'OTHER'];
const priorities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const blocks = ['Block A', 'Block B', 'Block C', 'Block D', 'Block E'];

export default function ComplaintFilters({ filters, onFilterChange, showBlock = true }: FiltersProps) {
  const [search, setSearch] = useState(filters.search || '');
  const [showFilters, setShowFilters] = useState(false);

  // Use refs to avoid stale closures in the debounce effect
  const filtersRef = useRef(filters);
  const onFilterChangeRef = useRef(onFilterChange);
  filtersRef.current = filters;
  onFilterChangeRef.current = onFilterChange;

  useEffect(() => {
    const timer = setTimeout(() => {
      if (search !== (filtersRef.current.search || '')) {
        onFilterChangeRef.current({ ...filtersRef.current, search, page: 0 });
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const handleChange = useCallback((key: string, value: string | undefined) => {
    onFilterChange({ ...filters, [key]: value || undefined, page: 0 });
  }, [filters, onFilterChange]);

  const clearFilters = useCallback(() => {
    setSearch('');
    onFilterChange({ page: 0, size: 10 });
  }, [onFilterChange]);

  const hasActiveFilters = filters.status || filters.category || filters.priority || filters.block || search;

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search complaints..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <Button
          variant={showFilters ? 'default' : 'outline'}
          size="icon"
          onClick={() => setShowFilters(!showFilters)}
        >
          <SlidersHorizontal className="h-4 w-4" />
        </Button>
        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            <X className="h-4 w-4 mr-1" /> Clear
          </Button>
        )}
      </div>

      {showFilters && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-4 rounded-xl bg-muted/50 border animate-scale-in">
          <Select value={filters.status || ''} onValueChange={(v) => handleChange('status', v)}>
            <SelectTrigger><SelectValue placeholder="Status" /></SelectTrigger>
            <SelectContent>
              {statuses.map((s) => (
                <SelectItem key={s} value={s}>{s.replace(/_/g, ' ')}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={filters.category || ''} onValueChange={(v) => handleChange('category', v)}>
            <SelectTrigger><SelectValue placeholder="Category" /></SelectTrigger>
            <SelectContent>
              {categories.map((c) => (
                <SelectItem key={c} value={c}>{c.replace(/_/g, ' ')}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={filters.priority || ''} onValueChange={(v) => handleChange('priority', v)}>
            <SelectTrigger><SelectValue placeholder="Priority" /></SelectTrigger>
            <SelectContent>
              {priorities.map((p) => (
                <SelectItem key={p} value={p}>{p}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          {showBlock && (
            <Select value={filters.block || ''} onValueChange={(v) => handleChange('block', v)}>
              <SelectTrigger><SelectValue placeholder="Block" /></SelectTrigger>
              <SelectContent>
                {blocks.map((b) => (
                  <SelectItem key={b} value={b}>{b}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
      )}
    </div>
  );
}
