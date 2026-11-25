import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { CategoryTag, StatusBadge, UrgencyBadge } from './Badges'
import VoiceRecorder from './VoiceRecorder'
import { formatDate, WORK_ORDER_STATUSES } from '../workOrders'
import { ProgressSteps, Timeline } from './WorkOrderParts'

export default function TenantView({ user }) {
  const [requests, setRequests] = useState([])

  const loadRequests = useCallback(() => {
    api.myRequests().then(setRequests).catch(() => setRequests([]))
  }, [])

  useEffect(loadRequests, [loadRequests])

  // While the AI is still working on a request in the background, check back every few seconds
  const aiWorking = requests.some((r) => r.aiStatus === 'PROCESSING')
  useEffect(() => {
    if (!aiWorking) return
    const timer = setInterval(loadRequests, 4000)
    return () => clearInterval(timer)
  }, [aiWorking, loadRequests])

  if (user.tenancyStatus !== 'APPROVED') {
    return (
      <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6">
        <h2 className="text-lg font-semibold text-amber-900">Not connected to a unit</h2>
        <p className="mt-2 text-amber-800">
          Your account isn't connected to a unit. Ask your landlord to check that they added you in LeasePilot.
        </p>
      </div>
    )
  }

  return (
    <div className="mx-auto grid max-w-5xl gap-8 lg:grid-cols-[1fr_1fr]">
      <ReportForm user={user} onSubmitted={loadRequests} />
      <section>
        <h2 className="mb-3 text-lg font-semibold">Your requests</h2>
        {requests.length === 0 ? (
          <p className="text-slate-500">No requests yet.</p>
        ) : (
          <ul className="space-y-3">
            {requests.map((r) => <RequestItem key={r.id} request={r} onChanged={loadRequests} />)}
          </ul>
        )}
      </section>
    </div>
  )
}

function ReportForm({ user, onSubmitted }) {
  const [description, setDescription] = useState('')
  const [photo, setPhoto] = useState(null)
  const [voiceNote, setVoiceNote] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)
  const photoPreview = useMemo(() => (photo ? URL.createObjectURL(photo) : null), [photo])

  useEffect(() => () => photoPreview && URL.revokeObjectURL(photoPreview), [photoPreview])

  const canSubmit = (description.trim() || voiceNote) && !submitting

  async function submit(e) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setResult(null)
    try {
      const saved = await api.submitRequest({ description: description.trim(), photo, voiceNote })
      setResult(saved)
      setDescription('')
      setPhoto(null)
      setVoiceNote(null)
      onSubmitted()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Report a problem</h2>
      <p className="mt-1 text-sm text-slate-500">Unit {user.unitNumber} · Describe it, add a photo, or just record a voice note.</p>

      <form onSubmit={submit} className="mt-5 space-y-5">
        <div>
          <label htmlFor="description" className="text-sm font-medium">What's wrong?</label>
          <textarea id="description" rows={3} value={description} onChange={(e) => setDescription(e.target.value)}
            placeholder="e.g. The kitchen sink is leaking under the cabinet"
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
        </div>

        <div>
          <span className="text-sm font-medium">Photo (optional)</span>
          <div className="mt-1 flex items-center gap-4">
            <label className="cursor-pointer rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
              📷 {photo ? 'Change photo' : 'Add a photo'}
              <input type="file" accept="image/jpeg,image/png,image/webp" className="hidden"
                onChange={(e) => setPhoto(e.target.files?.[0] ?? null)} />
            </label>
            {photo && (
              <button type="button" onClick={() => setPhoto(null)} className="text-sm text-slate-500 underline hover:text-slate-700">
                Remove
              </button>
            )}
          </div>
          {photoPreview && <img src={photoPreview} alt="Selected" className="mt-3 max-h-48 rounded-lg border border-slate-200 object-cover" />}
        </div>

        <div>
          <span className="text-sm font-medium">Voice note (optional)</span>
          <div className="mt-1"><VoiceRecorder value={voiceNote} onChange={setVoiceNote} /></div>
        </div>

        <button type="submit" disabled={!canSubmit}
          className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 font-semibold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-slate-300">
          {submitting ? 'Sending…' : 'Send to property manager'}
        </button>

        {error && <p className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        {result && (
          <div className="rounded-lg bg-emerald-50 p-4 text-sm text-emerald-900">
            <p className="font-semibold">Got it — your request is on its way to the property manager.</p>
            <p className="mt-1">Our AI assistant is preparing it now. You'll see it update in your requests.</p>
          </div>
        )}
      </form>
    </section>
  )
}

