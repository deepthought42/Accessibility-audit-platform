interface Analytics {
  identify(userId?: string, traits?: Record<string, unknown>): void;
  track(event: string, properties?: Record<string, unknown>): void;
  page(category?: string, name?: string, properties?: Record<string, unknown>): void;
  alias(userId: string, previousId?: string): void;
  group(groupId: string, traits?: Record<string, unknown>): void;
  reset(): void;
  trackLink(element: HTMLElement, event: string, properties?: Record<string, unknown>): void;
  trackForm(element: HTMLElement, event: string, properties?: Record<string, unknown>): void;
  ready(callback: () => void): void;
  timeout(ms: number): void;
  debug(enabled?: boolean): void;
  on(method: string, callback: (event: string, properties?: Record<string, unknown>) => void): void;
  once(method: string, callback: (event: string, properties?: Record<string, unknown>) => void): void;
  off(method: string, callback?: (event: string, properties?: Record<string, unknown>) => void): void;
}

declare global {
  interface Window {
    analytics: Analytics;
  }
}

export { };
