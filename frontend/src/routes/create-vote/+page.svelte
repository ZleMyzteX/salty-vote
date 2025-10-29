<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { getVoteApi, handleApiError } from '$lib/apiHelpers';
	import { isAuthenticated, clearToken } from '$lib/auth';
	import type { CreateAirbnbVoteDto, AirbnbVoteOptionDto } from '../../generated/models';

	// Vote option interface
	interface VoteOption {
		label: string;
		description: string;
		benefits: string;
		disadvantages: string;
		travelTime: number;
		totalPrice: number;
		country: string;
		flightNeeded: boolean;
		airbnbPrice: number;
		airbnbLink: string;
	}

	// Form state
	let title = '';
	let description = '';
	let voteType: 'SIMPLE' | 'RANKING' = 'RANKING';
	let allowMultiVote = false;
	let allowAnonymousVote = false;
	let options: VoteOption[] = [
		createEmptyOption(),
		createEmptyOption()
	];

	// UI state
	let loading = false;
	let error = '';
	let success = false;
	let createdVoteId: number | null = null;

	onMount(() => {
		if (!isAuthenticated()) {
			goto('/login');
		}
	});

	function createEmptyOption(): VoteOption {
		return {
			label: '',
			description: '',
			benefits: '',
			disadvantages: '',
			travelTime: 0,
			totalPrice: 0,
			country: '',
			flightNeeded: false,
			airbnbPrice: 0,
			airbnbLink: ''
		};
	}

	function addOption() {
		options = [...options, createEmptyOption()];
	}

	function removeOption(index: number) {
		if (options.length > 2) {
			options = options.filter((_, i) => i !== index);
		}
	}

	/**
	 * Validates the vote creation form
	 */
	function validateForm(): boolean {
		error = '';

		if (!title.trim()) {
			error = 'Title is required';
			return false;
		}

		// Filter out empty options
		const validOptions = options.filter(opt => opt.label.trim() && opt.airbnbLink.trim());

		if (validOptions.length < 2) {
			error = 'At least 2 valid options are required (with label and Airbnb link)';
			return false;
		}

		// Validate URLs
		for (const opt of validOptions) {
			try {
				new URL(opt.airbnbLink);
			} catch {
				error = `Invalid Airbnb URL for option "${opt.label}"`;
				return false;
			}
		}

		return true;
	}

	async function handleSubmit(event: Event) {
		event.preventDefault();

		if (!validateForm()) {
			return;
		}

		loading = true;
		error = '';

		try {
			const voteApi = getVoteApi();

			// Filter out empty options and convert to DTO format
			const validOptions = options
				.filter(opt => opt.label.trim() && opt.airbnbLink.trim())
				.map((opt, index): AirbnbVoteOptionDto => ({
					label: opt.label.trim(),
					description: opt.description.trim(),
					benefits: opt.benefits.trim(),
					disadvantages: opt.disadvantages.trim(),
					travelTime: opt.travelTime,
					totalPrice: opt.totalPrice,
					country: opt.country.trim(),
					flightNeeded: opt.flightNeeded,
					airbnbPrice: opt.airbnbPrice,
					airbnbLink: opt.airbnbLink.trim(),
					airbnbData: {
						description: opt.description.trim(),
						benefits: opt.benefits.trim(),
						disadvantages: opt.disadvantages.trim(),
						travelTime: opt.travelTime,
						totalPrice: opt.totalPrice,
						country: opt.country.trim(),
						flightNeeded: opt.flightNeeded,
						airbnbPrice: opt.airbnbPrice,
						airbnbLink: opt.airbnbLink.trim()
					}
				}));

			const voteCreation: CreateAirbnbVoteDto = {
				title: title.trim(),
				description: description.trim() || undefined,
				voteType,
				allowMultiVote,
				allowAnonymousVote,
				done: false,
				options: validOptions
			};

			// Create the vote
			const response = await voteApi.createNewAirbnbVote({
				createAirbnbVoteDto: voteCreation
			});

			success = true;
			createdVoteId = response.voteId;

			// Redirect to vote management page after a short delay
			setTimeout(() => {
				goto(`/votes/${createdVoteId}`);
			}, 1500);

		} catch (err) {
			// If unauthorized, clear token and redirect to login
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

	function handleLogout() {
		clearToken();
		goto('/');
	}

	function handleBack() {
		goto('/');
	}
</script>

<div class="min-h-screen bg-gray-900 py-8">
	<div class="mx-auto max-w-6xl px-4">
		<!-- Header -->
		<div class="mb-8 flex items-center justify-between">
			<div>
				<h1 class="text-3xl font-bold text-white">🏠 Create Airbnb Vote</h1>
				<p class="mt-1 text-gray-400">Compare vacation rentals and let others help decide</p>
			</div>
			<div class="flex gap-2">
				<button
					type="button"
					on:click={handleBack}
					class="rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-gray-600"
				>
					Back
				</button>
				<button
					on:click={handleLogout}
					class="rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-gray-600"
				>
					Logout
				</button>
			</div>
		</div>

		<!-- Main Form Card -->
		<div class="rounded-lg bg-gray-800 p-8 shadow-xl">
			{#if success}
				<div
					class="mb-6 rounded-lg border border-green-800 bg-green-900/20 p-3 text-center text-sm text-green-400"
				>
					✅ Vote created successfully! Redirecting...
				</div>
			{/if}

			{#if error}
				<div
					class="mb-6 rounded-lg border border-red-800 bg-red-900/20 p-3 text-center text-sm text-red-400"
				>
					{error}
				</div>
			{/if}

			<form on:submit={handleSubmit} class="space-y-6">
				<!-- Title -->
				<div>
					<label for="title" class="mb-2 block text-sm font-medium text-gray-300">
						Vote Title *
					</label>
					<input
						id="title"
						type="text"
						bind:value={title}
						placeholder="e.g., Where should we go for our next vacation?"
						disabled={loading}
						class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
					/>
				</div>

				<!-- Description -->
				<div>
					<label for="description" class="mb-2 block text-sm font-medium text-gray-300">
						Description (optional)
					</label>
					<textarea
						id="description"
						bind:value={description}
						placeholder="Add more details about this vote..."
						disabled={loading}
						rows="3"
						class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
					></textarea>
				</div>

				<div class="grid gap-6 md:grid-cols-2">
					<!-- Vote Type -->
					<div>
						<label for="voteType" class="mb-2 block text-sm font-medium text-gray-300">
							Vote Type *
						</label>
						<select
							id="voteType"
							bind:value={voteType}
							disabled={loading}
							class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
						>
							<option value="RANKING">Ranking Vote</option>
							<option value="SIMPLE">Simple Vote</option>
						</select>
					</div>

					<!-- Checkboxes -->
					<div class="space-y-3 rounded-lg bg-gray-700/50 p-4">
						<label class="flex cursor-pointer items-center">
							<input
								type="checkbox"
								bind:checked={allowMultiVote}
								disabled={loading}
								class="h-4 w-4 cursor-pointer rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0"
							/>
							<span class="ml-3 text-sm text-gray-300">Allow multiple votes</span>
						</label>

						<label class="flex cursor-pointer items-center">
							<input
								type="checkbox"
								bind:checked={allowAnonymousVote}
								disabled={loading}
								class="h-4 w-4 cursor-pointer rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0"
							/>
							<span class="ml-3 text-sm text-gray-300">Allow anonymous voting</span>
						</label>
					</div>
				</div>

				<!-- Airbnb Options Section -->
				<div class="rounded-lg bg-gray-700/30 p-6">
					<div class="mb-4 flex items-center justify-between">
						<div>
							<h3 class="text-lg font-medium text-gray-300">Airbnb Options *</h3>
							<p class="text-xs text-gray-400">Add at least 2 options for comparison</p>
						</div>
					</div>

					<div class="space-y-6">
						{#each options as option, index}
							<div class="rounded-lg border border-gray-600 bg-gray-800 p-6">
								<div class="mb-4 flex items-center justify-between">
									<h4 class="font-medium text-white">Option {index + 1}</h4>
									{#if options.length > 2}
										<button
											type="button"
											on:click={() => removeOption(index)}
											disabled={loading}
											class="rounded-lg bg-red-600 px-3 py-1 text-sm text-white transition-colors duration-200 hover:bg-red-700 disabled:opacity-50"
										>
											Remove
										</button>
									{/if}
								</div>

								<div class="grid gap-4 md:grid-cols-2">
									<!-- Label -->
									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Label *
										</label>
										<input
											type="text"
											bind:value={option.label}
											placeholder="e.g., Cozy Cabin in the Mountains"
											disabled={loading}
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										/>
									</div>

									<!-- Airbnb Link -->
									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Airbnb Link *
										</label>
										<input
											type="url"
											bind:value={option.airbnbLink}
											placeholder="https://www.airbnb.com/..."
											disabled={loading}
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										/>
									</div>

									<!-- Description -->
									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Description
										</label>
										<textarea
											bind:value={option.description}
											placeholder="Add details about this option..."
											disabled={loading}
											rows="2"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										></textarea>
									</div>

									<!-- Benefits -->
									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Benefits
										</label>
										<textarea
											bind:value={option.benefits}
											placeholder="What are the advantages of this option?"
											disabled={loading}
											rows="2"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										></textarea>
									</div>

									<!-- Disadvantages -->
									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Disadvantages
										</label>
										<textarea
											bind:value={option.disadvantages}
											placeholder="What are the downsides of this option?"
											disabled={loading}
											rows="2"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										></textarea>
									</div>

									<!-- Country -->
									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Country
										</label>
										<input
											type="text"
											bind:value={option.country}
											placeholder="e.g., Switzerland"
											disabled={loading}
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										/>
									</div>

									<!-- Travel Time -->
									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Travel Time (hours)
										</label>
										<input
											type="number"
											bind:value={option.travelTime}
											placeholder="0"
											disabled={loading}
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										/>
									</div>

									<!-- Flight Needed -->
									<div class="flex items-center">
										<label class="flex cursor-pointer items-center">
											<input
												type="checkbox"
												bind:checked={option.flightNeeded}
												disabled={loading}
												class="h-4 w-4 cursor-pointer rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0"
											/>
											<span class="ml-3 text-sm text-gray-300">Flight needed</span>
										</label>
									</div>

									<!-- Airbnb Price -->
									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Airbnb Price (€)
										</label>
										<input
											type="number"
											step="0.01"
											bind:value={option.airbnbPrice}
											placeholder="0.00"
											disabled={loading}
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										/>
									</div>

									<!-- Total Price -->
									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">
											Total Price Per Person (€)
										</label>
										<input
											type="number"
											step="0.01"
											bind:value={option.totalPrice}
											placeholder="0.00"
											disabled={loading}
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
										/>
									</div>
								</div>
							</div>
						{/each}
					</div>

					<button
						type="button"
						on:click={addOption}
						disabled={loading}
						class="mt-4 w-full rounded-lg border-2 border-dashed border-gray-600 bg-gray-700/30 px-4 py-3 text-sm font-semibold text-gray-300 transition-colors duration-200 hover:border-blue-500 hover:bg-gray-700/50 hover:text-blue-400 disabled:opacity-50"
					>
						+ Add Another Option
					</button>
				</div>

				<!-- Submit Button -->
				<button
					type="submit"
					disabled={loading}
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
							Creating Vote...
						</span>
					{:else}
						Create Vote
					{/if}
				</button>
			</form>
		</div>
	</div>
</div>

