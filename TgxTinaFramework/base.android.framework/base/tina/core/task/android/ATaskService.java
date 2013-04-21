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
package base.tina.core.task.android;

import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.SparseArray;
import base.tina.core.task.TaskService;
import base.tina.core.task.infc.ITaskRun;
import base.tina.core.task.infc.ITaskWakeTimer;

public class ATaskService
				extends
				TaskService
{
	PowerManager			powerManager;
	PowerManager.WakeLock	mainWakeLock;
	AlarmManager			alarmManager;
	//#debug
	volatile long			preWakeTime;
	final static long		AlarmGap		= TimeUnit.SECONDS.toMillis(600);
	Context					context;
	private boolean			isScreenOn;
	final BroadcastReceiver	screenReceiver	= new BroadcastReceiver()
											{

												@Override
												public void onReceive(Context context, Intent intent) {
													if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))
													{
														isScreenOn = false;
														//#debug
														base.tina.core.log.LogPrinter.d(null, "Screen Off Set Alarm | WakeLock");
														if (mainQueue.toWakeUpAbsoluteTime.get() > 0) setScheduleAlarmTime(mainQueue.toWakeUpAbsoluteTime.get());
														else wakeLock();
													}
													else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()))
													{
														//#debug
														base.tina.core.log.LogPrinter.d(null, "Screen On Release WakeLock");
														isScreenOn = true;
														wakeUnlock();
													}
													else if (TASK_SCHEDULE_ACTION.equals(intent.getAction()))
													{
														//#ifdef debug
														java.util.Calendar calendar = java.util.Calendar.getInstance();
														//#debug
														base.tina.core.log.LogPrinter.d(null, "CPU ON + AlarmReceiver1:-> cTime; " + java.text.DateFormat.getDateInstance().format(calendar.getTime()));
														//#endif
														/*
														 * 无需担心Unlock 当队列中的头节点
														 * 设置完时间时将释放wakelock
														 * ,当队列中无任务时也将自动释放wakelock
														 */
														wakeLock();
														wakeUp();
													}
													else if (TASK_ALARM_ACTION.equals(intent.getAction()))
													{
														int alarmKey = intent.getIntExtra(TASK_ALARM_ACTION, 0);
														//#ifdef debug
														java.util.Calendar calendar = java.util.Calendar.getInstance();
														//#debug
														base.tina.core.log.LogPrinter.d(null, "CPU ON + AlarmReceiver2:-> cTime; " + java.text.DateFormat.getDateInstance().format(calendar.getTime()));
														//#debug
														base.tina.core.log.LogPrinter.d(null, "toGet:" + alarmKey);
														//#endif
														TaskAlarmTimer taskAlarmTimer = taskAlarmMap.get(alarmKey);
														//#debug
														base.tina.core.log.LogPrinter.d(null, "TaskAlarmTimer:" + taskAlarmTimer);
														if (taskAlarmTimer != null) taskAlarmTimer.wakeUpTask();
														wakeLock(200);
													}
												}
											};

	public final boolean isScreenOn() {
		return isScreenOn;
	}

	/**
	 * @author Zhangzhuo
	 * @param RTC_WakeTime
	 *            负数将执行当前时间+AlarmGap <tt>long</tt> millsecond
	 */
	@Override
	protected final void setScheduleAlarmTime(long RTC_WakeTime) {
		//#ifdef debug
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTimeInMillis(RTC_WakeTime);
		//#debug
		base.tina.core.log.LogPrinter.d(null, "setAlarmTime:-> " + RTC_WakeTime + " Calendar:" + java.text.DateFormat.getDateInstance().format(calendar.getTime()));
		//#endif
		PendingIntent alarmSender = getAlarmCancel();
		if (alarmSender != null) alarmManager.cancel(alarmSender);
		if (RTC_WakeTime < 0) RTC_WakeTime = System.currentTimeMillis() + AlarmGap;
		alarmSender = getAlarmSet();
		alarmManager.set(AlarmManager.RTC_WAKEUP, RTC_WakeTime, alarmSender);
		wakeUnlock();
	}

	@Override
	protected final void noScheduleAlarmTime() {
		PendingIntent alarmSender = getAlarmCancel();
		if (alarmSender != null) alarmManager.cancel(alarmSender);
		wakeUnlock();
	}

	protected void onProcessorStop() {
		powerManager = null;
		alarmManager = null;
		this.context = null;
		started = false;
	}

	@Override
	public final ITaskWakeTimer setTaskAlarmTime(long RTC_WakeTime, ITaskWakeTimer owner, ITaskRun task) {
		if (owner != null) owner.cancel();
		else
		{
			owner = new TaskAlarmTimer();
			owner.setTask(task);
		}
		owner.setAlarmTime(RTC_WakeTime);
		return owner;
	}

	final SparseArray<TaskAlarmTimer>	taskAlarmMap	= new SparseArray<ATaskService.TaskAlarmTimer>(4);

	final class TaskAlarmTimer
					implements
					ITaskWakeTimer
	{

		public TaskAlarmTimer()
		{
			taskAlarmMap.put(hashCode(), this);
			//#debug
			base.tina.core.log.LogPrinter.d(null, "put TaskAlarmTimer:" + hashCode());
		}

		private ITaskRun			myTask;
		private volatile boolean	alarmOn;

		@Override
		public final void setAlarmTime(long absoluteTime) {
			if (absoluteTime < 0) absoluteTime = System.currentTimeMillis() + AlarmGap;
			PendingIntent tAlarmSender = tGetAlarmSet();
			if (tAlarmSender != null)
			{
				alarmOn = true;
				alarmManager.set(AlarmManager.RTC_WAKEUP, absoluteTime, tAlarmSender);
			}
		}

		@Override
		public final void cancel() {
			alarmOn = false;
			PendingIntent tAlarmSender = tGetAlarmCancel();
			if (tAlarmSender != null) alarmManager.cancel(tAlarmSender);
		}

		@Override
		public final void wakeUpTask() {
			if (alarmOn && myTask != null && myTask.needAlarm())
			{
				//#debug
				base.tina.core.log.LogPrinter.d(null, "Task wake up------------:" + myTask);
				myTask.wakeUp();
			}
		}

		@Override
		public final void setTask(ITaskRun myTask) {
			this.myTask = myTask;
		}

		@Override
		public final boolean isDisposable() {
			return true;
		}

		@Override
		public final void dispose() {
			cancel();
			taskAlarmMap.remove(hashCode());
			myTask = null;
		}

		private final PendingIntent tGetAlarmSet() {
			Intent intent = new Intent(TASK_ALARM_ACTION);
			intent.putExtra(TASK_ALARM_ACTION, hashCode());
			//#debug
			base.tina.core.log.LogPrinter.d(null, "TaskAlarmTimer:" + hashCode());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
			return pendingIntent;
		}

		private final PendingIntent tGetAlarmCancel() {
			Intent intent = new Intent(TASK_ALARM_ACTION);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, hashCode(), intent, PendingIntent.FLAG_NO_CREATE);
			return pendingIntent;
		}

	}

	protected final static String	TASK_SCHEDULE_ACTION	= "AlarmTaskSchedule";
	protected final static String	TASK_ALARM_ACTION		= "AlarmTaskSelf";
	private boolean					started;

	/**
	 * TaskService 启动函数 在此之前需要将listener都加入到监听队列中
	 * 
	 * @param context
	 */
	public final void startAService(Context context) {
		if (started) return;
		powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mainWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tina TaskService Schedule");
		mainWakeLock.setReferenceCounted(false);
		alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
		IntentFilter filter = new IntentFilter(TASK_ALARM_ACTION);
		filter.addAction(TASK_SCHEDULE_ACTION);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		context.registerReceiver(screenReceiver, filter);
		this.context = context;
		startService();
		started = true;
	}

	public final void stopAService() {
		if (!started) return;
		context.unregisterReceiver(screenReceiver);
		wakeUnlock();
		mainWakeLock = null;
		PendingIntent alarmSender = getAlarmCancel();
		if (alarmSender != null) alarmManager.cancel(alarmSender);
		stopService();
	}

	private final PendingIntent getAlarmSet() {
		Intent intent = new Intent(TASK_SCHEDULE_ACTION);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
		return pendingIntent;
	}

	private final PendingIntent getAlarmCancel() {
		Intent intent = new Intent(TASK_SCHEDULE_ACTION);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, hashCode(), intent, PendingIntent.FLAG_NO_CREATE);
		return pendingIntent;
	}

	public final void wakeLock() {
		wakeLock(0);
	}

	public final void wakeLock(long duration) {
		if (mainWakeLock != null)
		{
			if (duration > 0) mainWakeLock.acquire(duration);
			else
			{
				mainWakeLock.acquire();
				//#ifdef debug
				preWakeTime = System.currentTimeMillis();
				//#debug
				base.tina.core.log.LogPrinter.d(null, "WakeLock Time:->" + preWakeTime);
				//#endif
			}
		}
	}

	public final void wakeUnlock() {
		if (mainWakeLock == null || !mainWakeLock.isHeld()) return;
		//#ifdef debug
		long wakeKeepTime = System.currentTimeMillis() - preWakeTime;
		//#debug
		base.tina.core.log.LogPrinter.d(null, "WakeLock Keep Time:->" + wakeKeepTime);
		//#endif
		mainWakeLock.release();
	}

	@Override
	protected IWakeLock getWakeLock() {
		IWakeLock taskWakeLock = new IWakeLock()
		{
			private PowerManager.WakeLock	wakeLock;

			@Override
			public void release() {
				wakeLock.release();
			}

			@Override
			public void initialize(String lockName) {
				wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);
				wakeLock.setReferenceCounted(false);
			}

			@Override
			public void acquire() {
				wakeLock.acquire();
			}
		};
		return taskWakeLock;
	}

}
