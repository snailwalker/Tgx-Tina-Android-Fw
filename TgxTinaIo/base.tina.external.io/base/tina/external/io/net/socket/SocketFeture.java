package base.tina.external.io.net.socket;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import base.tina.core.task.TaskService;
import base.tina.core.task.infc.IDisposable;
import base.tina.core.task.infc.ITaskListener;
import base.tina.core.task.infc.ITaskResult;
import base.tina.core.task.timer.TimerTask;
import base.tina.external.io.IConnection;
import base.tina.external.io.IoFeture;
import base.tina.external.io.IoFilter;
import base.tina.external.io.IoService;
import base.tina.external.io.IoSession;
import base.tina.external.io.IoUtil;


public abstract class SocketFeture
        implements
        IoFeture<NioSocketICon>,
        ITaskListener,
        IDisposable
{
	final static String                tag         = "SOCKET_FETURE";
	protected IoSession<NioSocketICon> session;
	protected IoService                ioService;
	protected TimerTask                timer;
	protected final String             url;
	private State                      state       = State.UNKNOWN;
	private int                        bindSerial;
	private boolean                    enabled;
	private int                        pF0         = 0;
	private int                        pF1         = 1;
	private int                        nextConTime = pF0 + pF1;
	static
	{
		System.setProperty("java.net.preferIPv6Addresses", "false");
	}
	
	public SocketFeture(final String tarAddr, IoService ioService) {
		this.ioService = ioService;
		enabled = true;
		if (tarAddr == null || "".equals(tarAddr)) throw new NullPointerException();
		String[] split = IoUtil.splitURL(tarAddr);
		InetSocketAddress address = new InetSocketAddress(split[IoUtil.HOST], Integer.parseInt(split[IoUtil.PORT]));
		byte[] rawAddr = address.getAddress().getAddress();
		url = split[IoUtil.PROTOCOL] + "://" + (rawAddr[0] & 0xFF) + '.' + (rawAddr[1] & 0xFF) + '.' + (rawAddr[2] & 0xFF) + '.' + (rawAddr[3] & 0xFF) + ':' + split[IoUtil.PORT] + '/';
		setBindSerial(hashCode());
	}
	
	protected final void setNextConTime(int second) {
		nextConTime = second;
	}
	
	@Override
	public IoSession<NioSocketICon> getSession() {
		if (!isConnected()) return null;
		return session;
	}
	
	@Override
	public void dispose() {
		enabled = false;
		session = null;
		state = null;
		timer = null;
		ioService = null;
	}
	
	@Override
	public boolean isDisposable() {
		return true;
	}
	
	@Override
	public boolean isConnected() {
		return state.equals(State.CONNECTED);
	}
	
	@Override
	public final boolean isConnectedOrConnecting() {
		return state.equals(State.CONNECTED) || state.equals(State.CONNECTING);
	}
	
	@Override
	public final String getTargetAddress() {
		return url;
	}
	
	@Override
	public State getState() {
		return state;
	}
	
	@Override
	public boolean single() {
		return false;
	}
	
	@Override
	public void connect(final IoFilter filter, final ITaskListener ioHandler) {
		ioService.addHandler(ioHandler);
		state = State.CONNECTING;
		ioService.requestService(getTargetAddress(), IConnection.READ_WRITE, filter, ioHandler.getBindSerial());
		if (nextConTime > 900)
		{
			pF0 = 1;
			pF1 = 2;
			nextConTime = 901;
		}
		else
		{
			nextConTime = pF0 + pF1;
			pF0 = pF1;
			pF1 = nextConTime;
		}
	}
	
	public int getNextDelay() {
		return nextConTime;
	}
	
	public void onTimer(TimerTask timer) {
		boolean same = this.timer != null && this.timer == timer;
		//#debug 
		base.tina.core.log.LogPrinter.d(tag, "same: " + same + " |" + (this.timer != null ? "old.timer: " + this.timer.getDelay(TimeUnit.MILLISECONDS) : "null 0") + " new.timer: " + timer.getDelay(TimeUnit.MILLISECONDS));
		if (this.timer == null || this.timer.isDone() || (!same && this.timer.getDelay(TimeUnit.SECONDS) > timer.getDelay(TimeUnit.SECONDS)))
		{
			if (this.timer != null)
			{
				//#debug
				base.tina.core.log.LogPrinter.d(tag, "close timer: " + this.timer);
				this.timer.setDone();
				this.timer = null;
			}
			//#debug 
			base.tina.core.log.LogPrinter.d(tag, "change timer from: " + this.timer + " to " + timer);
			this.timer = timer;
			same = false;
		}
	}
	
	@Override
	public void disconnect() {
		state = State.DISCONNECTING;
		ioService.requestService(session, IConnection.CLOSE, false, 0);
	}
	
	@SuppressWarnings ("unchecked")
	@Override
	public boolean exceptionCaught(ITaskResult task, TaskService service) {
		boolean isHandled = true;
		switch (task.getSerialNum()) {
			case IoSession.SerialNum:
				state = State.DISCONNECTED;
				onConnectionReset((IoSession<NioSocketICon>) task);
				close();
				break;
			case CSocketTask.SerialNum:
				state = State.RESET;
				onConnnectFaild((CSocketTask) task);
				close();
				break;
			case SocketTask.SerialNum:
				//#debug
				base.tina.core.log.LogPrinter.d(tag, "socket caught");
				break;
			default:
				isHandled = false;
				break;
		}
		return isHandled;
	}
	
	@Override
	public int getBindSerial() {
		return bindSerial;
	}
	
	@SuppressWarnings ("unchecked")
	@Override
	public boolean ioHandle(ITaskResult taskOrResult, TaskService service) {
		boolean isHandled = true;
		switch (taskOrResult.getSerialNum()) {
			case IoSession.SerialNum:
				state = State.CONNECTED;
				onConnected(session = (IoSession<NioSocketICon>) taskOrResult);
				break;
			case CSocketTask.SerialNum:
				//#debug
				base.tina.core.log.LogPrinter.d(tag, "csocket over");
				break;
			case SocketTask.SerialNum:
				//#debug
				base.tina.core.log.LogPrinter.d(tag, "socket over");
				break;
			default:
				isHandled = false;
				break;
		}
		return isHandled;
	}
	
	@Override
	public boolean isEnable() {
		return enabled;
	}
	
	@Override
	public void setBindSerial(int bindSerial) {
		this.bindSerial = bindSerial;
	}
	
	public void close() {
		ioService.rmHandler(getBindSerial());
		dispose();
	}
}
