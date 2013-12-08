package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.locking.ReentrantRWLock;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

public class ConcurrentCertainBookStore implements BookStore, StockManager{
	private static ConcurrentCertainBookStore				singleInstance;
	public Map<Integer, BookStoreBook>	bookMap;
	
	private ReentrantRWLock lock;
	
	private ConcurrentCertainBookStore() {
		this.bookMap = new HashMap<Integer, BookStoreBook>();
		this.lock = new ReentrantRWLock();
	}

	public synchronized static ConcurrentCertainBookStore getInstance() {
		if (singleInstance == null) {
			singleInstance = new ConcurrentCertainBookStore();
		}
		return singleInstance;
	}

	public void addBooks(Set<StockBook> bookSet)
			throws BookStoreException {

		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		boolean writeLock = false;
		try {
			this.lock.readLock();
			for (StockBook book : bookSet) {
				int ISBN = book.getISBN();
				String bookTitle = book.getTitle();
				String bookAuthor = book.getAuthor();
				int noCopies = book.getNumCopies();
				float bookPrice = book.getPrice();
				if (BookStoreUtility.isInvalidISBN(ISBN)
						|| BookStoreUtility.isEmpty(bookTitle)
						|| BookStoreUtility.isEmpty(bookAuthor)
						|| BookStoreUtility.isInvalidNoCopies(noCopies)
						|| bookPrice < 0.0) {
					throw new BookStoreException(BookStoreConstants.BOOK
							+ book.toString() + BookStoreConstants.INVALID);
				} else if (bookMap.containsKey(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.DUPLICATED);
				}
				
			}
			this.lock.writeLock();
			writeLock = true;
			for (StockBook book : bookSet) {
				int ISBN = book.getISBN();
				bookMap.put(ISBN, new BookStoreBook(book));
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			throw new BookStoreException("Concurrency exception!");
		} finally {
			try {
				if(writeLock) this.lock.writeUnlock();
				this.lock.readUnlock();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				throw new BookStoreException("Concurrency exception!");
			}
		}
	}

	public void addCopies(Set<BookCopy> bookCopiesSet)
			throws BookStoreException {
		int ISBN, numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		boolean writeLock = false;
		try {
			this.lock.readLock();
			for (BookCopy bookCopy : bookCopiesSet) {
				ISBN = bookCopy.getISBN();
				numCopies = bookCopy.getNumCopies();
				if (BookStoreUtility.isInvalidISBN(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.INVALID);
				
				if (!bookMap.containsKey(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.INVALID);
				if (BookStoreUtility.isInvalidNoCopies(numCopies))
					throw new BookStoreException(BookStoreConstants.NUM_COPIES
							+ numCopies + BookStoreConstants.INVALID);
			}

			BookStoreBook book;
			// Update the number of copies
			// Grab a write lock.
			this.lock.writeLock();
			writeLock = true;
			for (BookCopy bookCopy : bookCopiesSet) {
				ISBN = bookCopy.getISBN();
				numCopies = bookCopy.getNumCopies();
				book = bookMap.get(ISBN);
				book.addCopies(numCopies);
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			throw new BookStoreException("Concurrency exception!");
		} finally {
			try {
				this.lock.readUnlock();
				if(writeLock) this.lock.writeUnlock();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}

	public List<StockBook> getBooks() {
		List<StockBook> listBooks = new ArrayList<StockBook>();
		try {
			this.lock.readLock();
			Collection<BookStoreBook> bookMapValues = bookMap.values();
			for (BookStoreBook book : bookMapValues) {
				listBooks.add(book.immutableStockBook());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				this.lock.readUnlock();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return listBooks;
	}

	public void updateEditorPicks(Set<BookEditorPick> editorPicks)
			throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int ISBNVal;
		boolean writeLock = false;
		try {
			this.lock.readLock();
			for (BookEditorPick editorPickArg : editorPicks) {
				ISBNVal = editorPickArg.getISBN();
				if (BookStoreUtility.isInvalidISBN(ISBNVal))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
							+ BookStoreConstants.INVALID);
				if (!bookMap.containsKey(ISBNVal))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
							+ BookStoreConstants.NOT_AVAILABLE);
			}

			this.lock.writeLock();
			writeLock = true;
			for (BookEditorPick editorPickArg : editorPicks) {
				bookMap.get(editorPickArg.getISBN()).setEditorPick(
						editorPickArg.isEditorPick());
			}
		} catch (InterruptedException ex) {
			throw new BookStoreException("Concurrency exception!");
		} finally {
			try {
				this.lock.readUnlock();
				if(writeLock) this.lock.writeUnlock();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void buyBooks(Set<BookCopy> bookCopiesToBuy)
			throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		
		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		Boolean saleMiss = false;
		try {
			// We grab a write lock right away, since we might add a missing sales.
			this.lock.writeLock();
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
				ISBN = bookCopyToBuy.getISBN();
				if (BookStoreUtility.isInvalidISBN(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.INVALID);
				if (!bookMap.containsKey(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.NOT_AVAILABLE);
				book = bookMap.get(ISBN);
				if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
					book.addSaleMiss(); // If we cannot sell the copies of the book
										// its a miss 
					saleMiss = true;
				}
			}
	
			// We throw exception now since we want to see how many books in the
			// order incurred misses which is used by books in demand
			if (saleMiss)
				throw new BookStoreException(BookStoreConstants.BOOK
						+ BookStoreConstants.NOT_AVAILABLE);
	
			// Then make purchase
			for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
					book = bookMap.get(bookCopyToBuy.getISBN());
					book.buyCopies(bookCopyToBuy.getNumCopies());
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			throw new BookStoreException("Concurrency exception!");
		} finally {
			try {
				this.lock.writeUnlock();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public List<Book> getBooks(Set<Integer> isbnSet)
			throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		
		List<Book> listBooks = new ArrayList<Book>();
		try {
			this.lock.readLock();
			// Check that all ISBNs that we rate are there first.
			for (Integer ISBN : isbnSet) {
				if (BookStoreUtility.isInvalidISBN(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.INVALID);
				if (!bookMap.containsKey(ISBN))
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN
							+ BookStoreConstants.NOT_AVAILABLE);
			}
			// Get the books
			for (Integer ISBN : isbnSet) {
				listBooks.add(bookMap.get(ISBN).immutableBook());
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			throw new BookStoreException("Concurrency exception!");
		} finally {
			try {
				this.lock.readUnlock();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return listBooks;
	}


	public List<Book> getEditorPicks(int numBooks)
			throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks
					+ ", but it must be positive");
		}

		List<Book> listEditorPicks = new ArrayList<Book>();
		
		try {
			List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
			
			try {
				this.lock.readLock();
				Iterator<Entry<Integer, BookStoreBook>> it = bookMap.entrySet()
						.iterator();
				BookStoreBook book;
		
				// Get all books that are editor picks
				while (it.hasNext()) {
					Entry<Integer, BookStoreBook> pair = (Entry<Integer, BookStoreBook>) it
							.next();
					book = (BookStoreBook) pair.getValue();
					if (book.isEditorPick()) {
						listAllEditorPicks.add(book);
					}
				}
		
				// Find numBooks random indices of books that will be picked
				Random rand = new Random();
				Set<Integer> tobePicked = new HashSet<Integer>();
				int rangePicks = listAllEditorPicks.size();
				if (rangePicks < numBooks) {
					throw new BookStoreException("Only " + rangePicks
							+ " editor picks are available.");
				}
				int randNum;
				while (tobePicked.size() < numBooks) {
					randNum = rand.nextInt(rangePicks);
					tobePicked.add(randNum);
				}
		
				// Get the numBooks random books
				for (Integer index : tobePicked) {
					book = listAllEditorPicks.get(index);
					listEditorPicks.add(book.immutableBook());
				}
			} finally {
				this.lock.readUnlock();
			}
		} catch (InterruptedException ex) {
			throw new BookStoreException("Concurrency exception!");
		}
		
		return listEditorPicks;
	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

	@Override
	public synchronized void clear() {
		this.bookMap = new HashMap<Integer, BookStoreBook>();
	}
}
