# Search Fields — As-You-Type Standard

Every search box in the platform behaves the same way: it searches **as the user types**,
debounced, and fires automatically once **3 characters** have been entered. There is no
"type the full word and press a button" anymore. This document is the contract for building
and maintaining search fields.

## The rule

A search field MUST:

- Search automatically as the user types (no button / no Enter required).
- Wait until the trimmed term is **≥ 3 characters** before searching (configurable, default 3).
- Debounce input (default 300 ms) so a single search fires after the user pauses, not per keystroke.
- Treat an **empty** box as "show everything" (it re-fires with `''` when cleared).
- Ignore partial input of 1–2 characters (the list keeps its last committed state).

## How to build one — use `<SearchInput>`

Never build a search box from a raw `<InputText>` (with or without an `IconField`/search
icon). Use the shared component:

```vue
<script setup lang="ts">
import SearchInput from '@/components/common/SearchInput.vue'

function onSearch(term: string) {
  // term is the committed search string; '' means "cleared / show all".
  page.value = 0
  fetchList(term)
}
</script>

<template>
  <SearchInput :placeholder="t('patient.search')" @search="onSearch" />
</template>
```

Props:

| Prop | Default | Purpose |
| ---- | ------- | ------- |
| `placeholder` | `t('common.search')` | Input placeholder. |
| `minLength` | `3` | Characters required before searching. |
| `debounceMs` | `300` | Debounce delay. |
| `disabled` | `false` | Disable the input. |
| `width` | — | Optional fixed CSS width. |

- `@search="(term: string) => void"` — emitted with the committed (debounced) term; `''` on clear.
- `v-model` — optional two-way binding to the live raw text (e.g. to seed it).
- `ref` + `reset()` — exposed method to clear the box from a parent "Clear filters" button
  **without** emitting a search.

```vue
<SearchInput ref="searchInputRef" v-model="filterSearch" @search="onSearch" />
<!-- in clearFilters(): searchInputRef.value?.reset() -->
```

The behavior lives in `@/composables/useDebouncedSearch`, which `SearchInput` wraps. Use the
composable directly only if you need the logic without the default markup.

## Server vs. client search

- **Server-backed lists** (patients, users, inventory, pharmacy, warehouse stock, treasury,
  nursing kardex, …): the `@search` handler resets to page 0 and re-fetches with the term as a
  query param. Backends already match case-insensitively (patients/kardex also accent-insensitively).
- **Client-side lists** (e.g. Bed Occupancy): filtering is already instant against in-memory
  data, so a 3-character floor is intentionally **not** applied there — short room-number
  searches like "5" must work. Keep those live.

## Enforcement (so future fields can't regress)

1. **One sanctioned component** — `<SearchInput>` is the only blessed way to build a search box.
2. **ESLint guard** — `eslint.config.js` bans the hand-rolled IconField search pattern
   (`<InputIcon class="pi pi-search">` next to an `<InputText>`) via `vue/no-restricted-syntax`.
   A violation points back here. `SearchInput.vue` itself is the lone exemption.
3. **Locked behavior via tests** — `useDebouncedSearch.spec.ts` and `SearchInput.spec.ts` pin
   the 3-character threshold, debounce coalescing, clear-to-empty, trimming, dedupe, and
   `reset()`-without-emit. Changing the behavior means updating those tests deliberately.

## Migrated fields (reference)

`PatientsView`, `UsersView`, `NursingKardexView`, `PharmacyListView`, `InventoryItemsView`,
`WarehouseStockView`, `ExpenseList`, `IncomeList`, `EmployeeList` — all use `<SearchInput>`.
In the treasury screens, the search auto-fires while the date/category filters still apply via
the "Filter" button. `BedOccupancyView` stays client-side live (see above).
