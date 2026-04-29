-- V094: Split rejection audit out of authorized_at/by.
--
-- Until v1.2, MedicalOrderService.reject() stamped authorized_at / authorized_by even
-- though the order was being rejected, so a rejected order looked authorized in the DB
-- and reports filtering "orders authorized by X" double-counted rejections by X.
-- Add dedicated rejected_at / rejected_by columns and migrate existing NO_AUTORIZADO
-- rows over.

ALTER TABLE medical_orders
    ADD COLUMN rejected_at TIMESTAMP,
    ADD COLUMN rejected_by BIGINT;

ALTER TABLE medical_orders
    ADD CONSTRAINT fk_medical_order_rejected_by FOREIGN KEY (rejected_by) REFERENCES users(id);

-- Backfill: for orders already in NO_AUTORIZADO, copy the (overloaded) authorize fields
-- into the new rejection fields, then null out the authorize fields so the column
-- semantics match their names from now on.
UPDATE medical_orders
   SET rejected_at    = authorized_at,
       rejected_by    = authorized_by,
       authorized_at  = NULL,
       authorized_by  = NULL
 WHERE status = 'NO_AUTORIZADO';
