import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'matchesCategory'
})
export class MatchesCategoryPipe implements PipeTransform {
    transform(items: unknown[], category: string): unknown[] {
        if (!items || !category) {
            return items;
        }
        return items.filter(item => {
            return item && typeof item === 'object' && 'category' in item && (item as { category: string }).category === category;
        });
    }
}