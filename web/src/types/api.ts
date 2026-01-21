export interface ApiResponse<T> {
  success: boolean
  data: T | null
  message: string | null
  timestamp: string
}

export interface ErrorDetails {
  [field: string]: string[]
}

export interface ErrorResponse {
  success: false
  error: {
    code: string
    message: string
    details?: ErrorDetails
  }
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: {
    totalElements: number
    totalPages: number
    size: number
    number: number
  }
}
