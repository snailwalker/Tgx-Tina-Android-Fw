package com.tgx.tina.android.plugin.downloader;

import java.util.ArrayList;

import com.tgx.tina.android.plugin.downloader.DownloadManager.DownloadItem;

public interface DownloadViewListener {

	public void updataDownloadView(ArrayList<DownloadItem> list);
}
