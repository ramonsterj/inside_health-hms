# Feature: Admissions List View (Cards & Grouping)

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-04-27 | @paniagua | Initial draft |
| 1.1 | 2026-04-27 | @paniagua | Revised card visual spec to avatar-centric layout (reference: `image-v1.png`) — gender icon as avatar, type color as small dot in subtitle, no left stripe / no corner chip. |
| 1.2 | 2026-04-27 | @paniagua | Type rendered as a filled colored **pill** with white text instead of a small dot. Added **Triage** as a Group-by option. Cards are sorted by triage code within each group (untriaged at the end), and the entire card is the click target — the redundant footer "View" button has been removed. |

---

## Overview

Adds an alternate **card view** and **grouping controls** to the admissions list, used both on the Dashboard landing page (for users whose dashboard is the admissions list) and on the Admissions screen. The user can toggle between **Cards** and **Table** views and group entries by **None**, **Gender**, or **Type of Admission**. Their choice is remembered across sessions via `localStorage` and is **shared** between the Dashboard and the Admissions screen so the experience stays consistent. This is a frontend-only enhancement — no backend, database, or permission changes.

---

## Use Case / User Story

1. As a **doctor**, I want to see my admitted patients as visual cards grouped by gender so that I can scan my caseload faster than reading a dense table.
2. As an **administrative staff member**, I want to group admissions by type (Hospitalization, Ambulatory, Emergency, etc.) so that I can quickly find patients in a given workflow.
3. As any **user of the admissions list**, I want my view-mode and grouping choice to be remembered between sessions so that I don't have to reconfigure the screen every time.
4. As a **user who prefers density**, I want to switch back to the existing table view at any time so that I can see more rows at once.

---

## Authorization / Role Access

No new permissions. The list contents continue to honor existing `admission:read` rules (doctors see only their patients; psychologists see only ACTIVE admissions; admin/administrative staff see all). View-mode and grouping are purely client-side preferences and have no authorization gating.

---

## Functional Requirements

### View Mode Toggle

