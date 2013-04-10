package base.tina.core.task;

import base.tina.core.task.infc.ITaskListener;

public abstract class AbstractListener
		implements
		ITaskListener
{
	private int	bindSerial;

	@Override
	public void setBindSerial(int bindSerial) {
		if (this.bindSerial == 0) this.bindSerial = bindSerial;
	}

	@Override
	public int getBindSerial() {
		return bindSerial;
	}

	@Override
	public boolean isEnable() {
		return enabled;
	}

	protected boolean	enabled;

	public AbstractListener()
	{
		enabled = true;
		setBindSerial(hashCode());
	}
}
