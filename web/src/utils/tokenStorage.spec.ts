import { describe, it, expect, beforeEach } from 'vitest'
import { tokenStorage } from './tokenStorage'

describe('tokenStorage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('getAccessToken', () => {
    it('should return null when no token is stored', () => {
      expect(tokenStorage.getAccessToken()).toBeNull()
    })

    it('should return the stored access token', () => {
      localStorage.setItem('access_token', 'test-access-token')
      expect(tokenStorage.getAccessToken()).toBe('test-access-token')
    })
  })

  describe('setAccessToken', () => {
    it('should store the access token', () => {
      tokenStorage.setAccessToken('new-access-token')
      expect(localStorage.getItem('access_token')).toBe('new-access-token')
    })
  })

  describe('getRefreshToken', () => {
    it('should return null when no token is stored', () => {
      expect(tokenStorage.getRefreshToken()).toBeNull()
    })

    it('should return the stored refresh token', () => {
      localStorage.setItem('refresh_token', 'test-refresh-token')
      expect(tokenStorage.getRefreshToken()).toBe('test-refresh-token')
    })
  })

  describe('setRefreshToken', () => {
    it('should store the refresh token', () => {
      tokenStorage.setRefreshToken('new-refresh-token')
      expect(localStorage.getItem('refresh_token')).toBe('new-refresh-token')
    })
  })

  describe('setTokens', () => {
    it('should store both tokens', () => {
      tokenStorage.setTokens('access-123', 'refresh-456')
      expect(localStorage.getItem('access_token')).toBe('access-123')
      expect(localStorage.getItem('refresh_token')).toBe('refresh-456')
    })
  })

  describe('clearTokens', () => {
    it('should remove both tokens', () => {
      tokenStorage.setTokens('access', 'refresh')
      tokenStorage.clearTokens()

      expect(localStorage.getItem('access_token')).toBeNull()
      expect(localStorage.getItem('refresh_token')).toBeNull()
    })
  })

  describe('hasTokens', () => {
    it('should return false when no tokens are stored', () => {
      expect(tokenStorage.hasTokens()).toBe(false)
    })

    it('should return false when only access token is stored', () => {
      tokenStorage.setAccessToken('access')
      expect(tokenStorage.hasTokens()).toBe(false)
    })

    it('should return false when only refresh token is stored', () => {
      tokenStorage.setRefreshToken('refresh')
      expect(tokenStorage.hasTokens()).toBe(false)
    })

    it('should return true when both tokens are stored', () => {
      tokenStorage.setTokens('access', 'refresh')
      expect(tokenStorage.hasTokens()).toBe(true)
    })
  })
})
