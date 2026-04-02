import { FilterSeverityPipe } from './severity_filter.pipe';

describe('FilterSeverityPipe', () => {
  const pipe = new FilterSeverityPipe();

  it('returns an empty array when issues is undefined', () => {
    expect(pipe.transform(undefined as never, 'HIGH')).toEqual([]);
  });

  it('returns issues that do not match severity case-insensitively', () => {
    const issues = [
      { category: 'Visual', priority: 'HIGH' },
      { category: 'content', priority: 'LOW' }
    ] as any;

    const result = pipe.transform(issues, 'high');
    expect(result.length).toBe(1);
    expect(result[0].priority).toBe('LOW');
  });
});
