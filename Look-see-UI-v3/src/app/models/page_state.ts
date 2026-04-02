export interface PageState {
    id: number
    key: string
    createdAt: Date

    src: string
    url: string
    login_required: boolean
    last_landability_check: string
    viewportScreenshotUrl: string
    fullPageScreenshotUrl: string
    browser: string
    landable: boolean
    scrollXOffset: number
    scrollYOffset: number
    viewport_width: number
    viewport_height: number
    full_page_width: number
    full_page_height: number
    name: string
}