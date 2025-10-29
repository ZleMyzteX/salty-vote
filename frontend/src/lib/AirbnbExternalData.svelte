<script lang="ts">
	import type { AirbnbExternalDataDto } from '../generated/models';
	import { getApiBaseUrl } from './apiHelpers';
	import { getToken } from './auth';
	import { onMount } from 'svelte';

	export let externalData: AirbnbExternalDataDto | null | undefined;
	export let optionId: number;
	export let compact: boolean = false;

	let imageDataUrl: string | null = null;
	let imageLoading = false;
	let imageError = false;
	let showModal = false;

	$: hasData = externalData != null;
	$: shouldLoadImage = externalData?.hasPictureDownloaded && !compact;

	function openImageModal() {
		if (imageDataUrl && !imageError) {
			showModal = true;
		}
	}

	function closeModal() {
		showModal = false;
	}

	function handleModalClick(event: MouseEvent) {
		// Close modal when clicking outside the image
		if (event.target === event.currentTarget) {
			closeModal();
		}
	}

	function handleKeyDown(event: KeyboardEvent) {
		if (event.key === 'Escape' && showModal) {
			closeModal();
		}
	}

	// Load image with authentication
	async function loadAuthenticatedImage() {
		if (!shouldLoadImage) return;

		imageLoading = true;
		imageError = false;

		try {
			const token = getToken();
			const url = `${getApiBaseUrl()}/api/v1/votes/airbnb/enriched/options/${optionId}/picture`;

			const response = await fetch(url, {
				headers: token ? {
					'Authorization': `Bearer ${token}`
				} : {}
			});

			if (!response.ok) {
				throw new Error('Failed to load image');
			}

			const blob = await response.blob();
			imageDataUrl = URL.createObjectURL(blob);
		} catch (error) {
			console.error('Error loading image:', error);
			imageError = true;
		} finally {
			imageLoading = false;
		}
	}

	// Load image when component mounts or when shouldLoadImage changes
	$: if (shouldLoadImage) {
		loadAuthenticatedImage();
	}

	// Cleanup blob URL on component destroy
	onMount(() => {
		return () => {
			if (imageDataUrl) {
				URL.revokeObjectURL(imageDataUrl);
			}
		};
	});
</script>

<svelte:window on:keydown={handleKeyDown} />

{#if hasData && externalData}
	<div class="mt-3 rounded-lg bg-gray-800/50 p-3 border border-gray-700">
		<div class="flex items-start gap-3 min-w-0">
			<!-- Airbnb Picture from backend endpoint -->
			{#if shouldLoadImage && !compact}
				<div class="flex-shrink-0">
					{#if imageLoading}
						<div class="h-20 w-20 rounded-lg bg-gray-700 flex items-center justify-center">
							<svg class="animate-spin h-6 w-6 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
								<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
								<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
							</svg>
						</div>
					{:else if imageDataUrl && !imageError}
						<img
							src={imageDataUrl}
							alt={externalData.title || 'Property'}
							class="h-20 w-20 rounded-lg object-cover cursor-pointer hover:opacity-80 transition-opacity"
							on:click={openImageModal}
							on:keydown={(e) => e.key === 'Enter' && openImageModal()}
							role="button"
							tabindex="0"
						/>
					{/if}
				</div>
			{/if}

			<div class="flex-1 min-w-0 overflow-hidden">
				<div class="flex items-start justify-between gap-2 min-w-0">
					<div class="flex-1 min-w-0">
						{#if externalData.title}
							<h4 class="text-sm font-semibold text-white break-words line-clamp-2" title={externalData.title}>
								🏠 {externalData.title}
							</h4>
						{/if}

						{#if externalData.starRating != null || externalData.reviewCount != null}
							<div class="mt-1 flex items-center gap-2 text-xs text-gray-300 flex-wrap">
								{#if externalData.starRating != null}
									<span class="flex items-center gap-1 flex-shrink-0">
										<span class="text-yellow-400">⭐</span>
										<span class="font-semibold">{externalData.starRating.toFixed(2)}</span>
									</span>
								{/if}
								{#if externalData.reviewCount != null}
									<span class="text-gray-400 flex-shrink-0">
										({externalData.reviewCount} reviews)
									</span>
								{/if}
							</div>
						{/if}
					</div>

					{#if externalData.sourceUrl}
						<a
							href={externalData.sourceUrl}
							target="_blank"
							rel="noopener noreferrer"
							class="flex-shrink-0 text-xs text-blue-400 hover:text-blue-300 transition-colors whitespace-nowrap"
						>
							View on Airbnb →
						</a>
					{/if}
				</div>

				{#if !externalData.hasPictureDownloaded && !compact}
					<div class="mt-1 text-xs text-yellow-400">
						⏳ Downloading image...
					</div>
				{/if}
			</div>
		</div>
	</div>
{:else if !hasData}
	<div class="mt-3 rounded-lg bg-yellow-900/20 border border-yellow-800/30 p-3">
		<div class="flex items-center gap-2 text-xs text-yellow-400">
			<svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
				<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
				<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
			</svg>
			<span>Loading Airbnb details...</span>
		</div>
		<p class="mt-1 text-xs text-gray-400">
			We're fetching additional information from Airbnb. This may take a moment.
		</p>
	</div>
{/if}

<!-- Image Modal / Lightbox -->
{#if showModal && imageDataUrl}
	<div
		class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-75 backdrop-blur-sm p-4"
		on:click={handleModalClick}
		role="dialog"
		aria-modal="true"
	>
		<button
			on:click={closeModal}
			class="absolute top-4 right-4 text-white text-4xl font-bold hover:text-gray-300 transition-colors z-10"
			aria-label="Close image viewer"
		>
			×
		</button>
		<div class="max-w-7xl max-h-full flex flex-col items-center">
			<img
				src={imageDataUrl}
				alt={externalData?.title || 'Property'}
				class="max-w-full max-h-[90vh] object-contain rounded-lg shadow-2xl"
			/>
			{#if externalData?.title}
				<div class="mt-4 text-white text-center">
					<h3 class="text-xl font-semibold">{externalData.title}</h3>
					{#if externalData.starRating != null && externalData.reviewCount != null}
						<p class="mt-2 text-gray-300">
							⭐ {externalData.starRating.toFixed(2)} ({externalData.reviewCount} reviews)
						</p>
					{/if}
				</div>
			{/if}
		</div>
	</div>
{/if}

