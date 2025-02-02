package de.christinecoenen.code.zapp.app.mediathek.ui.list.adapter

import android.graphics.Bitmap
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.christinecoenen.code.zapp.databinding.MediathekListFragmentItemBinding
import de.christinecoenen.code.zapp.models.shows.DownloadStatus
import de.christinecoenen.code.zapp.models.shows.MediathekShow
import de.christinecoenen.code.zapp.repositories.MediathekRepository
import de.christinecoenen.code.zapp.utils.system.ImageHelper.loadThumbnailAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class MediathekItemViewHolder(
	private val binding: MediathekListFragmentItemBinding
) : RecyclerView.ViewHolder(binding.root), KoinComponent {

	private val mediathekRepository: MediathekRepository by inject()

	private var thumbnailJob: Job? = null
	private var playbackPositionJob: Job? = null

	suspend fun setShow(show: MediathekShow) = withContext(Dispatchers.Main) {
		binding.root.visibility = View.GONE

		thumbnailJob?.cancel()
		playbackPositionJob?.cancel()

		binding.thumbnail.setImageBitmap(null)
		binding.thumbnail.visibility = View.GONE
		binding.viewingProgress.scaleX = 0f
		binding.title.text = show.title
		binding.topic.text = show.topic
		binding.duration.text = show.formattedDuration
		binding.channel.text = show.channel
		binding.time.text = show.formattedTimestamp
		binding.subtitle.isVisible = show.hasSubtitle
		binding.subtitleDivider.isVisible = show.hasSubtitle

		binding.root.visibility = View.VISIBLE

		thumbnailJob = launch { loadThumbnailFlow(show) }
		thumbnailJob = launch { updatePlaybackPositionFlow(show) }
	}

	private suspend fun loadThumbnailFlow(show: MediathekShow) {
		mediathekRepository
			.getPersistedShowByApiId(show.apiId)
			.filter { it.downloadStatus === DownloadStatus.COMPLETED }
			.map { it.downloadedVideoPath }
			.filterNotNull()
			.distinctUntilChanged()
			.map { loadThumbnailAsync(binding.root.context, it) }
			.catch { e -> Timber.e(e) }
			.collectLatest(::updatethumbnail)
	}

	private suspend fun updatePlaybackPositionFlow(show: MediathekShow) {
		mediathekRepository
			.getPlaybackPositionPercent(show.apiId)
			.filter { it > 0 }
			.collectLatest(::updatePlaybackPosition)
	}

	private fun updatePlaybackPosition(progressPercent: Float) {
		binding.viewingProgress.scaleX = progressPercent
	}

	private fun updatethumbnail(thumbnail: Bitmap) {
		binding.thumbnail.setImageBitmap(thumbnail)
		binding.thumbnail.visibility = View.VISIBLE
	}
}
