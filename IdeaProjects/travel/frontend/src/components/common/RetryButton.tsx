interface RetryButtonProps {
  onRetry: () => void
  loading?: boolean
  label?: string
}

export function RetryButton({ onRetry, loading, label = '重试' }: RetryButtonProps) {
  return (
    <button
      type="button"
      onClick={onRetry}
      disabled={loading}
      className="btn-secondary disabled:cursor-not-allowed disabled:opacity-60"
    >
      {loading ? '处理中...' : label}
    </button>
  )
}
