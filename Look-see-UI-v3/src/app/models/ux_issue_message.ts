import { PageState } from "./page_state";
import { PaletteColor } from "./palette_color";
import { SimpleElement } from "./simple_element";

export interface UXIssueMessage {
    id: number;
    key: string;
    type: string;
    createdAt: Date;
    title: string;
    description: string;
    recommendation: string;
    priority: string;
    category: string;
    wcagCompliance: string;
    
    // color contrast messages
    contrast? : number
    foregroundColor? : string
    backgroundColor? : string
    fontSize?: string
    element : SimpleElement // element/elenent state issue message
    goodExample?: SimpleElement
    
    //color palette messages
    palette_colors? : PaletteColor[]
    colors?: string[];
    color_scheme?: string;

    page_state?: PageState
}