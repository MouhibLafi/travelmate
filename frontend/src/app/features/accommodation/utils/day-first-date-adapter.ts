import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/**
 * NativeDateAdapter relies on Date.parse, which reads "13/11/2026" as month/day and fails.
 * This adapter accepts typed day-first dates (dd/mm/yyyy, dd.mm.yyyy, dd-mm-yyyy) to match the en-GB display format.
 */
@Injectable()
export class DayFirstDateAdapter extends NativeDateAdapter {
  override parse(value: unknown, parseFormat?: unknown): Date | null {
    if (typeof value === 'string') {
      const match = value.trim().match(/^(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})$/);
      if (match) {
        const [, d, m, y] = match.map(Number);
        const date = new Date(y, m - 1, d);
        return date.getMonth() === m - 1 && date.getDate() === d ? date : this.invalid();
      }
    }
    return super.parse(value, parseFormat);
  }
}
