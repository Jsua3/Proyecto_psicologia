import { signal } from '@angular/core';

// Pure-logic test: the URL-matching predicate
function isGameRoute(url: string): boolean {
  return /\/portal\/simulador\/\d+/.test(url);
}

describe('ShellComponent game-mode logic', () => {
  it('detects simulador/:id route', () => {
    expect(isGameRoute('/portal/simulador/42')).toBe(true);
  });

  it('does not match simulador list', () => {
    expect(isGameRoute('/portal/simulador')).toBe(false);
  });

  it('does not match other routes', () => {
    expect(isGameRoute('/portal/dashboard')).toBe(false);
  });
});
