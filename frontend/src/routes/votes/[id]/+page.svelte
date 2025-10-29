<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { getEnrichedVoteApi, getVoteApi, getVoteCollaboratorApi, getUserApi, handleApiError } from '$lib/apiHelpers';
	import { isAuthenticated, clearToken } from '$lib/auth';
	import AirbnbExternalData from '$lib/AirbnbExternalData.svelte';
	import type { VoteWithEnrichedAirbnbOptionsDto, CollaboratorDto, UserDto, AirbnbVoteOptionDto } from '../../../generated/models';

	// Get vote ID from URL
	$: voteId = parseInt($page.params.id);

	// State
	let vote: VoteWithEnrichedAirbnbOptionsDto | null = null;
	let collaborators: CollaboratorDto[] = [];
	let allUsers: UserDto[] = [];
	let loading = true;
	let error = '';

	// Collaborator management
	let showAddCollaborator = false;
	let selectedUserId = '';
	let addingCollaborator = false;
	let collaboratorError = '';

	// Share link
	let shareLink = '';
	let linkCopied = false;

	// Add option state
	let showAddOption = false;
	let newOption = createEmptyOption();
	let addingOption = false;
	let optionError = '';

	onMount(() => {
		if (!isAuthenticated()) {
			goto('/login');
			return;
		}
		loadVoteData();
		shareLink = `${window.location.origin}/vote/${voteId}`;
	});

	function createEmptyOption() {
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

	async function loadVoteData() {
		loading = true;
		error = '';

		try {
			const enrichedVoteApi = getEnrichedVoteApi();
			const collaboratorApi = getVoteCollaboratorApi();
			const userApi = getUserApi();

			// Load vote, collaborators, and all users in parallel
			const [voteData, collaboratorsData, usersData] = await Promise.all([
				enrichedVoteApi.getEnrichedAirbnbVote({ voteId }),
				collaboratorApi.getCollaborators({ voteId }),
				userApi.getAllUsers()
			]);

			vote = voteData;
			collaborators = collaboratorsData;
			allUsers = usersData;

			// Redirect to voting page if user is neither creator nor collaborator
			if (!vote.isCreator && !vote.isCollaborator) {
				goto(`/vote/${voteId}`);
				return;
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

	async function handleAddCollaborator() {
		if (!selectedUserId) {
			collaboratorError = 'Please select a user';
			return;
		}

		addingCollaborator = true;
		collaboratorError = '';

		try {
			const collaboratorApi = getVoteCollaboratorApi();
			const newCollaborator = await collaboratorApi.addCollaborator({
				voteId,
				addCollaboratorDto: { userId: selectedUserId }
			});

			collaborators = [...collaborators, newCollaborator];
			selectedUserId = '';
			showAddCollaborator = false;
		} catch (err) {
			collaboratorError = await handleApiError(err);
		} finally {
			addingCollaborator = false;
		}
	}

	async function handleRemoveCollaborator(collaboratorId: number) {
		if (!confirm('Are you sure you want to remove this collaborator?')) {
			return;
		}

		try {
			const collaboratorApi = getVoteCollaboratorApi();
			await collaboratorApi.removeCollaborator({ voteId, collaboratorId });
			collaborators = collaborators.filter(c => c.id !== collaboratorId);
		} catch (err) {
			alert(await handleApiError(err));
		}
	}

	async function handleAddOption() {
		if (!newOption.label.trim() || !newOption.airbnbLink.trim()) {
			optionError = 'Label and Airbnb link are required';
			return;
		}

		// Validate URL
		try {
			new URL(newOption.airbnbLink);
		} catch {
			optionError = 'Invalid Airbnb URL';
			return;
		}

		addingOption = true;
		optionError = '';

		try {
			const voteApi = getVoteApi();

			const optionDto: AirbnbVoteOptionDto = {
				label: newOption.label.trim(),
				description: newOption.description.trim(),
				benefits: newOption.benefits.trim(),
				disadvantages: newOption.disadvantages.trim(),
				travelTime: newOption.travelTime,
				totalPrice: newOption.totalPrice,
				country: newOption.country.trim(),
				flightNeeded: newOption.flightNeeded,
				airbnbPrice: newOption.airbnbPrice,
				airbnbLink: newOption.airbnbLink.trim(),
				airbnbData: {
					description: newOption.description.trim(),
					benefits: newOption.benefits.trim(),
					disadvantages: newOption.disadvantages.trim(),
					travelTime: newOption.travelTime,
					totalPrice: newOption.totalPrice,
					country: newOption.country.trim(),
					flightNeeded: newOption.flightNeeded,
					airbnbPrice: newOption.airbnbPrice,
					airbnbLink: newOption.airbnbLink.trim()
				}
			};

			await voteApi.addVoteOption({
				voteId,
				airbnbVoteOptionDto: optionDto
			});

			// Reload vote data to get the new option
			await loadVoteData();

			newOption = createEmptyOption();
			showAddOption = false;
		} catch (err) {
			optionError = await handleApiError(err);
		} finally {
			addingOption = false;
		}
	}

	async function handleDeleteOption(optionId: number) {
		if (!confirm('Are you sure you want to delete this option?')) {
			return;
		}

		try {
			const voteApi = getVoteApi();
			await voteApi.deleteVoteOption({ voteId, optionId });
			await loadVoteData();
		} catch (err) {
			alert(await handleApiError(err));
		}
	}

	async function handleToggleDone() {
		if (!vote) return;

		try {
			const voteApi = getVoteApi();
			await voteApi.markVoteAsDone({ voteId, done: !vote.done });
			vote.done = !vote.done;
		} catch (err) {
			alert(await handleApiError(err));
		}
	}

	function copyShareLink() {
		navigator.clipboard.writeText(shareLink);
		linkCopied = true;
		setTimeout(() => {
			linkCopied = false;
		}, 2000);
	}

	function goToResults() {
		goto(`/votes/${voteId}/results`);
	}

	function handleBack() {
		goto('/');
	}

	// Filter out users who are already collaborators
	$: availableUsers = allUsers.filter(
		user => !collaborators.some(c => c.userId === user.id)
	);
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
			<!-- Header -->
			<div class="mb-8 flex items-start justify-between">
				<div class="flex-1">
					<div class="flex items-center gap-3">
						<h1 class="text-3xl font-bold text-white">{vote.title}</h1>
						{#if vote.done}
							<span class="rounded-full bg-green-900/30 px-3 py-1 text-sm font-medium text-green-400">
								Completed
							</span>
						{:else}
							<span class="rounded-full bg-blue-900/30 px-3 py-1 text-sm font-medium text-blue-400">
								Active
							</span>
						{/if}
					</div>
					{#if vote.description}
						<p class="mt-2 text-gray-400">{vote.description}</p>
					{/if}
					<div class="mt-3 flex flex-wrap gap-2 text-sm">
						<span class="rounded bg-gray-700 px-2 py-1 text-gray-300">
							{vote.voteType === 'RANKING' ? '📊 Ranking Vote' : '✅ Simple Vote'}
						</span>
						{#if vote.allowMultiVote}
							<span class="rounded bg-gray-700 px-2 py-1 text-gray-300">Multiple votes allowed</span>
						{/if}
						{#if vote.allowAnonymousVote}
							<span class="rounded bg-gray-700 px-2 py-1 text-gray-300">Anonymous voting</span>
						{/if}
					</div>
				</div>
				<div class="flex gap-2">
					<button
						on:click={handleBack}
						class="rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-gray-600"
					>
						Back
					</button>
				</div>
			</div>

			<div class="grid gap-6 lg:grid-cols-3">
				<!-- Main Content -->
				<div class="lg:col-span-2 space-y-6">
					<!-- Options -->
					<div class="rounded-lg bg-gray-800 p-6 shadow-xl">
						<div class="mb-4 flex items-center justify-between">
							<h2 class="text-xl font-semibold text-white">Airbnb Options</h2>
							{#if vote.isCreator || vote.isCollaborator}
								<button
									on:click={() => showAddOption = !showAddOption}
									class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
								>
									{showAddOption ? 'Cancel' : '+ Add Option'}
								</button>
							{/if}
						</div>

						{#if showAddOption}
							<div class="mb-6 rounded-lg border border-gray-600 bg-gray-700/50 p-4">
								<h3 class="mb-4 font-medium text-white">Add New Option</h3>

								{#if optionError}
									<div class="mb-4 rounded-lg border border-red-800 bg-red-900/20 p-3 text-sm text-red-400">
										{optionError}
									</div>
								{/if}

								<div class="grid gap-4 md:grid-cols-2">
									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">Label *</label>
										<input
											type="text"
											bind:value={newOption.label}
											placeholder="e.g., Beach House in Malibu"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										/>
									</div>

									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">Airbnb Link *</label>
										<input
											type="url"
											bind:value={newOption.airbnbLink}
											placeholder="https://www.airbnb.com/..."
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										/>
									</div>

									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">Description</label>
										<textarea
											bind:value={newOption.description}
											placeholder="Additional details..."
											rows="2"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										></textarea>
									</div>

									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">Benefits</label>
										<textarea
											bind:value={newOption.benefits}
											placeholder="What are the advantages?"
											rows="2"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										></textarea>
									</div>

									<div class="md:col-span-2">
										<label class="mb-2 block text-sm font-medium text-gray-300">Disadvantages</label>
										<textarea
											bind:value={newOption.disadvantages}
											placeholder="What are the downsides?"
											rows="2"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										></textarea>
									</div>

									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">Country</label>
										<input
											type="text"
											bind:value={newOption.country}
											placeholder="e.g., USA"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										/>
									</div>

									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">Travel Time (hours)</label>
										<input
											type="number"
											bind:value={newOption.travelTime}
											placeholder="0"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										/>
									</div>

									<div class="flex items-center">
										<label class="flex cursor-pointer items-center">
											<input
												type="checkbox"
												bind:checked={newOption.flightNeeded}
												class="h-4 w-4 cursor-pointer rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-2 focus:ring-blue-500"
											/>
											<span class="ml-3 text-sm text-gray-300">Flight needed</span>
										</label>
									</div>

									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">Airbnb Price (€)</label>
										<input
											type="number"
											step="0.01"
											bind:value={newOption.airbnbPrice}
											placeholder="0.00"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										/>
									</div>

									<div>
										<label class="mb-2 block text-sm font-medium text-gray-300">Total Price Per Person (€)</label>
										<input
											type="number"
											step="0.01"
											bind:value={newOption.totalPrice}
											placeholder="0.00"
											class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 text-white placeholder-gray-400 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
										/>
									</div>
								</div>

								<button
									on:click={handleAddOption}
									disabled={addingOption}
									class="mt-4 w-full rounded-lg bg-blue-600 px-4 py-2 font-semibold text-white transition-colors duration-200 hover:bg-blue-700 disabled:bg-gray-600"
								>
									{addingOption ? 'Adding...' : 'Add Option'}
								</button>
							</div>
						{/if}

						<div class="space-y-4">
							{#each vote.options as option}
								<div class="rounded-lg border border-gray-600 bg-gray-700/30 p-4">
									<div class="flex items-start justify-between">
										<div class="flex-1">
											<h3 class="text-lg font-medium text-white">{option.label}</h3>
											{#if option.data}
												<div class="mt-2 space-y-2 text-sm text-gray-400">
													{#if option.data.description}
														<p>{option.data.description}</p>
													{/if}
													{#if option.data.benefits}
														<div>
															<span class="font-medium text-green-400">✓ Benefits:</span>
															<p class="ml-4">{option.data.benefits}</p>
														</div>
													{/if}
													{#if option.data.disadvantages}
														<div>
															<span class="font-medium text-red-400">✗ Disadvantages:</span>
															<p class="ml-4">{option.data.disadvantages}</p>
														</div>
													{/if}
													<div class="flex flex-wrap gap-4">
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
															<span>💵 €{option.data.totalPrice.toFixed(2)} (Per Person)</span>
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
														>
															View on Airbnb →
														</a>
													{/if}
												</div>
											{/if}
											<!-- External Airbnb Data -->
											<AirbnbExternalData externalData={option.externalData} optionId={option.id} />
										</div>
										{#if vote.isCreator || vote.isCollaborator}
											<button
												on:click={() => handleDeleteOption(option.id)}
												class="ml-4 rounded-lg bg-red-600 px-3 py-1 text-sm text-white transition-colors duration-200 hover:bg-red-700"
											>
												Delete
											</button>
										{/if}
									</div>
								</div>
							{:else}
								<p class="text-center text-gray-400">No options added yet</p>
							{/each}
						</div>
					</div>
				</div>

				<!-- Sidebar -->
				<div class="space-y-6">
					<!-- Vote Now Button - Always visible for creators and collaborators -->
					<div class="rounded-lg bg-gradient-to-r from-green-600 to-emerald-600 p-6 shadow-xl">
						<h2 class="mb-2 text-xl font-bold text-white">Ready to vote?</h2>
						<p class="mb-4 text-sm text-green-100">Cast your vote or see how others have voted</p>
						<button
							on:click={() => goto(`/vote/${voteId}`)}
							class="w-full rounded-lg bg-white px-6 py-3 text-lg font-bold text-green-600 transition-all duration-200 hover:bg-green-50 hover:shadow-lg active:scale-95"
						>
							Vote Now →
						</button>
					</div>

					<!-- Actions -->
					{#if vote.isCreator}
						<div class="rounded-lg bg-gray-800 p-6 shadow-xl">
							<h2 class="mb-4 text-lg font-semibold text-white">Actions</h2>
							<div class="space-y-3">
								<button
									on:click={handleToggleDone}
									class="w-full rounded-lg bg-blue-600 px-4 py-2 font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
								>
									{vote.done ? 'Reopen Vote' : 'Mark as Done'}
								</button>
								<button
									on:click={goToResults}
									class="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2 font-semibold text-white transition-colors duration-200 hover:bg-gray-600"
								>
									View Results
								</button>
							</div>
						</div>
					{/if}

					<!-- Share Link -->
					<div class="rounded-lg bg-gray-800 p-6 shadow-xl">
						<h2 class="mb-4 text-lg font-semibold text-white">Share Vote</h2>
						<p class="mb-3 text-sm text-gray-400">Share this link with others to let them vote:</p>
						<div class="flex gap-2">
							<input
								type="text"
								value={shareLink}
								readonly
								class="flex-1 rounded-lg border border-gray-600 bg-gray-700 px-3 py-2 text-sm text-white"
							/>
							<button
								on:click={copyShareLink}
								class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
							>
								{linkCopied ? '✓' : 'Copy'}
							</button>
						</div>
					</div>

					<!-- Collaborators -->
					{#if vote.isCreator}
						<div class="rounded-lg bg-gray-800 p-6 shadow-xl">
							<div class="mb-4 flex items-center justify-between">
								<h2 class="text-lg font-semibold text-white">Collaborators</h2>
								<button
									on:click={() => showAddCollaborator = !showAddCollaborator}
									class="rounded-lg bg-blue-600 px-3 py-1 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700"
								>
									{showAddCollaborator ? 'Cancel' : '+ Add'}
								</button>
							</div>

							{#if showAddCollaborator}
								<div class="mb-4 rounded-lg border border-gray-600 bg-gray-700/50 p-3">
									{#if collaboratorError}
										<div class="mb-3 rounded-lg border border-red-800 bg-red-900/20 p-2 text-sm text-red-400">
											{collaboratorError}
										</div>
									{/if}
									<select
										bind:value={selectedUserId}
										class="mb-3 w-full rounded-lg border border-gray-600 bg-gray-700 px-3 py-2 text-sm text-white focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
									>
										<option value="">Select a user...</option>
										{#each availableUsers as user}
											<option value={user.id}>{user.username} ({user.email})</option>
										{/each}
									</select>
									<button
										on:click={handleAddCollaborator}
										disabled={addingCollaborator || !selectedUserId}
										class="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors duration-200 hover:bg-blue-700 disabled:bg-gray-600"
									>
										{addingCollaborator ? 'Adding...' : 'Add Collaborator'}
									</button>
								</div>
							{/if}

							<div class="space-y-2">
								{#each collaborators as collaborator}
									<div class="flex items-center justify-between rounded-lg bg-gray-700/50 px-3 py-2">
										<div>
											<p class="text-sm font-medium text-white">{collaborator.username}</p>
											<p class="text-xs text-gray-400">{collaborator.email}</p>
										</div>
										<button
											on:click={() => handleRemoveCollaborator(collaborator.id)}
											class="rounded bg-red-600 px-2 py-1 text-xs text-white hover:bg-red-700"
										>
											Remove
										</button>
									</div>
								{:else}
									<p class="text-center text-sm text-gray-400">No collaborators yet</p>
								{/each}
							</div>
						</div>
					{/if}
				</div>
			</div>
		{/if}
	</div>
</div>

