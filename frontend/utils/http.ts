/** Best-effort human message from an ofetch/$fetch error (which carries the API's ApiError body). */
export function errorMessage(e: unknown): string {
  if (e && typeof e === 'object') {
    const err = e as { data?: { message?: string }; statusMessage?: string; message?: string }
    if (err.data?.message) return err.data.message
    if (err.statusMessage) return err.statusMessage
    if (err.message) return err.message
  }
  return 'Something went wrong — is the API running at the configured base URL?'
}
