import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ReferenceDataProvider } from '../data/ReferenceDataContext'
import { BankIcon, GridIcon, ListIcon, LogoutIcon } from './icons'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', Icon: GridIcon, end: true },
  { to: '/transactions', label: 'Transactions', Icon: ListIcon, end: false },
  { to: '/accounts', label: 'Accounts', Icon: BankIcon, end: false },
] as const

// Navigation is scoped to what's actually built (through Phase 5) - the
// remaining docs/06-frontend/ui-ux-specification.md sections (Statements,
// Budgets, Goals, Recurring, AI Assistant) come with their own phases.
export function Layout() {
  const { user, logout } = useAuth()

  async function handleLogout() {
    await logout()
  }

  return (
    <ReferenceDataProvider>
      <div className="app-shell">
        <aside className="sidebar">
          <div className="sidebar-brand">
            <div className="sidebar-brand-name">Spendwise</div>
            <div className="sidebar-brand-tag">Personal Finance</div>
          </div>
          <nav className="sidebar-nav">
            {NAV_ITEMS.map(({ to, label, Icon, end }) => (
              <NavLink key={to} to={to} end={end} className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
                <Icon />
                {label}
              </NavLink>
            ))}
          </nav>
          <div className="sidebar-footer">
            <div className="sidebar-divider" />
            <div className="sidebar-user">
              <div className="sidebar-avatar">{user ? user.fullName.charAt(0).toUpperCase() : '?'}</div>
              <div style={{ minWidth: 0 }}>
                <div className="sidebar-user-name">{user?.fullName}</div>
                <div className="sidebar-user-email">{user?.email}</div>
              </div>
            </div>
            <button type="button" className="sidebar-logout" onClick={() => void handleLogout()}>
              <LogoutIcon />
              Log out
            </button>
          </div>
        </aside>
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </ReferenceDataProvider>
  )
}
