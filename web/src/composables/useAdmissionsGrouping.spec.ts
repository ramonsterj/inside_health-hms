import { describe, it, expect } from 'vitest'
import { bucketAdmissions, type BucketLabelers } from './useAdmissionsGrouping'
import { AdmissionType, type AdmissionListItem } from '@/types/admission'
import { Sex } from '@/types/patient'

// Identity-ish labelers so assertions read off the dimension keys/codes.
const labelers: BucketLabelers = {
  genderLabel: sex => (sex === Sex.FEMALE ? 'F' : sex === Sex.MALE ? 'M' : 'O'),
  typeLabel: type => `type:${type}`,
  triageLabel: tc => (tc ? `triage:${tc.code}` : 'untriaged')
}

function makeItem(opts: {
  id: number
  sex?: Sex | null
  type?: AdmissionType
  triage?: { id: number; code: string } | null
}): AdmissionListItem {
  const triage = opts.triage
  // `sex: null` exercises the "other" bucket (the runtime API may return a sex
  // outside the MALE/FEMALE enum).
  return {
    id: opts.id,
    patient: { sex: opts.sex ?? null } as unknown as AdmissionListItem['patient'],
    triageCode: triage
      ? ({
          id: triage.id,
          code: triage.code,
          color: '#000',
          description: null
        } as AdmissionListItem['triageCode'])
      : null,
    room: null,
    type: opts.type ?? AdmissionType.HOSPITALIZATION
  } as AdmissionListItem
}

describe('bucketAdmissions', () => {
  describe('gender', () => {
    it('orders female, male, other and drops empty buckets', () => {
      const groups = bucketAdmissions(
        [
          makeItem({ id: 1, sex: Sex.MALE }),
          makeItem({ id: 2, sex: Sex.FEMALE }),
          makeItem({ id: 3, sex: null })
        ],
        'gender',
        labelers
      )
      expect(groups.map(g => g.key)).toEqual(['female', 'male', 'other'])
      expect(groups.map(g => g.label)).toEqual(['F', 'M', 'O'])
    })

    it('omits genders with no members', () => {
      const groups = bucketAdmissions(
        [makeItem({ id: 1, sex: Sex.FEMALE }), makeItem({ id: 2, sex: Sex.FEMALE })],
        'gender',
        labelers
      )
      expect(groups.map(g => g.key)).toEqual(['female'])
      expect(groups[0]!.items).toHaveLength(2)
    })
  })

  describe('type', () => {
    it('orders by ADMISSION_TYPE_ORDER (Hospitalization last) and drops empty buckets', () => {
      const groups = bucketAdmissions(
        [
          makeItem({ id: 1, type: AdmissionType.HOSPITALIZATION }),
          makeItem({ id: 2, type: AdmissionType.EMERGENCY }),
          makeItem({ id: 3, type: AdmissionType.AMBULATORY })
        ],
        'type',
        labelers
      )
      expect(groups.map(g => g.type)).toEqual([
        AdmissionType.AMBULATORY,
        AdmissionType.EMERGENCY,
        AdmissionType.HOSPITALIZATION
      ])
    })
  })

  describe('triage', () => {
    it('orders by code with untriaged last', () => {
      const groups = bucketAdmissions(
        [
          makeItem({ id: 1, triage: { id: 2, code: 'B' } }),
          makeItem({ id: 2, triage: null }),
          makeItem({ id: 3, triage: { id: 1, code: 'A' } })
        ],
        'triage',
        labelers
      )
      expect(groups.map(g => g.label)).toEqual(['triage:A', 'triage:B', 'untriaged'])
      expect(groups[groups.length - 1]!.key).toBe('triage-none')
    })
  })

  describe('leaf triage sort', () => {
    it('sorts members within a bucket by triage code (untriaged last)', () => {
      // All female so they land in one bucket; triage order must be A, B, none.
      const groups = bucketAdmissions(
        [
          makeItem({ id: 1, sex: Sex.FEMALE, triage: { id: 2, code: 'B' } }),
          makeItem({ id: 2, sex: Sex.FEMALE, triage: null }),
          makeItem({ id: 3, sex: Sex.FEMALE, triage: { id: 1, code: 'A' } })
        ],
        'gender',
        labelers
      )
      expect(groups[0]!.items.map(i => i.id)).toEqual([3, 1, 2])
    })
  })
})
