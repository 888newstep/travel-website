type ToastType = 'info' | 'success' | 'warning' | 'error'

export interface Toast {
  id: string
  type: ToastType
  message: string
  duration?: number
}

type ToastListener = (toasts: Toast[]) => void

let toasts: Toast[] = []
let listener: ToastListener | null = null

function generateId(): string {
  return Math.random().toString(36).substring(2, 9)
}

function notify() {
  if (listener) {
    listener([...toasts])
  }
}

export function showToast(type: ToastType, message: string, duration: number = 4000) {
  const id = generateId()
  const toast: Toast = { id, type, message, duration }
  toasts = [...toasts, toast]
  notify()

  if (duration > 0) {
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }

  return id
}

export function removeToast(id: string) {
  toasts = toasts.filter((t) => t.id !== id)
  notify()
}

export function subscribeToToasts(callback: ToastListener): () => void {
  listener = callback
  callback([...toasts])

  return () => {
    listener = null
  }
}

export function showSuccess(message: string) {
  return showToast('success', message)
}

export function showError(message: string) {
  return showToast('error', message, 6000)
}

export function showWarning(message: string) {
  return showToast('warning', message, 5000)
}

export function showInfo(message: string) {
  return showToast('info', message)
}
