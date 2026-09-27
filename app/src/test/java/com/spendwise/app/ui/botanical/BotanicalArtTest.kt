package com.spendwise.app.ui.botanical

import com.spendwise.app.domain.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BotanicalArtTest {

    @Test
    fun explicitPickWins() {
        assertEquals(CategoryArt.Zakat, categoryArtFor("Food", "art_zakat", isIncome = false))
        assertEquals(CategoryArt.Toll, CategoryArt.fromKey("art_toll"))
    }

    @Test
    fun builtInCategoriesKeepTheirIllustrations() {
        assertEquals(CategoryArt.Food, categoryArtFor("Food", "restaurant", false))
        assertEquals(CategoryArt.Transport, categoryArtFor("Transport", "directions_car", false))
        assertEquals(CategoryArt.Bills, categoryArtFor("Bills", "receipt", false))
        assertEquals(CategoryArt.Shopping, categoryArtFor("Shopping", "shopping_bag", false))
        assertEquals(CategoryArt.Health, categoryArtFor("Health", "local_hospital", false))
        assertEquals(CategoryArt.Salary, categoryArtFor("Salary", "account_balance_wallet", true))
    }

    @Test
    fun nameIsMoreSpecificThanAnOldGenericIcon() {
        assertEquals(CategoryArt.Petrol, categoryArtFor("Petrol", "directions_car", false))
        assertEquals(CategoryArt.Phone, categoryArtFor("Internet & phone", "phone", false))
        assertEquals(CategoryArt.Groceries, categoryArtFor("Groceries", "cart", false))
        assertEquals(CategoryArt.Zakat, categoryArtFor("Sedekah", "gift", false))
    }

    @Test
    fun keywordsMatchWordsNotFragments() {
        // "parent" must not read as rent, "airport" must not read as water.
        assertEquals(CategoryArt.Family, categoryArtFromName("Parents"))
        assertNull(categoryArtFromName("Airport"))
        assertEquals(CategoryArt.Utilities, categoryArtFromName("Bil air"))
        assertEquals(CategoryArt.Toll, categoryArtFromName("Tol & parking"))
        assertEquals(CategoryArt.Investment, categoryArtFromName("Investments"))
    }

    @Test
    fun unknownCategoriesFallBackByKind() {
        assertEquals(CategoryArt.Bills, categoryArtFor("Netflix", "movie", false))
        assertEquals(CategoryArt.Bills, categoryArtFor("Misc", "", false))
        assertEquals(CategoryArt.Salary, categoryArtFor("Refunds", "", true))
    }

    @Test
    fun accountScenesFollowPickThenType() {
        assertEquals(AccountScene.Alpine, accountSceneFor("scene_alpine", AccountType.Cash))
        assertEquals(AccountScene.Coast, accountSceneFor("account_balance_wallet", AccountType.Bank))
        assertEquals(AccountScene.Falls, accountSceneFor("wallet", AccountType.EWallet))
        assertEquals(AccountScene.Canyon, accountSceneFor(null, AccountType.Cash))
        assertEquals(AccountScene.Alpine, accountSceneFor("card", AccountType.Credit))
    }

    @Test
    fun storedColourIsOpaqueArgb() {
        assertEquals(0xFFFBC120L, CategoryArt.Food.storedColor)
        assertEquals(0xFF2AAC76L, CategoryArt.Zakat.storedColor)
    }
}
