import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RedirectIfAuthenticated, RequireAuth } from './auth/guards'
import { Layout } from './components/Layout'
import { TopProgressBar } from './components/ui/TopProgressBar'
import { AccountsPage } from './pages/AccountsPage'
import { DashboardPage } from './pages/DashboardPage'
import { BudgetsPage } from './pages/BudgetsPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { GoalsPage } from './pages/GoalsPage'
import { LoginPage } from './pages/LoginPage'
import { RecurringPage } from './pages/RecurringPage'
import { RegisterPage } from './pages/RegisterPage'
import { PrivacyPage } from './pages/PrivacyPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { SettingsPage } from './pages/SettingsPage'
import { StatementsPage } from './pages/StatementsPage'
import { TransactionsPage } from './pages/TransactionsPage'

function App() {
  return (
    <AuthProvider>
      <TopProgressBar />
      <Routes>
        <Route
          path="/login"
          element={
            <RedirectIfAuthenticated>
              <LoginPage />
            </RedirectIfAuthenticated>
          }
        />
        <Route
          path="/register"
          element={
            <RedirectIfAuthenticated>
              <RegisterPage />
            </RedirectIfAuthenticated>
          }
        />
        <Route
          path="/forgot-password"
          element={
            <RedirectIfAuthenticated>
              <ForgotPasswordPage />
            </RedirectIfAuthenticated>
          }
        />
        <Route
          path="/reset-password"
          element={
            <RedirectIfAuthenticated>
              <ResetPasswordPage />
            </RedirectIfAuthenticated>
          }
        />

        <Route path="/privacy" element={<PrivacyPage />} />

        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route index element={<DashboardPage />} />
            <Route path="transactions" element={<TransactionsPage />} />
            <Route path="accounts" element={<AccountsPage />} />
            <Route path="statements" element={<StatementsPage />} />
            <Route path="budgets" element={<BudgetsPage />} />
            <Route path="goals" element={<GoalsPage />} />
            <Route path="recurring" element={<RecurringPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
