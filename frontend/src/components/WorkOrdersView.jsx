import { useState } from 'react'
import { api } from '../api'
import { CategoryTag, StatusBadge, UrgencyBadge } from './Badges'
import { formatDate } from '../workOrders'
import { ProgressSteps, Timeline } from './WorkOrderParts'

const ACTIVE = ['REOPENED', 'APPROVED', 'SCHEDULED', 'IN_PROGRESS']
const URGENCY_ORDER = ['EMERGENCY', 'HIGH', 'MEDIUM', 'LOW']

// The landlord's approved jobs, grouped by what they need next
export default function WorkOrdersView({ workOrders, onChanged }) {
  const active = workOrders
    .filter((w) => ACTIVE.includes(w.status))
    // Reopened first (the tenant is waiting on a fix), then most urgent
    .sort((a, b) => (b.status === 'REOPENED') - (a.status === 'REOPENED')
      || URGENCY_ORDER.indexOf(a.urgency) - URGENCY_ORDER.indexOf(b.urgency))
  const awaitingTenant = workOrders.filter((w) => w.status === 'RESOLVED' && !w.tenantConfirmedAt)
  const closed = workOrders.filter((w) => w.status === 'CANCELLED' || (w.status === 'RESOLVED' && w.tenantConfirmedAt))

  if (workOrders.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500">
        No work orders yet. When you approve a draft in the Approval queue, it shows up here so you can track the repair.
      </p>
    )
  }

  return (
    <div className="space-y-8">
      <Group title="Needs action" empty="Nothing to do right now.">
        {active.map((w) => <WorkOrderCard key={w.requestId} workOrder={w} onChanged={onChanged} />)}
      </Group>
      {awaitingTenant.length > 0 && (
        <Group title="Fixed · waiting for the tenant to confirm">
          {awaitingTenant.map((w) => <WorkOrderCard key={w.requestId} workOrder={w} onChanged={onChanged} />)}
        </Group>
      )}
      {closed.length > 0 && (
        <details className="group">
          <summary className="cursor-pointer text-sm font-semibold uppercase tracking-wide text-slate-500">
            Closed ({closed.length})
          </summary>
          <div className="mt-3 space-y-4">
            {closed.map((w) => <WorkOrderCard key={w.requestId} workOrder={w} onChanged={onChanged} />)}
          </div>
        </details>
      )}
    </div>
  )
}

function Group({ title, empty, children }) {
  const items = [children].flat().filter(Boolean)
  return (
    <section>
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">{title}</h2>
      {items.length === 0 ? <p className="text-sm text-slate-400">{empty}</p> : <div className="space-y-4">{items}</div>}
    </section>
  )
}

