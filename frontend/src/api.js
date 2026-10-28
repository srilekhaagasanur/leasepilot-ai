// All calls to the Spring Boot backend. Vite forwards /api to localhost:8080 (see vite.config.js).

async function request(path, options = {}) {
  const response = await fetch(path, options)
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    // Spring puts our error text (e.g. "Photo must be JPEG...") in "message"
    throw new Error(body?.message || `Request failed (${response.status})`)
  }
  return body
}

function postJson(path, data) {
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export const api = {
  demoUsers: () => request('/api/demo/users'),

  // Tenant
  myRequests: (tenantId) => request(`/api/tenant/requests?tenantId=${tenantId}`),
  submitRequest: ({ tenantId, description, photo, voiceNote }) => {
    const form = new FormData()
    form.append('tenantId', tenantId)
    if (description) form.append('description', description)
    if (photo) form.append('photo', photo)
    if (voiceNote) form.append('voiceNote', voiceNote, 'voice-note.wav')
    return request('/api/tenant/requests', { method: 'POST', body: form })
  },

  // Manager
  drafts: (managerId, status = 'PENDING') => request(`/api/manager/drafts?managerId=${managerId}&status=${status}`),
  approveDraft: (draftId, managerId, note) => postJson(`/api/manager/drafts/${draftId}/approve`, { managerId, note }),
  rejectDraft: (draftId, managerId, note) => postJson(`/api/manager/drafts/${draftId}/reject`, { managerId, note }),
  pendingTenancies: (managerId) => request(`/api/manager/tenancies?managerId=${managerId}`),
  approveTenancy: (id, managerId) => postJson(`/api/manager/tenancies/${id}/approve`, { managerId }),
  rejectTenancy: (id, managerId) => postJson(`/api/manager/tenancies/${id}/reject`, { managerId }),

  photoUrl: (requestId) => `/api/requests/${requestId}/photo`,
}
