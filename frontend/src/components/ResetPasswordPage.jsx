import { useState } from 'react'
import { api } from '../api'

// Opened from the emailed link: /reset-password?token=...
export default function ResetPasswordPage({ token, onDone }) {
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(false)

  async function submit(e) {
    e.preventDefault()
    if (password !== confirm) {
      setError("The two passwords don't match.")
      return
    }
    setBusy(true)
    setError(null)
    try {
      await api.resetPassword(token, password)
      setDone(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const inputClass = 'mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200'

  return (
    <section className="mx-auto max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Choose a new password</h2>
      {done ? (
        <>
          <p className="mt-3 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">Your password has been changed.</p>
          <button onClick={onDone} className="mt-4 w-full rounded-lg bg-indigo-600 px-4 py-2.5 font-semibold text-white hover:bg-indigo-700">
            Go to log in
          </button>
        </>
      ) : !token ? (
        <p className="mt-3 text-sm text-rose-700">This link is missing its reset code. Please use the link from the email.</p>
      ) : (
        <form onSubmit={submit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="new-password" className="text-sm font-medium">New password</label>
            <input id="new-password" type="password" required minLength={8} autoComplete="new-password"
              value={password} onChange={(e) => setPassword(e.target.value)} className={inputClass} />
            <p className="mt-1 text-xs text-slate-500">At least 8 characters.</p>
          </div>
          <div>
            <label htmlFor="confirm-password" className="text-sm font-medium">Repeat new password</label>
            <input id="confirm-password" type="password" required autoComplete="new-password"
              value={confirm} onChange={(e) => setConfirm(e.target.value)} className={inputClass} />
          </div>
          {error && <p className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
          <button type="submit" disabled={busy}
            className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 font-semibold text-white hover:bg-indigo-700 disabled:bg-slate-300">
            {busy ? 'Saving…' : 'Save new password'}
          </button>
        </form>
      )}
    </section>
  )
}
