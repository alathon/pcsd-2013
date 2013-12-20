/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
	private WorkloadConfiguration configuration = null;
	private int numSuccessfulFrequentBookStoreInteraction = 0;
	private int numTotalFrequentBookStoreInteraction = 0;
	private static Logger logger = Logger.getLogger(Worker.class.getName());
	
	public Worker(WorkloadConfiguration config) {
		configuration = config;
		logger.addHandler(new ConsoleHandler());
	}

	/**
	 * Run the appropriate interaction while trying to maintain the configured
	 * distributions
	 * 
	 * Updates the counts of total runs and successful runs for customer
	 * interaction
	 * 
	 * @param chooseInteraction
	 * @return
	 */
	private boolean runInteraction(float chooseInteraction) {
		try {
			if (chooseInteraction < configuration
					.getPercentRareStockManagerInteraction()) {
				runRareStockManagerInteraction();
			} else if (chooseInteraction < configuration
					.getPercentFrequentStockManagerInteraction()) {
				runFrequentStockManagerInteraction();
			} else {
				numTotalFrequentBookStoreInteraction++;
				runFrequentBookStoreInteraction();
				numSuccessfulFrequentBookStoreInteraction++;
			}
		} catch (BookStoreException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Run the workloads trying to respect the distributions of the interactions
	 * and return result in the end
	 */
	public WorkerRunResult call() throws Exception {
		int count = 1;
		long startTimeInNanoSecs = 0;
		long endTimeInNanoSecs = 0;
		int successfulInteractions = 0;
		long timeForRunsInNanoSecs = 0;

		Random rand = new Random();
		float chooseInteraction;

		// Perform the warmup runs
		while (count++ <= configuration.getWarmUpRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			runInteraction(chooseInteraction);
		}

		count = 1;
		numTotalFrequentBookStoreInteraction = 0;
		numSuccessfulFrequentBookStoreInteraction = 0;

		// Perform the actual runs
		startTimeInNanoSecs = System.nanoTime();
		while (count++ <= configuration.getNumActualRuns()) {
			chooseInteraction = rand.nextFloat() * 100f;
			if (runInteraction(chooseInteraction)) {
				successfulInteractions++;
			}
		}
		
		endTimeInNanoSecs = System.nanoTime();
		timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
		return new WorkerRunResult(successfulInteractions,
				timeForRunsInNanoSecs, configuration.getNumActualRuns(),
				numSuccessfulFrequentBookStoreInteraction,
				numTotalFrequentBookStoreInteraction);
	}

	/**
	 * Runs the new stock acquisition interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runRareStockManagerInteraction() throws BookStoreException {
		List<StockBook> books = CertainBookStore.getInstance().getBooks();
		Set<StockBook> newBooks = configuration.getBookSetGenerator().nextSetOfStockBooks(configuration.getNumBooksToAdd());
		Set<StockBook> toAdd = new HashSet<StockBook>();
		for(StockBook book : newBooks) {
			if(!books.contains(book)) {
				toAdd.add(book);
			}
		}
		
		CertainBookStore.getInstance().addBooks(toAdd);
		
	}

	/**
	 * Runs the stock replenishment interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentStockManagerInteraction() throws BookStoreException {
		List<StockBook> books = CertainBookStore.getInstance().getBooks();
		Collections.sort(books, new Comparator<StockBook>() {
			@Override
			public int compare(StockBook arg0, StockBook arg1) {
				return Integer.valueOf(arg0.getNumCopies()).compareTo(Integer.valueOf(arg1.getNumCopies()));
			}
		});
		Set<BookCopy> copies = new HashSet<BookCopy>();
		for(int i = 0; i < configuration.getNumSmallestCopies(); i++) {
			StockBook book = books.get(i);
			BookCopy copy = new BookCopy(book.getISBN(), configuration.getNumAddCopies());
			copies.add(copy);
		}
		CertainBookStore.getInstance().addCopies(copies);
	}

	/**
	 * Runs the customer interaction
	 * 
	 * @throws BookStoreException
	 */
	private void runFrequentBookStoreInteraction() throws BookStoreException {
		List<Book> editorPicks = CertainBookStore.getInstance().getEditorPicks(configuration.getNumEditorPicksToGet());
		Set<Integer> isbns = new HashSet<Integer>();
		for(Book book : editorPicks) {
			isbns.add(book.getISBN());
		}
		Set<Integer> sampled = configuration.getBookSetGenerator().sampleFromSetOfISBNs(isbns, configuration.getNumEditorSubsetSize());
		Set<BookCopy> toBuy = new HashSet<BookCopy>();
		for(Book book : editorPicks) {
			if(sampled.contains(book.getISBN())) {
				toBuy.add(new BookCopy(book.getISBN(), configuration.getNumBooksToBuy()));
			}
		}
		CertainBookStore.getInstance().buyBooks(toBuy);
	}

}
