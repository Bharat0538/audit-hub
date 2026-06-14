import { format, formatDistanceToNow, parseISO, isValid } from 'date-fns';

export const IST_TIMEZONE = 'Asia/Kolkata';

export function formatIST(dateStr: string, fmt = 'dd MMM yyyy, HH:mm:ss'): string {
  try {
    const date = parseISO(dateStr);
    if (!isValid(date)) return dateStr;
    return new Intl.DateTimeFormat('en-IN', {
      timeZone: IST_TIMEZONE,
      day:    '2-digit',
      month:  'short',
      year:   'numeric',
      hour:   '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(date);
  } catch {
    return dateStr;
  }
}

export function formatRelative(dateStr: string): string {
  try {
    return formatDistanceToNow(parseISO(dateStr), { addSuffix: true });
  } catch {
    return dateStr;
  }
}

export function formatShort(dateStr: string): string {
  try {
    return format(parseISO(dateStr), 'dd MMM, HH:mm');
  } catch {
    return dateStr;
  }
}

export function toISOString(date: Date): string {
  return date.toISOString();
}

export function startOfDayISO(date: Date): string {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d.toISOString();
}

export function endOfDayISO(date: Date): string {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d.toISOString();
}
