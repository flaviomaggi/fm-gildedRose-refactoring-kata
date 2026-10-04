# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Exercise context
Gilded Rose Refactoring Kata (Emily Bache), Kotlin version only. Goal: refactor legacy `updateQuality()` in small, safe steps under test, then add the new "Conjured" item category. Not a rewrite from scratch — incremental improvement, tests run often. Full spec: `GildedRoseRequirements.md`.

## Commands
All Gradle commands run from `Kotlin/`.

**JDK:** Gradle 8.13 / Kotlin 2.1.20 cannot run on the machine's default JDK 27 (configure fails with `> 27`). Prefix commands with JDK 21:
`export JAVA_HOME=$(/usr/libexec/java_home -v 21)`

- Build: `./gradlew build`
- Unit tests: `./gradlew test`
- Single test: `./gradlew test --tests "com.gildedrose.GildedRoseTest.foo"`
- Text fixture (prints item state per day): `./gradlew -q texttest` (30 days) or `./gradlew -q run --args 10`
  (`Kotlin/README.md` says `./gradlew -q text` — task is actually `texttest`.)
- Approval test (from repo root, needs Python): `./start_texttest.sh` — compares fixture output to `texttests/ThirtyDays/stdout.gr` via `Kotlin/texttest_rig.py`.
  Quick manual equivalent (empty output = pass): `./gradlew -q run --args 30 | diff - ../texttests/ThirtyDays/stdout.gr`

Toolchain: Kotlin 2.1.20, JVM target 8, JUnit 5 (`kotlin("test")` + junit-jupiter).

## Architecture
- `Kotlin/src/main/kotlin/com/gildedrose/Item.kt` — data holder (`name`, `sellIn`, `quality`, `toString`). **Off limits.**
- `.../GildedRose.kt` — `GildedRose(items: List<Item>)` with `updateQuality()`: one daily tick mutating every item in place. All business logic lives here, as nested name-string `if`s.
- `.../TexttestFixture.kt` — `main`: builds fixed item list, runs `updateQuality()` N days, prints state. Its output is the golden master.
- `Kotlin/src/test/.../GildedRoseTest.kt` — starter test `foo`, intentionally failing (`"fixme"`).
- `texttests/` — TextTest approval suite (`ThirtyDays/stdout.gr` = approved output, `options.gr` = args `30`, `config.gr` = executable).

## Desired behaviour
One `updateQuality()` call = end of one day. Item type chosen by exact name.

General rules (all items unless stated):
- `sellIn` decreases by 1 each day.
- `quality` never < 0 and never > 50.

| Item (name) | sellIn | quality while sellIn ≥ 0 (after decrement) | quality once sell date passed (sellIn < 0) |
|---|---|---|---|
| Normal (any other name) | −1 | −1 | −2 |
| `Aged Brie` | −1 | +1 | +2 |
| `Backstage passes to a TAFKAL80ETC concert` | −1 | +1 if sellIn > 10; +2 if 6–10; +3 if ≤ 5 (thresholds on sellIn *before* decrement) | drops to 0 |
| `Sulfuras, Hand of Ragnaros` | never changes | always 80, never changes (exempt from 50 cap) | same |
| **Conjured (TO-BE)** e.g. `Conjured Mana Cake` | −1 | −2 | −4 |

Conjured = degrades twice as fast as normal; floor at 0. Currently NOT implemented (treated as normal item).

## Hard constraints
- Never modify `Item.kt` (class or properties) nor the `items` property of `GildedRose`. `updateQuality()`/`items` may be made static-equivalent (e.g. companion/object) if useful.
- Run `./gradlew test` (and the approval diff) after every code change; never leave tests red between steps.
- Refactor in small, behaviour-preserving steps. Refactoring must keep `stdout.gr` output identical.
- Never re-approve / edit `texttests/ThirtyDays/stdout.gr` to make a refactor pass. Only exception: when Conjured is implemented, the `Conjured Mana Cake` lines change intentionally — update those lines only, all others must stay identical.
- Git: never create a branch unless explicitly asked. Commit directly on the current branch (`main`).

## Assumptions
_None yet._
