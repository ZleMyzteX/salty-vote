<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { getAuthApi, handleApiError } from '$lib/apiHelpers';
	import { setToken, isAuthenticated } from '$lib/auth';
	import type { UserLoginRequestDto } from '../../generated/models';

	// Form state
	let username = '';
	let password = '';

	// UI state
	let loading = false;
	let error = '';

	/**
	 * Redirect if already authenticated
	 */
	onMount(() => {
		if (isAuthenticated()) {
			goto('/');
		}
	});

	/**
	 * Validates the login form
	 */
	function validateForm(): boolean {
		error = '';

		if (!username || !password) {
			error = 'Username and password are required';
			return false;
		}

		return true;
	}

	/**
	 * Handles form submission
	 */
	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!validateForm()) {
			return;
		}

		loading = true;
		error = '';

		try {
			const authApi = getAuthApi();

			const loginRequest: UserLoginRequestDto = {
				username,
				password
			};

			// Call the login endpoint
			const token = await authApi.login({
				userLoginRequestDto: loginRequest
			});

			// Store the token
			setToken(token);

			// Redirect to homepage
			goto('/');

		} catch (err) {
			error = await handleApiError(err);
		} finally {
			loading = false;
		}
	}
</script>

<div class="flex min-h-screen items-center justify-center bg-gray-900">
	<div class="mx-4 w-full max-w-md">
		<div class="rounded-lg bg-gray-800 p-8 shadow-xl">
			<div class="mb-8 text-center">
				<h1 class="mb-2 text-3xl font-bold text-white">🗳️ SaltyVote</h1>
				<p class="text-gray-400">Login to your account</p>
			</div>

			<form on:submit={handleSubmit} class="space-y-6">
				<div>
					<label
						for="username"
						class="mb-2 block text-sm font-medium text-gray-300"
					>
						Username
					</label>
					<input
						id="username"
						type="text"
						bind:value={username}
						placeholder="Enter username..."
						disabled={loading}
						class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
					/>
				</div>

				<div>
					<label
						for="password"
						class="mb-2 block text-sm font-medium text-gray-300"
					>
						Password
					</label>
					<input
						id="password"
						type="password"
						bind:value={password}
						placeholder="Enter password..."
						disabled={loading}
						class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
					/>
				</div>

				{#if error}
					<div
						class="rounded-lg border border-red-800 bg-red-900/20 p-3 text-center text-sm text-red-400"
					>
						{error}
					</div>
				{/if}

				<button
					type="submit"
					disabled={loading || !username.trim() || !password.trim()}
					class="w-full rounded-lg bg-blue-600 px-4 py-3 font-semibold text-white transition-colors duration-200 hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-600"
				>
					{#if loading}
						<span class="inline-flex items-center">
							<svg
								class="-ml-1 mr-3 h-5 w-5 animate-spin text-white"
								xmlns="http://www.w3.org/2000/svg"
								fill="none"
								viewBox="0 0 24 24"
							>
								<circle
									class="opacity-25"
									cx="12"
									cy="12"
									r="10"
									stroke="currentColor"
									stroke-width="4"
								></circle>
								<path
									class="opacity-75"
									fill="currentColor"
									d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
								></path>
							</svg>
							Logging in...
						</span>
					{:else}
						Login
					{/if}
				</button>
			</form>

			<div class="mt-6 text-center">
				<p class="text-sm text-gray-500">
					Don't have an account?
					<a
						href="/register"
						class="ml-1 text-blue-400 hover:text-blue-300"
					>
						Register here
					</a>
				</p>
			</div>
		</div>
	</div>
</div>

