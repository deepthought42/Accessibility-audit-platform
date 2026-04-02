import { PaletteColor } from "./palette_color"

export interface ColorPalette {
    id: number
    created_at: Date
    colorScheme: string
    paletteColors: PaletteColor[]
}