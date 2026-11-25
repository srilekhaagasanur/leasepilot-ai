import { formatDate } from '../workOrders'

// Pieces shared by the landlord's work orders and the tenant's requests page

const STEPS = [
  { status: 'APPROVED', label: 'Approved' },
  { status: 'SCHEDULED', label: 'Scheduled' },
  { status: 'IN_PROGRESS', label: 'In progress' },
  { status: 'RESOLVED', label: 'Fixed' },
]

// Approved -> Scheduled -> In progress -> Fixed, with the current step highlighted
export function ProgressSteps({ status }) {
  if (status === 'CANCELLED') return null
  // A reopened job is back to "approved, needs fixing again"
  const current = status === 'REOPENED' ? 0 : STEPS.findIndex((s) => s.status === status)

  return (
    <ol className="flex items-center gap-1 text-xs font-medium">
      {STEPS.map((step, i) => {
        const done = i <= current
        return (
          <li key={step.status} className="flex flex-1 items-center gap-1">
            <span className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] ${done ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-500'}`}>
              {done ? '✓' : i + 1}
            </span>
            <span className={done ? 'text-slate-900' : 'text-slate-400'}>{step.label}</span>
            {i < STEPS.length - 1 && <span className={`mx-1 h-0.5 flex-1 rounded ${i < current ? 'bg-indigo-600' : 'bg-slate-200'}`} />}
          </li>
        )
      })}
    </ol>
  )
}

const UPDATE_TEXT = {
  APPROVED: 'Approved the repair',
  SCHEDULED: 'Scheduled the repair',
  IN_PROGRESS: 'Started the work',
  RESOLVED: 'Marked it fixed',
  REOPENED: 'Said it’s still not fixed',
  CANCELLED: 'Cancelled the job',
}

// Who did what and when, oldest first
export function Timeline({ updates }) {
  if (!updates?.length) return null
  return (
    <ol className="space-y-2 border-l-2 border-slate-200 pl-4">
      {updates.map((u, i) => {
        const confirmed = u.status === 'RESOLVED' && u.authorRole === 'TENANT'
        return (
          <li key={i} className="relative text-sm">
            <span className="absolute -left-[21px] top-1.5 h-2.5 w-2.5 rounded-full bg-slate-400" />
            <p>
              <span className="font-medium">{u.authorName}</span>{' '}
              {confirmed ? 'confirmed it’s fixed' : UPDATE_TEXT[u.status]?.toLowerCase()}
              {u.scheduledFor && <> for <strong>{formatDate(u.scheduledFor)}</strong></>}
              <span className="text-slate-400"> · {new Date(u.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}</span>
            </p>
            {u.note && !confirmed && <p className="mt-0.5 text-slate-600">“{u.note}”</p>}
          </li>
        )
      })}
    </ol>
  )
}
