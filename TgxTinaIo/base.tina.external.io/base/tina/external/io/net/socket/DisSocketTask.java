 /*******************************************************************************
  * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *******************************************************************************/
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
