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
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [pending, approved, rejected] = await Promise.all([
        api.drafts('PENDING'),
        api.drafts('APPROVED'),
        api.drafts('REJECTED'),
      ])
      setDrafts(pending)
      setHistory([...approved, ...rejected].sort((a, b) => new Date(b.decidedAt) - new Date(a.decidedAt)))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

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
        {tab !== 'properties' && <button onClick={load} className="text-sm font-medium text-indigo-600 hover:text-indigo-800">↻ Refresh</button>}
      </div>

      {loading && tab !== 'properties' && <p className="text-slate-500">Loading…</p>}

      {!loading && tab === 'queue' && (
        drafts.length === 0
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

function Empty({ text }) {
  return <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500">{text}</p>
}
