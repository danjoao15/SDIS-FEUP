package utils;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SingletonThreadPoolExecutor {
	private static SingletonThreadPoolExecutor instance = null;
	private ScheduledThreadPoolExecutor thread;
	
	
	protected SingletonThreadPoolExecutor() {
		/* ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) */

		RejectedExecutionHandler handler = new MyRejectedExecutionHandler();
		thread = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), handler);
	}

	public static SingletonThreadPoolExecutor getInstance() {
		if(instance == null) {
			instance = new SingletonThreadPoolExecutor();
		}
		return instance;
	}
	
	/**
	 * @return the thread
	 */
	public ScheduledThreadPoolExecutor get() {
		return thread;
	}
}