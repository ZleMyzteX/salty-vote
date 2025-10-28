/**
 * Authentication utility for managing JWT tokens
 * Uses localStorage for client-side token storage
 * Note: For SSR-safe implementation, consider using cookies instead
 */

const TOKEN_KEY = 'salty_vote_auth_token';
const REFRESH_INTERVAL = 60000; // 1 minute in milliseconds

let refreshIntervalId: ReturnType<typeof setInterval> | null = null;

/**
 * Retrieves the stored authentication token
 */
export function getToken(): string | null {
	if (typeof globalThis.window === 'undefined') return null;
	return localStorage.getItem(TOKEN_KEY);
}

/**
 * Stores the authentication token
 */
export function setToken(token: string): void {
	if (typeof globalThis.window === 'undefined') return;
	localStorage.setItem(TOKEN_KEY, token);
}

/**
 * Clears the stored authentication token
 */
export function clearToken(): void {
	if (typeof globalThis.window === 'undefined') return;
	localStorage.removeItem(TOKEN_KEY);
	stopTokenRefresh();
}

/**
 * Checks if user is authenticated
 */
export function isAuthenticated(): boolean {
	return getToken() !== null;
}

/**
 * Starts the periodic token refresh
 * Calls the provided refresh function every minute
 * If refresh fails, calls the onFailure callback
 */
export function startTokenRefresh(
	refreshFn: () => Promise<string>,
	onFailure: () => void
): void {
	// Clear any existing interval
	stopTokenRefresh();

	// Set up periodic refresh
	refreshIntervalId = setInterval(async () => {
		try {
			const newToken = await refreshFn();
			setToken(newToken);
		} catch (error) {
			console.error('Token refresh failed:', error);
			onFailure();
		}
	}, REFRESH_INTERVAL);
}

/**
 * Stops the periodic token refresh
 */
export function stopTokenRefresh(): void {
	if (refreshIntervalId !== null) {
		clearInterval(refreshIntervalId);
		refreshIntervalId = null;
	}
}

