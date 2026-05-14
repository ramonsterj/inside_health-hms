import { LotExpiryStatus } from '@/types/pharmacy'
import { fromApiDate } from '@/utils/format'

const SENTINEL_NO_EXPIRY = '9999-12-31'
const MS_PER_DAY = 86_400_000
const URGENT_WINDOW_DAYS = 30
const WARNING_WINDOW_DAYS = 90

export function lotExpiryStatusFromDate(
  expirationDate: string | null | undefined
): LotExpiryStatus {
  if (!expirationDate || expirationDate === SENTINEL_NO_EXPIRY) {
    return LotExpiryStatus.NO_EXPIRY
  }
  const exp = fromApiDate(expirationDate)
  if (!exp) return LotExpiryStatus.NO_EXPIRY
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const days = Math.round((exp.getTime() - today.getTime()) / MS_PER_DAY)
  if (days < 0) return LotExpiryStatus.EXPIRED
  if (days <= URGENT_WINDOW_DAYS) return LotExpiryStatus.RED
  if (days <= WARNING_WINDOW_DAYS) return LotExpiryStatus.YELLOW
  return LotExpiryStatus.GREEN
}
