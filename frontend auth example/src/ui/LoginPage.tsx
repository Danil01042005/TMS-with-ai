import React, { useState } from 'react'
import { useAuth } from '../utils/auth'

export const LoginPage: React.FC = () => {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await login({ username, password })
    } catch (err: any) {
      setError(err?.message ?? 'Login failed')
    }
  }

  return (
    <div>
      <div className="brand" style={{ marginBottom: 8 }}>
        <div className="brand-badge">🏫</div>
        <div>Your logo</div>
      </div>
      <h2 style={{ margin: '0 0 4px' }}>Login</h2>
      <div className="muted" style={{ marginBottom: 12 }}>Sign in to continue</div>
      <form className="form" onSubmit={onSubmit}>
        <div className="col">
          <label htmlFor="username">Email</label>
          <input id="username" placeholder="jane@school.com" value={username} onChange={e => setUsername(e.target.value)} required autoComplete="username" />
        </div>
        <div className="col">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)} required autoComplete="current-password" />
        </div>
        {error && <div className="error">{error}</div>}
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <button type="submit" className="accent">Sign in</button>
          <button type="reset" className="ghost" onClick={() => { setUsername(''); setPassword(''); setError(null) }}>Reset</button>
        </div>
      </form>
      {/* signup link removed */}
    </div>
  )
}


