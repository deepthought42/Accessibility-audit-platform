import { Brand } from "../brand/brand";

export interface Competitor {
    id: number
    companyName: string
    url: string
    industry: string
    analysisRunning: boolean
    brand: Brand
}