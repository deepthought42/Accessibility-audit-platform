import { Pipe, PipeTransform } from '@angular/core';
import { UXIssueMessage } from '../models/ux_issue_message';

@Pipe({
    name: 'filterSeverity'
})
export class FilterSeverityPipe implements PipeTransform {

    transform(issues : UXIssueMessage[], severity: string): UXIssueMessage[] {
        if (issues) {
            return issues.filter((issue: UXIssueMessage) => issue.priority.toUpperCase() != severity.toUpperCase());
        }
        return []
    }
}