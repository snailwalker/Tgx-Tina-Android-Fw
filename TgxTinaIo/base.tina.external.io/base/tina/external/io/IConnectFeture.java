package base.tina.external.io;

import base.tina.core.task.timer.TimerTask;


public interface IConnectFeture
{
	public boolean isMySession(IoSession<?> ioSession);
	
	public void finishConnect(IoSession<?> ioSession);
	
	public void resetConnect(IoSession<?> ioSession);
	
	public void connectTarAddr(IoFilter ioFilter, int bindSerial);
	
	public void connectByTimer(TimerTask timerTask, int bindSerial);
	
	public String getTarAddr();
	
	public void onConnectFaild(Exception exception);
	
}
