export enum AuditAction {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE'
}

export interface AuditLog {
  id: number
  userId: number | null
  username: string | null
  action: AuditAction
  entityType: string
  entityId: number
  oldValues: string | null
  newValues: string | null
  ipAddress: string | null
  timestamp: string
}

export interface AuditLogFilters {
  userId?: number
  entityType?: string
  action?: AuditAction
}