function RequestItem({ request: r, onChanged }) {
  const isWorkOrder = WORK_ORDER_STATUSES.includes(r.status) && r.updates?.length > 0

  return (
    <li className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-medium">{r.aiSummary ?? r.description ?? (r.hasVoiceNote ? 'Voice note' : 'Maintenance request')}</p>
        {r.aiStatus === 'PROCESSING' ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-2.5 py-0.5 text-xs font-semibold text-indigo-700">
            <span className="h-3 w-3 animate-spin rounded-full border-2 border-indigo-200 border-t-indigo-600" />
            AI is reviewing…
          </span>
        ) : (
          <StatusBadge status={r.status} />
        )}
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <span>{new Date(r.createdAt).toLocaleDateString()}</span>
        <CategoryTag category={r.category} />
        <UrgencyBadge urgency={r.urgency} />
      </div>
      {isWorkOrder && (
        <div className="mt-3 space-y-3">
          <ProgressSteps status={r.status} />
          {r.status === 'SCHEDULED' && r.scheduledFor && (
            <p className="text-sm font-medium text-violet-800">📅 Repair scheduled for {formatDate(r.scheduledFor)}</p>
          )}
          <Timeline updates={r.updates} />
        </div>
      )}
      {r.status === 'RESOLVED' && !r.tenantConfirmedAt && <ConfirmFix request={r} onChanged={onChanged} />}
      {r.replyFromManagement && (
        <div className="mt-3 rounded-lg bg-slate-50 p-3 text-sm">
          <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500">Reply from management</p>
          <p className="whitespace-pre-line">{r.replyFromManagement}</p>
        </div>
      )}
    </li>
  )
}

// After the landlord marks a job fixed, the tenant confirms it or says it's still broken
function ConfirmFix({ request: r, onChanged }) {
  const [reopening, setReopening] = useState(false)
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function send(call) {
    setBusy(true)
    setError(null)
    try {
      await call()
      onChanged()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <div className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm">
      <p className="font-semibold text-emerald-900">Your landlord marked this fixed. Is it?</p>
      {!reopening ? (
        <div className="mt-2 flex flex-wrap gap-2">
          <button onClick={() => send(() => api.confirmFixed(r.id))} disabled={busy}
            className="rounded-lg bg-emerald-600 px-3 py-1.5 font-semibold text-white hover:bg-emerald-700 disabled:opacity-50">
            Yes, it's fixed
          </button>
          <button onClick={() => setReopening(true)} disabled={busy}
            className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50">
            Still not fixed
          </button>
        </div>
      ) : (
        <form onSubmit={(e) => { e.preventDefault(); send(() => api.reopen(r.id, comment)) }} className="mt-2 space-y-2">
          <textarea required rows={2} value={comment} onChange={(e) => setComment(e.target.value)}
            placeholder="What's still wrong?"
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200" />
          <div className="flex items-center gap-3">
            <button type="submit" disabled={busy}
              className="rounded-lg bg-indigo-600 px-3 py-1.5 font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
              {busy ? 'Sending…' : 'Send to landlord'}
            </button>
            <button type="button" onClick={() => setReopening(false)} className="text-slate-600 hover:text-slate-900">Back</button>
          </div>
        </form>
      )}
      {error && <p className="mt-2 text-rose-700">{error}</p>}
    </div>
  )
}
