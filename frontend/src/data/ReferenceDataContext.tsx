import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { accountsApi } from '../api/accounts'
import { categoriesApi } from '../api/categories'
import { merchantsApi } from '../api/merchants'
import type { Account, Category, Merchant } from '../api/types'

interface ReferenceDataValue {
  accounts: Account[]
  categories: Category[]
  merchants: Merchant[]
  loading: boolean
  refreshAccounts: () => Promise<void>
  refreshMerchants: () => Promise<void>
}

const ReferenceDataContext = createContext<ReferenceDataValue | null>(null)

// Accounts/categories/merchants are needed on Dashboard, Transactions and
// Accounts alike (to label a transaction's account/category/merchant, and to
// populate every select in the create/edit forms) - loaded once here instead
// of every page re-fetching the same three lists.
export function ReferenceDataProvider({ children }: { children: ReactNode }) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [merchants, setMerchants] = useState<Merchant[]>([])
  const [loading, setLoading] = useState(true)

  const refreshAccounts = useCallback(async () => {
    setAccounts(await accountsApi.list())
  }, [])

  const refreshMerchants = useCallback(async () => {
    setMerchants(await merchantsApi.list())
  }, [])

  useEffect(() => {
    let cancelled = false
    Promise.all([accountsApi.list(), categoriesApi.list(), merchantsApi.list()])
      .then(([a, c, m]) => {
        if (cancelled) return
        setAccounts(a)
        setCategories(c)
        setMerchants(m)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <ReferenceDataContext.Provider value={{ accounts, categories, merchants, loading, refreshAccounts, refreshMerchants }}>
      {children}
    </ReferenceDataContext.Provider>
  )
}

export function useReferenceData(): ReferenceDataValue {
  const ctx = useContext(ReferenceDataContext)
  if (!ctx) throw new Error('useReferenceData must be used within a ReferenceDataProvider')
  return ctx
}
