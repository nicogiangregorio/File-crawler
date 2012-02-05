import it.nicogiangregorio.filecrawler.FileCrawler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileCrawlerTest {

	private ConcurrentHashMap<String, String> fileMap;
	private String root;
	private int corePoolSize;
	private int maximumPoolSize;
	private int maxIdleTime;
	private int taskQueueSize;
	private String algorithm;
	private int bufferSize;

	/**
	 * Customize this in order to analyze tweaks on performance
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		root = "/home/nickg/Downloads";
		bufferSize = 4096;
		corePoolSize = 10;
		maximumPoolSize = 100;
		maxIdleTime = 5;
		taskQueueSize = 15;
		algorithm = "SHA";
		fileMap = new ConcurrentHashMap<>();
	}

	@After
	public void tearDown() throws Exception {

		root = null;
		bufferSize = -1;
		corePoolSize = -1;
		maximumPoolSize = -1;
		maxIdleTime = -1;
		taskQueueSize = -1;
		algorithm = null;
		fileMap = null;
	}

	@Test
	public void test() {

		FileCrawler sfcr = new FileCrawler.CrawlBuilder(root)
				.bufferSize(bufferSize)
				.configurePool(corePoolSize, maximumPoolSize, maxIdleTime,
						taskQueueSize).algorithm(algorithm).build();

		try {
			sfcr.execute(fileMap);
		} catch (ExecutionException e) {
			fail("Exception occurred: " + e.getMessage());
			e.printStackTrace();
		}
		assertTrue(fileMap != null);
		assertTrue(!fileMap.isEmpty()); // if you expect any results
	}
}
