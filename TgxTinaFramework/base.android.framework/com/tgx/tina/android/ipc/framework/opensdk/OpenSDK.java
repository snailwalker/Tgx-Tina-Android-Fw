package com.tgx.tina.android.ipc.framework.opensdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.tgx.tina.android.ipc.framework.BaseBridge;
import com.tgx.tina.android.ipc.framework.IBridge;


public abstract class OpenSDK<T extends BaseBridge, E extends IBridge<T>>
{
	private String                      lastBootAction;
	private final Context               context;
	private final ConsultResultReceiver resultReceiver;
	private final ThirdParty            tpImpl;
	protected final T                   mBridge;
	protected final E                   mIBridge;
	
	protected OpenSDK(Context ctx, T t, E e, ThirdParty tpImpl) {
		context = ctx;
		resultReceiver = new ConsultResultReceiver();
		mBridge = t;
		mIBridge = e;
		this.tpImpl = tpImpl;
	}
	
	public void init() {
		IntentFilter filter0 = new IntentFilter(tpImpl.getConsultResultAction());
		filter0.addDataAuthority(tpImpl.getConsultResultAuthority(), null);
		filter0.addDataScheme(tpImpl.getConsultResultScheme());
		context.registerReceiver(resultReceiver, filter0, tpImpl.getConsultResultPermission(), null);
		Intent aIntent = new Intent(tpImpl.getVoteAction());
		aIntent.setData(Uri.parse(tpImpl.getVoteData()));
		context.sendOrderedBroadcast(aIntent, tpImpl.getVotePermission());
		//#debug
		base.tina.core.log.LogPrinter.d("VOTE", "CRAct: " + tpImpl.getConsultResultAction() + " DS: " + tpImpl.getConsultResultScheme() + tpImpl.getConsultResultAuthority() + " CRPer: " + tpImpl.getConsultResultPermission() + " VAct: " + tpImpl.getVoteAction() + " VD: " + tpImpl.getVoteData() + " VPer: " + tpImpl.getVotePermission());
	}
	
	public void beforeTerminate() {
		context.unregisterReceiver(resultReceiver);
	}
	
	public abstract void onActionFirstSet();
	
	public boolean isNeedRebind() {
		boolean yes = needRebind;
		needRebind = false;
		return yes;
	}
	
	private boolean needRebind;
	
	class ConsultResultReceiver
	        extends
	        BroadcastReceiver
	{
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String bootAction = intent.getStringExtra("bootAction");
			//#debug
			base.tina.core.log.LogPrinter.d("CONSULT", bootAction);
			if (lastBootAction != null && lastBootAction.equals(bootAction))
			{
				//#debug 
				base.tina.core.log.LogPrinter.d("CONSULT", "same action, Ignore");
				return;
			}
			else if (lastBootAction == null && mBridge != null)
			{
				//#debug 
				base.tina.core.log.LogPrinter.d("CONSULT", "first bind");
				lastBootAction = bootAction;
				mIBridge.setBootAction(bootAction);
				onActionFirstSet();
			}
			else if (lastBootAction != null && !lastBootAction.equals(bootAction))
			{
				//#debug 
				base.tina.core.log.LogPrinter.d("CONSULT", "change bind");
				lastBootAction = bootAction;
				needRebind = true;
				mBridge.stopBind();
			}
		}
	}
}
