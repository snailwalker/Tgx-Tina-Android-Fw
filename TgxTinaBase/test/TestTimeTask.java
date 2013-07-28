import java.util.concurrent.TimeUnit;

import base.tina.core.log.LogPrinter;
import base.tina.core.log.SimplePrinter;
import base.tina.core.task.Task;
import base.tina.core.task.TaskService;
import base.tina.core.task.infc.ITaskListener;
import base.tina.core.task.infc.ITaskResult;


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
		test.requestService(new TestTask1(), false);
		for (int i = 0; i < 10; i++)
		{
			test.requestService(new TestTask2(), false);
			try
			{
				Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			}
			catch (Exception e)
			{
				// ignore
			}
		}
		try
		{
			Thread.sleep(TimeUnit.SECONDS.toMillis(15));
		}
		catch (Exception e)
		{
			// ignore
		}
		for (int i = 0; i < 10; i++)
		{
			test.requestService(new TestTask2(), false);
			try
			{
				Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			}
			catch (Exception e)
			{
				// ignore
			}
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
	
	final static int SerialNum = 0x111222;
	
	static class TestTask1
	        extends
	        Task
	{
		
		public TestTask1() {
			super(SerialNum);
		}
		
		@Override
		public void initTask() {
			isBloker = true;
			isCycle = true;
			super.initTask();
		}
		
		@Override
		public void run() throws Exception {
			synchronized (this)
			{
				try
				{
					wait(TimeUnit.SECONDS.toMillis(30));
				}
				catch (InterruptedException e)
				{
					//Ignore
				}
			}
			LogPrinter.d(null, "Task1 -- run ok");
		}
		
		final static int SerialNum = -0xFFFF01;
		
		@Override
		public int getSerialNum() {
			return SerialNum;
		}
		
	}
	
	static class TestTask2
	        extends
	        Task
	{
		
		public TestTask2() {
			super(SerialNum);
		}
		
		@Override
		public void initTask() {
			isBloker = true;
			super.initTask();
		}
		
		@Override
		public void run() throws Exception {
			synchronized (this)
			{
				try
				{
					wait(TimeUnit.SECONDS.toMillis(2));
				}
				catch (InterruptedException e)
				{
					//Ignore
				}
			}
		}
		
		final static int SerialNum = -0xFFFF02;
		
		@Override
		public int getSerialNum() {
			return SerialNum;
		}
	}
}
