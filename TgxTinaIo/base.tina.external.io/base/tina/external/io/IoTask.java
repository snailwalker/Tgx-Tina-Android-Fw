package base.tina.external.io;

import base.tina.core.task.Task;


public abstract class IoTask<E extends IConnection>
        extends
        Task
{
	public String       url;
	public IoFilter     ioFilterChain;
	public Object       toWrite;
	public IoSession<E> ioSession;
	
	public IoTask(String url, IoFilter ioFilter) {
		this(0, url, ioFilter);
	}
	
	public IoTask(int threadId, String url, IoFilter ioFilter) {
		super(threadId);
		if (url != null) this.url = formatUrl(url);
		ioFilterChain = ioFilter;
	}
	
	public IoTask(int threadId, IoFilter ioFilter) {
		this(threadId, null, ioFilter);
	}
	
	public IoTask(IoSession<E> ioSession, IoFilter ioFilter) {
		super(0);
		this.ioSession = ioSession;
		ioFilterChain = ioFilter;
		url = ioSession.url;
	}
	
	public IoTask(IoSession<E> ioSession) {
		this(ioSession, null);
	}
	
	private final String formatUrl(String url) {
		return IoUtil.mergeURL(IoUtil.splitURL(url));
	}
	
	protected void clone(IoTask<E> ioTask) {
		super.clone(ioTask);
		url = ioTask.url;
		ioFilterChain = ioTask.ioFilterChain;
	}
	
	public void dispose() {
		if (ioFilterChain != null) ioFilterChain.dispose(false);
		ioFilterChain = null;
		ioSession = null;
		toWrite = null;
		url = null;
		super.dispose();
	}
	
	public final void retry(String url) {
		retry();
		if (url != null) this.url = formatUrl(url);
	}
	
	protected final static int SerialDomain = -0x1000;
	
}
