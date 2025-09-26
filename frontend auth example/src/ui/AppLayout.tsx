import React, { useEffect } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../utils/auth'

export const AppLayout: React.FC = () => {
  const { isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    const onForcedLogout = () => navigate('/login')
    window.addEventListener('auth:logout', onForcedLogout as any)
    return () => window.removeEventListener('auth:logout', onForcedLogout as any)
  }, [navigate])

  const isAuthPage = location.pathname.startsWith('/login')

  return (
    <div className="container">
      <header className="header">
        <div className="brand">
          <div className="brand-badge">UPL</div>
          Unlimited Powerful Learning
        </div>
        <nav className="nav" role="navigation">
          <Link to="/">Home</Link>
          <Link to="#">Careers</Link>
          <Link to="#">Blog</Link>
          <Link to="#">About Us</Link>
          <Link to="#">Onboarding</Link>
          {isAuthenticated ? (
            <button className="ghost" onClick={async () => { await logout(); navigate('/'); }}>Logout</button>
          ) : (
            <>
              <Link to="/login">Login</Link>
              <button className="accent">Enroll now</button>
            </>
          )}
        </nav>
      </header>

      {isAuthPage ? (
        <section className="hero">
          <div className="login-grid">
            <div className="login-card">
              <Outlet />
            </div>
            <div className="hero-pane">
              <img className="hero-image" src="/aaa.png" alt="TMS screenshot" />
              <div className="floating-card">
                <div className="icon">📅</div>
                <div>
                  <div className="value">250k</div>
                  <div className="label">Assisted students</div>
                </div>
              </div>
            </div>
          </div>
        </section>
      ) : (
        <main>
          <Outlet />
        </main>
      )}
    </div>
  )
}


