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

<div class="flex min-h-screen items-start justify-center bg-gray-900 py-8">
	<div class="mx-4 w-full max-w-6xl">
		<div class="rounded-lg bg-gray-800 p-8 shadow-xl">
			<div class="mb-8 text-center">
				<h1 class="mb-2 text-4xl font-bold text-white">🏠 SaltyVote</h1>
				<p class="text-gray-400">
					Create and participate in Airbnb votes with ease
				</p>
			</div>

			<div class="mb-8 flex justify-center gap-4">
				{#if authenticated}
					<a
						href="/create-vote"
						class="rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
					>
						+ Create Vote
					</a>
					<button
						on:click={handleLogout}
						class="rounded-lg border border-gray-600 bg-gray-700 px-6 py-3 font-semibold text-white transition-colors duration-200 hover:bg-gray-600"
					>
						Logout
					</button>
				{:else}
					<a
						href="/login"
						class="rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
					>
						Login
					</a>
					<a
						href="/register"
						class="rounded-lg border border-gray-600 bg-gray-700 px-6 py-3 font-semibold text-white transition-colors duration-200 hover:bg-gray-600"
					>
						Register
					</a>
				{/if}
			</div>

			{#if authenticated}
				<!-- All Votes Section -->
				<div class="mb-8">
					<h2 class="mb-4 text-2xl font-semibold text-white">All Votes</h2>

					{#if loading}
						<div class="flex items-center justify-center py-12">
							<svg
								class="h-8 w-8 animate-spin text-blue-500"
								xmlns="http://www.w3.org/2000/svg"
								fill="none"
								viewBox="0 0 24 24"
							>
								<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
								<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
							</svg>
						</div>
					{:else if error}
						<div class="rounded-lg border border-red-800 bg-red-900/20 p-4 text-center">
							<p class="text-red-400">{error}</p>
						</div>
					{:else if votes.length === 0}
						<div class="rounded-lg border border-gray-600 bg-gray-700/30 p-8 text-center">
							<p class="text-gray-400">No votes available at the moment.</p>
							<a
								href="/create-vote"
								class="mt-4 inline-block rounded-lg bg-blue-600 px-6 py-2 font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
							>
								Create a Vote
							</a>
						</div>
					{:else}
						<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
							{#each votes as vote}
								<div
									class="group rounded-lg border p-6 transition-all duration-200
										{vote.isCreator
											? 'border-blue-500 bg-blue-900/20'
											: vote.isCollaborator
												? 'border-purple-500 bg-purple-900/20'
												: 'border-gray-600 bg-gray-700/30'}"
								>
									<div class="mb-3 flex items-start justify-between gap-2">
										<h3 class="flex-1 text-lg font-semibold text-white">
											{vote.title}
										</h3>
										<div class="flex flex-col gap-1">
											{#if vote.isCreator}
												<span class="rounded-full bg-blue-600 px-2 py-0.5 text-xs font-medium text-white">
													Creator
												</span>
											{/if}
											{#if vote.isCollaborator}
												<span class="rounded-full bg-purple-600 px-2 py-0.5 text-xs font-medium text-white">
													Collaborator
												</span>
											{/if}
										</div>
									</div>
									{#if vote.description}
										<p class="line-clamp-2 text-sm text-gray-400 mb-4">
											{vote.description}
										</p>
									{/if}
									<div class="mt-auto flex gap-2">
										<a
											href="/votes/{vote.id}"
											class="flex-1 rounded-lg border border-gray-500 bg-gray-700 px-4 py-2 text-center text-sm font-semibold text-white transition-colors duration-200 hover:bg-gray-600 hover:border-gray-400"
										>
                                            + Add
										</a>
										<a
											href="/vote/{vote.id}"
											class="flex-1 rounded-lg bg-green-600 px-4 py-2 text-center text-sm font-semibold text-white transition-colors duration-200 hover:bg-green-700"
										>
											🗳️ Vote
										</a>
									</div>
								</div>
							{/each}
						</div>
					{/if}
				</div>
			{/if}

			<div class="grid gap-6 md:grid-cols-3">
				<div class="rounded-lg bg-gray-700/50 p-6">
					<div class="mb-3 text-3xl">🏠</div>
					<h3 class="mb-2 text-xl font-semibold text-white">Airbnb Comparison</h3>
					<p class="text-sm text-gray-400">
						Compare vacation rentals with detailed pricing and location info
					</p>
				</div>
				<div class="rounded-lg bg-gray-700/50 p-6">
					<div class="mb-3 text-3xl">🤝</div>
					<h3 class="mb-2 text-xl font-semibold text-white">Collaborate</h3>
					<p class="text-sm text-gray-400">
						Invite collaborators to add options and help make decisions together
					</p>
				</div>
				<div class="rounded-lg bg-gray-700/50 p-6">
					<div class="mb-3 text-3xl">📊</div>
					<h3 class="mb-2 text-xl font-semibold text-white">Ranking System</h3>
					<p class="text-sm text-gray-400">
						Use ranking votes to get weighted results or simple yes/no votes
					</p>
				</div>
			</div>
		</div>
	</div>
</div>

