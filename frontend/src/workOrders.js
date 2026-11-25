// Helpers shared by the landlord's work orders and the tenant's requests page

// Work order statuses (everything after the manager approved the AI's draft)
export const WORK_ORDER_STATUSES = ['APPROVED', 'SCHEDULED', 'IN_PROGRESS', 'RESOLVED', 'REOPENED', 'CANCELLED']

// "2026-12-01" -> "Tue, Dec 1"
export function formatDate(isoDate) {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })
}
