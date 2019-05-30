package util;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SingletonThreadPoolExecutor {
	private static SingletonThreadPoolExecutor instance = null;
	private ScheduledThreadPoolExecutor thread;
	
	
	protected SingletonThreadPoolExecutor() {
		RejectedExecutionHandler handler = new MyRejectedExecutionHandler();
		thread = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), handler);
	}

	public ScheduledThreadPoolExecutor get() {
		return thread;
	}
	
	public static SingletonThreadPoolExecutor getInstance() {
		if(instance == null) {
			instance = new SingletonThreadPoolExecutor();
		}
		return instance;
	}
	
	
	
	
}