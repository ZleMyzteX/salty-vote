<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { getVoteSubmissionApi, getEnrichedVoteApi, handleApiError } from '$lib/apiHelpers';
	import { isAuthenticated, clearToken } from '$lib/auth';
	import AirbnbExternalData from '$lib/AirbnbExternalData.svelte';
	import type { VoteDetailedResultsDto, VoteWithEnrichedAirbnbOptionsDto } from '../../../../generated/models';

	// Get vote ID from URL
	$: voteId = parseInt($page.params.id);

	// State
	let results: VoteDetailedResultsDto | null = null;
	let vote: VoteWithEnrichedAirbnbOptionsDto | null = null;
	let loading = true;
	let error = '';
	let showDetailedResults = false;

	onMount(() => {
		if (!isAuthenticated()) {
			goto('/login');
			return;
		}
		loadResults();
	});

	async function loadResults() {
		loading = true;
		error = '';

		try {
			const submissionApi = getVoteSubmissionApi();
			const enrichedVoteApi = getEnrichedVoteApi();

			// Load both results and vote details
			const [resultsData, voteData] = await Promise.all([
				submissionApi.getDetailedVoteResults({ voteId }),
				enrichedVoteApi.getEnrichedAirbnbVote({ voteId })
			]);

			results = resultsData;
			vote = voteData;

		} catch (err) {
			if (err instanceof Response && err.status === 401) {
				clearToken();
				goto('/login');
				return;
			}
			error = await handleApiError(err);
		} finally {
			loading = false;
		}
	}

	function handleBack() {
		goto(`/votes/${voteId}`);
	}

	function getOptionById(optionId: number) {
		return vote?.options.find(o => o.id === optionId);
	}
</script>

