import java.util.concurrent.ThreadFactory;

class MyThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable run) {
        return new Thread(run, "my_thread " + Math.random() * 20000);
    }
}