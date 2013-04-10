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
 package base.tina.external.io;

import java.util.concurrent.TimeoutException;

import base.tina.core.task.infc.ICloseable;

/**
 * 提供 Connection封装,虽然从Channel继承会得到良好的系统支持属性<br>
 * 但是系统直接提供了IOException的约束,不利于集成抽象意义上的IO操作
 * 
 * @author Zhangzhuo
 */

public interface IConnection
		extends
		ICloseable
{
	/**
	 * @return true 当前通道出于打开状态<br>
	 *         false 当前通道已关闭,包含 已被彻底关闭以及await to close状态
	 */
	public boolean isOpen();

	/**
	 * @param
	 * @return 打开通道操作的结果
	 * @throws IllegalStateException
	 *             通道已关闭时将抛出此异常;
	 * @throws IllegalArgumentException
	 *             Connection创建时传递的参数不符合打开通道的要求;
	 * @throws TimeoutException
	 *             当通道在打开时存在阻塞特性时,超时过程将抛出此异常
	 * @throws Exception
	 *             其他类型的异常
	 */
	public boolean open(String toConnect, boolean await) throws IllegalStateException, IllegalArgumentException, TimeoutException, Exception;

	/**
	 * 当前通道的连接状态
	 * 
	 * @return true 已经完成连接<br>
	 *         false 尚未完成连接,原因可能比较多
	 */
	public boolean isConnected() throws Exception;

	/**
	 * 获取当前通道中可供读取的内容总量,单位由通道特性决定
	 * 
	 * @return 当前通道中可供读取的内容总量<br>
	 *         -1表示无法预估
	 */
	public int available();

	public final static int	WRITE		= 1 << 1;
	public final static int	READ		= 1 << 0;
	public final static int	READ_WRITE	= WRITE | READ;
	public final static int	READ_ONLY	= 1 << 2;
	public final static int	CLOSE		= 1 << 3;

}
