import { FilterByCategoryPipe } from './category_filter.pipe';

describe('FilterByCategoryPipe', () => {
  const pipe = new FilterByCategoryPipe();

  it('returns an empty array when issues is undefined', () => {
    expect(pipe.transform(undefined as never, 'visual')).toEqual([]);
  });

  it('matches category case-insensitively', () => {
    const issues = [
      { category: 'Visual', priority: 'HIGH' },
      { category: 'content', priority: 'LOW' }
    ] as any;

    const result = pipe.transform(issues, 'visual');
    expect(result.length).toBe(1);
    expect(result[0].category).toBe('Visual');
  });
});
