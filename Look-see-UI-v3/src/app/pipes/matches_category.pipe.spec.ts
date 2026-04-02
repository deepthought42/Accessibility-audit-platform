import { MatchesCategoryPipe } from './matches_category.pipe';

describe('MatchesCategoryPipe', () => {
  const pipe = new MatchesCategoryPipe();

  it('returns original items when category is missing', () => {
    const items = [{ category: 'a' }];

    expect(pipe.transform(items as unknown[], '')).toBe(items);
  });

  it('filters by exact category on object items only', () => {
    const items: unknown[] = [
      { category: 'design', id: 1 },
      { category: 'content', id: 2 },
      null,
      'text-only'
    ];

    expect(pipe.transform(items, 'design')).toEqual([{ category: 'design', id: 1 }]);
  });
});
