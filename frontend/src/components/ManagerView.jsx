import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import DraftCard from './DraftCard'
import PropertiesView from './PropertiesView'

const TABS = [
  { id: 'queue', label: 'Approval queue' },
  { id: 'history', label: 'History' },
  { id: 'properties', label: 'Properties' },
]

export default function ManagerView() {
  const [tab, setTab] = useState('queue')
  const [drafts, setDrafts] = useState([])
  const [history, setHistory] = useState([])
  const [unfinished, setUnfinished] = useState([])
  const [loading, setLoading] = useState(true)

  // quiet: refresh in the background without the "Loading…" flash
  const load = useCallback(async ({ quiet = false } = {}) => {
    if (!quiet) setLoading(true)
    try {
      const [pending, approved, rejected, notReady] = await Promise.all([
        api.drafts('PENDING'),
        api.drafts('APPROVED'),
        api.drafts('REJECTED'),
        api.unfinishedRequests(),
      ])
      setDrafts(pending)
      setUnfinished(notReady)
      setHistory([...approved, ...rejected].sort((a, b) => new Date(b.decidedAt) - new Date(a.decidedAt)))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  // While the AI is preparing a request, check back every few seconds so its draft appears by itself
  const aiWorking = unfinished.some((r) => r.aiStatus === 'PROCESSING')
  useEffect(() => {
    if (!aiWorking) return
    const timer = setInterval(() => load({ quiet: true }), 5000)
    return () => clearInterval(timer)
  }, [aiWorking, load])

  const counts = { queue: drafts.length, history: history.length }

  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <nav className="flex gap-1 rounded-xl bg-slate-200/60 p-1">
          {TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`rounded-lg px-4 py-2 text-sm font-semibold ${tab === t.id ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'}`}>
              {t.label}
              {counts[t.id] > 0 && <span className="ml-2 rounded-full bg-indigo-600 px-2 py-0.5 text-xs text-white">{counts[t.id]}</span>}
            </button>
          ))}
        </nav>
        {tab !== 'properties' && <button onClick={() => load()} className="text-sm font-medium text-indigo-600 hover:text-indigo-800">↻ Refresh</button>}
      </div>

      {loading && tab !== 'properties' && <p className="text-slate-500">Loading…</p>}

      {!loading && tab === 'queue' && unfinished.length > 0 && (
        <ul className="mb-6 space-y-3">
          {unfinished.map((r) => <UnfinishedItem key={r.requestId} request={r} onRetried={() => load({ quiet: true })} />)}
        </ul>
      )}

      {!loading && tab === 'queue' && (
        drafts.length === 0 && unfinished.length > 0 ? null : drafts.length === 0
          ? <Empty text="Nothing waiting for approval. New tenant reports will show up here once the AI has prepared them. To get started, add your building, units, and tenants under Properties." />
          : <div className="space-y-6">{drafts.map((d) => <DraftCard key={d.draftId} draft={d} onDecided={load} />)}</div>
      )}

      {!loading && tab === 'history' && (
        history.length === 0
          ? <Empty text="No decisions yet." />
          : <div className="space-y-6">{history.map((d) => <DraftCard key={d.draftId} draft={d} onDecided={load} />)}</div>
      )}

      {tab === 'properties' && <PropertiesView />}
    </div>
  )
}

// A new request whose draft isn't ready: the AI is still working on it, or failed and can be retried
function UnfinishedItem({ request: r, onRetried }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const failed = r.aiStatus === 'FAILED'

  async function retry() {
    setBusy(true)
    setError(null)
    try {
      await api.retryAi(r.requestId)
      onRetried()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <li className={`rounded-xl border p-4 ${failed ? 'border-rose-200 bg-rose-50/50' : 'border-indigo-100 bg-indigo-50/40'}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-semibold">
            {r.tenantName} <span className="font-normal text-slate-500">· Unit {r.unitNumber}, {r.propertyName}</span>
          </p>
          <p className="mt-1 text-sm text-slate-600">
            {r.description ?? (r.hasVoiceNote ? 'Voice note' : 'New request')}
            {r.hasPhoto && ' · 📷 photo'}
            {' · '}{new Date(r.reportedAt).toLocaleString()}
          </p>
        </div>
        {failed ? (
          <button onClick={retry} disabled={busy}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
            {busy ? 'Retrying…' : '↻ Retry AI'}
          </button>
        ) : (
          <span className="inline-flex items-center gap-2 text-sm font-medium text-indigo-700">
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-200 border-t-indigo-600" />
            AI is preparing the work order…
          </span>
        )}
      </div>
      {failed && <p className="mt-2 text-sm text-rose-700">The AI couldn't finish: {r.aiError}</p>}
      {error && <p className="mt-2 text-sm text-rose-700">{error}</p>}
    </li>
  )
}

function Empty({ text }) {
  return <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500">{text}</p>
}
