import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../../../build/generated/openapi/typescript-angular/model/problemDetail';

const safeFallback = 'The request could not be completed. Please try again.';

export function problemMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return safeFallback;
  }
  if (error.status === 0) {
    return 'The service could not be reached. Check your connection and try again.';
  }
  if (error.status === 401) {
    return 'Your session has expired. Sign in again to continue.';
  }
  if (error.status === 403) {
    return 'You do not have permission to perform this action.';
  }
  if (error.status === 404) {
    return 'The requested item could not be found.';
  }
  if (error.status === 409) {
    const problem = error.error as ProblemDetail | null;
    if (problem && typeof problem.detail === 'string' && problem.detail.trim()) {
      return problem.detail;
    }
    return 'This action conflicts with the current state. Refresh and try again.';
  }
  if (error.status >= 500) {
    return 'The service encountered a problem. Please try again later.';
  }

  const problem = error.error as ProblemDetail | null;
  if (problem && typeof problem.detail === 'string' && problem.detail.trim()) {
    return problem.detail;
  }
  if (problem && typeof problem.title === 'string' && problem.title.trim()) {
    return problem.title;
  }
  return safeFallback;
}
