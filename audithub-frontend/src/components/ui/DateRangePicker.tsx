import { CalendarDays } from 'lucide-react';
import { Button } from './Button';

interface DateRangePickerProps {
  startTime: string;
  endTime:   string;
  onChange:  (start: string, end: string) => void;
}

export function DateRangePicker({ startTime, endTime, onChange }: DateRangePickerProps) {
  // We use browser default native date inputs styled cleanly
  // representing date ranges to avoid DayPicker dependency errors.
  const handleStartChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.value) {
      onChange(new Date(e.target.value).toISOString(), endTime);
    }
  };

  const handleEndChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.value) {
      onChange(startTime, new Date(e.target.value).toISOString());
    }
  };

  const formatDate = (isoStr: string) => {
    try {
      return isoStr.split('T')[0];
    } catch {
      return '';
    }
  };

  return (
    <div className="flex items-center gap-2 bg-white border border-gray-300 rounded-lg px-2.5 py-1.5 shadow-sm text-sm">
      <CalendarDays className="h-4 w-4 text-gray-500" />
      <input
        type="date"
        value={formatDate(startTime)}
        onChange={handleStartChange}
        className="border-0 bg-transparent p-0 text-xs focus:ring-0 focus:outline-none text-gray-700"
      />
      <span className="text-gray-400 font-medium text-xs">to</span>
      <input
        type="date"
        value={formatDate(endTime)}
        onChange={handleEndChange}
        className="border-0 bg-transparent p-0 text-xs focus:ring-0 focus:outline-none text-gray-700"
      />
    </div>
  );
}
