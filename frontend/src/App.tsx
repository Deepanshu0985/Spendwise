import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { DashboardPage } from './pages/DashboardPage'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route
          path="transactions"
          element={<ComingSoonPage title="Transactions" phase="Phase 3" />}
        />
        <Route
          path="accounts"
          element={<ComingSoonPage title="Accounts" phase="Phase 2" />}
        />
        <Route
          path="statements"
          element={<ComingSoonPage title="Statements" phase="Phase 6" />}
        />
        <Route
          path="budgets"
          element={<ComingSoonPage title="Budgets" phase="Phase 10" />}
        />
        <Route
          path="goals"
          element={<ComingSoonPage title="Goals" phase="Phase 10" />}
        />
        <Route
          path="recurring"
          element={<ComingSoonPage title="Recurring" phase="Phase 9" />}
        />
        <Route
          path="assistant"
          element={<ComingSoonPage title="AI Assistant" phase="Phase 12" />}
        />
        <Route
          path="settings"
          element={<ComingSoonPage title="Settings" phase="Phase 14" />}
        />
      </Route>
    </Routes>
  )
}

export default App
