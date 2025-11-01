/**
 * Helper utilities for making authenticated API calls
 * Provides wrapper functions that inject Bearer tokens into requests
 */

import { Configuration } from '../generated/runtime';
import {
	AuthControllerApi,
	UserControllerApi,
	VoteCreationControllerApi,
	VoteSubmissionControllerApi,
	VoteCollaboratorControllerApi,
	EnrichedVoteControllerApi
} from '../generated/apis';
import { getToken } from './auth';

/**
 * Get the API base URL from environment variables
 * Falls back to '/api' for production (when Traefik is routing)
 */
export function getApiBaseUrl(): string {
	return import.meta.env.VITE_API_BASE_URL || '';
}

/**
 * Creates API configuration with authentication headers
 */
function getAuthConfig(): Configuration {
	const token = getToken();
	const apiBaseUrl = getApiBaseUrl();

	return new Configuration({
		basePath: apiBaseUrl,
		headers: token ? {
			'Authorization': `Bearer ${token}`
		} : {}
	});
}

/**
 * Creates an authenticated instance of AuthControllerApi
 */
export function getAuthApi(): AuthControllerApi {
	return new AuthControllerApi(getAuthConfig());
}

/**
 * Creates an authenticated instance of UserControllerApi
 */
export function getUserApi(): UserControllerApi {
	return new UserControllerApi(getAuthConfig());
}

/**
 * Creates an authenticated instance of VoteCreationControllerApi
 */
export function getVoteApi(): VoteCreationControllerApi {
	return new VoteCreationControllerApi(getAuthConfig());
}

/**
 * Creates an authenticated instance of VoteSubmissionControllerApi
 */
export function getVoteSubmissionApi(): VoteSubmissionControllerApi {
	return new VoteSubmissionControllerApi(getAuthConfig());
}

/**
 * Creates an authenticated instance of VoteCollaboratorControllerApi
 */
export function getVoteCollaboratorApi(): VoteCollaboratorControllerApi {
	return new VoteCollaboratorControllerApi(getAuthConfig());
}

/**
 * Creates an authenticated instance of EnrichedVoteControllerApi
 */
export function getEnrichedVoteApi(): EnrichedVoteControllerApi {
	return new EnrichedVoteControllerApi(getAuthConfig());
}

/**
 * Generic error handler for API calls
 */
export async function handleApiError(error: unknown): Promise<string> {
	if (error instanceof Response) {
		try {
			const errorData = await error.json();
			return errorData.message || errorData.error || 'An error occurred';
		} catch {
			return error.statusText || 'An error occurred';
		}
	}

	if (error instanceof Error) {
		return error.message;
	}

	return 'An unexpected error occurred';
}

