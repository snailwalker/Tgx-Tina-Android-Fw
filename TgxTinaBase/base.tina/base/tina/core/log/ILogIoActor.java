package base.tina.core.log;

import java.io.Closeable;
import java.io.IOException;


/**
 * @author Zhangzhuo
 */
public interface ILogIoActor
        extends
        Closeable
{
	public int write(String tag, int priority, String msg, Throwable throwable) throws IOException;
}
