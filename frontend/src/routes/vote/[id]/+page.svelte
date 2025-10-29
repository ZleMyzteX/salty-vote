<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { getEnrichedVoteApi, getVoteSubmissionApi, handleApiError } from '$lib/apiHelpers';
	import { isAuthenticated, clearToken } from '$lib/auth';
	import AirbnbExternalData from '$lib/AirbnbExternalData.svelte';
	import type { VoteWithEnrichedAirbnbOptionsDto, VoteSubmissionDto, VoteSubmissionEntryDto } from '../../../generated/models';

	// Get vote ID from URL
	$: voteId = parseInt($page.params.id);

	// State
	let vote: VoteWithEnrichedAirbnbOptionsDto | null = null;
	let loading = true;
	let error = '';
	let submitting = false;
	let submitSuccess = false;

	// Voting state
	let rankings: Map<number, number> = new Map(); // optionId -> rank
	let selections: Set<number> = new Set(); // optionId for simple votes

	// Drag and drop state for ranking
	let draggedItem: number | null = null;
	let rankedOptions: number[] = []; // Array of option IDs in ranked order

	onMount(() => {
		if (!isAuthenticated()) {
			goto('/login');
			return;
		}
		loadVote();
	});

	async function loadVote() {
		loading = true;
		error = '';

		try {
			const enrichedVoteApi = getEnrichedVoteApi();
			const submissionApi = getVoteSubmissionApi();

			vote = await enrichedVoteApi.getEnrichedAirbnbVote({ voteId });

			// Try to load existing submission
			try {
				const existingSubmission = await submissionApi.getMySubmission({ voteId });

				// Populate existing selections/rankings
				if (vote.voteType === 'RANKING') {
					existingSubmission.entries.forEach(entry => {
						if (entry.rank !== undefined && entry.rank !== null) {
							rankings.set(entry.optionId, entry.rank);
						}
					});
					// Sort options by rank for drag & drop
					rankedOptions = existingSubmission.entries
						.filter(e => e.rank !== undefined)
						.sort((a, b) => (a.rank || 0) - (b.rank || 0))
						.map(e => e.optionId);
				} else {
					existingSubmission.entries.forEach(entry => {
						if (entry.selected) {
							selections.add(entry.optionId);
						}
					});
					selections = selections; // Trigger reactivity
				}
			} catch {
				// No existing submission, that's fine
				if (vote.voteType === 'RANKING') {
					rankedOptions = vote.options.map(o => o.id);
				}
			}

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

	async function handleSubmit() {
		if (!vote) return;

		let entries: VoteSubmissionEntryDto[] = [];

		if (vote.voteType === 'RANKING') {
			// Create entries based on ranked order
			entries = rankedOptions.map((optionId, index) => ({
				optionId,
				rank: index + 1,
				selected: true
			}));
		} else {
			// Simple vote - just selected items
			entries = vote.options.map(option => ({
				optionId: option.id,
				selected: selections.has(option.id)
			}));
		}

		if (vote.voteType === 'SIMPLE' && entries.filter(e => e.selected).length === 0) {
			alert('Please select at least one option');
			return;
		}

		submitting = true;
		error = '';

		try {
			const submissionApi = getVoteSubmissionApi();

			const submission: VoteSubmissionDto = {
				voteId,
				entries
			};

			await submissionApi.submitVote({ voteSubmissionDto: submission });

			submitSuccess = true;

			// Redirect after a short delay
			setTimeout(() => {
				goto(`/votes/${voteId}/results`);
			}, 1500);

		} catch (err) {
			error = await handleApiError(err);
		} finally {
			submitting = false;
		}
	}

	function toggleSelection(optionId: number) {
		if (selections.has(optionId)) {
			selections.delete(optionId);
		} else {
			if (!vote?.allowMultiVote && selections.size > 0) {
				selections.clear();
			}
			selections.add(optionId);
		}
		selections = selections; // Trigger reactivity
	}

	// Drag and drop handlers
	function handleDragStart(optionId: number) {
		draggedItem = optionId;
	}

	function handleDragOver(event: DragEvent, targetId: number) {
		event.preventDefault();

		if (draggedItem === null || draggedItem === targetId) return;

		const draggedIndex = rankedOptions.indexOf(draggedItem);
		const targetIndex = rankedOptions.indexOf(targetId);

		if (draggedIndex !== -1 && targetIndex !== -1) {
			const newRankedOptions = [...rankedOptions];
			newRankedOptions.splice(draggedIndex, 1);
			newRankedOptions.splice(targetIndex, 0, draggedItem);
			rankedOptions = newRankedOptions;
		}
	}

	function handleDragEnd() {
		draggedItem = null;
	}

	function moveUp(optionId: number) {
		const index = rankedOptions.indexOf(optionId);
		if (index > 0) {
			const newRankedOptions = [...rankedOptions];
			[newRankedOptions[index - 1], newRankedOptions[index]] =
				[newRankedOptions[index], newRankedOptions[index - 1]];
			rankedOptions = newRankedOptions;
		}
	}

	function moveDown(optionId: number) {
		const index = rankedOptions.indexOf(optionId);
		if (index < rankedOptions.length - 1) {
			const newRankedOptions = [...rankedOptions];
			[newRankedOptions[index], newRankedOptions[index + 1]] =
				[newRankedOptions[index + 1], newRankedOptions[index]];
			rankedOptions = newRankedOptions;
		}
	}

	function handleBack() {
		goto('/');
	}
</script>

<div class="min-h-screen bg-gray-900 py-8">
	<div class="mx-auto max-w-4xl px-4">
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
					<p class="mt-4 text-gray-400">Loading vote...</p>
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
		{:else if vote}
			<div class="mb-8">
				<button
					on:click={handleBack}
					class="mb-4 text-gray-400 transition-colors hover:text-white"
				>
					← Back
				</button>

				<div class="flex items-center gap-3">
					<h1 class="text-3xl font-bold text-white">{vote.title}</h1>
					{#if vote.done}
						<span class="rounded-full bg-green-900/30 px-3 py-1 text-sm font-medium text-green-400">
							Completed
						</span>
					{/if}
				</div>

				{#if vote.description}
					<p class="mt-2 text-gray-400">{vote.description}</p>
				{/if}

				<div class="mt-4 flex flex-wrap gap-2 text-sm">
					<span class="rounded bg-gray-700 px-2 py-1 text-gray-300">
						{vote.voteType === 'RANKING' ? '📊 Ranking Vote' : '✅ Simple Vote'}
					</span>
					{#if vote.allowMultiVote}
						<span class="rounded bg-gray-700 px-2 py-1 text-gray-300">Multiple selections allowed</span>
					{/if}
				</div>
			</div>

			{#if submitSuccess}
				<div class="mb-6 rounded-lg border border-green-800 bg-green-900/20 p-4 text-center">
					<p class="text-green-400">✅ Vote submitted successfully! Redirecting to results...</p>
				</div>
			{/if}

			<div class="rounded-lg bg-gray-800 p-8 shadow-xl">
				{#if vote.voteType === 'RANKING'}
					<div class="mb-6">
						<h2 class="mb-2 text-xl font-semibold text-white">Rank Your Preferences</h2>
						<p class="text-sm text-gray-400">
							Drag to reorder or use the arrow buttons. Top option is your #1 choice.
						</p>
					</div>

					<div class="space-y-3">
						{#each rankedOptions as optionId, index}
							{@const option = vote.options.find(o => o.id === optionId)}
							{#if option}
								<div
									draggable="true"
									on:dragstart={() => handleDragStart(optionId)}
									on:dragover={(e) => handleDragOver(e, optionId)}
									on:dragend={handleDragEnd}
									class="flex items-start gap-4 rounded-lg border-2 border-gray-600 bg-gray-700 p-4 transition-all hover:border-blue-500"
									class:opacity-50={draggedItem === optionId}
								>
									<div class="flex flex-col gap-1">
										<button
											on:click={() => moveUp(optionId)}
											disabled={index === 0}
											class="rounded bg-gray-600 px-2 py-1 text-xs text-white hover:bg-gray-500 disabled:opacity-30"
										>
											▲
										</button>
										<button
											on:click={() => moveDown(optionId)}
											disabled={index === rankedOptions.length - 1}
											class="rounded bg-gray-600 px-2 py-1 text-xs text-white hover:bg-gray-500 disabled:opacity-30"
										>
											▼
										</button>
									</div>

									<div class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-blue-600 font-bold text-white">
										#{index + 1}
									</div>

									<div class="flex-1">
										<h3 class="text-lg font-medium text-white">{option.label}</h3>
										{#if option.data}
											<div class="mt-1 space-y-1 text-sm text-gray-400">
												{#if option.data.description}
													<p>{option.data.description}</p>
												{/if}
												{#if option.data.benefits}
													<p class="text-green-400">✓ {option.data.benefits}</p>
												{/if}
												{#if option.data.disadvantages}
													<p class="text-red-400">✗ {option.data.disadvantages}</p>
												{/if}
												<div class="flex flex-wrap gap-3">
													{#if option.data.country}
														<span>📍 {option.data.country}</span>
													{/if}
												{#if option.data.travelTime > 0}
													<span>⏱️ {option.data.travelTime} hours</span>
												{/if}
												{#if option.data.totalPrice > 0}
													<span>💵 €{option.data.totalPrice.toFixed(2)}</span>
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
										<AirbnbExternalData externalData={option.externalData} optionId={option.id} />
									</div>

									<div class="cursor-grab text-2xl text-gray-400">
										⋮⋮
									</div>
								</div>
							{/if}
						{/each}
					</div>
				{:else}
					<!-- Simple Vote -->
					<div class="mb-6">
						<h2 class="mb-2 text-xl font-semibold text-white">Select Your Choice{vote.allowMultiVote ? '(s)' : ''}</h2>
						<p class="text-sm text-gray-400">
							{vote.allowMultiVote ? 'Select one or more options' : 'Select one option'}
						</p>
					</div>

					<div class="space-y-3">
						{#each vote.options as option}
							<button
								type="button"
								on:click={() => toggleSelection(option.id)}
								class="w-full rounded-lg border-2 p-4 text-left transition-all {selections.has(option.id) ? 'border-blue-500 bg-blue-900 bg-opacity-20' : 'border-gray-600 bg-gray-700 hover:border-blue-400'}"
							>
								<div class="flex items-start gap-3">
									<div
										class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full border-2 transition-all {selections.has(option.id) ? 'border-blue-500 bg-blue-500' : 'border-gray-400'}"
									>
										{#if selections.has(option.id)}
											<svg class="h-4 w-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
											</svg>
										{/if}
									</div>

									<div class="flex-1">
										<h3 class="text-lg font-medium text-white">{option.label}</h3>
										{#if option.data}
											<div class="mt-1 space-y-1 text-sm text-gray-400">
												{#if option.data.description}
													<p>{option.data.description}</p>
												{/if}
												{#if option.data.benefits}
													<p class="text-green-400">✓ {option.data.benefits}</p>
												{/if}
												{#if option.data.disadvantages}
													<p class="text-red-400">✗ {option.data.disadvantages}</p>
												{/if}
												<div class="flex flex-wrap gap-3">
													{#if option.data.country}
														<span>📍 {option.data.country}</span>
													{/if}
												{#if option.data.travelTime > 0}
													<span>⏱️ {option.data.travelTime} hours</span>
												{/if}
												{#if option.data.totalPrice > 0}
													<span>💵 €{option.data.totalPrice.toFixed(2)}</span>
												{/if}
												{#if option.data.flightNeeded}
													<span>✈️ Flight</span>
												{:else}
													<span>🚗 Car</span>
												{/if}
											</div>
											{#if option.data.airbnbLink && !option.externalData}
													<a
														href={option.data.airbnbLink}
														target="_blank"
														rel="noopener noreferrer"
														class="inline-block text-blue-400 hover:text-blue-300"
														on:click|stopPropagation
													>
														View on Airbnb →
													</a>
												{/if}
											</div>
										{/if}
										<!-- External Airbnb Data -->
										<AirbnbExternalData externalData={option.externalData} optionId={option.id} />
									</div>
								</div>
							</button>
						{/each}
					</div>
				{/if}

				<button
					on:click={handleSubmit}
					disabled={submitting || submitSuccess}
					class="mt-8 w-full rounded-lg bg-blue-600 px-4 py-3 font-semibold text-white transition-colors duration-200 hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-600"
				>
					{#if submitting}
						<span class="inline-flex items-center">
							<svg
								class="-ml-1 mr-3 h-5 w-5 animate-spin text-white"
								xmlns="http://www.w3.org/2000/svg"
								fill="none"
								viewBox="0 0 24 24"
							>
								<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
								<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
							</svg>
							Submitting...
						</span>
					{:else if submitSuccess}
						✅ Submitted
					{:else}
						Submit Vote
					{/if}
				</button>
			</div>
		{/if}
	</div>
</div>

