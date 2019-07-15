package de.christinecoenen.code.zapp.model.json;


import android.content.Context;
import androidx.annotation.NonNull;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import de.christinecoenen.code.zapp.model.ChannelModel;
import de.christinecoenen.code.zapp.model.IChannelList;
import de.christinecoenen.code.zapp.model.ISortableChannelList;
import de.christinecoenen.code.zapp.model.SimpleChannelList;
import de.christinecoenen.code.zapp.app.settings.helper.PreferenceChannelOrderHelper;

public class SortableJsonChannelList implements ISortableChannelList {

	private IChannelList channelList;

	@Inject
	PreferenceChannelOrderHelper channelOrderHelper;

	@Inject
	public SortableJsonChannelList(Context context) {
		channelList = new JsonChannelList(context);
		loadSortingFromDisk();
	}

	@Override
	public ChannelModel get(int index) {
		return channelList.get(index);
	}

	@Override
	public ChannelModel get(String id) {
		return channelList.get(id);
	}

	@Override
	public int size() {
		return channelList.size();
	}

	@Override
	public List<ChannelModel> getList() {
		return channelList.getList();
	}

	@NonNull
	@Override
	public Iterator<ChannelModel> iterator() {
		return channelList.iterator();
	}

	@Override
	public int indexOf(String channelId) {
		return channelList.indexOf(channelId);
	}

	@Override
	public void reloadChannelOrder() {
		loadSortingFromDisk();
	}

	@Override
	public void persistChannelOrder() {
		channelOrderHelper.saveChannelOrder(channelList.getList());
	}

	private void loadSortingFromDisk() {
		List<ChannelModel> sortedChannels = channelOrderHelper.sortChannelList(channelList.getList());
		channelList = new SimpleChannelList(sortedChannels);
	}
}
