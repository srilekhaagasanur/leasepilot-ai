import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import DraftCard from './DraftCard'

const TABS = [
  { id: 'queue', label: 'Approval queue' },
  { id: 'tenants', label: 'Tenant requests' },
  { id: 'history', label: 'History' },
]

export default function ManagerView({ user, onTenancyChanged }) {
  const [tab, setTab] = useState('queue')
  const [drafts, setDrafts] = useState([])
  const [history, setHistory] = useState([])
  const [tenancies, setTenancies] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [pending, approved, rejected, joins] = await Promise.all([
        api.drafts(user.id, 'PENDING'),
        api.drafts(user.id, 'APPROVED'),
        api.drafts(user.id, 'REJECTED'),
        api.pendingTenancies(user.id),
      ])
      setDrafts(pending)
      setHistory([...approved, ...rejected].sort((a, b) => new Date(b.decidedAt) - new Date(a.decidedAt)))
      setTenancies(joins)
    } finally {
      setLoading(false)
    }
  }, [user.id])

  useEffect(() => { load() }, [load])

  const counts = { queue: drafts.length, tenants: tenancies.length, history: history.length }

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
        <button onClick={load} className="text-sm font-medium text-indigo-600 hover:text-indigo-800">↻ Refresh</button>
      </div>

      {loading && <p className="text-slate-500">Loading…</p>}

      {!loading && tab === 'queue' && (
        drafts.length === 0
          ? <Empty text="Nothing waiting for approval. New tenant reports will show up here once the AI has prepared them." />
          : <div className="space-y-6">{drafts.map((d) => <DraftCard key={d.draftId} draft={d} managerId={user.id} onDecided={load} />)}</div>
      )}

      {!loading && tab === 'history' && (
        history.length === 0
          ? <Empty text="No decisions yet." />
          : <div className="space-y-6">{history.map((d) => <DraftCard key={d.draftId} draft={d} managerId={user.id} onDecided={load} />)}</div>
      )}

      {!loading && tab === 'tenants' && (
        tenancies.length === 0
          ? <Empty text="No tenants waiting to join a unit." />
          : <ul className="space-y-3">
              {tenancies.map((t) => (
                <TenancyItem key={t.tenancyId} tenancy={t} managerId={user.id}
                  onDecided={() => { load(); onTenancyChanged() }} />
              ))}
            </ul>
      )}
    </div>
  )
}

function TenancyItem({ tenancy: t, managerId, onDecided }) {
  const [busy, setBusy] = useState(false)

  async function decide(approve) {
    setBusy(true)
    await (approve ? api.approveTenancy : api.rejectTenancy)(t.tenancyId, managerId)
    onDecided()
  }

  return (
    <li className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div>
        <p className="font-semibold">{t.tenantName} <span className="font-normal text-slate-500">({t.tenantEmail})</span></p>
        <p className="text-sm text-slate-600">
          Wants to join <strong>Unit {t.unitNumber}</strong>, {t.propertyName} · used the unit's invite code · requested {new Date(t.requestedAt).toLocaleDateString()}
        </p>
      </div>
      <div className="flex gap-2">
        <button onClick={() => decide(true)} disabled={busy}
          className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50">Approve</button>
        <button onClick={() => decide(false)} disabled={busy}
          className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50">Reject</button>
      </div>
    </li>
  )
}

function Empty({ text }) {
  return <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500">{text}</p>
}
