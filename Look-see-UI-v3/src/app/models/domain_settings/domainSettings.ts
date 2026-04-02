import { DesignSystem } from "../design_system";
import { TestUser } from "../testUser/testUser";

export interface DomainSettings {
    designSystem: DesignSystem
    testUsers : TestUser[]
}