export interface PageStatistic {
    id: number
    url: string
    elementsReviewed: number
    elementsExtracted: number
    auditRecordId: number
    screenshotUrl: string
    contentScore: number
    contentAuditProgress: number
    infoArchScore: number
    infoArchProgress: number
    accessibilityScore: number
    accessibilityProgress: number
    aestheticScore: number
    aestheticAuditProgress: number
    complete: boolean
}
