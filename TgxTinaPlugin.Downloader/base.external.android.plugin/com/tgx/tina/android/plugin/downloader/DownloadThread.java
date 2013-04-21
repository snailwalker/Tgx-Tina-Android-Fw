/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tgx.tina.android.plugin.downloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;

/**
 * Runs an actual download
 */
public class DownloadThread
				extends
				Thread
{

	private Context			mContext;
	private DownloadInfo	mInfo;

	public DownloadThread(Context context, DownloadInfo info)
	{
		mContext = context;
		mInfo = info;
	}

	/**
	 * Returns the user agent provided by the initiating app, or use the default
	 * one
	 */
	private String userAgent() {
		String userAgent = mInfo.mUserAgent;
		if (userAgent != null)
		{
		}
		if (userAgent == null)
		{
			userAgent = Constants.DEFAULT_USER_AGENT;
		}
		return userAgent;
	}

	/**
	 * Executes the download in a separate thread
	 */
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

		int finalStatus = GlobalDownload.STATUS_UNKNOWN_ERROR;
		boolean countRetry = false;
		int retryAfter = 0;
		int redirectCount = mInfo.mRedirectCount;
		String newUri = null;
		boolean gotData = false;
		String filename = null;
		String mimeType = sanitizeMimeType(mInfo.mMimeType);
		FileOutputStream stream = null;
		DefaultHttpClient client = null;
		PowerManager.WakeLock wakeLock = null;
		Uri contentUri = Uri.parse(GlobalDownload.CONTENT_URI + "/" + mInfo.mId);

		try
		{
			boolean continuingDownload = false;
			String headerAcceptRanges = null;
			String headerContentDisposition = null;
			String headerContentLength = null;
			String headerContentLocation = null;
			String headerETag = null;
			String headerTransferEncoding = null;

			byte data[] = new byte[Constants.BUFFER_SIZE];

			int bytesSoFar = 0;

			PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG);
			wakeLock.acquire();

			filename = mInfo.mFileName;
			if (filename != null)
			{
				if (!Helpers.isFilenameValid(filename))
				{
					finalStatus = GlobalDownload.STATUS_FILE_ERROR;
					notifyDownloadCompleted(finalStatus, false, 0, 0, false, filename, null, mInfo.mMimeType);
					return;
				}
				// We're resuming a download that got interrupted
				File f = new File(filename);
				if (f.exists())
				{
					long fileLength = f.length();
					if (fileLength == 0)
					{
						// The download hadn't actually started, we can restart from scratch
						f.delete();
						filename = null;
					}
					else if (mInfo.mETag == null && !mInfo.mNoIntegrity)
					{
						// Tough luck, that's not a resumable download
						//#debug
						base.tina.core.log.LogPrinter.d(Constants.TAG, "can't resume interrupted non-resumable download");
						f.delete();
						finalStatus = GlobalDownload.STATUS_PRECONDITION_FAILED;
						notifyDownloadCompleted(finalStatus, false, 0, 0, false, filename, null, mInfo.mMimeType);
						return;
					}
					else
					{
						// All right, we'll be able to resume this download
						stream = new FileOutputStream(filename, true);
						bytesSoFar = (int) fileLength;
						if (mInfo.mTotalBytes != -1)
						{
							headerContentLength = Integer.toString(mInfo.mTotalBytes);
						}
						headerETag = mInfo.mETag;
						continuingDownload = true;
					}
				}
			}

			int bytesNotified = bytesSoFar;
			// starting with MIN_VALUE means that the first write will commit
			//     progress to the database
			long timeLastNotification = 0;

			client = newHttpClient(userAgent());
			/*
			 * This loop is run once for every individual HTTP request that gets
			 * sent. The very first HTTP request is a "virgin" request, while
			 * every subsequent request is done with the original ETag and a
			 * byte-range.
			 */
			http_request_loop:
			while (true)
			{
				// Prepares the request and fires it.
				HttpGet request = new HttpGet(mInfo.mUri);

				//#debug verbose
				base.tina.core.log.LogPrinter.v(Constants.TAG, "initiating download for " + mInfo.mUri);

				if (mInfo.mCookies != null)
				{
					request.addHeader("Cookie", mInfo.mCookies);
				}
				if (mInfo.mReferer != null)
				{
					request.addHeader("Referer", mInfo.mReferer);
				}
				if (continuingDownload)
				{
					if (headerETag != null)
					{
						request.addHeader("If-Match", headerETag);
					}
					request.addHeader("Range", "bytes=" + bytesSoFar + "-");
				}

				HttpResponse response;
				try
				{
					response = client.execute(request);
				}
				catch (IllegalArgumentException ex)
				{
					//#debug
					base.tina.core.log.LogPrinter.d(Constants.TAG, "Arg exception trying to execute request for " + mInfo.mId + " : " + ex);
					finalStatus = GlobalDownload.STATUS_BAD_REQUEST;
					request.abort();
					break http_request_loop;
				}
				catch (IOException ex)
				{
					//#ifdef debug
					//#if debug<=2
					if (Helpers.isNetworkAvailable(mContext))
					{
						base.tina.core.log.LogPrinter.i(Constants.TAG, "Execute Failed " + mInfo.mId + ", Net Up");
					}
					else
					{
						base.tina.core.log.LogPrinter.i(Constants.TAG, "Execute Failed " + mInfo.mId + ", Net Down");
					}
					//#endif
					//#endif

					if (!Helpers.isNetworkAvailable(mContext))
					{
						finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
					}
					else if (mInfo.mNumFailed < Constants.MAX_RETRIES)
					{
						finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
						countRetry = true;
					}
					else
					{
						//#debug
						base.tina.core.log.LogPrinter.d(Constants.TAG, "IOException trying to execute request for " + mInfo.mId + " : " + ex);
						finalStatus = GlobalDownload.STATUS_HTTP_DATA_ERROR;
					}
					request.abort();
					break http_request_loop;
				}

				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 503 && mInfo.mNumFailed < Constants.MAX_RETRIES)
				{
					//#debug verbose
					base.tina.core.log.LogPrinter.v(Constants.TAG, "got HTTP response code 503");
					finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
					countRetry = true;
					Header header = response.getFirstHeader("Retry-After");
					if (header != null)
					{
						try
						{
							//#debug verbose
							base.tina.core.log.LogPrinter.v(Constants.TAG, "Retry-After :" + header.getValue());
							retryAfter = Integer.parseInt(header.getValue());
							if (retryAfter < 0)
							{
								retryAfter = 0;
							}
							else
							{
								if (retryAfter < Constants.MIN_RETRY_AFTER)
								{
									retryAfter = Constants.MIN_RETRY_AFTER;
								}
								else if (retryAfter > Constants.MAX_RETRY_AFTER)
								{
									retryAfter = Constants.MAX_RETRY_AFTER;
								}
								retryAfter += Helpers.sRandom.nextInt(Constants.MIN_RETRY_AFTER + 1);
								retryAfter *= 1000;
							}
						}
						catch (NumberFormatException ex)
						{
							// ignored - retryAfter stays 0 in this case.
						}
					}
					request.abort();
					break http_request_loop;
				}
				if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307)
				{
					//#debug verbose
					base.tina.core.log.LogPrinter.v(Constants.TAG, "got HTTP redirect " + statusCode);
					if (redirectCount >= Constants.MAX_REDIRECTS)
					{
						//#debug
						base.tina.core.log.LogPrinter.d(Constants.TAG, "too many redirects for download " + mInfo.mId);
						finalStatus = GlobalDownload.STATUS_TOO_MANY_REDIRECTS;
						request.abort();
						break http_request_loop;
					}
					Header header = response.getFirstHeader("Location");
					if (header != null)
					{
						//#debug verbose
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Location :" + header.getValue());
						try
						{
							newUri = new URI(mInfo.mUri).resolve(new URI(header.getValue())).toString();
						}
						catch (URISyntaxException ex)
						{
							//#debug
							base.tina.core.log.LogPrinter.d(Constants.TAG, "Couldn't resolve redirect URI for download " + mInfo.mId);
							finalStatus = GlobalDownload.STATUS_BAD_REQUEST;
							request.abort();
							break http_request_loop;
						}
						++redirectCount;
						finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
						request.abort();
						break http_request_loop;
					}
				}
				if ((!continuingDownload && statusCode != GlobalDownload.STATUS_SUCCESS) || (continuingDownload && statusCode != 206))
				{
					//#debug
					base.tina.core.log.LogPrinter.d(Constants.TAG, "http error " + statusCode + " for download " + mInfo.mId);
					if (GlobalDownload.isStatusError(statusCode))
					{
						finalStatus = statusCode;
					}
					else if (statusCode >= 300 && statusCode < 400)
					{
						finalStatus = GlobalDownload.STATUS_UNHANDLED_REDIRECT;
					}
					else if (continuingDownload && statusCode == GlobalDownload.STATUS_SUCCESS)
					{
						finalStatus = GlobalDownload.STATUS_PRECONDITION_FAILED;
					}
					else
					{
						finalStatus = GlobalDownload.STATUS_UNHANDLED_HTTP_CODE;
					}
					request.abort();
					break http_request_loop;
				}
				else
				{
					// Handles the response, saves the file
					//#debug verbose
					base.tina.core.log.LogPrinter.v(Constants.TAG, "received response for " + mInfo.mUri);

					if (!continuingDownload)
					{
						Header header = response.getFirstHeader("Accept-Ranges");
						if (header != null)
						{
							headerAcceptRanges = header.getValue();
						}
						header = response.getFirstHeader("Content-Disposition");
						if (header != null)
						{
							headerContentDisposition = header.getValue();
						}
						header = response.getFirstHeader("Content-Location");
						if (header != null)
						{
							headerContentLocation = header.getValue();
						}
						if (mimeType == null)
						{
							header = response.getFirstHeader("Content-Type");
							if (header != null)
							{
								mimeType = sanitizeMimeType(header.getValue());
							}
						}
						header = response.getFirstHeader("ETag");
						if (header != null)
						{
							headerETag = header.getValue();
						}
						header = response.getFirstHeader("Transfer-Encoding");
						if (header != null)
						{
							headerTransferEncoding = header.getValue();
						}
						if (headerTransferEncoding == null)
						{
							header = response.getFirstHeader("Content-Length");
							if (header != null)
							{
								headerContentLength = header.getValue();
							}
						}
						else
						{
							// Ignore content-length with transfer-encoding - 2616 4.4 3
							//#debug verbose
							base.tina.core.log.LogPrinter.v(Constants.TAG, "ignoring content-length because of xfer-encoding");
						}
						//#ifdef debug
						//#if debug<=2
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Accept-Ranges: " + headerAcceptRanges);
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Content-Disposition: " + headerContentDisposition);
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Content-Length: " + headerContentLength);
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Content-Location: " + headerContentLocation);
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Content-Type: " + mimeType);
						base.tina.core.log.LogPrinter.v(Constants.TAG, "ETag: " + headerETag);
						base.tina.core.log.LogPrinter.v(Constants.TAG, "Transfer-Encoding: " + headerTransferEncoding);
						//#endif
						//#endif

						if (!mInfo.mNoIntegrity && headerContentLength == null && (headerTransferEncoding == null || !headerTransferEncoding.equalsIgnoreCase("chunked")))
						{
							//#debug
							base.tina.core.log.LogPrinter.d(Constants.TAG, "can't know size of download, giving up");
							finalStatus = GlobalDownload.STATUS_LENGTH_REQUIRED;
							request.abort();
							break http_request_loop;
						}

						DownloadFileInfo fileInfo = Helpers.generateSaveFile(mContext, mInfo.mUri, mInfo.mHint, headerContentDisposition, headerContentLocation, mimeType, mInfo.mDestination,
										(headerContentLength != null) ? Integer.parseInt(headerContentLength) : 0);
						if (fileInfo.mFileName == null)
						{
							finalStatus = fileInfo.mStatus;
							request.abort();
							break http_request_loop;
						}
						filename = fileInfo.mFileName;
						stream = fileInfo.mStream;
						//#debug verbose
						base.tina.core.log.LogPrinter.v(Constants.TAG, "writing " + mInfo.mUri + " to " + filename);

						ContentValues values = new ContentValues();
						values.put(GlobalDownload._DATA, filename);
						if (headerETag != null)
						{
							values.put(Constants.ETAG, headerETag);
						}
						if (mimeType != null)
						{
							values.put(GlobalDownload.COLUMN_MIME_TYPE, mimeType);
						}
						int contentLength = -1;
						if (headerContentLength != null)
						{
							contentLength = Integer.parseInt(headerContentLength);
						}
						values.put(GlobalDownload.COLUMN_TOTAL_BYTES, contentLength);
						mContext.getContentResolver().update(contentUri, values, null, null);
					}

					InputStream entityStream;
					try
					{
						entityStream = response.getEntity().getContent();
					}
					catch (IOException ex)
					{
						//#ifdef debug
						//#if debug<=2
						if (Helpers.isNetworkAvailable(mContext))
						{
							base.tina.core.log.LogPrinter.i(Constants.TAG, "Get Failed " + mInfo.mId + ", Net Up");
						}
						else
						{
							base.tina.core.log.LogPrinter.i(Constants.TAG, "Get Failed " + mInfo.mId + ", Net Down");
						}
						//#endif
						//#endif
						if (!Helpers.isNetworkAvailable(mContext))
						{
							finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
						}
						else if (mInfo.mNumFailed < Constants.MAX_RETRIES)
						{
							finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
							countRetry = true;
						}
						else
						{
							//#debug
							base.tina.core.log.LogPrinter.d(Constants.TAG, "IOException getting entity for download " + mInfo.mId + " : " + ex);
							finalStatus = GlobalDownload.STATUS_HTTP_DATA_ERROR;
						}
						request.abort();
						break http_request_loop;
					}
					for (;;)
					{
						int bytesRead;
						try
						{
							bytesRead = entityStream.read(data);
						}
						catch (IOException ex)
						{
							//#ifdef debug
							//#if debug<=2
							if (Helpers.isNetworkAvailable(mContext))
							{
								base.tina.core.log.LogPrinter.i(Constants.TAG, "Read Failed " + mInfo.mId + ", Net Up");
							}
							else
							{
								base.tina.core.log.LogPrinter.i(Constants.TAG, "Read Failed " + mInfo.mId + ", Net Down");
							}
							//#endif
							//#endif

							ContentValues values = new ContentValues();
							values.put(GlobalDownload.COLUMN_CURRENT_BYTES, bytesSoFar);
							mContext.getContentResolver().update(contentUri, values, null, null);
							if (!mInfo.mNoIntegrity && headerETag == null)
							{
								//#debug
								base.tina.core.log.LogPrinter.d(Constants.TAG, "download IOException for download " + mInfo.mId + " : " + ex);
								finalStatus = GlobalDownload.STATUS_PRECONDITION_FAILED;
							}
							else if (!Helpers.isNetworkAvailable(mContext))
							{
								finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
							}
							else if (mInfo.mNumFailed < Constants.MAX_RETRIES)
							{
								finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
								countRetry = true;
							}
							else
							{
								//#debug
								base.tina.core.log.LogPrinter.d(Constants.TAG, "download IOException for download " + mInfo.mId + " : " + ex);
								finalStatus = GlobalDownload.STATUS_HTTP_DATA_ERROR;
							}
							request.abort();
							break http_request_loop;
						}
						if (bytesRead == -1)
						{ // success
							ContentValues values = new ContentValues();
							values.put(GlobalDownload.COLUMN_CURRENT_BYTES, bytesSoFar);
							if (headerContentLength == null)
							{
								values.put(GlobalDownload.COLUMN_TOTAL_BYTES, bytesSoFar);
							}
							mContext.getContentResolver().update(contentUri, values, null, null);
							if ((headerContentLength != null) && (bytesSoFar != Integer.parseInt(headerContentLength)))
							{
								if (!mInfo.mNoIntegrity && headerETag == null)
								{
									//#debug
									base.tina.core.log.LogPrinter.d(Constants.TAG, "mismatched content length for " + mInfo.mId);

									finalStatus = GlobalDownload.STATUS_LENGTH_REQUIRED;
								}
								else if (!Helpers.isNetworkAvailable(mContext))
								{
									finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
								}
								else if (mInfo.mNumFailed < Constants.MAX_RETRIES)
								{
									finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
									countRetry = true;
								}
								else
								{
									//#debug
									base.tina.core.log.LogPrinter.d(Constants.TAG, "closed socket for download " + mInfo.mId);

									finalStatus = GlobalDownload.STATUS_HTTP_DATA_ERROR;
								}
								break http_request_loop;
							}
							break;
						}
						gotData = true;
						for (;;)
						{
							try
							{
								if (stream == null)
								{
									stream = new FileOutputStream(filename, true);
								}
								stream.write(data, 0, bytesRead);
								break;
							}
							catch (IOException ex)
							{
								finalStatus = GlobalDownload.STATUS_FILE_ERROR;
								break http_request_loop;
							}
						}
						bytesSoFar += bytesRead;
						long now = System.currentTimeMillis();
						if (bytesSoFar - bytesNotified > Constants.MIN_PROGRESS_STEP && now - timeLastNotification > Constants.MIN_PROGRESS_TIME)
						{
							ContentValues values = new ContentValues();
							values.put(GlobalDownload.COLUMN_CURRENT_BYTES, bytesSoFar);
							mContext.getContentResolver().update(contentUri, values, null, null);
							bytesNotified = bytesSoFar;
							timeLastNotification = now;
						}

						//#debug verbose
						base.tina.core.log.LogPrinter.v(Constants.TAG, "downloaded " + bytesSoFar + " for " + mInfo.mUri);

						synchronized (mInfo)
						{
							if (mInfo.mControl == GlobalDownload.CONTROL_PAUSED)
							{
								//#debug verbose
								base.tina.core.log.LogPrinter.v(Constants.TAG, "paused " + mInfo.mUri);

								finalStatus = GlobalDownload.STATUS_RUNNING_PAUSED;
								request.abort();
								break http_request_loop;
							}
						}
						if (mInfo.mStatus == GlobalDownload.STATUS_CANCELED)
						{
							//#debug
							base.tina.core.log.LogPrinter.d(Constants.TAG, "canceled id " + mInfo.mId);

							finalStatus = GlobalDownload.STATUS_CANCELED;
							break http_request_loop;
						}
					}
					//#debug verbose
					base.tina.core.log.LogPrinter.v(Constants.TAG, "download completed for " + mInfo.mUri);

					finalStatus = GlobalDownload.STATUS_SUCCESS;
				}
				break;
			}
		}
		catch (FileNotFoundException ex)
		{
			//#debug
			base.tina.core.log.LogPrinter.d(Constants.TAG, "FileNotFoundException for " + filename + " : " + ex);

			finalStatus = GlobalDownload.STATUS_FILE_ERROR;
			// falls through to the code that reports an error
		}
		catch (RuntimeException ex)
		{
			//sometimes the socket code throws unchecked exceptions
			//#debug
			base.tina.core.log.LogPrinter.d(Constants.TAG, "Exception for id " + mInfo.mId, ex);

			finalStatus = GlobalDownload.STATUS_UNKNOWN_ERROR;
			// falls through to the code that reports an error
		}
		finally
		{
			mInfo.mHasActiveThread = false;
			if (wakeLock != null)
			{
				wakeLock.release();
				wakeLock = null;
			}
			if (client != null)
			{
				client.getConnectionManager().shutdown();//这里可能会有问题，还要再研究
				client = null;
			}
			try
			{
				// close the file
				if (stream != null)
				{
					stream.close();
				}
			}
			catch (IOException ex)
			{
				//#debug verbose
				base.tina.core.log.LogPrinter.v(Constants.TAG, "exception when closing the file after download : " + ex);

				// nothing can really be done if the file can't be closed
			}
			if (filename != null)
			{
				// if the download wasn't successful, delete the file
				if (GlobalDownload.isStatusError(finalStatus))
				{
					new File(filename).delete();
					filename = null;
				}
				else if (GlobalDownload.isStatusSuccess(finalStatus))
				{
					// make sure the file is readable
					//                    FileUtils.setPermissions(filename, 0644, -1, -1);//这个是本地方法实现的，先去掉

					// Sync to storage after completion
					try
					{
						new FileOutputStream(filename, true).getFD().sync();
					}
					catch (FileNotFoundException ex)
					{
						//#debug warn
						base.tina.core.log.LogPrinter.w(Constants.TAG, "file " + filename + " not found: " + ex);
					}
					catch (SyncFailedException ex)
					{
						//#debug warn
						base.tina.core.log.LogPrinter.w(Constants.TAG, "file " + filename + " sync failed: " + ex);
					}
					catch (IOException ex)
					{
						//#debug warn
						base.tina.core.log.LogPrinter.w(Constants.TAG, "IOException trying to sync " + filename + ": " + ex);
					}
					catch (RuntimeException ex)
					{
						//#debug warn
						base.tina.core.log.LogPrinter.w(Constants.TAG, "exception while syncing file: ", ex);
					}
				}
			}
			notifyDownloadCompleted(finalStatus, countRetry, retryAfter, redirectCount, gotData, filename, newUri, mimeType);
		}
	}

	/**
	 * Stores information about the completed download, and notifies the
	 * initiating application.
	 */
	private void notifyDownloadCompleted(int status, boolean countRetry, int retryAfter, int redirectCount, boolean gotData, String filename, String uri, String mimeType) {
		notifyThroughDatabase(status, countRetry, retryAfter, redirectCount, gotData, filename, uri, mimeType);
		if (GlobalDownload.isStatusCompleted(status))
		{
			notifyThroughIntent();
		}
	}

	private void notifyThroughDatabase(int status, boolean countRetry, int retryAfter, int redirectCount, boolean gotData, String filename, String uri, String mimeType) {
		ContentValues values = new ContentValues();
		values.put(GlobalDownload.COLUMN_STATUS, status);
		values.put(GlobalDownload._DATA, filename);
		if (uri != null)
		{
			values.put(GlobalDownload.COLUMN_URI, uri);
		}
		values.put(GlobalDownload.COLUMN_MIME_TYPE, mimeType);
		values.put(GlobalDownload.COLUMN_LAST_MODIFICATION, System.currentTimeMillis());
		values.put(Constants.RETRY_AFTER_X_REDIRECT_COUNT, retryAfter + (redirectCount << 28));
		if (!countRetry)
		{
			values.put(Constants.FAILED_CONNECTIONS, 0);
		}
		else if (gotData)
		{
			values.put(Constants.FAILED_CONNECTIONS, 1);
		}
		else
		{
			values.put(Constants.FAILED_CONNECTIONS, mInfo.mNumFailed + 1);
		}

		mContext.getContentResolver().update(ContentUris.withAppendedId(GlobalDownload.CONTENT_URI, mInfo.mId), values, null, null);
	}

	/**
	 * Notifies the initiating app if it requested it. That way, it can know
	 * that the download completed even if it's not actively watching the
	 * cursor.
	 */
	private void notifyThroughIntent() {
		Uri uri = Uri.parse(GlobalDownload.CONTENT_URI + "/" + mInfo.mId);
		mInfo.sendIntentIfRequested(uri, mContext);
	}

	private DefaultHttpClient newHttpClient(String userAgent) {
		HttpParams params = new BasicHttpParams();

		// Turn off stale checking.  Our connections break all the time anyway,
		// and it's not worth it to pay the penalty of checking every time.
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		// Default connection and socket timeout of 20 seconds.  Tweak to taste.
		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
		HttpConnectionParams.setSoTimeout(params, 20 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		// Don't handle redirects -- return them to the caller.  Our code
		// often wants to re-POST after a redirect, which we must do ourselves.
		HttpClientParams.setRedirecting(params, false);

		// Set the specified user agent and register standard protocols.
		HttpProtocolParams.setUserAgent(params, userAgent);
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ClientConnectionManager conman = new ThreadSafeClientConnManager(params, schemeRegistry);

		return new DefaultHttpClient(conman, params);
	}

	/**
	 * Clean up a mimeType string so it can be used to dispatch an intent to
	 * view a downloaded asset.
	 * 
	 * @param mimeType
	 *            either null or one or more mime types (semi colon separated).
	 * @return null if mimeType was null. Otherwise a string which represents a
	 *         single mimetype in lowercase and with surrounding whitespaces
	 *         trimmed.
	 */
	private String sanitizeMimeType(String mimeType) {
		try
		{
			mimeType = mimeType.trim().toLowerCase(Locale.ENGLISH);

			final int semicolonIndex = mimeType.indexOf(';');
			if (semicolonIndex != -1)
			{
				mimeType = mimeType.substring(0, semicolonIndex);
			}
			return mimeType;
		}
		catch (NullPointerException npe)
		{
			return null;
		}
	}
}
