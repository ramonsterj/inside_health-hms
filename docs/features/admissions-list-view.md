# Feature: Admissions List View (Cards & Grouping)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-27 | @paniagua | Initial draft |
| 1.1 | 2026-04-27 | @paniagua | Revised card visual spec to avatar-centric layout (reference: `image-v1.png`) — gender icon as avatar, type color as small dot in subtitle, no left stripe / no corner chip. |
| 1.2 | 2026-04-27 | @paniagua | Type rendered as a filled colored **pill** with white text instead of a small dot. Added **Triage** as a Group-by option. Cards are sorted by triage code within each group (untriaged at the end), and the entire card is the click target — the redundant footer "View" button has been removed. |
| 1.3 | 2026-06-09 | @paniagua | **Removed the Table view entirely** (cards-only). Introduced **two-level additive grouping**: a primary "Group by" plus a "Then by" secondary level (None/Sex/Type/Triage), where the secondary excludes the primary dimension and is hidden when primary = None. Default grouping is **Type** (single level). Persistence migrated to a `v2` key with shape `{primaryGroupBy, secondaryGroupBy}` (no migration — old `v1` payloads fall back to defaults). |

---

## Overview

Renders the admissions list as **cards** with **two-level grouping controls**, used both on the Dashboard landing page (for users whose dashboard is the admissions list) and on the Admissions screen. The user groups entries by a **primary** dimension (**None**, **Sex**, **Type**, or **Triage**) and, optionally, a **secondary** "Then by" dimension (e.g. Type → Sex) — at most two levels, and the second level can never repeat the first. Their choice is remembered across sessions via `localStorage` and is **shared** between the Dashboard and the Admissions screen so the experience stays consistent. This is a frontend-only enhancement — no backend, database, or permission changes.

> The original Table view was removed in v1.3 (confirmed unused); the card view is the only list rendering. Removing the table also lifted the single-level grouping constraint imposed by PrimeVue `DataTable`, making nested grouping a clean change to our own card rendering.

---

## Use Case / User Story

1. As a **doctor**, I want to see my admitted patients as visual cards grouped by sex so that I can scan my caseload faster than reading a dense table.
2. As an **administrative staff member**, I want to group admissions by type (Hospitalization, Ambulatory, Emergency, etc.) so that I can quickly find patients in a given workflow.
3. As any **user of the admissions list**, I want my grouping choice to be remembered between sessions so that I don't have to reconfigure the screen every time.
4. As a **supervisor**, I want to add a second grouping level (e.g. group by Type, then by Sex) so I can break a large group into meaningful sub-sections at a glance.

---

## Authorization / Role Access

No new permissions. The list contents continue to honor existing `admission:read` rules (doctors see only their patients; psychologists see only ACTIVE admissions; admin/administrative staff see all). Grouping is purely a client-side preference and has no authorization gating.

---

## Functional Requirements

### Grouping Selectors (Two Levels)

The list is always rendered as cards (no view-mode toggle). Two `SelectButton` controls drive grouping:

- **Group by** (primary) — four options: **None**, **Sex**, **Type**, **Triage**.
- **Then by** (secondary) — the same dimensions **excluding the current primary value**, plus **None**. The control is **hidden when the primary is None** (there is nothing to subdivide). Selecting the same dimension at both levels is therefore impossible from the UI, and is defensively collapsed to a single level by the store's `normalize()` (see Persistence).

Grouping renders as nested collapsible `Panel`s:

- **Primary None**: a flat grid of cards, no group headers.
- **Primary set, Secondary None**: one outer `Panel` per primary group, each containing that group's card grid.
- **Primary + Secondary set**: one outer `Panel` per primary group; inside each, one inner `Panel` per secondary bucket, each containing the leaf card grid.

Each level shows its own header (dimension icon/swatch/badge + label) and its own member-count `Badge`. Group dimensions:

