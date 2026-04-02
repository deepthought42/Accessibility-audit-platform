import { Pipe, PipeTransform } from '@angular/core';
import { UXIssueMessage } from '../models/ux_issue_message';

@Pipe({
    name: 'filterByCategory'
})
export class FilterByCategoryPipe implements PipeTransform {

    transform(issues : UXIssueMessage[], category: string): UXIssueMessage[] {
        if (issues) {
            return issues.filter((issue: UXIssueMessage) => issue.category.toUpperCase() == category.toUpperCase());
        }
        return []
    }
}