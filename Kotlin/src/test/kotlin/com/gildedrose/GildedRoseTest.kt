package com.gildedrose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

private const val NORMAL = "Elixir of the Mongoose"
private const val BRIE = "Aged Brie"
private const val BACKSTAGE = "Backstage passes to a TAFKAL80ETC concert"
private const val SULFURAS = "Sulfuras, Hand of Ragnaros"
private const val CONJURED = "Conjured Mana Cake"

private fun updated(name: String, sellIn: Int, quality: Int): Item {
    val item = Item(name, sellIn, quality)
    GildedRose(listOf(item)).updateQuality()
    return item
}

private fun assertItem(expectedSellIn: Int, expectedQuality: Int, item: Item) {
    assertEquals(expectedSellIn, item.sellIn, "sellIn of $item")
    assertEquals(expectedQuality, item.quality, "quality of $item")
}

internal class GildedRoseTest {

    @Nested
    inner class General {
        @Test
        fun `sellIn decreases by one each day`() = assertEquals(4, updated(NORMAL, 5, 7).sellIn)

        @Test
        fun `quality never negative before sell date`() = assertItem(4, 0, updated(NORMAL, 5, 0))

        @Test
        fun `quality never negative after sell date`() = assertItem(-1, 0, updated(NORMAL, 0, 0))

        @Test
        fun `quality never above 50`() = assertItem(4, 50, updated(BRIE, 5, 50))

        @Test
        fun `updates every item in the list independently`() {
            val items = listOf(Item(NORMAL, 5, 7), Item(BRIE, 2, 0), Item(SULFURAS, 0, 80), Item(BACKSTAGE, 5, 20))
            GildedRose(items).updateQuality()
            assertItem(4, 6, items[0])
            assertItem(1, 1, items[1])
            assertItem(0, 80, items[2])
            assertItem(4, 23, items[3])
        }

        @Test
        fun `empty list is a no-op`() {
            val app = GildedRose(emptyList())
            app.updateQuality()
            assertEquals(0, app.items.size)
        }
    }

    @Nested
    inner class Normal {
        @Test
        fun `degrades by one before sell date`() = assertItem(4, 6, updated(NORMAL, 5, 7))

        @Test
        fun `degrades by two once sell date passed`() = assertItem(-1, 8, updated(NORMAL, 0, 10))

        @Test
        fun `double degradation floors at zero`() = assertItem(-2, 0, updated(NORMAL, -1, 1))

        @Test
        fun `Aged not at start of name is normal`() = assertItem(4, 6, updated("Brie Aged", 5, 7))

        @Test
        fun `Aged as part of a longer word is normal`() = assertItem(4, 6, updated("Agedness", 5, 7))

        @Test
        fun `Conjured not at start of name is normal`() = assertItem(4, 6, updated("Mana Cake Conjured", 5, 7))

        @Test
        fun `Conjured as part of a longer word is normal`() = assertItem(4, 6, updated("Conjuredness", 5, 7))
    }

    @Nested
    inner class AgedBrie {
        @Test
        fun `increases by one before sell date`() = assertItem(1, 1, updated(BRIE, 2, 0))

        @Test
        fun `increases by two once sell date passed`() = assertItem(-1, 12, updated(BRIE, 0, 10))

        @Test
        fun `double increase caps at 50`() = assertItem(-1, 50, updated(BRIE, 0, 49))

        @Test
        fun `any name starting with Aged follows Brie rules`() = assertItem(1, 1, updated("Aged Cheese", 2, 0))

        @Test
        fun `Aged prefix match ignores case`() = assertItem(1, 1, updated("aged brie", 2, 0))
    }

    @Nested
    inner class BackstagePasses {
        @Test
        fun `increases by one when more than 10 days left`() = assertItem(10, 21, updated(BACKSTAGE, 11, 20))

        @Test
        fun `increases by two when 10 days left`() = assertItem(9, 22, updated(BACKSTAGE, 10, 20))

        @Test
        fun `increases by two when 6 days left`() = assertItem(5, 22, updated(BACKSTAGE, 6, 20))

        @Test
        fun `increases by three when 5 days left`() = assertItem(4, 23, updated(BACKSTAGE, 5, 20))

        @Test
        fun `increases by three when 1 day left`() = assertItem(0, 23, updated(BACKSTAGE, 1, 20))

        @Test
        fun `drops to zero after the concert`() = assertItem(-1, 0, updated(BACKSTAGE, 0, 20))

        @Test
        fun `triple increase caps at 50`() = assertItem(4, 50, updated(BACKSTAGE, 5, 49))

        @Test
        fun `any name containing Backstage pass follows backstage rules`() =
            assertItem(4, 23, updated("Backstage passes to a Metallica concert", 5, 20))

        @Test
        fun `Backstage pass match ignores case and position`() =
            assertItem(4, 23, updated("VIP backstage pass", 5, 20))
    }

    @Nested
    inner class Sulfuras {
        @Test
        fun `never changes`() = assertItem(0, 80, updated(SULFURAS, 0, 80))

        @Test
        fun `never changes with negative sellIn`() = assertItem(-1, 80, updated(SULFURAS, -1, 80))

        @Test
        fun `name match ignores case`() = assertItem(0, 80, updated("sulfuras, hand of ragnaros", 0, 80))
    }

    @Nested
    inner class Conjured {
        @Test
        fun `degrades by two before sell date`() = assertItem(2, 4, updated(CONJURED, 3, 6))

        @Test
        fun `degrades by four once sell date passed`() = assertItem(-1, 6, updated(CONJURED, 0, 10))

        @Test
        fun `degradation floors at zero before sell date`() = assertItem(4, 0, updated(CONJURED, 5, 1))

        @Test
        fun `degradation floors at zero after sell date`() = assertItem(-1, 0, updated(CONJURED, 0, 3))

        @Test
        fun `Conjured prefix match ignores case`() = assertItem(2, 4, updated("conjured mana cake", 3, 6))

        @Test
        fun `Conjured takes precedence over other categories`() = assertItem(2, 4, updated("Conjured Aged Brie", 3, 6))
    }

    @Nested
    inner class QualityClamping {
        @Test
        fun `Brie below zero is clamped after the daily change`() = assertItem(4, 0, updated(BRIE, 5, -1))

        @Test
        fun `normal below zero is clamped to zero`() = assertItem(4, 0, updated(NORMAL, 5, -1))

        @Test
        fun `normal above 50 is clamped to 50`() = assertItem(4, 50, updated(NORMAL, 5, 60))

        @Test
        fun `Brie above 50 is clamped to 50`() = assertItem(4, 50, updated(BRIE, 5, 60))

        @Test
        fun `Backstage above 50 is clamped to 50`() = assertItem(14, 50, updated(BACKSTAGE, 15, 60))
    }

    @Test
    fun `thirty days of the fixture match the golden master`() {
        val expected = File("../texttests/ThirtyDays/stdout.gr").readText()
        val buffer = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(buffer, true))
        try {
            main(arrayOf("30"))
        } finally {
            System.setOut(originalOut)
        }
        assertEquals(expected, buffer.toString())
    }
}
