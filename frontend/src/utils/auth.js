const TOKEN_KEY = 'ideal_agent_token'
const PROFILE_KEY = 'ideal_agent_profile'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAuth(token, profile) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(PROFILE_KEY)
}

export function getStoredProfile() {
  const raw = localStorage.getItem(PROFILE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch {
    clearToken()
    return null
  }
}
