<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { getEnrichedVoteApi, getVoteSubmissionApi, handleApiError } from '$lib/apiHelpers';
	import { isAuthenticated, clearToken } from '$lib/auth';
	import AirbnbExternalData from '$lib/AirbnbExternalData.svelte';
	import type { VoteWithEnrichedAirbnbOptionsDto, VoteSubmissionDto, VoteSubmissionEntryDto } from '../../../generated/models';

	// Get vote ID from URL
	$: voteId = parseInt(String($page.params.id));

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

	// Touch handling state for mobile
	let touchStartY: number = 0;
	let touchedItem: number | null = null;
	let isDraggingFromHandle: boolean = false;

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
					// Initialize with options in the order returned by backend (random/as-is)
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
			}, 120);

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

	// Touch handlers for mobile - only allow dragging from handle
	function handleTouchStart(event: TouchEvent, optionId: number) {
		// Don't start dragging from main container touch
		isDraggingFromHandle = false;
	}

	function handleHandleTouchStart(event: TouchEvent, optionId: number) {
		event.stopPropagation();
		touchStartY = event.touches[0].clientY;
		touchedItem = optionId;
		draggedItem = optionId; // Show visual feedback
		isDraggingFromHandle = true;
	}

	function handleTouchMove(event: TouchEvent) {
		if (touchedItem === null || !isDraggingFromHandle) return;

		event.preventDefault(); // Prevent scrolling while dragging

		const touch = event.touches[0];
		const deltaY = touch.clientY - touchStartY;

		// Determine which item we're hovering over
		const elements = document.elementsFromPoint(touch.clientX, touch.clientY);
		const targetElement = elements.find(el => el.hasAttribute('data-option-id'));

		if (targetElement) {
			const targetId = parseInt(targetElement.getAttribute('data-option-id') || '0');
			if (targetId && targetId !== touchedItem) {
				const touchedIndex = rankedOptions.indexOf(touchedItem);
				const targetIndex = rankedOptions.indexOf(targetId);

				if (touchedIndex !== -1 && targetIndex !== -1) {
					const newRankedOptions = [...rankedOptions];
					newRankedOptions.splice(touchedIndex, 1);
					newRankedOptions.splice(targetIndex, 0, touchedItem);
					rankedOptions = newRankedOptions;
					touchStartY = touch.clientY; // Update start position
				}
			}
		}
	}

	function handleTouchEnd() {
		touchedItem = null;
		draggedItem = null;
		touchStartY = 0;
		isDraggingFromHandle = false;
	}
</script>

