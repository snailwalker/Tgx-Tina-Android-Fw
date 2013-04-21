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

import base.tina.core.task.Task;
import base.tina.core.task.infc.IDisposable;
import base.tina.core.task.infc.ITaskResult;

/**
 * 过滤器链,用于处理IConnection通道上的数据双向封包解包
 * 1.0版本不支持单独处理链节点注销再连接,dispose特性将注销当前处理节点之后所有的相关节点
 * 
 * @version 1.0
 * @author Zhangzhuo
 * @see #dispose(boolean)
 */

public abstract class IoFilter
		implements
		IDisposable
{
	public static enum ResultType
	{
		NOT_OK, NEED_DATA, OK, HANDLED, INSIDE
	};

	protected String	name;
	protected IoFilter	nextFilter;
	protected IoFilter	preFilter;
	protected boolean	isManual;

	/**
	 * 默认为单次使用过滤器,将在{@link #dispose(boolean)}中完成注销
	 * 
	 * @author Zhangzhuo
	 * @param name
	 *            处理器名称,对过滤器进行增删时的标示,需唯一化
	 * @see #IoFilter(String, boolean)
	 */
	public IoFilter(String name)
	{
		this.name = name;
	}

	/**
	 * @author Zhangzhuo
	 * @param name
	 *            处理器名称,对过滤器进行增删时的标示,需唯一化
	 * @param isManual
	 *            是否手动注销过滤器实例.如果需要对过滤器进行重用需要指定为true
	 */
	public IoFilter(String name, boolean isManual)
	{
		this.name = name;
		this.isManual = isManual;
	}

	/**
	 * Endoder 过滤器链
	 * 
	 * @author Zhangzhuo
	 * @param task
	 *            当前Filter的任务归属
	 * @param ioSession
	 *            已建立的处理管道 以及携带处理过程中的Context
	 * @param content
	 *            需要进行处理的目标数据,贯穿处理链的中间过程的状态存单
	 * @return 已通过处理链完成的处理结果值
	 * @throws NullPointerException
	 *             当context 为空时抛出此异常
	 * @throws Exception
	 *             执行过程中可能出现的其他异常
	 * @see #preEncode(IoTask, IConnection, Object)
	 */
	public final Object filterChainEncode(Task task, IoSession<?> ioSession, Object content) throws NullPointerException, Exception {
		if (content == null) throw new NullPointerException("Nothing to encode!");
		ResultType resultType = ResultType.OK;
		IoFilter nextFilter = this;
		SearchHandler:
		{
			do
			{
				resultType = nextFilter.preEncode(task, ioSession, content);
				switch (resultType) {
					case NOT_OK:
						//#debug warn
						base.tina.core.log.LogPrinter.w(null, "Content NOT_OK: " + content.getClass().getSimpleName() + "@" + hashCode());
						return null;
					case NEED_DATA:
						//#debug warn
						base.tina.core.log.LogPrinter.w(null, "Content NEED_DATA: " + content.getClass().getSimpleName() + "@" + hashCode());
						return null;
					case OK:
						break;
					case HANDLED:
						content = nextFilter.encode(task, ioSession, content);
						break SearchHandler;
					case INSIDE:
						//#debug warn
						base.tina.core.log.LogPrinter.w(null, "Content INSIDE: " + content.getClass().getSimpleName() + "@" + hashCode());
						return null;
				}
				nextFilter = nextFilter.nextFilter;
			}
			while (nextFilter != null && !resultType.equals(ResultType.HANDLED));
		}
		IoFilter preFilter = nextFilter.preFilter;
		while (preFilter != null && resultType.equals(ResultType.HANDLED))
		{
			resultType = preFilter.preEncode(task, ioSession, content);
			switch (resultType) {
				case NOT_OK:
					//#debug warn
					base.tina.core.log.LogPrinter.w(null, "Content NOT_OK: " + content.getClass().getSimpleName() + "@" + hashCode());
					return null;
				case NEED_DATA:
					//#debug warn
					base.tina.core.log.LogPrinter.w(null, "Content NEED_DATA: " + content.getClass().getSimpleName() + "@" + hashCode());
					return null;
				case OK:
					break;
				case HANDLED:
					content = preFilter.encode(task, ioSession, content);
					break;
				case INSIDE:
					//#debug warn
					base.tina.core.log.LogPrinter.w(null, "Content INSIDE: " + content.getClass().getSimpleName() + "@" + hashCode());
					return null;
			}
			preFilter = preFilter.preFilter;
		}
		return content;
	}

	/**
	 * Decoder 过滤器链
	 * 
	 * @author Zhangzhuo
	 * @param task
	 *            当前Filter的任务归属
	 * @param ioSession
	 *            已建立的处理管道 负责携带处理过程中的context
	 * @param content
	 *            需要进行处理的目标数据,贯穿处理链的中间过程的状态存单
	 * @return 已通过处理链完成的处理结果,符合<code>ITaskResult</code>接口规范
	 * @throws Exception
	 *             执行过程中可能出现的异常
	 * @see {@link ITaskResult}
	 */
	public final ITaskResult filterChainDecode(Task task, IoSession<?> ioSession, Object content) throws Exception {
		ResultType resultType = null;
		Object result = null;
		IoFilter nextFilter = this;
		do
		{
			resultType = nextFilter.preDecode(task, ioSession, content);
			switch (resultType) {
				//#ifdef debug
				case NOT_OK:
					//#debug warn
					base.tina.core.log.LogPrinter.w(null, "Content NOT_OK: " + content == null ? " null " : content.getClass().getSimpleName() + "@" + hashCode());
					break;
				case NEED_DATA:
					//#debug warn
					base.tina.core.log.LogPrinter.w(null, "Content NEED_DATA: " + content == null ? " null " : content.getClass().getSimpleName() + "@" + hashCode());
					break;
				//#endif
				case OK:
					result = content = nextFilter.decode(task, ioSession, content);
					nextFilter = nextFilter.nextFilter;
					break;
				case HANDLED://当前结果已抵达最终完成位置
					//#debug info
					base.tina.core.log.LogPrinter.i(null, "handle result");
					result = nextFilter.decode(task, ioSession, content);
					break;
				case INSIDE:
					//#debug info
					base.tina.core.log.LogPrinter.i(null, "commit result in function <decode> ");
					result = null;
					break;
			}
		}
		while (nextFilter != null && resultType.equals(ResultType.OK));
		return (ITaskResult) result;
	}

	protected final void linkAfter(IoFilter ioFilter) {
		if (ioFilter == null) return;
		IoFilter filter = ioFilter.nextFilter;
		ioFilter.nextFilter = this;
		preFilter = ioFilter;
		nextFilter = filter;
	}

	public abstract ResultType preEncode(Task task, IoSession<?> ioSession, Object content);

	public abstract ResultType preDecode(Task task, IoSession<?> ioSession, Object content);

	public abstract Object encode(Task task, IoSession<?> ioSession, Object content) throws Exception;

	public abstract Object decode(Task task, IoSession<?> ioSession, Object content) throws Exception;

	public final static void insertBeforeFilter(IoFilter ioFilter, String preName, String name, IoFilter filter) {
		if (name == null) throw new NullPointerException("filter name can't be NULL");
		if (ioFilter != null)
		{
			IoFilter preFilter = ioFilter.preFilter;
			if (ioFilter.name.equals(preName))
			{
				filter.nextFilter = ioFilter;
				ioFilter.preFilter = filter;
				filter.preFilter = preFilter;
				if (preFilter != null) preFilter.nextFilter = filter;
			}
			else
			{
				while (preFilter.preFilter != null && !preFilter.name.equals(preName))
				{
					preFilter = preFilter.preFilter;
				}
				if (preFilter.preFilter == null)
				{
					preFilter.preFilter = filter;
					filter.nextFilter = preFilter;
				}
				else
				{
					filter.preFilter = preFilter.preFilter;
					preFilter.preFilter.nextFilter = filter;
					filter.nextFilter = preFilter;
					preFilter.preFilter = filter;
				}
			}
		}
		filter.name = name;
	}

	public final static void insertAfterFilter(IoFilter ioFilter, String nextName, String name, IoFilter filter) {
		if (name == null) throw new NullPointerException("filter name can't be NULL");
		if (ioFilter != null)
		{
			IoFilter afterFilter = ioFilter.nextFilter;
			if (ioFilter.name.equals(nextName))
			{
				ioFilter.nextFilter = filter;
				filter.preFilter = ioFilter;
				filter.nextFilter = afterFilter;
				afterFilter.preFilter = filter;
			}
			else
			{

				while (afterFilter.nextFilter != null && !afterFilter.name.equals(nextName))
				{
					afterFilter = afterFilter.nextFilter;
				}
				if (afterFilter.nextFilter == null)
				{
					afterFilter.nextFilter = filter;
					filter.preFilter = afterFilter;
				}
				else
				{
					afterFilter.nextFilter.preFilter = filter;
					filter.nextFilter = afterFilter.nextFilter;
					filter.preFilter = afterFilter;
					afterFilter.nextFilter = filter;
				}
			}
		}
		filter.name = name;
	}

	/**
	 * @param force
	 *            强制注销当前节点链
	 */
	public void dispose(boolean force) {
		if (isDisposable() || force) dispose();
	}

	@Override
	public void dispose() {
		IoFilter filter;
		while (nextFilter != null)
		{
			filter = nextFilter.nextFilter;
			nextFilter.nextFilter = null;
			nextFilter = filter;
		}
	}

	@Override
	public boolean isDisposable() {
		return !isManual;
	}

	public final String getName() {
		return name;
	}
}
