package base.tina.external.io.net.socket;

import java.nio.channels.Selector;


public interface ISelectorX
{
	public Selector getSelector();
	
	public void wakeUp();
}
