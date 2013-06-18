package base.tina.external.io;

import base.tina.core.task.infc.ITaskListener;
import base.tina.external.io.net.socket.CSocketTask;
import base.tina.external.io.net.socket.NioSocketICon;


public interface IoFeture<T extends IConnection>
{
	public IoSession<T> getSession();
	
	public boolean isConnected();
	
	public boolean isConnectedOrConnecting();
	
	public boolean single();
	
	public String getTargetAddress();
	
	public State getState();
	
	public void connect(final IoFilter filter, final ITaskListener ioHandler);
	
	public void disconnect();
	
	public void onConnected(final IoSession<NioSocketICon> session);
	
	public void onConnnectFaild(CSocketTask cSocketTask);
	
	public void onConnectionReset(IoSession<NioSocketICon> session);
	
	public static enum State {
		CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, RESET, UNKNOWN
	}
	
}
