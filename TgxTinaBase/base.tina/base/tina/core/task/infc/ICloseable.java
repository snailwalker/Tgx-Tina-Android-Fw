package base.tina.core.task.infc;

public interface ICloseable
{
	/**
	 * 关闭当前链接,如果是阻塞性IO操作
	 * 将抛出InterruptExcetion
	 * 
	 * @param await
	 *            是否等待通道内数据完成传送后再进行关闭<br>
	 *            <code>true</code> 等待;<br>
	 *            有两种可能的行为: <br>
	 *            1) 同步实现,此方法将阻塞<br>
	 *            2) 异步实现,仅给当前通道标注toClose标记,由通道自身决定何时关闭 <code>false</code> 不等待
	 *            立即执行 可能中断当前的读写操作
	 * @throws Exception
	 */
	public void close(boolean await) throws Exception;
}
