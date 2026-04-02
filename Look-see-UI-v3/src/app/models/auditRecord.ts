import { Audit } from './audit';
import { SimplePage } from './simple_page';

export interface AuditRecord {
    id: number;
    key: string;
    audits: Audit[];
    simplePage: SimplePage;
    status: string;
    level: string;
    url: string;
    contentAuditScore: number;
    infoArchScore: number;
    aestheticScore: number;
}