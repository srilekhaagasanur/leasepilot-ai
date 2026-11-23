import { useEffect, useState } from 'react'
import { api } from '../api'

const inputClass = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200'
const primaryButton = 'rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:bg-slate-300'
const linkButton = 'text-sm font-semibold text-indigo-600 hover:text-indigo-800'

// The owner's buildings: properties -> units -> tenants.
// Every change returns the owner's full updated list from the backend, so we just replace it.
export default function PropertiesView() {
  const [properties, setProperties] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.properties().then(setProperties).catch((e) => setError(e.message))
  }, [])

  if (error) return <p className="rounded-lg bg-rose-50 p-4 text-rose-700">{error}</p>
  if (properties === null) return <p className="text-slate-500">Loading…</p>

  return (
    <div className="space-y-6">
      {properties.length === 0 && (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-slate-500">
          No properties yet. Add your first building, then its units and tenants. Tenants can create their account
          as soon as you've added their email.
        </p>
      )}
      {properties.map((p) => <PropertyCard key={p.id} property={p} onChange={setProperties} />)}
      <AddPropertyForm onChange={setProperties} startOpen={properties.length === 0} />
    </div>
  )
}

// Runs a backend change; shows its error message if it fails
function useAction(onChange) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function run(call) {
    setBusy(true)
    setError(null)
    try {
      onChange(await call())
      return true
    } catch (e) {
      setError(e.message)
      return false
    } finally {
      setBusy(false)
    }
  }

  return { busy, error, run }
}

function AddPropertyForm({ onChange, startOpen }) {
  const [open, setOpen] = useState(startOpen)
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const { busy, error, run } = useAction(onChange)

  async function submit(e) {
    e.preventDefault()
    if (await run(() => api.addProperty(name, address))) {
      setName('')
      setAddress('')
      setOpen(false)
    }
  }

  if (!open) {
    return <button onClick={() => setOpen(true)} className={linkButton}>+ Add a property</button>
  }

  return (
    <form onSubmit={submit} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="font-semibold">New property</h3>
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <input required placeholder="Name, e.g. Maple Court" value={name} onChange={(e) => setName(e.target.value)} className={inputClass} />
        <input required placeholder="Address" value={address} onChange={(e) => setAddress(e.target.value)} className={inputClass} />
      </div>
      {error && <p className="mt-3 text-sm text-rose-600">{error}</p>}
      <div className="mt-4 flex items-center gap-4">
        <button type="submit" disabled={busy} className={primaryButton}>{busy ? 'Saving…' : 'Add property'}</button>
        <button type="button" onClick={() => setOpen(false)} className="text-sm text-slate-600 hover:text-slate-900">Cancel</button>
      </div>
    </form>
  )
}

function PropertyCard({ property: p, onChange }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
      <header className="border-b border-slate-100 px-5 py-4">
        <h2 className="text-lg font-semibold">{p.name}</h2>
        <p className="text-sm text-slate-500">{p.address} · {p.units.length} {p.units.length === 1 ? 'unit' : 'units'}</p>
      </header>
      <ul className="divide-y divide-slate-100">
        {p.units.map((u) => <UnitRow key={u.id} unit={u} onChange={onChange} />)}
      </ul>
      <div className="border-t border-slate-100 px-5 py-4">
        <AddUnitForm propertyId={p.id} onChange={onChange} />
      </div>
    </section>
  )
}

function AddUnitForm({ propertyId, onChange }) {
  const [unitNumber, setUnitNumber] = useState('')
  const { busy, error, run } = useAction(onChange)

  async function submit(e) {
    e.preventDefault()
    if (await run(() => api.addUnit(propertyId, unitNumber))) setUnitNumber('')
  }

  return (
    <form onSubmit={submit}>
      <div className="flex flex-wrap items-center gap-2">
        <input required placeholder="New unit, e.g. 5A" value={unitNumber} onChange={(e) => setUnitNumber(e.target.value)}
          className={`${inputClass} max-w-48`} />
        <button type="submit" disabled={busy} className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50">
          {busy ? 'Adding…' : '+ Add unit'}
        </button>
      </div>
      {error && <p className="mt-2 text-sm text-rose-600">{error}</p>}
    </form>
  )
}

