import base.tina.core.log.LogPrinter;
import base.tina.core.log.SimplePrinter;
import base.tina.core.task.TaskService;
import base.tina.core.task.infc.ITaskListener;
import base.tina.core.task.infc.ITaskResult;
import base.tina.core.task.timer.TimerTask;


public class TestTimeTask
        extends
        TaskService
        implements
        ITaskListener
{
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LogPrinter.setIPrinter(new SimplePrinter());
		final TestTimeTask test = new TestTimeTask();
		TimerTask t;
		test.requestService(t = new TimerTask(4)
		{
			
			@Override
			public int getSerialNum() {
				return 0x9900;
			}
			
			@Override
			protected boolean doTimeMethod() {
				LogPrinter.d(null, "do:" + "--" + hashCode());
				return false;
			}
		}, test.getBindSerial());
		for (;;)
			try
			{
				Thread.sleep(1000);
				t.refresh(2);
			}
			catch (Exception e)
			{
			}
		
	}
	
	public TestTimeTask() {
		super();
		addListener(this);
		setRecycle(this);
		startService();
	}
	
	@Override
	public boolean ioHandle(ITaskResult message, TaskService service) {
		LogPrinter.d(null, "x" + message);
		return true;
	}
	
	@Override
	public boolean exceptionCaught(ITaskResult taskResult, TaskService service) {
		return true;
	}
	
	@Override
	public boolean isEnable() {
		return true;
	}
	
	@Override
	public void setBindSerial(int bindSerial) {
		
	}
	
	@Override
	public int getBindSerial() {
		return SerialNum;
	}
	
	final static int SerialNum = 0x011122;
}
