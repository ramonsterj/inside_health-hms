export interface Permission {
  id: number
  code: string
  name: string
  description: string | null
  resource: string
  action: string
}

export interface Role {
  id: number
  code: string
  name: string
  description: string | null
  isSystem: boolean
  permissions: Permission[]
}

export interface CreateRoleRequest {
  code: string
  name: string
  description?: string
  permissionCodes?: string[]
}

export interface UpdateRoleRequest {
  name?: string
  description?: string
}

export interface AssignPermissionsRequest {
  permissionCodes: string[]
}