function WorkOrderCard({ workOrder: w, onChanged }) {
  const isActive = ACTIVE.includes(w.status)
  const lastTenantComment = w.status === 'REOPENED'
    ? [...w.updates].reverse().find((u) => u.status === 'REOPENED')?.note
    : null

  return (
    <article className={`rounded-2xl border bg-white p-5 shadow-sm ${w.status === 'REOPENED' ? 'border-rose-300' : 'border-slate-200'}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold">{w.summary}</h3>
          <p className="mt-0.5 text-sm text-slate-500">
            Unit {w.unitNumber}, {w.propertyName} · {w.tenantName} · reported {new Date(w.reportedAt).toLocaleDateString()}
          </p>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <StatusBadge status={w.status} />
            <UrgencyBadge urgency={w.urgency} />
            <CategoryTag category={w.category} />
            {w.vendorName && <span className="text-xs text-slate-500">🛠 {w.vendorName}{w.vendorPhone ? ` · ${w.vendorPhone}` : ''}</span>}
          </div>
        </div>
        {w.scheduledFor && w.status === 'SCHEDULED' && (
          <p className="rounded-lg bg-violet-50 px-3 py-1.5 text-sm font-medium text-violet-800">📅 {formatDate(w.scheduledFor)}</p>
        )}
      </div>

      {lastTenantComment && (
        <p className="mt-3 rounded-lg bg-rose-50 p-3 text-sm text-rose-800">
          <span className="font-semibold">Tenant says it's still not fixed:</span> “{lastTenantComment}”
        </p>
      )}

      <div className="mt-4"><ProgressSteps status={w.status} /></div>

      {w.workOrderText && (
        <details className="mt-3 text-sm">
          <summary className="cursor-pointer font-medium text-slate-600">Work order</summary>
          <p className="mt-2 whitespace-pre-line rounded-lg bg-slate-50 p-3 text-slate-700">{w.workOrderText}</p>
        </details>
      )}

      <div className="mt-4"><Timeline updates={w.updates} /></div>

      {isActive && <ActionPanel workOrder={w} onChanged={onChanged} />}
    </article>
  )
}

const ACTIONS = {
  SCHEDULED: { button: '📅 Schedule', confirm: 'Save date', noteLabel: 'Message to tenant (optional), e.g. the time window' },
  IN_PROGRESS: { button: '🔧 Start work', confirm: 'Mark in progress', noteLabel: 'Message to tenant (optional)' },
  RESOLVED: { button: '✅ Mark fixed', confirm: 'Mark fixed', noteLabel: 'Message to tenant (optional), e.g. what was done' },
  CANCELLED: { button: 'Cancel job', confirm: 'Cancel job', noteLabel: 'Reason (sent to the tenant)' },
}

// Buttons to move the job forward; picking one opens a small form for the date and/or note
function ActionPanel({ workOrder: w, onChanged }) {
  const [action, setAction] = useState(null)
  const [note, setNote] = useState('')
  const [date, setDate] = useState(w.scheduledFor ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  // Earliest date the picker allows (today, in the browser's own time zone)
  const [today] = useState(() => new Date().toLocaleDateString('en-CA'))

  // Every action is available, except starting work that's already started
  const available = Object.keys(ACTIONS).filter((a) => !(a === 'IN_PROGRESS' && w.status === 'IN_PROGRESS'))

  function pick(next) {
    setAction(next)
    setError(null)
  }

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.updateWorkOrder(w.requestId, {
        status: action,
        note: note.trim() || null,
        scheduledFor: action === 'SCHEDULED' ? date : null,
      })
      setAction(null)
      setNote('')
      onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!action) {
    return (
      <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
        {available.map((a) => (
          <button key={a} onClick={() => pick(a)}
            className={a === 'CANCELLED'
              ? 'rounded-lg px-3 py-2 text-sm font-medium text-slate-500 hover:text-rose-700'
              : 'rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50'}>
            {a === 'SCHEDULED' && w.status === 'SCHEDULED' ? '📅 Reschedule' : ACTIONS[a].button}
          </button>
        ))}
      </div>
    )
  }

  return (
    <form onSubmit={submit} className="mt-4 space-y-3 border-t border-slate-100 pt-4">
      {action === 'SCHEDULED' && (
        <label className="block text-sm font-medium">Repair date
          <input type="date" required min={today} value={date} onChange={(e) => setDate(e.target.value)}
            className="mt-1 block rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
        </label>
      )}
      <label className="block text-sm font-medium">{ACTIONS[action].noteLabel}
        <textarea rows={2} required={action === 'CANCELLED'} value={note} onChange={(e) => setNote(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
      </label>
      {error && <p className="text-sm text-rose-600">{error}</p>}
      <div className="flex items-center gap-4">
        <button type="submit" disabled={busy}
          className={`rounded-lg px-4 py-2 text-sm font-semibold text-white disabled:opacity-50 ${action === 'CANCELLED' ? 'bg-rose-600 hover:bg-rose-700' : 'bg-indigo-600 hover:bg-indigo-700'}`}>
          {busy ? 'Saving…' : ACTIONS[action].confirm}
        </button>
        <button type="button" onClick={() => pick(null)} className="text-sm text-slate-600 hover:text-slate-900">Back</button>
      </div>
      <p className="text-xs text-slate-500">The tenant sees this update and gets an email about it.</p>
    </form>
  )
}
