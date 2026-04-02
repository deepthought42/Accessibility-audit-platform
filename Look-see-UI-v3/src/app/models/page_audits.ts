import { ObservationElementMapTwoWayBinding } from './ObservationElementMapTwoWayBinding';
import { SimplePage } from './simple_page';

export interface PageAudits {
    auditRecordId: number
    simplePage: SimplePage
    elementIssueMap: ObservationElementMapTwoWayBinding
    status: string
    level: string
}