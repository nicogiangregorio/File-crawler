package it.nicogiangregorio.filecrawler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Custom ThreadPoolExecutor, aiming to obtain statistics about computation
 * 
 * @author Nico Giangregorio
 * 
 */
public class TimingThreadPoolExecutor extends ThreadPoolExecutor {

	private final ThreadLocal<Long> taskStartTime = new ThreadLocal<>();
	private final AtomicLong numTasks = new AtomicLong();
	private final AtomicLong totalTime = new AtomicLong();
	private final AtomicLong globalStartTime = new AtomicLong();

	private final Logger log = Logger.getLogger("TimingThreadPool");

	public TimingThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	@Override
	public void execute(Runnable arg0) {

		try {
			globalStartTime.set(System.nanoTime());

		} finally {
			super.execute(arg0);
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {

		super.beforeExecute(t, r);
		log.fine(String.format("Thread %s: start %s", t, r));
		taskStartTime.set(new Long(System.nanoTime()));
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {

		try {
			long endTime = System.nanoTime();
			long taskTime = endTime - taskStartTime.get();
			numTasks.incrementAndGet();
			log.fine(String.format("Thread %s: end %s, time=%dns", t, r,
					taskTime));
		} finally {
			super.afterExecute(r, t);
		}
	}

	@Override
	protected void terminated() {

		try {
			totalTime.set(new Long(System.nanoTime() - globalStartTime.get()));
			log.info(String.format(
					"Total time elapsed: %dms computing %d files",
					(totalTime.get() / 1000000), numTasks.get()));

		} finally {
			super.terminated();
		}
	}
}
