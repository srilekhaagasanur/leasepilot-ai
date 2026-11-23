import { useEffect, useState } from 'react'
import { api } from './api'
import AuthPage from './components/AuthPage'
import ManagerView from './components/ManagerView'
import ResetPasswordPage from './components/ResetPasswordPage'
import TenantView from './components/TenantView'

export default function App() {
  // undefined = still checking, null = nobody logged in
  const [user, setUser] = useState(undefined)
  const [error, setError] = useState(null)
  // The emailed reset link opens /reset-password?token=... (there's no router; this is the only other page)
  const [resetToken, setResetToken] = useState(() =>
    window.location.pathname === '/reset-password' ? new URLSearchParams(window.location.search).get('token') ?? '' : null)

  function leaveResetPage() {
    window.history.replaceState(null, '', '/')
    setResetToken(null)
  }

  useEffect(() => {
    api.me().then(setUser).catch(() => setError('Could not reach the backend. Is Spring Boot running on port 8080?'))
  }, [])

  async function logout() {
    await api.logout().catch(() => {})
    // Logging out also clears the CSRF cookie; asking "who am I?" again gets a fresh one
    setUser(await api.me().catch(() => null))
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-4">
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white">L</span>
            <span className="text-lg font-bold">LeasePilot <span className="text-indigo-600">AI</span></span>
          </div>
          {user && (
            <div className="flex items-center gap-3 text-sm">
              <span className="font-semibold">{user.fullName}</span>
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                {user.role === 'MANAGER' ? 'Property manager' : `Tenant${user.unitNumber ? ` · ${user.unitNumber}` : ''}`}
              </span>
              {user.demo && <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">Demo</span>}
              <button onClick={logout} className="font-medium text-indigo-600 hover:text-indigo-800">Log out</button>
            </div>
          )}
        </div>
      </header>

      <main className="px-6 py-8">
        {error && <p className="mx-auto max-w-xl rounded-lg bg-rose-50 p-4 text-rose-700">{error}</p>}
        {!error && user === undefined && <p className="text-center text-slate-500">Loading…</p>}
        {!error && resetToken !== null && user !== undefined && <ResetPasswordPage token={resetToken} onDone={leaveResetPage} />}
        {!error && resetToken === null && user === null && <AuthPage onLoggedIn={setUser} />}
        {resetToken === null && user?.role === 'MANAGER' && <ManagerView key={user.id} />}
        {resetToken === null && user?.role === 'TENANT' && <TenantView key={user.id} user={user} />}
      </main>
    </div>
  )
}