- **Sex**: groups are `Female`, `Male`, and (if applicable) `Other / Unknown`. Headers show the Venus/Mars icon and a count badge.
- **Type**: groups are the `AdmissionType` enum values, ordered by `ADMISSION_TYPE_ORDER` (Hospitalization last). Headers show the type label, the type color as a small swatch, and a count badge.
- **Triage**: groups are the configured triage codes used by admissions in the current page (e.g. `A`, `B`, `C`), plus an "No triage" group that collects un-triaged admissions. Headers render the triage badge (color background, code letter), the triage description, and a count badge. Groups are ordered by code (A, B, C, …) with the untriaged bucket last.
- **Empty groups are hidden at both levels.**
- Sort within the **innermost (leaf) grid** is by triage code ascending (most-urgent first), with untriaged entries last; ties preserve the original list ordering (most recent first). The same sort is applied globally when **None** is selected.

### Card Visual Spec

The card follows the reference style in `.context/attachments/image-v1.png`: a clean white card with rounded corners, subtle shadow, generous internal padding, and no colored borders or corner badges. Information is laid out as an **avatar + name/subtitle header** on top and a **vertical list of label/value rows** below.

```
┌────────────────────────────────────────────┐
│  ┌────┐  Juana Pérez                       │
│  │ ♀  │  ● Hospitalization                 │
│  └────┘                                    │
│                                            │
│  Doctor          Dr. García                │
│  Room            204                       │
│  Triage          Critical                  │
│  Admitted        2 hours ago               │
│                                            │
│                              [   View   ]  │
└────────────────────────────────────────────┘
```

**Header row**

