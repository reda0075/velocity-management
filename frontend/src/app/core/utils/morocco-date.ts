const MOROCCO_TIME_ZONE = 'Africa/Casablanca';

export interface MoroccoPeriod {
  year: number;
  month: number;
}

/**
 * Current year and month in Morocco (Africa/Casablanca), whatever the
 * browser's local timezone is. Velocity calculations are tracked per
 * Gregorian month in Morocco, so every "current month" default in the
 * app must be derived from here instead of `new Date()`.
 */
export function currentMoroccoPeriod(): MoroccoPeriod {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: MOROCCO_TIME_ZONE,
    year: 'numeric',
    month: 'numeric'
  }).formatToParts(new Date());

  let year = 0;
  let month = 0;

  for (const part of parts) {
    if (part.type === 'year') {
      year = Number(part.value);
    } else if (part.type === 'month') {
      month = Number(part.value);
    }
  }

  if (year === 0 || month === 0) {
    const fallback = new Date();
    return { year: fallback.getFullYear(), month: fallback.getMonth() + 1 };
  }

  return { year, month };
}