- A `SelectButton` (or equivalent) labeled **View** with two options: **Cards** and **Table**.
- Visible on both the Dashboard (when the user's dashboard is the admissions list) and the Admissions screen.
- The Table option renders the existing `DataTable` unchanged, with the same columns, filters, and pagination behavior currently in `DashboardView.vue` and `AdmissionsView.vue`.
- The Cards option renders the new card layout described below.

### Group-By Selector

- A `SelectButton` labeled **Group by** with four options: **None**, **Gender**, **Type**, **Triage**.
- Applies to **both** the Cards view and the Table view.
  - In Cards view: groups appear as collapsible sections (`Panel` or `Fieldset`) each containing the cards belonging to that group.
  - In Table view: groups appear as section headers between row groups (PrimeVue `DataTable` row grouping with `groupRowsBy` and `rowGroupMode="subheader"`).
- **None**: flat list, no grouping.
- **Gender**: groups are `Female`, `Male`, and (if applicable) `Other / Unknown`. Headers show the Venus/Mars icon and a count badge (e.g. "♀ Female · 8").
- **Type**: groups are the five `AdmissionType` enum values (HOSPITALIZATION, AMBULATORY, ELECTROSHOCK_THERAPY, KETAMINE_INFUSION, EMERGENCY). Headers show the type label, the type color as a small swatch, and a count badge.
- **Triage**: groups are the configured triage codes used by admissions in the current page (e.g. `A`, `B`, `C`), plus an "No triage" group that collects ambulatory / un-triaged admissions. Headers render the triage badge (color background, code letter), the triage description, and a count badge. Groups are ordered by code (A, B, C, …) with the untriaged bucket last.
- Default sort within a group is by triage code ascending (most-urgent first), with untriaged entries last; ties preserve the original list ordering (most recent first). The same sort is applied globally when **None** is selected.

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

- A single Pinia store, e.g. `useAdmissionsListPreferencesStore`, holds the preference and persists to `localStorage`.
- **Storage key**: `hms.admissionsListView.v1`. Versioned suffix lets us evolve the schema later without surprising parses.
- **Stored shape**:
  ```ts
  {
    viewMode: 'cards' | 'table',
    groupBy: 'none' | 'gender' | 'type'
  }
  ```
- **Scope**: per-browser, per-logged-in user. Key is namespaced by `authStore.user.id` to avoid collisions on shared workstations: `hms.admissionsListView.v1.user.{userId}`.
- **Defaults on first visit** (no stored value): `viewMode: 'cards'`, `groupBy: 'gender'`.
- **Shared between screens**: the same store + key is read/written by both the Dashboard's admissions list and the Admissions screen, so changing the preference in one place updates both.
- **Read failure / corrupt JSON**: fall back to defaults, do not crash.

### Backend Behavior

Unchanged. The list endpoint already returns the patient's gender via `PatientSummary` and the admission type. No new fields are required.

> If during implementation we find that `gender` is not currently included on `AdmissionListItem`, the spec is amended to add it to the response DTO — not a new feature, just exposing existing patient data.

---

## Acceptance Criteria / Scenarios

- **Default state**: When a user logs in and visits the Dashboard or Admissions screen for the first time, the list is rendered as cards grouped by gender.
- **View switch persists**: When a user switches from Cards to Table on the Dashboard, then navigates to the Admissions screen, the Admissions screen also opens in Table view. Same in reverse.
- **Group switch persists**: When a user changes Group by from Gender to Type on the Admissions screen, then logs out and logs back in, the same screen opens grouped by Type.
- **Cards in cards view, grouped by type**: Each group section header shows the type label, the type's color swatch, and a count of cards inside; cards within the group all show the same colored subtitle dot matching their type.
- **Cards in cards view, grouped by gender**: Section headers show ♀/♂ icon and label; cards inside continue to show their type-colored subtitle dot (gender does not change card colors).
- **Group by None**: A flat list of cards (or rows) is shown without group headers.
- **Table view continues to work**: The existing `DataTable` columns, filters (`statusFilter`, `typeFilter` on the Admissions screen), pagination, and lazy loading remain functional in Table view, including when grouping is enabled.
- **Empty groups**: Groups with zero items are not rendered.
- **Permission filtering**: Cards/table both respect existing role filtering — a doctor sees only their patients regardless of view mode.
- **Action parity**: Clicking the `View` button on a card navigates to the same admission detail route as clicking `View` on the table row.
- **Accessibility**: Card type information is conveyed by the subtitle's text label, not color alone — verified by removing color in dev tools (the dot disappears, the label "Hospitalization" / "Emergency" / etc. remains readable).
- **Storage corruption**: Manually setting `localStorage` value to invalid JSON results in the user seeing the defaults (cards / gender), not a blank page or error.

---

## Non-Functional Requirements

- **Performance**: Card rendering for up to 100 admissions on a single screen (no virtualization needed at this scale; Dashboard typically shows fewer).
- **Responsiveness**: Cards reflow into 1, 2, or 3 columns based on viewport width (mobile / tablet / desktop). Table view keeps its current responsive behavior.
- **i18n**: All new user-facing strings (View, Cards, Table, Group by, None, Gender, Type, Female, Male, etc.) added to existing locale files (`en`, `es`).
- **No regression**: The existing table experience must remain unchanged in all aspects (column set, filters, pagination, lazy loading, role filtering) when the user keeps Table view.

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
| `AdmissionsListToolbar.vue` | `web/src/components/admissions/` | Houses the View and Group by `SelectButton`s. Used by both Dashboard and Admissions screen. |
| `AdmissionCard.vue` | `web/src/components/admissions/` | Single admission card matching the visual spec above. |
| `AdmissionCardGrid.vue` | `web/src/components/admissions/` | Renders cards in a responsive grid, with optional grouped collapsible sections. |
| `GenderIcon.vue` | `web/src/components/icons/` | Inline SVG component rendering ♀ or ♂ in a single neutral color. |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useAdmissionsListPreferencesStore` | `web/src/stores/admissionsListPreferences.ts` | Holds `{ viewMode, groupBy }`, hydrates from and writes to `localStorage` keyed by user id. Initialization on `authStore.user` load. |

### Modified Views

- `web/src/views/DashboardView.vue` — keep existing `DataTable`, wrap it in a conditional that switches to `AdmissionCardGrid` based on the preference store. Add `AdmissionsListToolbar` above the list.
- `web/src/views/admissions/AdmissionsView.vue` — same pattern: existing toolbar already has filters; add the new `AdmissionsListToolbar` controls (View / Group by) alongside them. Switch list rendering between table and cards.

### Routes

No route changes.

### Validation (Zod Schemas)

```ts
// web/src/stores/admissionsListPreferences.ts
const preferencesSchema = z.object({
  viewMode: z.enum(['cards', 'table']),
  groupBy: z.enum(['none', 'gender', 'type'])
});
```

Used to validate the `localStorage` payload on hydration; on parse failure, fall back to defaults.

---

## Implementation Notes

- **Gender icon source**: PrimeIcons does not include Venus/Mars symbols. Use an inline SVG component (`GenderIcon.vue`) with two paths/glyphs selected by prop. Avoid Unicode `♀` / `♂` glyphs — their rendering varies across operating systems and may not match the rest of the icon set in weight/size.
- **Color tokens**: Reuse Tailwind color utility classes (`bg-indigo-500`, `bg-emerald-500`, etc.) for the subtitle dot and group-header swatch so the palette stays in sync with the rest of the app's theme. If a `theme.ts` token map exists for the admission type, extend it; otherwise create one in `web/src/constants/admissionType.ts` mapping `AdmissionType` → Tailwind class name. The map should expose both the dot color and the human-readable label key for i18n.
- **Card aesthetic**: Match `image-v1.png` reference — white background, rounded corners (~`rounded-xl`), subtle shadow (~`shadow-sm`), no colored borders, ample internal padding (~`p-5`). The card's only color comes from the small subtitle dot and the optional triage badge.
- **Table grouping**: PrimeVue `DataTable` supports row grouping via `groupRowsBy="patient.gender"` (or a computed key) with `rowGroupMode="subheader"`. The subheader template renders the same group header used by the cards grid for visual consistency.
- **Preference store hydration**: The store should hydrate after `authStore.user.id` is available; before then, use defaults but do not write to `localStorage`. On user logout, leave the previous user's key in place — it will be re-read on their next login on the same machine.
- **Dashboard scope**: The Dashboard already filters to ACTIVE admissions only. Group-by Type on the Dashboard will likely show a single `EMERGENCY` group rarely populated; that is acceptable — empty groups are hidden.
- **Existing patient-admission spec**: A one-line cross-reference will be added to `docs/features/patient-admission.md` under the section that mentions admission viewing, pointing here.

---

## QA Checklist

### Backend
- [ ] N/A — no backend changes (verify `AdmissionListItem` already includes `patient.gender`; if not, add to response and confirm tests still pass)

### Frontend
- [ ] `AdmissionCard.vue`, `AdmissionCardGrid.vue`, `AdmissionsListToolbar.vue`, `GenderIcon.vue` created
- [ ] `useAdmissionsListPreferencesStore` implemented with Zod validation on hydrate
- [ ] Dashboard and Admissions screen both use the toolbar and switch list mode based on the store
- [ ] Default preferences (cards / gender) applied on first visit
- [ ] Preference persists across page reloads (verified by inspecting `localStorage`)
- [ ] Preference shared: setting on Dashboard takes effect on Admissions screen and vice versa
- [ ] Storage corruption handling: invalid JSON does not break the page
- [ ] Table view + grouping renders subheaders correctly
- [ ] Cards view + grouping renders collapsible group panels with counts
- [ ] i18n keys added to `en` and `es`
- [ ] ESLint/oxlint passes
- [ ] Vitest unit tests for the preferences store (hydrate, persist, invalid payload, default fallback)
- [ ] Vitest unit tests for `AdmissionCard.vue` rendering (gender avatar icon, subtitle type label + colored dot, conditional rows)

### E2E Tests (Playwright)
- [ ] First-visit default: cards grouped by gender
- [ ] Toggle to Table view on Dashboard, navigate to Admissions screen, verify Table view active
- [ ] Change Group by to Type, log out and back in, verify preference restored
- [ ] Card `View` button navigates to admission detail route

### General
- [ ] Existing table behavior unchanged when Table view selected (filters, pagination, lazy loading)
- [ ] Role-based filtering still applied (verified for DOCTOR and ADMIN)
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
