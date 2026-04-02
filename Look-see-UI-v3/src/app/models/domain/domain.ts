export interface Domain {
    id: number
    key: string
    url: string
    progress: number
    pageCount: number
    pagesAudited: number
    contentScore: number
    contentAuditProgress: number
    aestheticsScore: number
    aestheticsProgress: number
    infoArchitectureScore: number
    infoArchitectureAuditProgress: number
    accessibilityScore: number
    accessibilityProgress: number
    dataExtractionProgress: number
    overallProgress: number
    isAuditRunning: boolean
    message: string
    status: string
}