<div class="min-h-screen bg-gray-900 py-8">
	<div class="mx-auto max-w-6xl px-4">
		{#if loading}
			<div class="flex items-center justify-center py-20">
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
					<p class="mt-4 text-gray-400">Loading results...</p>
				</div>
			</div>
		{:else if error}
			<div class="rounded-lg border border-red-800 bg-red-900/20 p-6 text-center">
				<p class="text-red-400">{error}</p>
				<button
					on:click={handleBack}
					class="mt-4 rounded-lg bg-gray-700 px-4 py-2 text-white hover:bg-gray-600"
				>
					Go Back
				</button>
			</div>
		{:else if results}
			<!-- Header -->
			<div class="mb-8">
				<div class="mb-4 flex items-center gap-2">
					<button
						on:click={handleBack}
						class="text-gray-400 transition-colors hover:text-white"
					>
						← Back to Vote
					</button>
					<button
						on:click={loadResults}
						disabled={loading}
						class="rounded-lg border border-blue-600 bg-blue-600/20 px-3 py-1.5 text-sm font-semibold text-blue-300 transition-colors duration-200 hover:bg-blue-600/30 disabled:opacity-50"
						title="Reload results"
					>
						{loading ? '↻ Reloading...' : '↻ Reload'}
					</button>
				</div>

				<div>
					<h1 class="text-3xl font-bold text-white">{results.title}</h1>
					<p class="mt-2 text-gray-400">
						{results.totalSubmissions} {results.totalSubmissions === 1 ? 'vote' : 'votes'} submitted
					</p>
				</div>
			</div>

			<!-- Results Overview -->
			<div class="mb-8 rounded-lg bg-gray-800 p-8 shadow-xl">
				<div class="mb-6 flex items-center justify-between">
					<h2 class="text-2xl font-semibold text-white">Results Overview</h2>
					{#if vote?.isCreator || vote?.isCollaborator}
						<button
							on:click={() => showDetailedResults = !showDetailedResults}
							class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
						>
							{showDetailedResults ? 'Hide Details' : 'Show Details'}
						</button>
					{/if}
				</div>

				{#if results.totalSubmissions === 0}
					<div class="rounded-lg border border-yellow-800 bg-yellow-900/20 p-6 text-center">
						<p class="text-yellow-400">No votes have been submitted yet.</p>
					</div>
				{:else}
					<div class="space-y-4">
						{#each results.results as result, index}
							{@const option = getOptionById(result.optionId)}
							<div class="rounded-lg border border-gray-600 bg-gray-700/30 p-6">
								<div class="mb-3 flex items-start justify-between">
									<div class="flex items-start gap-3">
										<div class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-blue-600 font-bold text-white">
											#{index + 1}
										</div>
										<div>
											<h3 class="text-xl font-semibold text-white">{result.label}</h3>
											{#if option?.data}
												<div class="mt-2 space-y-1">
													{#if option.data.description}
														<p class="text-sm text-gray-400">{option.data.description}</p>
													{/if}
													{#if option.data.benefits}
														<p class="text-sm text-green-400">✓ {option.data.benefits}</p>
													{/if}
													{#if option.data.disadvantages}
														<p class="text-sm text-red-400">✗ {option.data.disadvantages}</p>
													{/if}
													<div class="flex flex-wrap gap-3 text-sm text-gray-400">
														{#if option.data.country}
															<span>📍 {option.data.country}</span>
														{/if}
														{#if option.data.travelTime > 0}
															<span>⏱️ {option.data.travelTime} hours</span>
														{/if}
														{#if option.data.airbnbPrice > 0}
															<span>💰 €{option.data.airbnbPrice.toFixed(2)} (Airbnb)</span>
														{/if}
														{#if option.data.totalPrice > 0}
															<span>💵 €{option.data.totalPrice.toFixed(2)} (Total/Person)</span>
														{/if}
														{#if option.data.flightNeeded}
															<span>✈️ Flight</span>
														{:else}
															<span>🚗 Car</span>
														{/if}
													</div>
												</div>
											{/if}
											<!-- External Airbnb Data -->
											{#if option}
												<AirbnbExternalData externalData={option.externalData} optionId={result.optionId} />
											{/if}
										</div>
									</div>
									<div class="text-right">
										<div class="text-2xl font-bold text-white">{result.voteCount}</div>
										<div class="text-sm text-gray-400">
											{result.percentage.toFixed(1)}%
										</div>
										{#if result.averageRank !== undefined && result.averageRank !== null}
											<div class="mt-1 text-xs text-gray-500">
												Avg rank: {result.averageRank.toFixed(2)}
											</div>
										{/if}
									</div>
								</div>

								<!-- Progress bar -->
								<div class="mt-4 h-3 overflow-hidden rounded-full bg-gray-600">
									<div
										class="h-full rounded-full bg-gradient-to-r from-blue-500 to-blue-600 transition-all duration-500"
										style="width: {result.percentage}%"
									></div>
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Detailed Results (only for creators and collaborators) -->
			{#if showDetailedResults && results.individualVoters.length > 0}
				<div class="rounded-lg bg-gray-800 p-8 shadow-xl">
					<h2 class="mb-6 text-2xl font-semibold text-white">Individual Votes</h2>

					<div class="space-y-6">
						{#each results.individualVoters as voter}
							<div class="rounded-lg border border-gray-600 bg-gray-700/30 p-4">
								<div class="mb-3 flex items-center justify-between">
									<div>
										<p class="font-medium text-white">{voter.username}</p>
										<p class="text-sm text-gray-400">{voter.email}</p>
									</div>
									<p class="text-sm text-gray-400">
										{new Date(voter.submittedAt).toLocaleDateString()} {new Date(voter.submittedAt).toLocaleTimeString()}
									</p>
								</div>

								{#if results.voteType === 'RANKING'}
									<!-- Show rankings -->
									<div class="space-y-2">
										{#each voter.rankings.filter(r => r.rank !== undefined).sort((a, b) => (a.rank || 0) - (b.rank || 0)) as ranking}
											<div class="flex items-center gap-3 text-sm">
												<span class="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">
													{ranking.rank}
												</span>
												<span class="text-gray-300">{ranking.optionLabel}</span>
											</div>
										{/each}
									</div>
								{:else}
									<!-- Show selections -->
									<div class="flex flex-wrap gap-2">
										{#each voter.rankings.filter(r => r.selected) as ranking}
											<span class="rounded-full bg-blue-600 px-3 py-1 text-sm text-white">
												{ranking.optionLabel}
											</span>
										{/each}
									</div>
								{/if}
							</div>
						{/each}
					</div>
				</div>
			{/if}
		{/if}
	</div>
</div>

