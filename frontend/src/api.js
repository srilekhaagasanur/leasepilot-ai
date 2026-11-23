// All calls to the Spring Boot backend. Vite forwards /api to localhost:8080 (see vite.config.js).
// The login lives in a session cookie the browser sends automatically.

// Spring Security puts a CSRF token in the XSRF-TOKEN cookie; every POST must echo it back in a header
function csrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

async function request(path, options = {}) {
  const method = options.method ?? 'GET'
  const headers = { ...options.headers }
  if (method !== 'GET' && csrfToken()) headers['X-XSRF-TOKEN'] = csrfToken()

  const response = await fetch(path, { ...options, headers })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    // Spring puts our error text (e.g. "Photo must be JPEG...") in "message"
    const error = new Error(body?.message || `Request failed (${response.status})`)
    error.status = response.status
    throw error
  }
  return body
}

function postJson(path, data) {
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data ?? {}),
  })
}

export const api = {
  // Login. me() returns null when nobody is logged in.
  me: () => request('/api/auth/me').catch((e) => { if (e.status === 401) return null; throw e }),
  login: (email, password) => postJson('/api/auth/login', { email, password }),
  // role: 'MANAGER' (property owner) or 'TENANT'
  signup: (fullName, email, password, role) => postJson('/api/auth/signup', { fullName, email, password, role }),
  demoLogin: (email) => postJson('/api/auth/demo-login', { email }),
  forgotPassword: (email) => postJson('/api/auth/forgot-password', { email }),
  resetPassword: (token, password) => postJson('/api/auth/reset-password', { token, password }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),

  // Demo mode
  demoUsers: () => request('/api/demo/users'),
  resetDemo: () => request('/api/demo/reset', { method: 'POST' }),

  // Tenant (the logged-in tenant)
  myRequests: () => request('/api/tenant/requests'),
  submitRequest: ({ description, photo, voiceNote }) => {
    const form = new FormData()
    if (description) form.append('description', description)
    if (photo) form.append('photo', photo)
    if (voiceNote) form.append('voiceNote', voiceNote, 'voice-note.wav')
    return request('/api/tenant/requests', { method: 'POST', body: form })
  },

  // Manager (the logged-in manager)
  drafts: (status = 'PENDING') => request(`/api/manager/drafts?status=${status}`),
  approveDraft: (draftId, note) => postJson(`/api/manager/drafts/${draftId}/approve`, { note }),
  rejectDraft: (draftId, note) => postJson(`/api/manager/drafts/${draftId}/reject`, { note }),

  // The owner's properties, units, and tenants. Each change returns the full updated list.
  properties: () => request('/api/manager/properties'),
  addProperty: (name, address) => postJson('/api/manager/properties', { name, address }),
  addUnit: (propertyId, unitNumber) => postJson(`/api/manager/properties/${propertyId}/units`, { unitNumber }),
  addTenant: (unitId, tenant) => postJson(`/api/manager/units/${unitId}/tenants`, tenant),
  cancelInvite: (inviteId) => request(`/api/manager/invites/${inviteId}`, { method: 'DELETE' }),

  photoUrl: (requestId) => `/api/requests/${requestId}/photo`,
}