- **Avatar** — circular, ~48px, light neutral background (e.g. `slate-100`). Inside it: the **Venus or Mars symbol** rendered in a single neutral color (the page's secondary text color — same for both genders). Shape, not color, communicates gender. If the patient's gender is `OTHER` / unknown, render a neutral circle icon instead.
- **Name** (large, semibold) on the first line.
- **Admission type pill** (small, semibold white text) on the second line: a filled rounded pill whose background is the admission type color and whose label is the localized type name (e.g. a red pill reading "Emergency", a purple pill reading "Ketamine Infusion"). The pill is the only color encoding for type. Replaces the previous "small dot + muted label" pattern.

**Body — label/value rows**

Stacked vertical rows, each with a left-aligned label (muted) and a right-aligned value (primary text). Order:

| Label | Value | Notes |
|-------|-------|-------|
| Doctor | Salutation + name (e.g. "Dr. García") | Hidden if not present |
| Room | Room number | Hidden if not assigned |
| Triage | Triage code label, prefixed by its existing colored dot/badge | Hidden if not assigned |
| Admitted | Relative time ("2 hours ago") | Always shown |
| Status | `ACTIVE` / `DISCHARGED` `Tag` | Only on Admissions screen — Dashboard already filters to ACTIVE |

Empty/missing optional rows are omitted entirely (no placeholder dashes), so card height varies slightly with available data.

**Click target**

- The entire card acts as the click target — clicking anywhere on the card navigates to the admission detail page (equivalent to the old footer "View" button, which has been removed). The card sets `role="button"`, is focusable (`tabindex="0"`), responds to <kbd>Enter</kbd> / <kbd>Space</kbd>, and has an aria-label naming the patient (e.g. "View admission for Juana Pérez"). On hover it raises slightly via shadow + transform, providing the affordance.

### Admission Type Color Palette

Use Tailwind palette tokens for consistency with the rest of the app:

| Type | Color | Hex (Tailwind 500) |
|------|-------|--------------------|
| HOSPITALIZATION | indigo | `#6366F1` |
| AMBULATORY | emerald | `#10B981` |
| ELECTROSHOCK_THERAPY | amber | `#F59E0B` |
| KETAMINE_INFUSION | purple | `#A855F7` |
| EMERGENCY | red | `#EF4444` |

The 500 shade is used for the **subtitle dot** on the card and for the **swatch in group headers** (when grouped by Type). No filled chips or stripes — keeping the card clean per the reference.

### Persistence (localStorage)

- A single Pinia store, `useAdmissionsListPreferencesStore`, holds the preference and persists to `localStorage`.
- **Storage key**: `hms.admissionsListView.v2`. Bumped from `v1` when `viewMode` was dropped and the single `groupBy` became two fields. **No migration** — an old `v1` payload simply fails the `v2` zod schema and falls back to defaults.
- **Stored shape**:
  ```ts
  {
    primaryGroupBy: 'none' | 'gender' | 'type' | 'triage',
    secondaryGroupBy: 'none' | 'gender' | 'type' | 'triage'
  }
  ```
- **Invariants** — enforced by a `normalize()` applied on read and in both setters, so the render layer never needs defensive checks:
  - if `primaryGroupBy === 'none'` → `secondaryGroupBy` is forced to `'none'`;
  - if `secondaryGroupBy === primaryGroupBy` (and not `'none'`) → `secondaryGroupBy` collapses to `'none'`.
  - `setPrimaryGroupBy` re-normalizes the secondary; `setSecondaryGroupBy` is a no-op when the value would equal the primary or when primary is `'none'`.
- **Scope**: per-browser, per-logged-in user. Key is namespaced by `authStore.user.id` to avoid collisions on shared workstations: `hms.admissionsListView.v2.user.{userId}`.
- **Defaults on first visit** (no stored value): `{ primaryGroupBy: 'type', secondaryGroupBy: 'none' }`.
- **Shared between screens**: the same store + key is read/written by both the Dashboard's admissions list and the Admissions screen, so changing the preference in one place updates both.
- **Read failure / corrupt JSON / legacy v1 payload**: fall back to defaults, do not crash.

### Backend Behavior

Unchanged. The list endpoint already returns the patient's gender via `PatientSummary` and the admission type. No new fields are required.

> If during implementation we find that `gender` is not currently included on `AdmissionListItem`, the spec is amended to add it to the response DTO — not a new feature, just exposing existing patient data.

---

## Acceptance Criteria / Scenarios

- **Default state**: When a user logs in and visits the Dashboard or Admissions screen for the first time, the list is rendered as cards grouped by **Type** (single level).
- **Group switch persists**: When a user changes Group by from Type to Sex on the Admissions screen, then logs out and logs back in, the same screen opens grouped by Sex.
- **Two-level grouping**: Selecting **Group by = Type** and **Then by = Sex** renders an outer `Panel` per type (Hospitalization last), each containing inner `Panel`s per sex, each with its own count badge. The leaf grid within each inner panel is triage-sorted.
- **Secondary excludes primary**: The "Then by" options never include the current primary dimension; choosing the same dimension twice is impossible, and any persisted payload that violates this collapses to a single level.
- **Secondary hidden for None**: When Group by = None, the "Then by" control is not shown and the list is a flat, triage-sorted grid of cards.
- **Cards grouped by type**: Each group section header shows the type label, the type's color swatch, and a count of cards inside; cards within the group all show the same colored subtitle pill matching their type.
- **Empty groups**: Groups with zero items are not rendered, at either level.
- **Permission filtering**: Cards respect existing role filtering — a doctor sees only their patients.
- **Action parity**: Clicking anywhere on a card navigates to the admission detail route.
- **Accessibility**: Card type information is conveyed by the subtitle's text label, not color alone — verified by removing color in dev tools (the pill background disappears, the label "Hospitalization" / "Emergency" / etc. remains readable).
- **Storage corruption / legacy data**: Manually setting the `localStorage` value to invalid JSON (or leaving only an old `v1` payload) results in the user seeing the defaults (cards grouped by Type), not a blank page or error.

---

## Non-Functional Requirements

- **Performance**: Card rendering for up to 100 admissions on a single screen (no virtualization needed at this scale; Dashboard typically shows fewer).
- **Responsiveness**: Cards reflow into 1, 2, or 3 columns based on viewport width (mobile / tablet / desktop), at every grouping depth.
- **i18n**: All user-facing strings (Group by, Then by, None, Sex, Type, Triage, Female, Male, etc.) live in the existing locale files (`en`, `es`).
- **No regression**: Filters (`statusFilter`, `typeFilter`), pagination, and role-based list filtering continue to work unchanged with the card list.

---

## API Contract

No new endpoints. No DTO changes (assuming `AdmissionListItem` already exposes `patient.gender` — to be verified during implementation; if missing, add `gender: Gender` to `PatientSummary` in the response).

---

## Database Changes

None.

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `AdmissionsListToolbar.vue` | `web/src/components/admissions/` | Houses the "Group by" (primary) and "Then by" (secondary) `SelectButton`s. Used by both Dashboard and Admissions screen. |
| `AdmissionCard.vue` | `web/src/components/admissions/` | Single admission card matching the visual spec above. |
| `AdmissionCardGrid.vue` | `web/src/components/admissions/` | Renders cards in a responsive grid; flat when primary = None, else one outer `Panel` per primary group containing either a leaf grid or an `AdmissionCardSubGroup`. |
| `AdmissionCardSubGroup.vue` | `web/src/components/admissions/` | Buckets a primary group's items by the secondary dimension into inner `Panel`s (fixed depth 2, no recursion). |
| `AdmissionGroupHeader.vue` | `web/src/components/admissions/` | Shared group header (icon/swatch/triage badge + label + count badge) used at both grouping levels. |
| `GenderIcon.vue` | `web/src/components/icons/` | Inline SVG component rendering ♀ or ♂ in a single neutral color. |

The single-dimension bucketing is a pure, vue-i18n-free helper `bucketAdmissions(items, dimension, labelers)` in `web/src/composables/useAdmissionsGrouping.ts` (renamed from the former `useAdmissionsTableGrouping.ts`); i18n labels are injected via `labelers` so it stays unit-testable. The table-only `AdmissionsGroupRowHeader.vue` and the `useAdmissionsTableGrouping` function were removed with the table view.

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useAdmissionsListPreferencesStore` | `web/src/stores/admissionsListPreferences.ts` | Holds `{ primaryGroupBy, secondaryGroupBy }`, hydrates from and writes to `localStorage` keyed by user id, and enforces the two-level invariants via `normalize()`. Initialization on `authStore.user` load. |

### Modified Views

- `web/src/views/DashboardView.vue` — renders `AdmissionsListSection` (cards + paginator) with `AdmissionsListToolbar` above the list. No table.
- `web/src/views/admissions/AdmissionsView.vue` — same pattern: existing filters plus the `AdmissionsListToolbar` grouping controls above the card list.

### Routes

No route changes.

### Validation (Zod Schemas)

```ts
// web/src/stores/admissionsListPreferences.ts
const preferencesSchema = z.object({
  primaryGroupBy: z.enum(['none', 'gender', 'type', 'triage']),
  secondaryGroupBy: z.enum(['none', 'gender', 'type', 'triage'])
});
```

Used to validate the `localStorage` payload on hydration; on parse failure (including any legacy `v1` shape), fall back to defaults. A `normalize()` step then enforces the two-level invariants.

---

## Implementation Notes

- **Gender icon source**: PrimeIcons does not include Venus/Mars symbols. Use an inline SVG component (`GenderIcon.vue`) with two paths/glyphs selected by prop. Avoid Unicode `♀` / `♂` glyphs — their rendering varies across operating systems and may not match the rest of the icon set in weight/size.
- **Color tokens**: Reuse Tailwind color utility classes (`bg-indigo-500`, `bg-emerald-500`, etc.) for the subtitle dot and group-header swatch so the palette stays in sync with the rest of the app's theme. If a `theme.ts` token map exists for the admission type, extend it; otherwise create one in `web/src/constants/admissionType.ts` mapping `AdmissionType` → Tailwind class name. The map should expose both the dot color and the human-readable label key for i18n.
- **Card aesthetic**: Match `image-v1.png` reference — white background, rounded corners (~`rounded-xl`), subtle shadow (~`shadow-sm`), no colored borders, ample internal padding (~`p-5`). The card's only color comes from the small subtitle dot and the optional triage badge.
- **Nested grouping depth**: Grouping is fixed at two levels by design (`AdmissionCardSubGroup` is non-recursive). The pure `bucketAdmissions` helper is reused at both levels; the leaf grid triage-sorts its members.
- **Preference store hydration**: The store should hydrate after `authStore.user.id` is available; before then, use defaults but do not write to `localStorage`. On user logout, leave the previous user's key in place — it will be re-read on their next login on the same machine.
- **Dashboard scope**: The Dashboard already filters to ACTIVE admissions only. Group-by Type on the Dashboard will likely show a single `EMERGENCY` group rarely populated; that is acceptable — empty groups are hidden.
- **Existing patient-admission spec**: A one-line cross-reference will be added to `docs/features/patient-admission.md` under the section that mentions admission viewing, pointing here.

---

## QA Checklist

### Backend
- [ ] N/A — no backend changes (verify `AdmissionListItem` already includes `patient.gender`; if not, add to response and confirm tests still pass)

### Frontend
- [x] `AdmissionCard.vue`, `AdmissionCardGrid.vue`, `AdmissionCardSubGroup.vue`, `AdmissionGroupHeader.vue`, `AdmissionsListToolbar.vue`, `GenderIcon.vue` present
- [x] `useAdmissionsListPreferencesStore` implemented with Zod validation + `normalize()` on hydrate
- [x] Dashboard and Admissions screen both use the toolbar and render cards from the store
- [x] Default preferences (cards grouped by Type, single level) applied on first visit
- [x] Storage corruption / legacy `v1` handling: invalid or old JSON falls back to defaults
- [x] Cards + two-level grouping renders nested collapsible panels with per-level counts
- [x] Secondary "Then by" excludes the primary dimension and is hidden when primary = None
- [x] i18n keys present in `en` and `es` (`thenBy` added; `view`/`viewModes` removed)
- [x] ESLint/oxlint passes
- [x] Vitest unit tests for the preferences store (hydrate, persist, invariants, invalid/legacy payload)
- [x] Vitest unit tests for `bucketAdmissions` (gender/type/triage ordering, empty-bucket dropping, leaf triage sort)

### E2E Tests (Playwright)
- [x] First-visit default: cards grouped by Type
- [x] Two-level grouping (Type → Sex) nests gender panels inside the type panel
- [x] Group by Triage renders code order with untriaged last
- [x] Card click navigates to admission detail route

### General
- [x] Filters, pagination, and role-based filtering still work with the card list
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)** — under "Implemented Features → Frontend", add: "Admissions list cards view with grouping by gender or type, persisted per user via localStorage"
- [ ] **[patient-admission.md](./patient-admission.md)** — add one-line cross-reference under the admission viewing section: "Alternate list views (cards, grouping) are specified in [admissions-list-view.md](./admissions-list-view.md)."

### Review for Consistency

- [ ] **[nursing-kardex.md](./nursing-kardex.md)** — visually similar card-based dashboard; ensure the new `AdmissionCard.vue` doesn't fork visual conventions unnecessarily. If shared primitives emerge, factor them out.

### Code Documentation

- [ ] `AdmissionCard.vue`, `AdmissionCardGrid.vue` — short JSDoc on the public props
- [ ] `admissionsListPreferences.ts` — note the `localStorage` key naming scheme on the store

---

## Related Docs/Commits/Issues

- Related feature: [`patient-admission.md`](./patient-admission.md)
- Related feature: [`nursing-kardex.md`](./nursing-kardex.md) (similar card-style aggregated view)
- Design discussion: Claude session 2026-04-27 (branch `feature/admissions-cards`)
