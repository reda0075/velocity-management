import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts a human-readable message from a backend error response.
 * Defensive by design: we don't yet know the exact shape of validation
 * error bodies, so this tries the common Spring Boot shapes and falls
 * back to a friendly generic message otherwise.
 */
export function extractErrorMessage(err: HttpErrorResponse, fallback: string): string {
  const body = err.error;

  if (typeof body === 'string' && body.trim()) {
    return body;
  }

  if (body && typeof body === 'object') {
    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message;
    }
    if (typeof body.error === 'string' && body.error.trim()) {
      return body.error;
    }
  }

  if (err.status === 409) {
    return 'This action conflicts with existing data.';
  }
  if (err.status === 404) {
    return 'Not found. It may have already been removed.';
  }
  if (err.status === 0) {
    return 'Could not reach the server. Is the backend running?';
  }

  return fallback;
}