<div class="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 py-4 sm:py-8">
	<div class="mx-auto max-w-4xl px-3 sm:px-4">
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
					<p class="mt-4 text-gray-300">Loading vote...</p>
				</div>
			</div>
		{:else if error}
			<div class="rounded-2xl border-2 border-red-800 bg-red-900/30 p-8 text-center backdrop-blur-sm">
				<div class="mb-3 text-4xl">⚠️</div>
				<p class="mb-4 text-lg font-semibold text-red-400">{error}</p>
				<button
					on:click={handleBack}
					class="rounded-xl bg-gray-700 px-6 py-2 font-semibold text-white transition-all hover:bg-gray-600"
				>
					Go Back
				</button>
			</div>
		{:else if vote}
			<div class="mb-6 sm:mb-8">
				<div class="mb-3 sm:mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-2">
					<button
						on:click={handleBack}
						class="text-gray-300 transition-colors hover:text-white text-sm sm:text-base"
					>
						← Back
					</button>
					<button
						on:click={loadVote}
						disabled={loading}
						class="rounded-xl border-2 border-blue-600 bg-blue-600/20 px-3 py-1.5 text-xs sm:text-sm font-semibold text-blue-300 backdrop-blur-sm transition-all duration-300 hover:bg-blue-600/30 disabled:opacity-50 self-start"
						title="Reload vote data"
					>
						{loading ? '↻ Reloading...' : '↻ Reload'}
					</button>
				</div>

				<div class="flex flex-wrap items-center gap-2 sm:gap-3">
					<h1 class="bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-2xl sm:text-3xl font-extrabold text-transparent">{vote.title}</h1>
					{#if vote.done}
						<span class="flex items-center gap-1 rounded-full bg-green-900/40 px-2.5 sm:px-3 py-1 text-xs sm:text-sm font-bold text-green-400 shadow-lg">
							<svg class="h-3 w-3 sm:h-4 sm:w-4" fill="currentColor" viewBox="0 0 20 20">
								<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
							</svg>
							Completed
						</span>
					{/if}
				</div>

				{#if vote.description}
					<p class="mt-2 text-sm sm:text-base text-gray-300">{vote.description}</p>
				{/if}

				<div class="mt-3 sm:mt-4 flex flex-wrap gap-2 text-xs sm:text-sm">
					<span class="rounded-lg bg-gray-700/50 px-2.5 sm:px-3 py-1.5 font-medium text-gray-300">
						{vote.voteType === 'RANKING' ? '📊 Ranking Vote' : '✅ Simple Vote'}
					</span>
					{#if vote.allowMultiVote}
						<span class="rounded-lg bg-gray-700/50 px-2.5 sm:px-3 py-1.5 font-medium text-gray-300">Multiple selections allowed</span>
					{/if}
				</div>
			</div>

			{#if submitSuccess}
				<div class="mb-6 rounded-xl border-2 border-green-800 bg-green-900/30 p-4 sm:p-6 text-center backdrop-blur-sm">
					<div class="mb-2 text-3xl sm:text-4xl">✅</div>
					<p class="font-semibold text-green-400 text-sm sm:text-base">Vote submitted successfully! Redirecting to results...</p>
				</div>
			{/if}

			<div class="rounded-2xl border-2 border-gray-700 bg-gradient-to-br from-gray-800/80 to-gray-900/80 p-4 sm:p-6 md:p-8 shadow-2xl backdrop-blur-sm">
				{#if vote.voteType === 'RANKING'}
					<div class="mb-4 sm:mb-6">
						<h2 class="mb-2 text-lg sm:text-xl font-bold text-white">Rank Your Preferences</h2>
						<p class="text-xs sm:text-sm text-gray-300">
							Touch and drag to reorder on mobile, or use the arrow buttons. Top option is your #1 choice.
						</p>
					</div>

					<div class="space-y-3">
						{#each rankedOptions as optionId, index (optionId)}
							{@const option = vote.options.find(o => o.id === optionId)}
							{#if option}
								<div
									draggable="true"
									on:dragstart={() => handleDragStart(optionId)}
									on:dragover={(e) => handleDragOver(e, optionId)}
									on:dragend={handleDragEnd}
									on:touchstart={(e) => handleTouchStart(e, optionId)}
									on:touchmove={handleTouchMove}
									on:touchend={handleTouchEnd}
									data-option-id={optionId}
									class="flex items-start gap-2 sm:gap-4 rounded-xl border-2 border-gray-600 bg-gradient-to-br from-gray-700/50 to-gray-800/50 p-3 sm:p-4 backdrop-blur-sm transition-all hover:border-blue-500 hover:shadow-lg"
									class:opacity-50={draggedItem === optionId && !isDraggingFromHandle}
									class:scale-105={touchedItem === optionId && isDraggingFromHandle}
									class:shadow-blue-500={touchedItem === optionId && isDraggingFromHandle}
								>
									<div class="flex flex-col gap-1">
										<button
											on:click={() => moveUp(optionId)}
											disabled={index === 0}
											class="rounded-lg bg-gray-600 px-2 sm:px-2.5 py-1.5 sm:py-2 text-xs font-semibold text-white transition-all hover:bg-gray-500 disabled:opacity-30 min-w-[28px] sm:min-w-[32px]"
										>
											▲
										</button>
										<button
											on:click={() => moveDown(optionId)}
											disabled={index === rankedOptions.length - 1}
											class="rounded-lg bg-gray-600 px-2 sm:px-2.5 py-1.5 sm:py-2 text-xs font-semibold text-white transition-all hover:bg-gray-500 disabled:opacity-30 min-w-[28px] sm:min-w-[32px]"
										>
											▼
										</button>
									</div>

									<div class="flex h-8 w-8 sm:h-10 sm:w-10 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-600 text-sm sm:text-base font-bold text-white shadow-lg">
										#{index + 1}
									</div>

									<div class="flex-1 min-w-0">
										<h3 class="text-base sm:text-lg font-semibold text-white break-words">{option.label}</h3>
										{#if option.data}
											<div class="mt-1 space-y-1 text-xs sm:text-sm text-gray-300">
												{#if option.data.description}
													<p class="break-words">{option.data.description}</p>
												{/if}
												{#if option.data.benefits}
													<p class="text-green-400 break-words">✓ {option.data.benefits}</p>
												{/if}
												{#if option.data.disadvantages}
													<p class="text-red-400 break-words">✗ {option.data.disadvantages}</p>
												{/if}
												<div class="flex flex-wrap gap-2 sm:gap-3">
													{#if option.data.country}
														<span class="whitespace-nowrap">📍 {option.data.country}</span>
													{/if}
													{#if option.data.travelTime > 0}
														<span class="whitespace-nowrap">⏱️ {option.data.travelTime} hours</span>
													{/if}
													{#if option.data.airbnbPrice > 0}
														<span class="whitespace-nowrap">💰 €{option.data.airbnbPrice.toFixed(2)} (Airbnb)</span>
													{/if}
													{#if option.data.totalPrice > 0}
														<span class="whitespace-nowrap">💵 €{option.data.totalPrice.toFixed(2)} (Total/Person)</span>
													{/if}
													{#if option.data.flightNeeded}
														<span class="whitespace-nowrap">✈️ Flight</span>
													{:else}
														<span class="whitespace-nowrap">🚗 Car</span>
													{/if}
												</div>
												{#if option.data.airbnbLink && !option.externalData}
													<a
														href={option.data.airbnbLink}
														target="_blank"
														rel="noopener noreferrer"
														class="inline-block text-blue-400 hover:text-blue-300 break-all"
													>
														View on Airbnb →
													</a>
												{/if}
											</div>
										{/if}
									<!-- External Airbnb Data -->
										<AirbnbExternalData externalData={option.externalData} optionId={option.id} />
									</div>

									<div
										on:touchstart={(e) => handleHandleTouchStart(e, optionId)}
										class="cursor-grab active:cursor-grabbing text-xl sm:text-2xl text-gray-400 px-2 py-4 -mr-2 select-none touch-manipulation"
										role="button"
										tabindex="0"
										aria-label="Drag to reorder"
									>
										⋮⋮
									</div>
								</div>
							{/if}
						{/each}
					</div>
				{:else}
					<!-- Simple Vote -->
					<div class="mb-4 sm:mb-6">
						<h2 class="mb-2 text-lg sm:text-xl font-bold text-white">Select Your Choice{vote.allowMultiVote ? '(s)' : ''}</h2>
						<p class="text-xs sm:text-sm text-gray-300">
							{vote.allowMultiVote ? 'Select one or more options' : 'Select one option'}
						</p>
					</div>

					<div class="space-y-3">
						{#each vote.options as option (option.id)}
							<button
								type="button"
								on:click={() => toggleSelection(option.id)}
								class="w-full rounded-xl border-2 p-3 sm:p-4 text-left transition-all {selections.has(option.id) ? 'border-blue-500 bg-gradient-to-br from-blue-900/40 to-blue-800/30 shadow-lg shadow-blue-500/20' : 'border-gray-600 bg-gradient-to-br from-gray-700/50 to-gray-800/50 hover:border-blue-400 hover:shadow-lg'}"
							>
								<div class="flex items-start gap-3">
									<div
										class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full border-2 transition-all {selections.has(option.id) ? 'border-blue-500 bg-gradient-to-br from-blue-500 to-blue-600 shadow-lg' : 'border-gray-400'}"
									>
										{#if selections.has(option.id)}
											<svg class="h-4 w-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
												<path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
											</svg>
										{/if}
									</div>

									<div class="flex-1 min-w-0">
										<h3 class="text-base sm:text-lg font-semibold text-white break-words">{option.label}</h3>
										{#if option.data}
											<div class="mt-1 space-y-1 text-xs sm:text-sm text-gray-300">
												{#if option.data.description}
													<p class="break-words">{option.data.description}</p>
												{/if}
												{#if option.data.benefits}
													<p class="text-green-400 break-words">✓ {option.data.benefits}</p>
												{/if}
												{#if option.data.disadvantages}
													<p class="text-red-400 break-words">✗ {option.data.disadvantages}</p>
												{/if}
												<div class="flex flex-wrap gap-2 sm:gap-3">
													{#if option.data.country}
														<span class="whitespace-nowrap">📍 {option.data.country}</span>
													{/if}
													{#if option.data.travelTime > 0}
														<span class="whitespace-nowrap">⏱️ {option.data.travelTime} hours</span>
													{/if}
													{#if option.data.airbnbPrice > 0}
														<span class="whitespace-nowrap">💰 €{option.data.airbnbPrice.toFixed(2)} (Airbnb)</span>
													{/if}
													{#if option.data.totalPrice > 0}
														<span class="whitespace-nowrap">💵 €{option.data.totalPrice.toFixed(2)} (Total/Person)</span>
													{/if}
													{#if option.data.flightNeeded}
														<span class="whitespace-nowrap">✈️ Flight</span>
													{:else}
														<span class="whitespace-nowrap">🚗 Car</span>
													{/if}
												</div>
												{#if option.data.airbnbLink && !option.externalData}
													<a
														href={option.data.airbnbLink}
														target="_blank"
														rel="noopener noreferrer"
														class="inline-block text-blue-400 hover:text-blue-300 break-all"
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
					class="mt-6 sm:mt-8 w-full rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-4 py-3 sm:py-4 text-base sm:text-lg font-bold text-white shadow-lg transition-all duration-300 hover:scale-105 hover:shadow-blue-500/50 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:scale-100"
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
