-- V108: Pharmacy & Inventory Evolution — Backfill review surface.
--
-- AC-19: "The `pharmacy_backfill_review` view returns zero rows for items
-- whose `MedicationDetails.reviewStatus` has been transitioned to CONFIRMED."
--
-- Defined as a view (not a table) so it stays in sync automatically when the
-- review workflow transitions a row to CONFIRMED via the normal edit path.
-- A view also avoids a second copy of the parser output and avoids a write
-- whenever the backfill is rerun.

CREATE VIEW pharmacy_backfill_review AS
SELECT md.id              AS medication_details_id,
       md.item_id         AS item_id,
       i.sku              AS sku,
       i.name             AS raw_name,
       md.generic_name    AS parsed_generic_name,
       md.commercial_name AS parsed_brand,
       md.strength        AS parsed_strength,
       md.section         AS parsed_section,
       md.dosage_form     AS parsed_dosage_form,
       md.review_notes    AS review_notes,
       md.created_at      AS created_at
FROM medication_details md
JOIN inventory_items i ON i.id = md.item_id
WHERE md.review_status = 'NEEDS_REVIEW'
  AND md.deleted_at IS NULL
  AND i.deleted_at IS NULL;
