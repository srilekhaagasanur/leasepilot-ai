import { useCallback, useEffect, useState } from 'react'
import { api } from './api'
import ManagerView from './components/ManagerView'
import TenantView from './components/TenantView'

// No login for the hackathon demo: pick a demo user to "view as"
export default function App() {
  const [users, setUsers] = useState([])
  const [currentId, setCurrentId] = useState(null)
  const [error, setError] = useState(null)

  const loadUsers = useCallback(() => {
    api.demoUsers().then(setUsers).catch(() => setError('Could not reach the backend. Is Spring Boot running on port 8080?'))
  }, [])

  useEffect(loadUsers, [loadUsers])

  const current = users.find((u) => u.id === currentId)
  const manager = users.find((u) => u.role === 'MANAGER')
  const tenants = users.filter((u) => u.role === 'TENANT')

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-4">
          <button onClick={() => setCurrentId(null)} className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white">L</span>
            <span className="text-lg font-bold">LeasePilot <span className="text-indigo-600">AI</span></span>
          </button>
          {current && (
            <div className="flex items-center gap-3 text-sm">
              <span className="text-slate-500">Viewing as</span>
              <span className="font-semibold">{current.fullName}</span>
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                {current.role === 'MANAGER' ? 'Property manager' : `Tenant · ${current.unitNumber}`}
              </span>
              <button onClick={() => setCurrentId(null)} className="font-medium text-indigo-600 hover:text-indigo-800">Switch</button>
            </div>
          )}
        </div>
      </header>

      <main className="px-6 py-8">
        {error && <p className="mx-auto max-w-xl rounded-lg bg-rose-50 p-4 text-rose-700">{error}</p>}

        {!current && !error && (
          <div className="mx-auto max-w-3xl">
            <h1 className="text-3xl font-bold tracking-tight">Maintenance requests, handled.</h1>
            <p className="mt-2 text-slate-600">
              Tenants report problems by photo, text, or voice. An AI agent checks the lease and the unit's history
              and drafts the work order and reply — and nothing goes out until the property manager approves it.
            </p>

            <h2 className="mt-10 text-sm font-semibold uppercase tracking-wide text-slate-500">Demo · choose who you are</h2>
            {manager && (
              <button onClick={() => setCurrentId(manager.id)}
                className="mt-3 flex w-full items-center justify-between rounded-2xl border border-indigo-200 bg-indigo-50 p-5 text-left hover:border-indigo-400">
                <div>
                  <p className="font-semibold">{manager.fullName}</p>
                  <p className="text-sm text-slate-600">Property manager · Oakwood Apartments</p>
                </div>
                <span className="font-semibold text-indigo-700">Open dashboard →</span>
              </button>
            )}
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              {tenants.map((t) => (
                <button key={t.id} onClick={() => setCurrentId(t.id)}
                  className="rounded-2xl border border-slate-200 bg-white p-5 text-left hover:border-indigo-300">
                  <p className="font-semibold">{t.fullName}</p>
                  <p className="text-sm text-slate-600">
                    Tenant · Unit {t.unitNumber}
                    {t.tenancyStatus === 'PENDING' && <span className="ml-2 text-amber-700">(awaiting approval)</span>}
                  </p>
                </button>
              ))}
            </div>
          </div>
        )}

        {current?.role === 'MANAGER' && <ManagerView user={current} onTenancyChanged={loadUsers} />}
        {current?.role === 'TENANT' && <TenantView key={current.id} user={current} />}
      </main>
    </div>
  )
}
