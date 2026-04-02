
export interface DomainAuditStats {
    id: number,
    journeysExplored: number,
    journeysTotal: number,
    pageCount: number,
    accessibilityScore: number,
    contentScore: number,
    writtenContentScore: number,
    imageryScore: number,
    infoArchitectureScore: number,
    seoScore: number,
    linkScore: number,
    aestheticsScore: number,
    textContrastScore: number,
    nonTextContrastScore: number,
    status: string
}