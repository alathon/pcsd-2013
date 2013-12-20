/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {
	private static Logger fileLogger = Logger.getLogger(CertainWorkload.class.getName());
	private static Logger consoleLogger = Logger.getLogger(CertainWorkload.class.getName());
	private static Random random = new Random();
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		FileHandler handler = new FileHandler("experiment.log");
		SimpleFormatter formatter = new SimpleFormatter();
		handler.setFormatter(formatter);
		fileLogger.addHandler(handler);
		
		Set<Integer> numThreads = new HashSet<Integer>();
		numThreads.add(10);
		numThreads.add(8);
		numThreads.add(6);
		numThreads.add(4);
		numThreads.add(2);
		numThreads.add(1);
		
		boolean localTest = true;
		
		for(Integer threads : numThreads) {
			consoleLogger.info("Running with " + threads + " threads now.");
			int numConcurrentWorkloadThreads = threads;
			String serverAddress = "http://localhost:8081";
			
			List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
			List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();
	
			initializeBookStoreData(serverAddress, localTest);
	
			ExecutorService exec = Executors
					.newFixedThreadPool(numConcurrentWorkloadThreads);
	
			for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
				// The server address is ignored if localTest is true
				WorkloadConfiguration config = new WorkloadConfiguration(
						serverAddress, localTest);
				Worker workerTask = new Worker(config);
				// Keep the futures to wait for the result from the thread
				runResults.add(exec.submit(workerTask));
			}
	
			// Get the results from the threads using the futures returned
			for (Future<WorkerRunResult> futureRunResult : runResults) {
				WorkerRunResult runResult = futureRunResult.get(); // blocking call
				workerRunResults.add(runResult);
			}
	
			exec.shutdownNow(); // shutdown the executor
			reportMetric(workerRunResults);
		}
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
        double allThroughput = 0;
        long allLatency = 0;
        double avgLatency = 0;
        double succRatio = 0;
        double customerRatio = 0;
        int workers = workerRunResults.size();
        double summedInteractions = 0;
        double summedSuccInteractions = 0;
        double summedAllInteractions = 0;
        
		for (WorkerRunResult result : workerRunResults) {
			double time = result.getElapsedTimeInNanoSecs() / 1E9;
			int interactions = result
					.getSuccessfulFrequentBookStoreInteractionRuns();

			allThroughput += interactions / time;
			allLatency += result.getElapsedTimeInNanoSecs();
			summedInteractions += result
					.getTotalFrequentBookStoreInteractionRuns();
			summedSuccInteractions += result
					.getSuccessfulFrequentBookStoreInteractionRuns();
			summedAllInteractions += result.getSuccessfulInteractions();
		}
        
		customerRatio = summedSuccInteractions / summedAllInteractions;
        avgLatency = (allLatency/workers)/1E9;
        succRatio = summedSuccInteractions / summedInteractions;

        String formatStr = "";
        formatStr += String.format("Aggregated throughput (Interactions pr. second): %f\n", allThroughput);
        formatStr += String.format("Total Latency: %d\n", allLatency);
        formatStr += String.format("Average Latency: %f\n", avgLatency);
        formatStr += String.format("Success ratio: %f\n", succRatio);
        formatStr += String.format("Customer Transaction ratio: %f\n", customerRatio);
        formatStr += String.format("Number of workers(threads): %d\n", workers);
        formatStr += String.format("Bookstore interactions: %f\n", summedInteractions);
        formatStr += String.format("Successful bookstore interactions: %f\n", summedSuccInteractions);
        formatStr += String.format("All interactions: %f\n", summedAllInteractions);
        consoleLogger.info(formatStr);
        fileLogger.info(formatStr);
	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 * @param serverAddress
	 * @param localTest
	 * @throws Exception
	 */
	public static void initializeBookStoreData(String serverAddress,
			boolean localTest) throws Exception {
		BookStore bookStore = null;
		StockManager stockManager = null;
		// Initialize the RPC interfaces if its not a localTest
		if (localTest) {
			stockManager = CertainBookStore.getInstance();
			bookStore = CertainBookStore.getInstance();
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}
		
		stockManager.clearBooks();
		BookSetGenerator gen = new BookSetGenerator();
		Set<StockBook> newBooks = gen.nextSetOfStockBooks(10000);
		stockManager.addBooks(newBooks);
		
		Set<BookEditorPick> picks = new HashSet<BookEditorPick>();
		for(StockBook book : newBooks) {
			if(random.nextFloat() <= 0.4) {
				picks.add(new BookEditorPick(book.getISBN(), true));
			}
		}
		stockManager.updateEditorPicks(picks);
		
		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

	}
}
