package de.christinecoenen.code.zapp.app.livestream.ui.views;


import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.christinecoenen.code.zapp.R;
import de.christinecoenen.code.zapp.app.livestream.api.ProgramGuideRequest;
import de.christinecoenen.code.zapp.app.livestream.api.model.Channel;
import de.christinecoenen.code.zapp.app.livestream.model.LiveShow;
import de.christinecoenen.code.zapp.model.ChannelModel;
import timber.log.Timber;

public abstract class ProgramInfoViewBase extends LinearLayout {

	protected
	@BindView(R.id.text_show_title)
	TextView showTitleView;
	protected
	@BindView(R.id.text_show_subtitle)
	TextView showSubtitleView;
	protected
	@BindView(R.id.text_show_time)
	TextView showTimeView;
	protected
	@BindView(R.id.progressbar_show_progress)
	ProgressBar progressBarView;

	protected
	@BindInt(R.integer.view_program_info_update_show_info_interval_seconds)
	int updateShowInfoIntervalSeconds;
	protected
	@BindInt(R.integer.view_program_info_update_show_time_interval_seconds)
	int updateShowTimeIntervalSeconds;

	private ProgramGuideRequest currentShowInfoRequest;
	private LiveShow currentShow = null;
	private ChannelModel currentChannel;

	private final Handler handler = new Handler();
	private Timer timer;
	private final ObjectAnimator showProgressAnimator;

	private final ProgramGuideRequest.Listener programGuideListener = new ProgramGuideRequest.Listener() {
		@Override
		public void onRequestError() {
			logMessage("could not load show info");
			ProgramInfoViewBase.this.currentShow = null;
			showTitleView.setText(R.string.activity_channel_detail_info_error);
			showSubtitleView.setVisibility(GONE);
			showTimeView.setVisibility(GONE);
			progressBarView.setVisibility(GONE);
		}

		@Override
		public void onRequestSuccess(LiveShow currentShow) {
			logMessage("show info loaded: " + currentShow);

			ProgramInfoViewBase.this.currentShow = currentShow;
			displayTitles();
			displayTime();
		}
	};

	public ProgramInfoViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);

		setOrientation(LinearLayout.VERTICAL);
		setGravity(Gravity.CENTER_VERTICAL);

		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//noinspection ConstantConditions
		inflater.inflate(getViewId(), this, true);

		ButterKnife.bind(this, this);

		showProgressAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context,
			R.animator.view_program_info_show_progress);
		showProgressAnimator.setTarget(progressBarView);
	}

	public ProgramInfoViewBase(Context context) {
		this(context, null);
	}

	public void setChannel(ChannelModel channel) {
		if (channel == currentChannel) {
			return;
		}

		currentShow = null;
		currentChannel = channel;

		showTitleView.setText("");
		showSubtitleView.setText("");
		showTimeView.setText("");
		progressBarView.setProgress(0);

		chancelProgramGuideLoading();
		loadProgramGuide();
	}

	public void pause() {
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}

	public void resume() {
		if (timer != null) {
			return;
		}

		timer = new Timer();
		timer.scheduleAtFixedRate(new UpdateShowInfoTask(), 0,
			TimeUnit.SECONDS.toMillis(updateShowInfoIntervalSeconds));
		timer.scheduleAtFixedRate(new UpdateShowTimeTask(), 0,
			TimeUnit.SECONDS.toMillis(updateShowTimeIntervalSeconds));
	}

	protected abstract int getViewId();

	private void updateShowInfo() {
		if (currentShow == null ||
			currentShow.getEndTime() == null ||
			currentShow.getEndTime().isBeforeNow()) {
			reloadProgramGuide();
		}
	}

	private void displayTime() {
		if (currentShow == null) {
			return;
		}

		if (currentShow.hasDuration()) {
			float progressPercent = currentShow.getProgressPercent();
			int progress = Math.round(progressPercent * progressBarView.getMax());
			String startTime = DateUtils.formatDateTime(getContext(),
				currentShow.getStartTime().getMillis(), DateUtils.FORMAT_SHOW_TIME);
			String endTime = DateUtils.formatDateTime(getContext(),
				currentShow.getEndTime().getMillis(), DateUtils.FORMAT_SHOW_TIME);
			String fullTime = getContext().getString(R.string.view_program_info_show_time, startTime, endTime);
			showTimeView.setText(fullTime);
			showTimeView.setVisibility(VISIBLE);
			progressBarView.setIndeterminate(false);
			progressBarView.setEnabled(true);
			setShowProgressBar(progress);
		} else {
			setShowProgressBar(0);
			showTimeView.setVisibility(GONE);
			progressBarView.setIndeterminate(false);
			progressBarView.setEnabled(false);
		}

		progressBarView.setVisibility(VISIBLE);
	}

	private void displayTitles() {
		if (currentShow == null) {
			return;
		}

		showTitleView.setText(currentShow.getTitle());

		if (currentShow.getSubtitle().isEmpty()) {
			showSubtitleView.setVisibility(GONE);
		} else {
			showSubtitleView.setText(currentShow.getSubtitle());
			showSubtitleView.setVisibility(VISIBLE);
		}
	}

	private void reloadProgramGuide() {
		if (currentChannel == null) {
			return;
		}

		logMessage("reloadProgramGuide");
		chancelProgramGuideLoading();
		loadProgramGuide();
	}

	private void loadProgramGuide() {
		progressBarView.setEnabled(true);
		progressBarView.setIndeterminate(true);
		currentShowInfoRequest = new ProgramGuideRequest(Channel.getById(currentChannel.getId()), programGuideListener)
			.execute();
	}

	private void chancelProgramGuideLoading() {
		progressBarView.setIndeterminate(false);
		if (currentShowInfoRequest != null) {
			currentShowInfoRequest.cancel();
		}
	}

	private void setShowProgressBar(int value) {
		showProgressAnimator.setIntValues(value);
		showProgressAnimator.start();
	}

	private void logMessage(String message) {
		String channelId = null;
		if (currentChannel != null) {
			channelId = currentChannel.getId();
		}
		Timber.d(channelId + " - " + message);
	}

	private class UpdateShowTimeTask extends TimerTask {
		@Override
		public void run() {
			handler.post(ProgramInfoViewBase.this::displayTime);
		}
	}

	private class UpdateShowInfoTask extends TimerTask {
		@Override
		public void run() {
			handler.post(() -> {
				logMessage("UpdateShowInfoTask");
				updateShowInfo();
			});
		}
	}
}
