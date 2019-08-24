package de.christinecoenen.code.zapp.app.mediathek.ui.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.UnknownServiceException;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.christinecoenen.code.zapp.R;
import de.christinecoenen.code.zapp.app.mediathek.api.request.QueryRequest;
import de.christinecoenen.code.zapp.app.mediathek.model.MediathekShow;
import de.christinecoenen.code.zapp.app.mediathek.repository.MediathekRepository;
import de.christinecoenen.code.zapp.app.mediathek.ui.detail.MediathekDetailActivity;
import de.christinecoenen.code.zapp.utils.view.InfiniteScrollListener;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class MediathekListFragment extends Fragment implements MediathekItemAdapter.Listener, SwipeRefreshLayout.OnRefreshListener, MediathekListFilterFragment.Listener {

	private static final int ITEM_COUNT_PER_PAGE = 10;

	public static MediathekListFragment getInstance() {
		return new MediathekListFragment();
	}

	@BindView(R.id.list)
	protected RecyclerView recyclerView;

	@BindView(R.id.error)
	protected TextView errorView;

	@BindView(R.id.no_shows)
	protected View noShowsWarning;

	@BindView(R.id.refresh_layout)
	protected SwipeRefreshLayout swipeRefreshLayout;

	private QueryRequest queryRequest;
	private MediathekItemAdapter adapter;
	private InfiniteScrollListener scrollListener;
	private MediathekShow longClickShow;
	private MediathekRepository mediathekRepository;
	private Disposable getShowsCall;
	private MediathekListFilterFragment filter;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public MediathekListFragment() {
	}

	public void search(String query) {
		queryRequest.setSimpleSearch(query);
		loadItems(0, true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		queryRequest = new QueryRequest()
			.setSize(ITEM_COUNT_PER_PAGE);

		mediathekRepository = new MediathekRepository();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_mediathek_list, container, false);
		ButterKnife.bind(this, view);

		LinearLayoutManager layoutManager = new LinearLayoutManager(view.getContext());
		recyclerView.setLayoutManager(layoutManager);

		scrollListener = new InfiniteScrollListener(layoutManager) {
			@Override
			public void onLoadMore(int totalItemCount) {
				loadItems(totalItemCount, false);
			}
		};
		recyclerView.addOnScrollListener(scrollListener);
		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary);

		adapter = new MediathekItemAdapter(MediathekListFragment.this);
		recyclerView.setAdapter(adapter);

		filter = (MediathekListFilterFragment) getChildFragmentManager().findFragmentById(R.id.fragment_filter);

		loadItems(0, true);

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (getShowsCall != null) {
			getShowsCall.dispose();
		}
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_mediathek_list, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem filterMenuItem = menu.findItem(R.id.menu_filter);
		filterMenuItem.setIcon(filter.getMenuIconResId());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_filter:
				filter.toggle();
				Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
				return true;
			case R.id.menu_refresh:
				onRefresh();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onShowClicked(MediathekShow show) {
		startActivity(MediathekDetailActivity.getStartIntent(getContext(), show));
	}

	@Override
	public void onShowLongClicked(MediathekShow show, View view) {
		this.longClickShow = show;
		PopupMenu menu = new PopupMenu(getContext(), view, Gravity.TOP | Gravity.END);
		menu.inflate(R.menu.activity_mediathek_detail);
		menu.show();
		menu.setOnMenuItemClickListener(this::onContextMenuItemClicked);
	}

	@Override
	public void onRefresh() {
		swipeRefreshLayout.setRefreshing(true);
		loadItems(0, true);
	}

	@Override
	public void onChannelFilterChanged() {
		// TODO: implement
	}

	private boolean onContextMenuItemClicked(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case R.id.menu_share:
				startActivity(Intent.createChooser(longClickShow.getShareIntentPlain(), getString(R.string.action_share)));
				return true;
		}
		return false;
	}

	private void loadItems(int startWith, boolean replaceItems) {
		Timber.d("loadItems: %s", startWith);

		if (getShowsCall != null) {
			getShowsCall.dispose();
		}

		noShowsWarning.setVisibility(View.GONE);
		adapter.setLoading(true);

		queryRequest.setOffset(startWith);
		getShowsCall = mediathekRepository
			.listShows(queryRequest)
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(shows -> onMediathekLoadSuccess(shows, replaceItems), this::onMediathekLoadError);
	}

	private void showError(int messageResId) {
		errorView.setText(messageResId);
		errorView.setVisibility(View.VISIBLE);
	}

	private void onMediathekLoadSuccess(List<MediathekShow> shows, boolean replaceItems) {
		adapter.setLoading(false);
		scrollListener.setLoadingFinished();
		swipeRefreshLayout.setRefreshing(false);
		errorView.setVisibility(View.GONE);

		if (replaceItems) {
			adapter.setShows(shows);
		} else {
			adapter.addShows(shows);
		}

		if (adapter.getItemCount() == 1) {
			noShowsWarning.setVisibility(View.VISIBLE);
		}
	}

	private void onMediathekLoadError(Throwable e) {
		adapter.setLoading(false);
		swipeRefreshLayout.setRefreshing(false);

		Timber.e(e);

		if (e instanceof SSLHandshakeException || e instanceof UnknownServiceException) {
			showError(R.string.error_mediathek_ssl_error);
		} else {
			showError(R.string.error_mediathek_info_not_available);
		}
	}
}
