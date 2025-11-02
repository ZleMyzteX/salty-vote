<script lang="ts">
	import { onMount } from 'svelte';
	import { isAuthenticated, clearToken } from '$lib/auth';
	import { getVoteApi, handleApiError } from '$lib/apiHelpers';
	import type { VoteListWithRelationshipDto } from '../generated/models';

	let authenticated = false;
	let votes: VoteListWithRelationshipDto[] = [];
	let loading = false;
	let error = '';

	onMount(async () => {
		authenticated = isAuthenticated();
		if (authenticated) {
			await loadVotes();
		}
	});

	async function loadVotes() {
		loading = true;
		error = '';

		try {
			const voteApi = getVoteApi();
			// Use the new efficient endpoint that returns votes with relationship status
			votes = await voteApi.getAllVotesWithRelationship();
		} catch (err) {
			if (err instanceof Response && err.status === 401) {
				clearToken();
				authenticated = false;
			} else {
				error = await handleApiError(err);
			}
		} finally {
			loading = false;
		}
	}

	function handleLogout() {
		clearToken();
		authenticated = false;
		votes = [];
	}
</script>

<div class="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
	<!-- Hero Section -->
	<div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
		<div class="text-center">
			<div class="mb-6 inline-flex items-center justify-center">
				<div class="text-7xl sm:text-8xl">🏠</div>
			</div>
			<h1 class="mb-4 bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-5xl font-extrabold text-transparent sm:text-6xl md:text-7xl">
				SaltyVote
			</h1>
			<p class="mx-auto mb-8 max-w-2xl text-lg text-gray-300 sm:text-xl">
				Compare Airbnb properties and make group decisions with ease.
				<span class="text-blue-400">Collaborate</span>,
				<span class="text-purple-400">vote</span>, and
				<span class="text-green-400">choose together</span>.
			</p>

			<!-- Action Buttons -->
			<div class="mb-16 flex flex-wrap justify-center gap-4">
				{#if authenticated}
                    <!-- TODO: Create disabled
					<a
						href="/create-vote"
						class="group relative inline-flex items-center gap-2 overflow-hidden rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-4 font-bold text-white shadow-lg transition-all duration-300 hover:scale-105 hover:shadow-blue-500/50"
					>
						<span class="relative z-10">+ Create New Vote</span>
						<svg class="relative z-10 h-5 w-5 transition-transform group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
						</svg>
					</a>
				    -->
				{:else}
					<a
						href="/login"
						class="group relative inline-flex items-center gap-2 overflow-hidden rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-4 font-bold text-white shadow-lg transition-all duration-300 hover:scale-105 hover:shadow-blue-500/50"
					>
						<span class="relative z-10">Login</span>
						<svg class="relative z-10 h-5 w-5 transition-transform group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
						</svg>
					</a>
					<a
						href="/register"
						class="inline-flex items-center gap-2 rounded-xl border-2 border-blue-500 bg-blue-500/10 px-8 py-4 font-bold text-blue-400 backdrop-blur-sm transition-all duration-300 hover:scale-105 hover:bg-blue-500/20"
					>
						<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
						</svg>
						Register
					</a>
				{/if}
			</div>
		</div>

		{#if authenticated}
			<!-- Votes Section -->
			<div class="mx-auto max-w-6xl">
				<div class="mb-8 flex items-center justify-between">
					<h2 class="text-3xl font-bold text-white">
						<span class="bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">Active Votes</span>
					</h2>
					{#if !loading && votes.length > 0}
						<div class="rounded-full bg-blue-600/20 px-4 py-2 text-sm font-semibold text-blue-400">
							{votes.length} {votes.length === 1 ? 'Vote' : 'Votes'}
						</div>
					{/if}
				</div>

				{#if loading}
					<div class="flex items-center justify-center py-24">
						<div class="text-center">
							<svg
								class="mx-auto h-12 w-12 animate-spin text-blue-500"
								xmlns="http://www.w3.org/2000/svg"
								fill="none"
								viewBox="0 0 24 24"
							>
								<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
								<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
							</svg>
							<p class="mt-4 text-gray-400">Loading votes...</p>
						</div>
					</div>
				{:else if error}
					<div class="rounded-2xl border-2 border-red-800 bg-red-900/20 p-8 text-center backdrop-blur-sm">
						<div class="mb-3 text-4xl">⚠️</div>
						<p class="text-lg font-semibold text-red-400">{error}</p>
					</div>
				{:else if votes.length === 0}
					<div class="rounded-2xl border-2 border-dashed border-gray-600 bg-gray-800/30 p-12 text-center backdrop-blur-sm">
						<div class="mb-4 text-6xl">🗳️</div>
						<h3 class="mb-3 text-2xl font-bold text-white">No votes yet</h3>
						<p class="mb-6 text-gray-400">Be the first to create a vote and start comparing Airbnb properties!</p>
						<a
							href="/create-vote"
							class="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-4 font-bold text-white shadow-lg transition-all duration-300 hover:scale-105"
						>
							<span>+ Create Your First Vote</span>
							<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
							</svg>
						</a>
					</div>
				{:else}
					<div class="flex justify-center">
						<div class="grid gap-6 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3 w-full max-w-7xl mx-auto">
							{#each votes as vote}
								<div
									class="group relative overflow-hidden rounded-2xl border-2 p-5 backdrop-blur-sm transition-all duration-300 hover:scale-[1.02] hover:shadow-2xl flex flex-col
										{vote.isCreator
											? 'border-blue-500 bg-gradient-to-br from-blue-900/40 to-blue-800/20 hover:shadow-blue-500/30'
											: vote.isCollaborator
												? 'border-purple-500 bg-gradient-to-br from-purple-900/40 to-purple-800/20 hover:shadow-purple-500/30'
												: 'border-gray-600 bg-gradient-to-br from-gray-800/60 to-gray-700/30 hover:border-gray-500 hover:shadow-gray-500/20'}"
								>
								<!-- Decorative gradient overlay -->
								<div class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100
									{vote.isCreator
										? 'bg-gradient-to-br from-blue-500/10 to-transparent'
										: vote.isCollaborator
											? 'bg-gradient-to-br from-purple-500/10 to-transparent'
											: 'bg-gradient-to-br from-gray-500/10 to-transparent'}"
								></div>

								<div class="relative z-10 flex flex-col h-full">
									<div class="mb-3 flex items-start justify-between gap-3">
										<h3 class="flex-1 text-xl font-bold leading-tight text-white">
											{vote.title}
										</h3>
										<div class="flex flex-col gap-1.5">
											{#if vote.isCreator}
												<span class="flex items-center gap-1 rounded-full bg-blue-600 px-2.5 py-1 text-xs font-bold text-white shadow-lg">
													<svg class="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
														<path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
													</svg>
													Creator
												</span>
											{/if}
											{#if vote.isCollaborator}
												<span class="flex items-center gap-1 rounded-full bg-purple-600 px-2.5 py-1 text-xs font-bold text-white shadow-lg">
													<svg class="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
														<path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z" />
													</svg>
													Collab
												</span>
											{/if}
										</div>
									</div>
									{#if vote.description}
										<p class="mb-4 line-clamp-3 text-base leading-relaxed text-gray-300">
											{vote.description}
										</p>
									{:else}
										<div class="mb-4"></div>
									{/if}
									<div class="mt-auto flex gap-3 w-full">
										<a
											href="/votes/{vote.id}"
											class="group/btn flex flex-1 items-center justify-center gap-2 rounded-lg border-2 border-gray-500 bg-gray-700/50 px-5 py-3.5 text-base font-bold text-white transition-all duration-200 hover:border-gray-400 hover:bg-gray-600"
										>
											<svg class="h-5 w-5 transition-transform group-hover/btn:scale-110" fill="none" stroke="currentColor" viewBox="0 0 24 24">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
											</svg>
											Add
										</a>
										<a
											href="/vote/{vote.id}"
											class="group/btn flex flex-1 items-center justify-center gap-2 rounded-lg bg-gradient-to-r from-green-600 to-green-700 px-5 py-3.5 text-base font-bold text-white shadow-md transition-all duration-200 hover:scale-105 hover:shadow-green-500/50"
										>
											<span>🗳️</span>
											Vote
										</a>
									</div>
								</div>
							</div>
							{/each}
						</div>
					</div>
				{/if}

				<!-- Logout Button -->
				<div class="mt-12 flex justify-center">
					<button
						on:click={handleLogout}
						class="inline-flex items-center gap-2 rounded-xl border-2 border-gray-600 bg-gray-800/50 px-8 py-4 font-bold text-white backdrop-blur-sm transition-all duration-300 hover:scale-105 hover:border-gray-500 hover:bg-gray-700/50"
					>
						<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
						</svg>
						Logout
					</button>
				</div>
			</div>
		{:else}
			<!-- Welcome section for non-authenticated users -->
			<div class="mx-auto max-w-4xl">
				<div class="rounded-3xl border-2 border-gray-700 bg-gradient-to-br from-gray-800/60 to-gray-900/60 p-12 backdrop-blur-sm">
					<div class="text-center">
						<div class="mb-6 text-6xl">✨</div>
						<h2 class="mb-4 text-3xl font-bold text-white">Welcome to SaltyVote</h2>
						<p class="mb-8 text-lg leading-relaxed text-gray-300">
							The easiest way to compare Airbnb properties and make group decisions.
							Invite friends, add properties, rank your favorites, and let the votes decide!
						</p>
						<div class="flex flex-wrap justify-center gap-4 text-left">
							<div class="flex items-start gap-3 rounded-xl bg-gray-700/50 p-4">
								<div class="text-2xl">🏠</div>
								<div>
									<div class="font-bold text-white">Compare Properties</div>
									<div class="text-sm text-gray-400">Side-by-side Airbnb comparison</div>
								</div>
							</div>
							<div class="flex items-start gap-3 rounded-xl bg-gray-700/50 p-4">
								<div class="text-2xl">🤝</div>
								<div>
									<div class="font-bold text-white">Collaborate</div>
									<div class="text-sm text-gray-400">Invite friends to help decide</div>
								</div>
							</div>
							<div class="flex items-start gap-3 rounded-xl bg-gray-700/50 p-4">
								<div class="text-2xl">📊</div>
								<div>
									<div class="font-bold text-white">Rank & Vote</div>
									<div class="text-sm text-gray-400">Weighted ranking system</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		{/if}
	</div>
</div>

