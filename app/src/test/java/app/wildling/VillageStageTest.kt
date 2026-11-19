package app.wildling

import app.wildling.bridge.VillageProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class VillageStageTest {
    /** Lingvale hides stage 0 as an egg Wildling has no picture for; every creature must come hatched. */
    @Test fun everyTrustStageIsHatchedInTheVillage() {
        assertEquals(listOf(1, 1, 2, 3, 4, 4), (0..5).map(VillageProvider::villageStage))
    }
}
