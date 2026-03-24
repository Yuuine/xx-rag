export function encodeUtf8Base64(str: string): string {
  return btoa(String.fromCharCode(...new TextEncoder().encode(str)))
}

export function decodeUtf8Base64(b64: string): string {
  const bytes = Uint8Array.from(atob(b64), c => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export function handleCopyClick(
  e: MouseEvent,
  showToast: (text: string, type?: 'success' | 'error') => void
): void {
  const target = e.target as HTMLElement | null
  const btn = target?.closest?.('.copy-btn') as HTMLButtonElement | null
  if (!btn) return
  const encoded = btn.getAttribute('data-code')
  if (!encoded) return

  const codeText = decodeUtf8Base64(encoded)
  navigator.clipboard
    .writeText(codeText)
    .then(() => {
      const old = btn.textContent || '复制'
      btn.textContent = '已复制'
      btn.classList.add('copied')
      window.setTimeout(() => {
        btn.textContent = old
        btn.classList.remove('copied')
      }, 2000)
      showToast('已复制', 'success')
    })
    .catch(() => {
      showToast('复制失败', 'error')
    })
}
