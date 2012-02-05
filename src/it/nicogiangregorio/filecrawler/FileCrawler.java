package it.nicogiangregorio.filecrawler;

import it.nicogiangregorio.hash.FileHasher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FileCrawler {
	private final CompletionService<FileEntry> completionService;
	private final String root;
	private AtomicInteger NUM_FILES = new AtomicInteger();
	private final String algorithm;
	private final int bufSize;
	private final TimingThreadPoolExecutor tPoolExe;

	/**
	 * Private constructor, called by inner builder class
	 * 
	 * @param builder : builder inner class
	 */
	private FileCrawler(CrawlBuilder builder) {

		this.root = builder.root;
		this.bufSize = builder.bufferSize;
		this.algorithm = builder.algorithm;

		tPoolExe = new TimingThreadPoolExecutor(builder.corePoolsize,
				builder.maximumPoolSize, builder.maxIdleTime, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(builder.tasksQueueSize));
		tPoolExe.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		this.completionService = new ExecutorCompletionService<>(tPoolExe);
	}

	/**
	 * Processor method. It starts processing a filesystem tree and elaborates
	 * results as soon as they are available
	 * 
	 * 
	 * @param fileMap : resulting map with file - hash pairs
	 * @throws ExecutionException
	 */
	public void execute(ConcurrentHashMap<String, String> fileMap)
			throws ExecutionException {
		try {

			Files.walkFileTree(Paths.get(root), fileVisitor);

			for (int ii = 0; ii < NUM_FILES.get(); ii++) {
				FileEntry entry = completionService.take().get();
				fileMap.put(entry.getFilePath(), entry.getHashCode());
			}

		} catch (IOException e) {
			Thread.currentThread().interrupt();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Shutdown the pool
	 * 
	 */
	public void quit() {
		tPoolExe.shutdown();
	}

	/**
	 * Visitor implementation defining actions to take while traversing a
	 * filesystem tree
	 * 
	 */
	private FileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attributes) throws IOException {

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(final Path filePath,
				BasicFileAttributes attributes) throws IOException {

			NUM_FILES.incrementAndGet();
			completionService.submit(new Callable<FileEntry>() {

				@Override
				public FileEntry call() throws Exception {

					byte[] bytes = FileHasher.createFileHash(
							filePath.toString(), algorithm, bufSize);
					String result = FileHasher.toHex(bytes);

					return new FileEntry(filePath.toString(), result);
				}
			});
			return FileVisitResult.CONTINUE;
		}
	};

	/**
	 * Inner class used as builder for outer class
	 * 
	 * 
	 * @author Nico Giangregorio
	 * 
	 */
	public static class CrawlBuilder {
		// Mandatory fields
		private final String root;

		// Optional parameters - initialized to default values
		private String algorithm = "MD5";
		private int bufferSize = 2048;
		private int corePoolsize = 10;
		private int maximumPoolSize = 100;
		private int maxIdleTime = 5;
		private int tasksQueueSize = 20;

		/**
		 * Define root directory. It is mandatory
		 * 
		 * @param root : root folder
		 */
		public CrawlBuilder(String root) {
			this.root = root;
		}

		/**
		 * Define algorithm to be used
		 * 
		 * @param algorithm : hash function to apply, default one is MD5
		 * @return an istance of CrawlBuilder
		 */
		public CrawlBuilder algorithm(String algorithm) {
			this.algorithm = algorithm;
			return this;
		}

		/**
		 * 
		 * Configure the thread pool.
		 * 
		 * @param corePoolsize : base number of threads
		 * @param maximumPoolSize : max number of allowed threads
		 * @param maxIdleTime : max time in seconds to wait before discard a
		 *            thread
		 * @param tasksQueueSize : size of queue of submitted task
		 * @return an istance of CrawlBuilder
		 */
		public CrawlBuilder configurePool(int corePoolsize,
				int maximumPoolSize, int maxIdleTime, int tasksQueueSize) {

			this.corePoolsize = corePoolsize;
			this.maximumPoolSize = maximumPoolSize;
			this.maxIdleTime = maxIdleTime;
			this.tasksQueueSize = tasksQueueSize;

			return this;
		}

		/**
		 * Size of IO buffer
		 * 
		 * 
		 * @param bufferSize : size in bytes
		 * @return an istance of CrawlBuilder
		 */
		public CrawlBuilder bufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
			return this;
		}

		/**
		 * 
		 * Builds an instance of the outer class
		 * 
		 * @return an instance of FileCrawler
		 */
		public FileCrawler build() {
			return new FileCrawler(this);
		}
	}

}
