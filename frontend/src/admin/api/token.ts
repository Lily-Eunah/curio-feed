// Admin token storage + header helper.
// Stored in sessionStorage so it clears when the tab closes (admin sessions are short-lived).

const STORAGE_KEY = 'curio_admin_token';

export function getAdminToken(): string {
  try {
    return sessionStorage.getItem(STORAGE_KEY) ?? '';
  } catch {
    return '';
  }
}

export function setAdminToken(token: string): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, token.trim());
  } catch {
    // ignore storage errors
  }
}

export function clearAdminToken(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // ignore storage errors
  }
}

/** Headers to attach to admin API requests. Empty object when no token is set. */
export function adminAuthHeaders(): Record<string, string> {
  const token = getAdminToken();
  return token ? { 'X-Admin-Token': token } : {};
}

/** Event name dispatched on window when an admin request is rejected with 401. */
export const ADMIN_UNAUTHORIZED_EVENT = 'admin-unauthorized';

export function notifyAdminUnauthorized(): void {
  clearAdminToken();
  window.dispatchEvent(new Event(ADMIN_UNAUTHORIZED_EVENT));
}
