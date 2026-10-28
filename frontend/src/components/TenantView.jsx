import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { CategoryTag, StatusBadge, UrgencyBadge } from './Badges'
import VoiceRecorder from './VoiceRecorder'

export default function TenantView({ user }) {
  const [requests, setRequests] = useState([])

  const loadRequests = useCallback(() => {
    api.myRequests(user.id).then(setRequests).catch(() => setRequests([]))
  }, [user.id])

  useEffect(loadRequests, [loadRequests])

  if (user.tenancyStatus !== 'APPROVED') {
    return (
      <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6">
        <h2 className="text-lg font-semibold text-amber-900">Waiting for approval</h2>
        <p className="mt-2 text-amber-800">
          Your request to join Unit {user.unitNumber} is waiting for the property manager's approval.
          You'll be able to report maintenance issues once it's approved.
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
            {requests.map((r) => <RequestItem key={r.id} request={r} />)}
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
      const saved = await api.submitRequest({ tenantId: user.id, description: description.trim(), photo, voiceNote })
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

        {submitting && (
          <div className="flex items-start gap-3 rounded-lg bg-indigo-50 p-4 text-sm text-indigo-900">
            <span className="mt-0.5 h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-indigo-300 border-t-indigo-700" />
            <span>Our AI assistant is reviewing your report{voiceNote ? ', transcribing your voice note,' : ''} and preparing it for the manager. This usually takes 20–40 seconds.</span>
          </div>
        )}
        {error && <p className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        {result && (
          <div className="rounded-lg bg-emerald-50 p-4 text-sm text-emerald-900">
            <p className="font-semibold">Got it — your request is with the property manager.</p>
            <p className="mt-1">We logged it as: <strong>{result.aiSummary ?? 'Maintenance request'}</strong></p>
          </div>
        )}
      </form>
    </section>
  )
}

function RequestItem({ request: r }) {
  return (
    <li className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-medium">{r.aiSummary ?? r.description ?? 'Maintenance request'}</p>
        <StatusBadge status={r.status} />
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <span>{new Date(r.createdAt).toLocaleDateString()}</span>
        <CategoryTag category={r.category} />
        <UrgencyBadge urgency={r.urgency} />
      </div>
      {r.replyFromManagement && (
        <div className="mt-3 rounded-lg bg-slate-50 p-3 text-sm">
          <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-500">Reply from management</p>
          <p className="whitespace-pre-line">{r.replyFromManagement}</p>
        </div>
      )}
    </li>
  )
}
