import { UXIssueMessage } from "./ux_issue_message";

export interface Audit {
    id: number;
    key: string;
    category: string;
    subcategory: string;
    name: string;
    level: string;
    createdAt: string;
    messages: UXIssueMessage[]
    points: number;
    totalPossiblePoints: number;
    url: string;
    whyItMatters: string;
    adaCompliance: string;
    recommendations: string[];
}