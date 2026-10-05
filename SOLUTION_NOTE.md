# Solution note: why `updateQuality()` stays a single `when`

## Where we are

After refactoring the legacy code and adding Conjured items, all business logic in
`Kotlin/src/main/kotlin/com/gildedrose/GildedRose.kt` fits in ~37 lines:

```kotlin
val change = when {
    // first: Conjured wins over other categories, e.g. "Conjured Aged Brie"
    item.name.startsWith(CONJURED_PREFIX, ignoreCase = true) -> if (expired) -4 else -2
    item.name.startsWith(AGED_PREFIX, ignoreCase = true) -> if (expired) 2 else 1
    item.name.contains(BACKSTAGE_PASS, ignoreCase = true) -> backstagePassesChange(item)
    else -> if (expired) -2 else -1
}
item.quality = (item.quality + change).coerceIn(MIN_QUALITY, MAX_QUALITY)
```

Sulfuras is skipped up front with a guard. Adding Conjured cost one `when` branch and one constant.
The behaviour is covered by 42 unit tests (one per rule) plus a golden-master test
against `texttests/ThirtyDays/stdout.gr`.

## The question

Should the code be made more "extensible" or "future-proof", even though no requirement asks for it?

## Options considered

| | Current `when` | Enum, abstract members | Enum, compact lambdas | Enum, named lambdas | Strategy + Factory |
|---|---|---|---|---|---|
| Lines (whole file) | ~37 | ~60 | ~40 | ~55 | several files/types |
| Category matching testable in isolation | no | yes | yes | yes | yes |
| Each category fully defined in one place | yes (one branch) | yes | no (backstage pulled out) | yes | yes (one class each) |
| Readability per category | high | medium (`override` boilerplate) | low (two `it`s, long lines) | high | low (behaviour spread across files) |
| Sulfuras fits naturally | yes (guard only) | no (dummy `dailyChange`) | no | no | no |
| Room for a 2nd/3rd per-category rule | weak | strong (compiler forces every entry) | weak | good | strong |

**1. Keep the current `when`.**
- Simplest option: the whole rule set is on one screen, with no indirection.
- Cons:
  - Matching a name to a category and computing its daily change are fused.
  - Precedence lives in the order of the branches.
  - A category that varies in more than one dimension would bring scattered `if`s back.

**2. Enum with abstract members.** Each category overrides `matches(name)` and `dailyChange(item)`, and `Category.of(name)` returns the first match.
- Pros:
  - Category matching becomes a unit you can test on its own.
  - A new per-category rule is one abstract member, and the compiler forces every category to handle it.
- Cost:
  - About 60% more code.
  - `override` boilerplate.
  - Sulfuras needs a `dailyChange` that is never called.

**3. Enum with compact constructor lambdas.**
```kotlin
CONJURED({ it.startsWith("Conjured ", ignoreCase = true) }, { if (it.expired) -4 else -2 }),
```
- Pro: it reads like a table and is barely larger than the `when`.
- Cons:
  - Backstage passes don't fit on one line, so their rule moves out of the enum.
  - Two different `it`s share each line (the first is a `String`, the second an `Item`).
  - It doesn't scale past two rules per category.

**4. Enum with named lambdas.** This is the middle ground.
```kotlin
CONJURED(
    matches = { name -> name.startsWith("Conjured ", ignoreCase = true) },
    dailyChange = { item -> if (item.expired) -4 else -2 },
),
```
- Pros:
  - Every entry has the same readable shape, and every category is fully defined in one place.
  - It stays readable up to 3–4 rules per category.
- Cost: about 50% more code than today, and the same Sulfuras mismatch as option 2.

**5. Strategy + Factory.** The textbook solution to this kata.
- `Item` is off limits, so strategies would wrap items or be looked up by a factory: several types, for rules that each fit in one expression.
- Its real benefit is adding categories without touching existing code, which matters when several teams or plugins add them. Nothing here points to that.

## Decision

**Keep the code as it is.**

If a requirement asked for it, the choice would be **option 2 (abstract members)** or **option 4 (named lambdas)**. Today, adopting either would rest only on guesses about future change, and no requirement justifies it.

## Why

- **The code is already simple and easy to extend.** The kata's goal is to make adding features easy, and Conjured showed it: one branch, one constant. An enum would make that same change *slightly* different, not easier.
- **Changing it later is cheap.** Building ahead pays off when later change is expensive. Here, 42 rule-level tests and a golden master make moving to an enum a safe, mechanical refactor whenever it's needed. The tests are the future-proofing.
- **Speculative structure has a cost too.** More code to read, a do-nothing Sulfuras entry, and precedence moved away from the loop are paid today for a benefit that may never come. If the future requirement doesn't match the shape we guessed, the abstraction has to be bent or removed.

## When to revisit

Move to option 2 or option 4 when one of these shows up:

- A category needs to vary in **more than the daily quality change**: its own `sellIn` rule, a different quality cap, or another exemption like Sulfuras.
- Category matching needs to be **used or tested on its own**: shown to users, reported, or with priority rules that grow beyond "Conjured first".
- The number of categories grows enough that the `when` stops fitting on one screen.

Strategy + Factory becomes worth discussing only if categories have to be added **without changing this module**: by plugins, by configuration, or by other teams.
