import { UXIssueMessage } from "./ux_issue_message";

export interface ColorPaletteObservation extends UXIssueMessage {
    id: number;
    key: string;
    type: string;
    createdAt: Date;
    description: string;
    colors: string[];
    color_scheme: string
}