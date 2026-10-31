import { useState } from 'react'
import { api } from '../api'
import { CategoryTag, ResponsibilityBadge, UrgencyBadge } from './Badges'
import DamagePhoto from './DamagePhoto'

const TOOL_LABELS = {
  get_lease_clauses: 'Read the lease',
  get_unit_history: 'Checked unit history',
  find_vendors: 'Looked up vendors',
  submit_draft: 'Submitted draft',
}

// One item in the approval queue: the tenant's report, the agent's proposal, and approve/reject
export default function DraftCard({ draft: d, managerId, onDecided }) {
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const pending = d.approvalStatus === 'PENDING'

  async function decide(approve) {
    setBusy(true)
    setError(null)
    try {
      await (approve ? api.approveDraft : api.rejectDraft)(d.draftId, managerId, note.trim() || null)
      onDecided()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      {/* Header */}
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-6 py-4">
        <div>
          <h3 className="text-lg font-semibold">{d.aiSummary}</h3>
          <p className="text-sm text-slate-500">
            Unit {d.unitNumber} · {d.tenantName} · {new Date(d.reportedAt).toLocaleString()}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <UrgencyBadge urgency={d.urgency} />
          <CategoryTag category={d.category} />
          <ResponsibilityBadge responsibility={d.responsibility} />
        </div>
      </header>

      <div className="grid gap-6 p-6 lg:grid-cols-[2fr_3fr]">
        {/* Left: what the tenant sent + how the agent worked */}
        <div className="space-y-5">
          <Section title="Tenant's report">
            {d.description && <p>"{d.description}"</p>}
            {d.transcript && (
              <p className="mt-2 text-slate-600"><span className="font-medium">🎙️ Voice note:</span> "{d.transcript}"</p>
            )}
            {d.hasPhoto && (
              <DamagePhoto src={api.photoUrl(d.requestId)} segmentation={d.damageSegmentation} phrase={d.damagePhrase} />
            )}
            {d.photoObservations && <p className="mt-2 text-sm text-slate-600"><span className="font-medium">AI sees:</span> {d.photoObservations}</p>}
            {d.urgencyReason && <p className="mt-2 text-sm text-slate-600"><span className="font-medium">Why {d.urgency?.toLowerCase()}:</span> {d.urgencyReason}</p>}
          </Section>

          <Section title="Agent work log">
            <ol className="space-y-2">
              {d.toolTrace.map((step, i) => (
                <li key={i} className="flex gap-3 text-sm">
                  <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-700">{i + 1}</span>
                  <span>
                    <span className="font-medium">{TOOL_LABELS[step.tool] ?? step.tool}</span>
                    <span className="text-slate-500"> — {step.resultSummary}</span>
                  </span>
                </li>
              ))}
            </ol>
          </Section>

          <Section title="Agent's reasoning">
            <p className="text-sm">{d.reasoning}</p>
          </Section>
        </div>

        {/* Right: what the agent proposes to send */}
        <div className="space-y-5">
          {d.citedClauseNumber && (
            <div className="rounded-xl border-l-4 border-sky-400 bg-sky-50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-sky-800">Lease clause {d.citedClauseNumber} · {d.citedClauseTitle}</p>
              <p className="mt-1 text-sm text-sky-950">{d.citedClauseText}</p>
            </div>
          )}

          <Section title={d.vendorName ? `Work order → ${d.vendorName} (${d.vendorPhone})` : 'Work order (no vendor)'}>
            <p className="whitespace-pre-line rounded-lg bg-slate-50 p-4 text-sm">{d.workOrderText}</p>
          </Section>

          <Section title={`Reply to ${d.tenantName}`}>
            <p className="whitespace-pre-line rounded-lg bg-slate-50 p-4 text-sm">{d.tenantReplyText}</p>
          </Section>

          {pending ? (
            <div className="space-y-3 border-t border-slate-100 pt-4">
              <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional note (e.g. why you're rejecting)"
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
              <div className="flex gap-3">
                <button onClick={() => decide(true)} disabled={busy}
                  className="flex-1 rounded-lg bg-emerald-600 px-4 py-2.5 font-semibold text-white hover:bg-emerald-700 disabled:opacity-50">
                  Approve & send
                </button>
                <button onClick={() => decide(false)} disabled={busy}
                  className="rounded-lg border border-slate-300 px-4 py-2.5 font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50">
                  Reject
                </button>
              </div>
              {error && <p className="text-sm text-rose-600">{error}</p>}
            </div>
          ) : (
            <p className="border-t border-slate-100 pt-4 text-sm text-slate-600">
              <span className="font-semibold">{d.approvalStatus === 'APPROVED' ? '✅ Approved' : '✖️ Rejected'}</span>
              {d.decidedAt && ` on ${new Date(d.decidedAt).toLocaleString()}`}
              {d.managerNote && <> · "{d.managerNote}"</>}
            </p>
          )}
        </div>
      </div>
    </article>
  )
}

function Section({ title, children }) {
  return (
    <section>
      <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</h4>
      {children}
    </section>
  )
}