function UnitRow({ unit: u, onChange }) {
  const [adding, setAdding] = useState(false)

  return (
    <li className="px-5 py-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-semibold">Unit {u.unitNumber}</p>
        {!adding && <button onClick={() => setAdding(true)} className={linkButton}>+ Add tenant</button>}
      </div>
      {u.tenants.length === 0 && !adding && <p className="mt-1 text-sm text-slate-400">No tenants yet.</p>}
      {u.tenants.length > 0 && (
        <ul className="mt-2 space-y-2">
          {u.tenants.map((t) => <TenantItem key={t.inviteId} tenant={t} onChange={onChange} />)}
        </ul>
      )}
      {adding && <AddTenantForm unitId={u.id} onChange={onChange} onDone={() => setAdding(false)} />}
    </li>
  )
}

function TenantItem({ tenant: t, onChange }) {
  const { busy, error, run } = useAction(onChange)
  const date = (d) => new Date(`${d}T00:00:00`).toLocaleDateString()
  const rent = Number(t.monthlyRent).toLocaleString(undefined, { style: 'currency', currency: 'USD' })

  return (
    <li className="rounded-lg bg-slate-50 px-3 py-2 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-medium">{t.fullName}</span>
          {t.signedUp
            ? <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">Signed up</span>
            : <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">Invited</span>}
        </div>
        {!t.signedUp && (
          <button onClick={() => run(() => api.cancelInvite(t.inviteId))} disabled={busy}
            className="text-xs text-slate-500 underline hover:text-rose-700 disabled:opacity-50">
            {busy ? 'Removing…' : 'Remove'}
          </button>
        )}
      </div>
      <p className="mt-1 text-slate-600">
        {t.email}{t.phone ? ` · ${t.phone}` : ''} · Lease {date(t.leaseStart)} – {date(t.leaseEnd)} · {rent}/month
      </p>
      {!t.signedUp && (
        <p className="mt-1 text-xs text-slate-500">They can now create a tenant account on LeasePilot with {t.email}.</p>
      )}
      {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
    </li>
  )
}

function AddTenantForm({ unitId, onChange, onDone }) {
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', leaseStart: '', leaseEnd: '', monthlyRent: '' })
  const { busy, error, run } = useAction(onChange)
  const field = (name) => ({ value: form[name], onChange: (e) => setForm({ ...form, [name]: e.target.value }) })

  async function submit(e) {
    e.preventDefault()
    if (await run(() => api.addTenant(unitId, form))) onDone()
  }

  return (
    <form onSubmit={submit} className="mt-3 rounded-xl border border-indigo-100 bg-indigo-50/40 p-4">
      <div className="grid gap-3 sm:grid-cols-3">
        <label className="text-xs font-medium text-slate-600">Full name
          <input required {...field('fullName')} className={`${inputClass} mt-1`} />
        </label>
        <label className="text-xs font-medium text-slate-600">Email
          <input required type="email" {...field('email')} className={`${inputClass} mt-1`} />
        </label>
        <label className="text-xs font-medium text-slate-600">Phone (optional)
          <input type="tel" {...field('phone')} className={`${inputClass} mt-1`} />
        </label>
        <label className="text-xs font-medium text-slate-600">Lease start
          <input required type="date" {...field('leaseStart')} className={`${inputClass} mt-1`} />
        </label>
        <label className="text-xs font-medium text-slate-600">Lease end
          <input required type="date" {...field('leaseEnd')} className={`${inputClass} mt-1`} />
        </label>
        <label className="text-xs font-medium text-slate-600">Monthly rent ($)
          <input required type="number" min="1" step="0.01" {...field('monthlyRent')} className={`${inputClass} mt-1`} />
        </label>
      </div>
      <p className="mt-3 text-xs text-slate-500">
        The tenant gets our standard lease repair clauses, which the AI uses to decide who pays for a repair.
      </p>
      {error && <p className="mt-2 text-sm text-rose-600">{error}</p>}
      <div className="mt-3 flex items-center gap-4">
        <button type="submit" disabled={busy} className={primaryButton}>{busy ? 'Saving…' : 'Add tenant'}</button>
        <button type="button" onClick={onDone} className="text-sm text-slate-600 hover:text-slate-900">Cancel</button>
      </div>
    </form>
  )
}
