package de.christinecoenen.code.zapp.app.livestream.api.model

import de.christinecoenen.code.zapp.app.livestream.model.LiveShow
import org.joda.time.format.ISODateTimeFormat

data class Show(val title: String? = null,
				val subtitle: String? = null,
				val description: String? = null,
				private val startTime: String? = null,
				private val endTime: String? = null) {

	fun toLiveShow(): LiveShow {
		val liveShow = LiveShow(
			title = title ?: "",
			subtitle = subtitle ?: "",
			description = description ?: "")

		if (startTime != null && endTime != null) {
			liveShow.startTime = formatter.parseDateTime(startTime)
			liveShow.endTime = formatter.parseDateTime(endTime)
		}

		return liveShow
	}

	companion object {
		private val formatter = ISODateTimeFormat.dateTimeParser()
	}
}
