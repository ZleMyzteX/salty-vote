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

<div class="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 py-4 sm:py-8">
	<div class="mx-auto max-w-6xl px-3 sm:px-4">
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
			<div class="rounded-2xl border-2 border-red-800 bg-red-900/20 p-4 sm:p-6 text-center backdrop-blur-sm">
				<div class="mb-3 text-3xl sm:text-4xl">⚠️</div>
				<p class="text-sm sm:text-base font-semibold text-red-400">{error}</p>
				<button
					on:click={handleBack}
					class="mt-4 rounded-xl bg-gray-700 px-4 py-2 text-sm sm:text-base text-white transition-all hover:bg-gray-600"
				>
					Go Back
				</button>
			</div>
		{:else if results}
			<!-- Header -->
			<div class="mb-6 sm:mb-8">
				<div class="mb-3 sm:mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-2">
					<button
						on:click={handleBack}
						class="text-gray-300 transition-colors hover:text-white text-sm sm:text-base"
					>
						← Back to Vote
					</button>
					<button
						on:click={loadResults}
						disabled={loading}
						class="rounded-xl border-2 border-blue-600 bg-blue-600/20 px-3 py-1.5 text-xs sm:text-sm font-semibold text-blue-300 backdrop-blur-sm transition-all duration-300 hover:bg-blue-600/30 disabled:opacity-50 self-start"
						title="Reload results"
					>
						{loading ? '↻ Reloading...' : '↻ Reload'}
					</button>
				</div>

				<div>
					<h1 class="bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-2xl sm:text-3xl font-extrabold text-transparent">{results.title}</h1>
					<p class="mt-2 text-sm sm:text-base text-gray-300">
						🗳️ {results.totalSubmissions} {results.totalSubmissions === 1 ? 'vote' : 'votes'} submitted
					</p>
				</div>
			</div>

			<!-- Results Overview -->
			<div class="relative mb-6 sm:mb-8 rounded-2xl border-2 border-gray-700 bg-gradient-to-br from-gray-800/80 to-gray-900/80 p-4 sm:p-6 md:p-8 shadow-2xl backdrop-blur-sm">
				<div class="relative z-20 mb-4 sm:mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
					<h2 class="text-xl sm:text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
                        Results Overview
					</h2>
					<button
						on:click={() => showDetailedResults = !showDetailedResults}
						class="rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-4 py-2 sm:py-2.5 text-sm font-bold text-white shadow-lg transition-all duration-300 hover:scale-105 hover:shadow-blue-500/50 self-start cursor-pointer"
					>
						{showDetailedResults ? '👁️ Hide Details' : '👁️ Show Details'}
					</button>
				</div>

				{#if results.totalSubmissions === 0}
					<div class="rounded-xl border-2 border-yellow-800 bg-yellow-900/20 p-4 sm:p-6 text-center backdrop-blur-sm">
						<div class="mb-2 text-3xl sm:text-4xl">📭</div>
						<p class="text-sm sm:text-base text-yellow-400 font-semibold">No votes have been submitted yet.</p>
					</div>
				{:else}
					<div class="space-y-3 sm:space-y-4">
						{#each results.results as result, index}
							{@const option = getOptionById(result.optionId)}
							<div class="group rounded-xl border-2 transition-all duration-300 hover:scale-[1.01] hover:shadow-xl p-3 sm:p-4 md:p-5 {index === 0 ? 'border-blue-500 bg-gradient-to-br from-blue-900/40 to-blue-800/20 hover:shadow-blue-500/30' : index === 1 ? 'border-purple-500 bg-gradient-to-br from-purple-900/40 to-purple-800/20 hover:shadow-purple-500/30' : index === 2 ? 'border-green-500 bg-gradient-to-br from-green-900/40 to-green-800/20 hover:shadow-green-500/30' : 'border-gray-600 bg-gradient-to-br from-gray-700/50 to-gray-800/50 hover:border-gray-500 hover:shadow-gray-500/20'}">
								<!-- Decorative gradient overlay -->
								<div class="absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100 rounded-xl pointer-events-none {index === 0 ? 'bg-gradient-to-br from-blue-500/10 to-transparent' : index === 1 ? 'bg-gradient-to-br from-purple-500/10 to-transparent' : index === 2 ? 'bg-gradient-to-br from-green-500/10 to-transparent' : 'bg-gradient-to-br from-gray-500/10 to-transparent'}"></div>

								<div class="relative z-10">
									<div class="mb-3 flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
										<div class="flex items-start gap-2 sm:gap-3 flex-1 min-w-0">
											<div class="flex h-8 w-8 sm:h-10 sm:w-10 flex-shrink-0 items-center justify-center rounded-full font-bold text-white shadow-lg text-sm sm:text-base {index === 0 ? 'bg-gradient-to-br from-blue-500 to-blue-600' : index === 1 ? 'bg-gradient-to-br from-purple-500 to-purple-600' : index === 2 ? 'bg-gradient-to-br from-green-500 to-green-600' : 'bg-gradient-to-br from-gray-500 to-gray-600'}">
												#{index + 1}
											</div>
											<div class="flex-1 min-w-0">
												<h3 class="text-base sm:text-lg font-bold text-white break-words">{result.label}</h3>
												{#if option?.data}
													<div class="mt-1 sm:mt-2 space-y-1">
														{#if option.data.description}
															<p class="text-xs sm:text-sm text-gray-300 break-words">{option.data.description}</p>
														{/if}
														{#if option.data.benefits}
															<p class="text-xs sm:text-sm text-green-400 break-words">✓ {option.data.benefits}</p>
														{/if}
														{#if option.data.disadvantages}
															<p class="text-xs sm:text-sm text-red-400 break-words">✗ {option.data.disadvantages}</p>
														{/if}
														<div class="flex flex-wrap gap-2 text-xs sm:text-sm text-gray-300">
															{#if option.data.country}
																<span class="whitespace-nowrap">📍 {option.data.country}</span>
															{/if}
															{#if option.data.travelTime > 0}
																<span class="whitespace-nowrap">⏱️ {option.data.travelTime} hours</span>
															{/if}
															{#if option.data.airbnbPrice > 0}
																<span class="whitespace-nowrap">💰 €{option.data.airbnbPrice.toFixed(2)}</span>
															{/if}
															{#if option.data.totalPrice > 0}
																<span class="whitespace-nowrap">💵 €{option.data.totalPrice.toFixed(2)}/person</span>
															{/if}
															{#if option.data.flightNeeded}
																<span class="whitespace-nowrap">✈️ Flight</span>
															{:else}
																<span class="whitespace-nowrap">🚗 Car</span>
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
										<div class="text-left sm:text-right flex-shrink-0">
											{#if result.averageRank !== undefined && result.averageRank !== null}
												<!-- Primary metric: Average Rank -->
												<div class="text-xl sm:text-2xl font-bold {index === 0 ? 'text-blue-400' : index === 1 ? 'text-purple-400' : index === 2 ? 'text-green-400' : 'text-gray-300'}">
													Avg: {result.averageRank.toFixed(2)}
												</div>
												<!-- Secondary metrics: Vote count and percentage -->
												<div class="mt-1 text-xs text-gray-500">
													{result.voteCount} {result.voteCount === 1 ? 'vote' : 'votes'} · {result.percentage.toFixed(1)}%
												</div>
											{:else}
												<!-- Fallback for non-ranking votes -->
												<div class="text-xl sm:text-2xl font-bold text-white">{result.voteCount} {result.voteCount === 1 ? 'vote' : 'votes'}</div>
												<div class="text-sm sm:text-base font-semibold {index === 0 ? 'text-blue-400' : index === 1 ? 'text-purple-400' : index === 2 ? 'text-green-400' : 'text-gray-400'}">
													{result.percentage.toFixed(1)}%
												</div>
											{/if}
										</div>
									</div>

									<!-- Progress bar -->
									<div class="mt-3 sm:mt-4 h-2.5 sm:h-3 overflow-hidden rounded-full bg-gray-600/50 backdrop-blur-sm">
										<div
											class="h-full rounded-full transition-all duration-500 {index === 0 ? 'bg-gradient-to-r from-blue-500 to-blue-600' : index === 1 ? 'bg-gradient-to-r from-purple-500 to-purple-600' : index === 2 ? 'bg-gradient-to-r from-green-500 to-green-600' : 'bg-gradient-to-r from-gray-500 to-gray-600'}"
											style="width: {result.percentage}%"
										></div>
									</div>
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Detailed Results -->
			{#if showDetailedResults }
				<div class="rounded-2xl border-2 border-gray-700 bg-gradient-to-br from-gray-800/80 to-gray-900/80 p-4 sm:p-6 md:p-8 shadow-2xl backdrop-blur-sm">
					<h2 class="mb-4 sm:mb-6 text-xl sm:text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
						👥 Individual Votes
					</h2>

					{#if results.individualVoters.length === 0}
						<div class="rounded-xl border-2 border-yellow-800 bg-yellow-900/20 p-4 sm:p-6 text-center backdrop-blur-sm">
							<div class="mb-2 text-3xl sm:text-4xl">📭</div>
							<p class="text-sm sm:text-base text-yellow-400 font-semibold">No individual vote details available yet.</p>
						</div>
					{:else}
						<div class="space-y-3 sm:space-y-4">
							{#each results.individualVoters as voter}
								<div class="rounded-xl border-2 border-gray-600 bg-gradient-to-br from-gray-700/50 to-gray-800/50 p-3 sm:p-4 backdrop-blur-sm transition-all hover:border-blue-500 hover:shadow-lg">
									<div class="mb-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
										<div class="min-w-0">
											<p class="font-semibold text-white text-sm sm:text-base truncate">{voter.username}</p>
											<p class="text-xs sm:text-sm text-gray-400 truncate">{voter.email}</p>
										</div>
										<p class="text-xs sm:text-sm text-gray-400 flex-shrink-0">
											{new Date(voter.submittedAt).toLocaleDateString()} {new Date(voter.submittedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
										</p>
									</div>

									{#if results.voteType === 'RANKING'}
										<!-- Show rankings -->
										<div class="space-y-2">
											{#each voter.rankings.filter(r => r.rank !== undefined).sort((a, b) => (a.rank || 0) - (b.rank || 0)) as ranking}
												<div class="flex items-center gap-2 sm:gap-3 text-xs sm:text-sm">
													<span class="flex h-6 w-6 sm:h-7 sm:w-7 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-600 text-xs font-bold text-white shadow-md flex-shrink-0">
														{ranking.rank}
													</span>
													<span class="text-gray-300 break-words">{ranking.optionLabel}</span>
												</div>
											{/each}
										</div>
									{:else}
										<!-- Show selections -->
										<div class="flex flex-wrap gap-2">
											{#each voter.rankings.filter(r => r.selected) as ranking}
												<span class="rounded-full bg-gradient-to-r from-blue-600 to-blue-700 px-3 py-1 text-xs sm:text-sm font-medium text-white shadow-md">
													{ranking.optionLabel}
												</span>
											{/each}
										</div>
									{/if}
								</div>
							{/each}
						</div>
					{/if}
				</div>
			{/if}
		{/if}
	</div>
</div>

