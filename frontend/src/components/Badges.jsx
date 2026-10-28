const URGENCY_STYLES = {
  EMERGENCY: 'bg-red-600 text-white',
  HIGH: 'bg-orange-100 text-orange-800 ring-1 ring-orange-300',
  MEDIUM: 'bg-amber-50 text-amber-800 ring-1 ring-amber-200',
  LOW: 'bg-slate-100 text-slate-700 ring-1 ring-slate-200',
}

const STATUS_LABELS = {
  SUBMITTED: { text: 'Received', style: 'bg-slate-100 text-slate-700' },
  AWAITING_APPROVAL: { text: 'Under review', style: 'bg-indigo-50 text-indigo-700' },
  APPROVED: { text: 'Scheduled', style: 'bg-emerald-50 text-emerald-700' },
  REJECTED: { text: 'Needs follow-up', style: 'bg-rose-50 text-rose-700' },
  RESOLVED: { text: 'Resolved', style: 'bg-emerald-100 text-emerald-800' },
}

const RESPONSIBILITY_STYLES = {
  LANDLORD: 'bg-sky-50 text-sky-800 ring-1 ring-sky-200',
  TENANT: 'bg-purple-50 text-purple-800 ring-1 ring-purple-200',
  UNCLEAR: 'bg-yellow-50 text-yellow-800 ring-1 ring-yellow-300',
}

const base = 'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold'

export function UrgencyBadge({ urgency }) {
  if (!urgency) return null
  return <span className={`${base} ${URGENCY_STYLES[urgency]}`}>{urgency}</span>
}

export function StatusBadge({ status }) {
  const label = STATUS_LABELS[status] ?? { text: status, style: 'bg-slate-100 text-slate-700' }
  return <span className={`${base} ${label.style}`}>{label.text}</span>
}

export function ResponsibilityBadge({ responsibility }) {
  if (!responsibility) return null
  const text = responsibility === 'UNCLEAR' ? 'Responsibility unclear' : `${responsibility.toLowerCase()} pays`
  return <span className={`${base} capitalize ${RESPONSIBILITY_STYLES[responsibility]}`}>{text}</span>
}

export function CategoryTag({ category }) {
  if (!category) return null
  return (
    <span className={`${base} bg-white text-slate-600 ring-1 ring-slate-200`}>
      {category.replace('_', ' ').toLowerCase()}
    </span>
  )
}
