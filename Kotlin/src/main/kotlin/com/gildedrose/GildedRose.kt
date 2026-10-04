package com.gildedrose

private const val AGED_BRIE = "Aged Brie"
private const val BACKSTAGE_PASSES = "Backstage passes to a TAFKAL80ETC concert"
private const val SULFURAS = "Sulfuras, Hand of Ragnaros"
private const val MIN_QUALITY = 0
private const val MAX_QUALITY = 50

class GildedRose(val items: List<Item>) {

    fun updateQuality() {
        for (item in items) {
            if (item.name == SULFURAS) continue // legendary: never sold, never changes

            item.sellIn -= 1
            val expired = item.sellIn < 0

            when (item.name) {
                AGED_BRIE -> increaseQuality(item, if (expired) 2 else 1)
                BACKSTAGE_PASSES -> updateBackstagePasses(item)
                else -> decreaseQuality(item, if (expired) 2 else 1)
            }
        }
    }

    // thresholds use sellIn after today's decrement (legacy: < 11 / < 6 before it)
    private fun updateBackstagePasses(item: Item) = when {
        item.sellIn < 0 -> item.quality = MIN_QUALITY
        item.sellIn < 5 -> increaseQuality(item, 3)
        item.sellIn < 10 -> increaseQuality(item, 2)
        else -> increaseQuality(item, 1)
    }

    // legacy semantics: only move toward the limit, never past it; out-of-range values are left alone
    private fun increaseQuality(item: Item, by: Int) {
        if (item.quality < MAX_QUALITY) item.quality = minOf(item.quality + by, MAX_QUALITY)
    }

    private fun decreaseQuality(item: Item, by: Int) {
        if (item.quality > MIN_QUALITY) item.quality = maxOf(item.quality - by, MIN_QUALITY)
    }
}
