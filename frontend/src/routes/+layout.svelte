<script lang="ts">
	import '../app.css';
	import favicon from '$lib/assets/favicon.svg';
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import { isAuthenticated, startTokenRefresh, stopTokenRefresh, clearToken } from '$lib/auth';
	import { getAuthApi } from '$lib/apiHelpers';

	let { children } = $props();

	onMount(() => {
		// Start token refresh if user is authenticated
		if (isAuthenticated()) {
			const authApi = getAuthApi();

			startTokenRefresh(
				// Refresh function
				async () => {
					return await authApi.refreshToken();
				},
				// On failure callback - log out and redirect
				() => {
					clearToken();
					goto('/login');
				}
			);
		}
	});

	onDestroy(() => {
		// Clean up the refresh interval when component is destroyed
		stopTokenRefresh();
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{@render children?.()}

