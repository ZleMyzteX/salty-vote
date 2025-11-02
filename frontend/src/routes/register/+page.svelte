<script lang="ts">
	import { goto } from '$app/navigation';
	import { getAuthApi, handleApiError } from '$lib/apiHelpers';
	import { setToken } from '$lib/auth';
	import type { UserCreateRequestDto } from '../../generated/models';

	// Form state
	let username = '';
	let password = '';
	let confirmPassword = '';

	// UI state
	let loading = false;
	let error = '';
	let success = false;

	/**
	 * Validates the registration form
	 */
	function validateForm(): boolean {
		error = '';

		if (!username || !password || !confirmPassword) {
			error = 'All fields are required';
			return false;
		}

		if (password !== confirmPassword) {
			error = 'Passwords do not match';
			return false;
		}

		if (password.length < 6) {
			error = 'Password must be at least 6 characters';
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

            const email = "not-used-currently"
			const userCreateRequest: UserCreateRequestDto = {
				email,
				username,
				password
			};

			// Call the registration endpoint - it now returns a token
			const token = await authApi.registerNewUser({
				userCreateRequestDto: userCreateRequest
			});

			// Store the token
			setToken(token);

			success = true;

			// Redirect to homepage after successful registration
			setTimeout(() => {
				goto('/');
			}, 150);

		} catch (err) {
			error = await handleApiError(err);
		} finally {
			loading = false;
		}
	}
</script>

<div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
	<div class="mx-4 w-full max-w-md">
		<div class="rounded-2xl border-2 border-gray-700 bg-gradient-to-br from-gray-800/80 to-gray-900/80 p-8 shadow-2xl backdrop-blur-sm">
			<div class="mb-8 text-center">
				<div class="mb-4 inline-flex items-center justify-center">
					<div class="text-5xl">🏠</div>
				</div>
				<h1 class="mb-2 bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-4xl font-extrabold text-transparent">
					SaltyVote
				</h1>
				<p class="text-gray-300">Create a new account</p>
			</div>

			{#if success}
				<div
					class="mb-6 rounded-xl border-2 border-green-800 bg-green-900/30 p-4 text-center backdrop-blur-sm"
				>
					<div class="mb-2 text-3xl">✅</div>
					<p class="text-sm font-semibold text-green-400">Registration successful! Logging you in...</p>
				</div>
			{/if}

			<form on:submit={handleSubmit} class="space-y-6">
				<div>
					<label
						for="username"
						class="mb-2 block text-sm font-semibold text-gray-300"
					>
						Username
					</label>
					<input
						id="username"
						type="text"
						bind:value={username}
						placeholder="Choose username..."
						disabled={loading || success}
						class="w-full rounded-xl border-2 border-gray-600 bg-gray-700/50 px-4 py-3 text-white placeholder-gray-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 disabled:opacity-50"
					/>
				</div>

				<div>
					<label
						for="password"
						class="mb-2 block text-sm font-semibold text-gray-300"
					>
						Password
					</label>
					<input
						id="password"
						type="password"
						bind:value={password}
						placeholder="Create password..."
						disabled={loading || success}
						class="w-full rounded-xl border-2 border-gray-600 bg-gray-700/50 px-4 py-3 text-white placeholder-gray-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 disabled:opacity-50"
					/>
				</div>

				<div>
					<label
						for="confirmPassword"
						class="mb-2 block text-sm font-semibold text-gray-300"
					>
						Confirm Password
					</label>
					<input
						id="confirmPassword"
						type="password"
						bind:value={confirmPassword}
						placeholder="Confirm password..."
						disabled={loading || success}
						class="w-full rounded-xl border-2 border-gray-600 bg-gray-700/50 px-4 py-3 text-white placeholder-gray-400 transition-all focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 disabled:opacity-50"
					/>
				</div>

				{#if error}
					<div
						class="rounded-xl border-2 border-red-800 bg-red-900/30 p-4 text-center backdrop-blur-sm"
					>
						<div class="mb-2 text-2xl">⚠️</div>
						<p class="text-sm font-semibold text-red-400">{error}</p>
					</div>
				{/if}

				<button
					type="submit"
					disabled={loading || success || !username.trim() || !password.trim() || !confirmPassword.trim()}
					class="w-full rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-4 py-3 font-bold text-white shadow-lg transition-all duration-300 hover:scale-105 hover:shadow-blue-500/50 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:scale-100"
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
							Registering...
						</span>
					{:else}
						Create Account
					{/if}
				</button>
			</form>

			<div class="mt-6 text-center">
				<p class="text-sm text-gray-400">
					Already have an account?
					<a
						href="/login"
						class="ml-1 font-semibold text-blue-400 transition-colors hover:text-blue-300"
					>
						Login here
					</a>
				</p>
			</div>
		</div>
	</div>
</div>

