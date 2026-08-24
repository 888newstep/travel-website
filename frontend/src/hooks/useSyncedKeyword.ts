import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

interface UseSyncedKeywordOptions {
  key?: string
}

export function useSyncedKeyword(options: UseSyncedKeywordOptions = {}) {
  const { key = 'keyword' } = options
  const [searchParams, setSearchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(() => searchParams.get(key)?.trim() || '')
  const normalizedKeyword = useMemo(() => keyword.trim(), [keyword])

  useEffect(() => {
    const nextKeyword = searchParams.get(key)?.trim() || ''
    setKeyword((current) => (current === nextKeyword ? current : nextKeyword))
  }, [key, searchParams])

  useEffect(() => {
    const currentKeyword = searchParams.get(key)?.trim() || ''
    if (currentKeyword === normalizedKeyword) {
      return
    }

    const nextParams = new URLSearchParams(searchParams)
    if (normalizedKeyword) {
      nextParams.set(key, normalizedKeyword)
    } else {
      nextParams.delete(key)
    }

    setSearchParams(nextParams, { replace: true })
  }, [key, normalizedKeyword, searchParams, setSearchParams])

  return {
    keyword,
    setKeyword,
    normalizedKeyword,
  }
}
