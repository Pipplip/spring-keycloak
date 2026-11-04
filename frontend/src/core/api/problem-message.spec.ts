import { HttpErrorResponse } from '@angular/common/http';
import { problemMessage } from './problem-message';

describe('problemMessage', () => {
  it('uses the safe Problem Details message for conflict errors', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { status: 409, title: 'Conflict', detail: 'Inventory is no longer available.' },
    });
    expect(problemMessage(error)).toBe('Inventory is no longer available.');
  });

  it('does not expose server diagnostics', () => {
    const error = new HttpErrorResponse({
      status: 500,
      error: { status: 500, detail: 'SQL connection password: do-not-show' },
    });
    expect(problemMessage(error)).toBe(
      'The service encountered a problem. Please try again later.',
    );
  });

  it('gives a clear message for network failures and unknown errors', () => {
    expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toContain('could not be reached');
    expect(problemMessage(new Error('private exception'))).toBe(
      'The request could not be completed. Please try again.',
    );
  });
});
