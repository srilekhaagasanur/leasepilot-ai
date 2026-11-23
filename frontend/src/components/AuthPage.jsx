import { useEffect, useState } from 'react'
import { api } from '../api'

// The start page when nobody is logged in: log in, create an owner account, or try a demo account
export default function AuthPage({ onLoggedIn }) {
  return (
    <div className="mx-auto grid max-w-5xl gap-10 lg:grid-cols-[1fr_1fr]">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Maintenance requests, handled.</h1>
        <p className="mt-2 text-slate-600">
          Tenants report problems by photo, text, or voice. An AI agent checks the lease and the unit's history
          and drafts the work order and reply — and nothing goes out until the property manager approves it.
        </p>
        <DemoAccounts onLoggedIn={onLoggedIn} />
      </div>
      <AccountForm onLoggedIn={onLoggedIn} />
    </div>
  )
}

function AccountForm({ onLoggedIn }) {
  const [mode, setMode] = useState('login') // login | signup | forgot
  const [role, setRole] = useState('MANAGER') // who is signing up: MANAGER (owner) or TENANT
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    if (mode === 'forgot') {
      try {
        setNotice((await api.forgotPassword(email)).message)
      } catch (err) {
        setError(err.message)
      }
      setBusy(false)
      return
    }
    try {
      onLoggedIn(mode === 'login'
        ? await api.login(email, password)
        : await api.signup(fullName, email, password, role))
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  function switchMode(next) {
    setMode(next)
    setError(null)
    setNotice(null)
  }

  const inputClass = 'mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200'

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">
        {{ login: 'Log in', signup: 'Create an account', forgot: 'Reset your password' }[mode]}
      </h2>
      {mode === 'forgot' && (
        <p className="mt-1 text-sm text-slate-500">Enter your email and we'll send you a link to choose a new password.</p>
      )}
      {mode === 'signup' && (
        <>
          <div className="mt-4 grid grid-cols-2 gap-1 rounded-xl bg-slate-100 p-1">
            {[['MANAGER', "I'm a property owner"], ['TENANT', "I'm a tenant"]].map(([value, label]) => (
              <button key={value} type="button" onClick={() => { setRole(value); setError(null) }}
                className={`rounded-lg px-3 py-2 text-sm font-semibold ${role === value ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'}`}>
                {label}
              </button>
            ))}
          </div>
          <p className="mt-3 text-sm text-slate-500">
            {role === 'MANAGER'
              ? 'Add your properties, units, and tenants, and review what the AI prepares.'
              : "Use the email your landlord added in LeasePilot. You'll be connected to your unit automatically."}
          </p>
        </>
      )}

      <form onSubmit={submit} className="mt-5 space-y-4">
        {mode === 'signup' && (
          <div>
            <label htmlFor="fullName" className="text-sm font-medium">Full name</label>
            <input id="fullName" required value={fullName} onChange={(e) => setFullName(e.target.value)}
              autoComplete="name" className={inputClass} />
          </div>
        )}
        <div>
          <label htmlFor="email" className="text-sm font-medium">Email</label>
          <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
            autoComplete="email" className={inputClass} />
        </div>
        {mode !== 'forgot' && <div>
          <div className="flex items-center justify-between">
            <label htmlFor="password" className="text-sm font-medium">Password</label>
            {mode === 'login' && (
              <button type="button" onClick={() => switchMode('forgot')} className="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                Forgot password?
              </button>
            )}
          </div>
          <input id="password" type="password" required minLength={mode === 'signup' ? 8 : undefined}
            value={password} onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'} className={inputClass} />
          {mode === 'signup' && <p className="mt-1 text-xs text-slate-500">At least 8 characters.</p>}
        </div>}

        {error && <p className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        {notice && <p className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{notice}</p>}

        <button type="submit" disabled={busy}
          className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 font-semibold text-white hover:bg-indigo-700 disabled:bg-slate-300">
          {busy ? 'Please wait…' : { login: 'Log in', signup: 'Create account', forgot: 'Send reset link' }[mode]}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-600">
        {mode === 'forgot' ? (
          <button onClick={() => switchMode('login')} className="font-semibold text-indigo-600 hover:text-indigo-800">Back to log in</button>
        ) : mode === 'login' ? (
          <>New here? <button onClick={() => switchMode('signup')} className="font-semibold text-indigo-600 hover:text-indigo-800">Create an account</button></>
        ) : (
          <>Already have an account? <button onClick={() => switchMode('login')} className="font-semibold text-indigo-600 hover:text-indigo-800">Log in</button></>
        )}
      </p>
    </section>
  )
}

// "Try as..." buttons for the seeded demo accounts (hidden when the backend has demo mode off)
function DemoAccounts({ onLoggedIn }) {
  const [users, setUsers] = useState([])
  const [busyEmail, setBusyEmail] = useState(null)
  const [error, setError] = useState(null)

  const loadUsers = () => api.demoUsers().then(setUsers).catch(() => setUsers([]))
  useEffect(() => { loadUsers() }, [])

  async function tryAs(email) {
    setBusyEmail(email)
    setError(null)
    try {
      onLoggedIn(await api.demoLogin(email))
    } catch (err) {
      setError(err.message)
      setBusyEmail(null)
    }
  }

  if (users.length === 0) return null

  const manager = users.find((u) => u.role === 'MANAGER')
  const tenants = users.filter((u) => u.role === 'TENANT')

  return (
    <div className="mt-10">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Or try the demo · no account needed</h2>
      {manager && (
        <button onClick={() => tryAs(manager.email)} disabled={busyEmail !== null}
          className="mt-3 flex w-full items-center justify-between rounded-2xl border border-indigo-200 bg-indigo-50 p-4 text-left hover:border-indigo-400 disabled:opacity-60">
          <div>
            <p className="font-semibold">{manager.fullName}</p>
            <p className="text-sm text-slate-600">Property manager · Oakwood Apartments</p>
          </div>
          <span className="font-semibold text-indigo-700">{busyEmail === manager.email ? 'Opening…' : 'Try as manager →'}</span>
        </button>
      )}
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        {tenants.map((t) => (
          <button key={t.id} onClick={() => tryAs(t.email)} disabled={busyEmail !== null}
            className="rounded-2xl border border-slate-200 bg-white p-4 text-left hover:border-indigo-300 disabled:opacity-60">
            <p className="font-semibold">{busyEmail === t.email ? 'Opening…' : t.fullName}</p>
            <p className="text-sm text-slate-600">
              Tenant · Unit {t.unitNumber}
            </p>
          </button>
        ))}
      </div>
      <p className="mt-3 text-sm text-slate-500">
        To see a tenant join: Dana has added <strong>jordan.kim@example.com</strong> to Unit 4D. Create a tenant
        account with that email and it connects to the unit automatically.
      </p>
      {error && <p className="mt-3 text-sm text-rose-600">{error}</p>}
      <ResetDemo onReset={loadUsers} />
    </div>
  )
}

// Puts the demo back to its starting data. Asks for confirmation in the page itself.
function ResetDemo({ onReset }) {
  const [state, setState] = useState('idle') // idle | confirming | resetting | done | failed

  async function reset() {
    setState('resetting')
    try {
      await api.resetDemo()
      onReset()
      setState('done')
    } catch {
      setState('failed')
    }
  }

  return (
    <div className="mt-10 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-6 text-sm text-slate-500">
      {state === 'confirming' ? (
        <>
          <span>Delete all new requests and drafts and restore the original demo data?</span>
          <button onClick={reset} className="rounded-lg bg-rose-600 px-3 py-1.5 font-semibold text-white hover:bg-rose-700">Yes, reset</button>
          <button onClick={() => setState('idle')} className="font-medium text-slate-600 hover:text-slate-900">Cancel</button>
        </>
      ) : (
        <>
          <button onClick={() => setState('confirming')} disabled={state === 'resetting'}
            className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50">
            {state === 'resetting' ? 'Resetting…' : '↺ Reset demo'}
          </button>
          {state === 'idle' && <span>Starts the demo over with the original sample data.</span>}
          {state === 'done' && <span className="text-emerald-700">Demo reset. Everything is back to the starting point.</span>}
          {state === 'failed' && <span className="text-rose-600">Reset failed. Is the backend running?</span>}
        </>
      )}
    </div>
  )
}

