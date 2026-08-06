import { useEffect, useRef } from 'react'

interface UseDebouncedKeywordEffectOptions {
  keyword: string
  delay: number
  immediateFirstRun?: boolean
  onKeywordChange: (keyword: string) => void | Promise<void>
}

export function useDebouncedKeywordEffect({
  keyword,
  delay,
  immediateFirstRun = true,
  onKeywordChange,
}: UseDebouncedKeywordEffectOptions) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const hasRunRef = useRef(false)

  useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current)
    }

    const run = () => {
      void onKeywordChange(keyword)
    }

    if (!hasRunRef.current && immediateFirstRun) {
      hasRunRef.current = true
      run()

      return () => {
        if (timerRef.current) {
          clearTimeout(timerRef.current)
        }
      }
    }

    hasRunRef.current = true
    timerRef.current = setTimeout(run, delay)

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
      }
    }
  }, [delay, immediateFirstRun, keyword, onKeywordChange])
}
