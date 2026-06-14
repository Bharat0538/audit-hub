import { Button } from './Button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  hasMore:     boolean;
  pageToken?:  string | null;
  onNext:      () => void;
  onPrev?:     () => void;
  canGoPrev?:  boolean;
  totalEstimate?: number;
  pageSize:    number;
  currentCount:number;
}

export function Pagination({
  hasMore, onNext, onPrev, canGoPrev, totalEstimate, currentCount
}: PaginationProps) {
  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
      <span className="text-sm text-gray-500">
        Showing <span className="font-medium text-gray-700">{currentCount}</span> events
        {totalEstimate ? (
          <> · ~<span className="font-medium text-gray-700">{totalEstimate.toLocaleString('en-IN')}</span> total</>
        ) : null}
      </span>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary" size="sm"
          icon={<ChevronLeft className="h-4 w-4" />}
          onClick={onPrev}
          disabled={!canGoPrev}
        >
          Prev
        </Button>
        <Button
          variant="secondary" size="sm"
          iconRight={<ChevronRight className="h-4 w-4" />}
          onClick={onNext}
          disabled={!hasMore}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
