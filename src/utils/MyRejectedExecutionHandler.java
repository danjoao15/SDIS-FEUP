package utils;

import java.util.Random;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyRejectedExecutionHandler implements RejectedExecutionHandler{

	@Override
	public void rejectedExecution(Runnable subprotocol, ThreadPoolExecutor thread) {
		Random r = new Random();
		((ScheduledThreadPoolExecutor) thread).schedule(subprotocol, 5000 + r.nextInt(5000) , TimeUnit.MILLISECONDS);
	}
	
	

}