import { AuditScore } from "./audit_score";
import { SimpleElement } from "./simple_element";
import { UXIssueMessage } from "./ux_issue_message";

type ElementIssuesMap = Record<string, string[]>;

type IssueElementMap = Record<string, string>;

export interface ObservationElementMapTwoWayBinding {
    issues: UXIssueMessage[]
    elements: SimpleElement[]
    elementIssues: ElementIssuesMap
    issueElementMap: IssueElementMap
    scores: AuditScore
    pageSrc: string
}
