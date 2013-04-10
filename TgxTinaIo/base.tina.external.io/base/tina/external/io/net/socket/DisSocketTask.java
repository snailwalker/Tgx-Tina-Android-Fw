package base.tina.external.io.net.socket;

import base.tina.external.io.IoSession;
import base.tina.external.io.IoTask;

public class DisSocketTask
		extends
		IoTask<NioSocketICon>
{

	public DisSocketTask(IoSession<NioSocketICon> ioSession)
	{
		super(ioSession);
	}

	public final static int	SerialNum	= SerialDomain + 10;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public void run() throws Exception {
		for (;;)
		{
			boolean dis = ioSession.disconnect.get();
			if (dis || ioSession.disconnect.compareAndSet(false, true))
			{
				LSocketTask lSocketTask = LSocketTask.open();
				for (;;)
				{
					boolean hasDis = lSocketTask.hasDisConnect.get();
					if (hasDis) break;
					else if (lSocketTask.hasDisConnect.compareAndSet(false, true))
					{
						lSocketTask.wakeUp();
						break;
					}
				}
				break;
			}
		}
	}
}
