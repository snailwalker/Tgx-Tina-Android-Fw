package com.tgx.io.connect.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import base.tina.external.io.IoFilter;

import com.tgx.tina.android.ipc.framework.BaseService;


public abstract class ConnectionService
        extends
        BaseService
{
	
	static int                     lastNetType     = ConnectivityManager.TYPE_MOBILE;
	static NetworkInfo.State       lastNetState    = NetworkInfo.State.UNKNOWN;
	static String                  lastWifiAP_SSID = null;
	static String                  lastWifiAP_MAC  = null;
	
	public static volatile boolean isWifi;
	public static volatile boolean networkOk;
	public static volatile boolean networkChType;
	
	@Override
	public void onCreate() {
		super.onCreate();
		ConnectReceiver.service = this;
		checkNetwork(this);
	}
	
	@Override
	public void onDestroy() {
		ConnectReceiver.service = null;
		getSocketListener().dispose();
		getSocketFilter().dispose();
		super.onDestroy();
	}
	
	public abstract SocketIOListener getSocketListener();
	
	public abstract IoFilter getSocketFilter();
	
	final static String TAG = "Connect";
	
	static void checkNetwork(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		networkOk = false;
		if (info != null && info.isAvailable())
		{
			int nowNetType = info.getType();
			if (ConnectivityManager.isNetworkTypeValid(lastNetType))
			{
				NetworkInfo.State nowNetState = info.getState();
				if (lastNetType != nowNetType)
				{
					//#debug
					base.tina.core.log.LogPrinter.d(TAG, "change network type: from " + lastNetType + " to " + nowNetType);
					networkChType = true;
				}
				else
				{
					switch (nowNetType) {
						case ConnectivityManager.TYPE_WIFI:
							switch (nowNetState) {
								case CONNECTED: {
									WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
									WifiInfo wifiInfo = wifi.getConnectionInfo();
									String ap_Mac = wifiInfo.getBSSID();
									String ap_SSID = wifiInfo.getSSID();
									if (ap_Mac != lastWifiAP_MAC || ap_SSID != lastWifiAP_SSID || !lastNetState.equals(NetworkInfo.State.CONNECTED))
									{
										//#debug
										if (ap_Mac != lastWifiAP_MAC || ap_SSID != lastWifiAP_SSID) base.tina.core.log.LogPrinter.d(TAG, "change wifi network from" + lastWifiAP_SSID + "@" + lastWifiAP_MAC + " to " + ap_SSID + "@" + ap_Mac);
										//#debug
										if (!lastNetState.equals(NetworkInfo.State.CONNECTED)) base.tina.core.log.LogPrinter.d(TAG, "wifi network has been connected!");
										networkChType = true;
									}
									lastWifiAP_MAC = ap_Mac;
									lastWifiAP_SSID = ap_SSID;
									isWifi = true;
								}
									break;
								default:
									if (lastNetState.equals(NetworkInfo.State.CONNECTED))
									{
										//#debug
										base.tina.core.log.LogPrinter.d(TAG, "wifi network isn't connected!");
										networkChType = true;
									}
									break;
							}
							break;
						case ConnectivityManager.TYPE_MOBILE:
							switch (nowNetState) {
								case CONNECTED:
									if (!lastNetState.equals(nowNetState))
									{
										//#debug
										base.tina.core.log.LogPrinter.d(TAG, "mobile network is connected!");
										networkChType = true;
									}
									break;
								default:
									break;
							}
							break;
						default:
							break;
					}
					
				}
				lastNetState = nowNetState;
			}
			lastNetType = nowNetType;
			networkOk = info.isConnected();
		}
		else
		{
			//#debug 
			base.tina.core.log.LogPrinter.d(TAG, "network disconnect");
			lastNetType = -1;
			lastNetState = NetworkInfo.State.UNKNOWN;
		}
	}
}
