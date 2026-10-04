package com.gildedrose

private const val AGED_PREFIX = "Aged " // trailing space: whole word only
private const val BACKSTAGE_PASS = "Backstage pass"
private const val CONJURED_PREFIX = "Conjured " // trailing space: whole word only
private const val SULFURAS = "Sulfuras, Hand of Ragnaros"
private const val MIN_QUALITY = 0
private const val MAX_QUALITY = 50

class GildedRose(val items: List<Item>) {

    fun updateQuality() {
        for (item in items) {
            if (item.name.equals(SULFURAS, ignoreCase = true)) continue // legendary: never sold, never changes

            item.sellIn -= 1
            val expired = item.sellIn < 0

            val change = when {
                // first: Conjured wins over other categories, e.g. "Conjured Aged Brie"
                item.name.startsWith(CONJURED_PREFIX, ignoreCase = true) -> if (expired) -4 else -2
                item.name.startsWith(AGED_PREFIX, ignoreCase = true) -> if (expired) 2 else 1
                item.name.contains(BACKSTAGE_PASS, ignoreCase = true) -> backstagePassesChange(item)
                else -> if (expired) -2 else -1
            }
            item.quality = (item.quality + change).coerceIn(MIN_QUALITY, MAX_QUALITY)
        }
    }

    // thresholds use sellIn after today's decrement (legacy: < 11 / < 6 before it)
    private fun backstagePassesChange(item: Item) = when {
        item.sellIn < 0 -> -item.quality // worthless after the concert
        item.sellIn < 5 -> 3
        item.sellIn < 10 -> 2
        else -> 1
    }
}
