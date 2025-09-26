import React, { useState } from 'react'
import { useAuth } from '../utils/auth'
import { api } from '../utils/http'

export const HomePage: React.FC = () => {
  const { isAuthenticated, tokenPayload, refresh } = useAuth()
  const [msg, setMsg] = useState<string | null>(null)
  const [me, setMe] = useState<any | null>(null)
  const [loading, setLoading] = useState(false)

  return (
    <div className="panel">
      <div className="home-hero">
        <div>
          <h1>Welcome to UPL Learning</h1>
          <p>Practice secure auth with RS256, refresh tokens and JWKS.</p>
          {isAuthenticated ? (
            <div className="row">
              <button onClick={async () => { setMsg(null); try { await refresh(); setMsg('Token refreshed') } catch (e:any) { setMsg(e?.message || 'Failed to refresh') } }}>Refresh token</button>
              <button onClick={async () => {
                setMsg(null)
                setMe(null)
                setLoading(true)
                try {
                  const res = await api.get('/example-under-armor/me')
                  if (!res.ok) throw new Error(await res.text())
                  const data = await res.json()
                  setMe(data)
                } catch (e:any) {
                  setMsg(e?.message || 'Failed to fetch /me')
                } finally {
                  setLoading(false)
                }
              }}>Me</button>
            </div>
          ) : (
            <div className="muted">Please log in to continue.</div>
          )}
          {msg && <div className="muted" style={{ marginTop: 8 }}>{msg}</div>}
        </div>
        <div className="hero-pane" style={{ minHeight: 240 }}>
          <img className="hero-image" src="https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=1200&auto=format&fit=crop" alt="learning" />
        </div>
      </div>

      {loading && <div className="muted" style={{ marginTop: 12 }}>Loading...</div>}
      {me && (
        <pre className="panel" style={{ overflow: 'auto', marginTop: 12 }}>{JSON.stringify(me, null, 2)}</pre>
      )}
      {tokenPayload && (
        <pre className="panel" style={{ overflow: 'auto', marginTop: 12 }}>{JSON.stringify(tokenPayload, null, 2)}</pre>
      )}
    </div>
  )
}


