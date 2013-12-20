package com.acertainbookstore.business;

import java.util.Set;

import com.acertainbookstore.interfaces.ReplicatedBookStore;
import com.acertainbookstore.interfaces.ReplicatedReadOnlyBookStore;
import com.acertainbookstore.interfaces.ReplicatedStockManager;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResult;

/**
 * SlaveCertainBookStore is a wrapper over the CertainBookStore class and
 * supports the ReplicatedReadOnlyBookStore and ReplicatedStockManager
 * interfaces
 * 
 * This class must also handle replication requests sent by the master,
 * which is why it implements ReplicatedStockManager and not ReplicatedReadOnlyStockManager.
 * 
 * Designed using the singleton design pattern
 * 
 */
public class SlaveCertainBookStore implements ReplicatedBookStore,
		ReplicatedStockManager {
	private CertainBookStore bookStore = null;
	private static SlaveCertainBookStore instance = null;
	private long snapshotId = 0;

	private SlaveCertainBookStore() {
		bookStore = CertainBookStore.getInstance();
	}

	public synchronized static SlaveCertainBookStore getInstance() {
		if (instance == null) {
			instance = new SlaveCertainBookStore();
		}
		return instance;
	}

	public synchronized BookStoreResult getBooks() throws BookStoreException {
		BookStoreResult result = new BookStoreResult(bookStore.getBooks(),
				snapshotId);
		return result;
	}

	public synchronized BookStoreResult getBooksInDemand()
			throws BookStoreException {
		throw new BookStoreException();
	}

	public synchronized BookStoreResult getBooks(Set<Integer> ISBNList)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getBooks(ISBNList), snapshotId);
		return result;
	}

	public synchronized BookStoreResult getTopRatedBooks(int numBooks)
			throws BookStoreException {
		throw new BookStoreException();
	}

	public synchronized BookStoreResult getEditorPicks(int numBooks)
			throws BookStoreException {
		BookStoreResult result = new BookStoreResult(
				bookStore.getEditorPicks(numBooks), snapshotId);
		return result;
	}

	@Override
	public BookStoreResult addBooks(Set<StockBook> bookSet)
			throws BookStoreException {
		bookStore.addBooks(bookSet);
		return new BookStoreResult(null, snapshotId);
	}

	@Override
	public BookStoreResult addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		bookStore.addCopies(bookCopiesSet);
		return new BookStoreResult(null, snapshotId);
	}

	@Override
	public BookStoreResult updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		bookStore.updateEditorPicks(editorPicks);
		return new BookStoreResult(null, snapshotId);
	}

	@Override
	public BookStoreResult buyBooks(Set<BookCopy> booksToBuy)
			throws BookStoreException {
		bookStore.buyBooks(booksToBuy);
		return new BookStoreResult(null, snapshotId);
	}

	@Override
	public BookStoreResult rateBooks(Set<BookRating> bookRating)
			throws BookStoreException {
		bookStore.rateBooks(bookRating);
		return new BookStoreResult(null, snapshotId);
	}

}
