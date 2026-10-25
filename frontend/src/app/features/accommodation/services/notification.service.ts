import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiError } from '../models/accommodation.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, undefined, { duration: 3000, panelClass: 'tm-snack-success' });
  }

  error(error: unknown, fallback = 'Something went wrong'): void {
    this.snackBar.open(errorMessage(error, fallback), 'Close', { duration: 6000, panelClass: 'tm-snack-error' });
  }
}

/** Extracts a readable message from the backend's error JSON. */
export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Cannot reach the server. Is the backend running on port 8080?';
    const body = error.error as Partial<ApiError> | null;
    if (body?.fieldErrors && Object.keys(body.fieldErrors).length) {
      const [field, msg] = Object.entries(body.fieldErrors)[0];
      return `${body.message ?? 'Validation failed'}: ${field.replace(/Valid$/, '')} ${msg}`;
    }
    if (body?.message) return body.message;
  }
  return fallback;
}
