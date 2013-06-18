package com.tgx.io.connect.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.tgx.tina.android.ipc.framework.BaseService;


public abstract class ConnectionService
        extends
        BaseService
{
	
	static volatile int               lastNetType     = -1;
	static volatile NetworkInfo.State lastNetState    = NetworkInfo.State.UNKNOWN;
	static volatile String            lastWifiAP_SSID = null;
	static volatile String            lastWifiAP_MAC  = null;
	
	public static volatile boolean    isWifi;
	public static volatile boolean    networkOk;
	public static volatile boolean    networkChType;
	
	@Override
	public void onCreate() {
		super.onCreate();
		ConnectReceiver.service = this;
		checkNetwork(this);
	}
	
	@Override
	public void onDestroy() {
		ConnectReceiver.service = null;
		super.onDestroy();
	}
	
	public abstract void onNetworkChange();
	
	final static String TAG = "CONNECT_TGX";
	
	static void checkNetwork(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		boolean networkOk = false;
		boolean networkChType = false;
		NetworkInfo.State nowNetState = NetworkInfo.State.UNKNOWN;
		int nowNetType = -1;
		isWifi = false;
		if (info != null && info.isAvailable())
		{
			nowNetState = info.getState();
			nowNetType = info.getType();
			//#debug
			base.tina.core.log.LogPrinter.d(TAG, "network type: " + nowNetType + " |now state: " + nowNetState.name());
			if (ConnectivityManager.isNetworkTypeValid(nowNetType))//
			{
				isWifi = nowNetType == ConnectivityManager.TYPE_WIFI;
				if (lastNetType != nowNetType)//从无到有，或者首次检测网络装态时
				{
					//#debug
					base.tina.core.log.LogPrinter.d(TAG, "change network type: from " + lastNetType + " to " + nowNetType);
					networkChType = true;
				}
				else
				//网路类型一致
				{
					switch (nowNetType) {
						case ConnectivityManager.TYPE_WIFI:
							switch (nowNetState) {
								case CONNECTED: {
									WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
									WifiInfo wifiInfo = wifi.getConnectionInfo();
									String ap_Mac = wifiInfo.getBSSID();
									String ap_SSID = wifiInfo.getSSID();
									if (!ap_Mac.equalsIgnoreCase(lastWifiAP_MAC) || !ap_SSID.equals(lastWifiAP_SSID) || !lastNetState.equals(NetworkInfo.State.CONNECTED))
									{
										//#debug
										if (!ap_Mac.equalsIgnoreCase(lastWifiAP_MAC) || !ap_SSID.equals(lastWifiAP_SSID)) base.tina.core.log.LogPrinter.d(TAG, "change wifi network from" + lastWifiAP_SSID + "@" + lastWifiAP_MAC + " to " + ap_SSID + "@" + ap_Mac);
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
						//API lv8	
						/*
						 * case ConnectivityManager.TYPE_WIMAX: case
						 * ConnectivityManager.TYPE_MOBILE_DUN: case
						 * ConnectivityManager.TYPE_MOBILE_HIPRI: case
						 * ConnectivityManager.TYPE_MOBILE_MMS: case
						 * ConnectivityManager.TYPE_MOBILE_SUPL:
						 */
						case ConnectivityManager.TYPE_MOBILE:
							//#debug
							base.tina.core.log.LogPrinter.d(TAG, "not wifi type");
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
									if (lastNetState.equals(NetworkInfo.State.CONNECTED))
									{
										//#debug
										base.tina.core.log.LogPrinter.d(TAG, "mobile network is disconnected!");
										networkChType = true;
									}
									break;
							}
							break;
						default:
							//#debug
							base.tina.core.log.LogPrinter.d(TAG, "unknown type: " + nowNetType);
							switch (nowNetState) {
								case CONNECTED:
									if (!lastNetState.equals(nowNetState))
									{
										//#debug
										base.tina.core.log.LogPrinter.d(TAG, "unknow network is connected!");
										networkChType = true;
									}
									break;
								default:
									if (lastNetState.equals(NetworkInfo.State.CONNECTED))
									{
										//#debug
										base.tina.core.log.LogPrinter.d(TAG, "unknow network is disconnected!");
										networkChType = true;
									}
									break;
							}
							break;
					}
				}
				
			}
			lastNetType = nowNetType;
			lastNetState = nowNetState;
			networkOk = info.isConnected();
		}
		else //no info or no available info
		{
			networkChType = lastNetState.equals(NetworkInfo.State.CONNECTED);
			//#debug 
			base.tina.core.log.LogPrinter.d(TAG, "network disconnect");
			lastNetType = -1;
			lastNetState = nowNetState;
		}
		ConnectionService.networkChType = networkChType;
		ConnectionService.networkOk = networkOk;
		//#debug
		base.tina.core.log.LogPrinter.d(TAG, "NW change: " + networkChType + " NW type: " + lastNetType + " info.Connected: " + networkOk);
		
	}
}
