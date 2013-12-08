/**
 * 
 */
package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * CertainBookStore implements the bookstore and its functionality which is defined in the BookStore
 * 
 * Designed using the singleTon design pattern so there is always just one CertainBookStore object
 * 
 */
public class CertainBookStore implements BookStore, StockManager {
	private static CertainBookStore				singleInstance;
	public Map<Integer, BookStoreBook>	bookMap = new HashMap<Integer, BookStoreBook>();
	private Comparator<BookStoreBook> avgRatingDescComparator;
	
	private CertainBookStore() {
		avgRatingDescComparator = new Comparator<BookStoreBook>() {
			@Override
			public int compare(BookStoreBook arg0, BookStoreBook arg1) {
				float first = arg0.getAverageRating();
				float second = arg1.getAverageRating();
				return Float.compare(second, first);
			}
		};
	}
	
	public synchronized static CertainBookStore getInstance() {
		if (singleInstance == null) {
			singleInstance = new CertainBookStore();
		}
		return singleInstance;
	}

	public synchronized void addBooks(Set<StockBook> bookSet) throws BookStoreException {

		if (bookSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check if all are there
		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			String bookTitle = book.getTitle();
			String bookAuthor = book.getAuthor();
			int noCopies = book.getNumCopies();
			float bookPrice = book.getPrice();
			if (BookStoreUtility.isInvalidISBN(ISBN) || BookStoreUtility.isEmpty(bookTitle) || BookStoreUtility.isEmpty(bookAuthor) || BookStoreUtility.isInvalidNoCopies(noCopies)
					|| bookPrice < 0.0) {
				throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
			} else if (bookMap.containsKey(ISBN)) {
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.DUPLICATED);
			}
		}

		for (StockBook book : bookSet) {
			int ISBN = book.getISBN();
			bookMap.put(ISBN, new BookStoreBook(book));
		}
	}

	public synchronized void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		int ISBN, numCopies;

		if (bookCopiesSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			if (BookStoreUtility.isInvalidNoCopies(numCopies))
				throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);

		}

		BookStoreBook book;
		// Update the number of copies
		for (BookCopy bookCopy : bookCopiesSet) {
			ISBN = bookCopy.getISBN();
			numCopies = bookCopy.getNumCopies();
			book = bookMap.get(ISBN);
			book.addCopies(numCopies);
		}
	}

	public synchronized List<StockBook> getBooks() {
		List<StockBook> listBooks = new ArrayList<StockBook>();
		Collection<BookStoreBook> bookMapValues = bookMap.values();
		for (BookStoreBook book : bookMapValues) {
			listBooks.add(book.immutableStockBook());
		}
		return listBooks;
	}

	public synchronized void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		int ISBNVal;

		for (BookEditorPick editorPickArg : editorPicks) {
			ISBNVal = editorPickArg.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal + BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBNVal))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal + BookStoreConstants.NOT_AVAILABLE);
		}

		for (BookEditorPick editorPickArg : editorPicks) {
			bookMap.get(editorPickArg.getISBN()).setEditorPick(editorPickArg.isEditorPick());
		}
	}

	public synchronized void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we buy are there first.
		int ISBN;
		BookStoreBook book;
		Boolean saleMiss = false;
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			ISBN = bookCopyToBuy.getISBN();
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
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
			throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);

		// Then make purchase
		for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
			book = bookMap.get(bookCopyToBuy.getISBN());
			book.buyCopies(bookCopyToBuy.getNumCopies());
		}
	}

	public synchronized List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Check that all ISBNs that we rate are there first.
		for (Integer ISBN : isbnSet) {
			if (BookStoreUtility.isInvalidISBN(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
			if (!bookMap.containsKey(ISBN))
				throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
		}

		List<Book> listBooks = new ArrayList<Book>();

		// Get the books
		for (Integer ISBN : isbnSet) {
			listBooks.add(bookMap.get(ISBN).immutableBook());
		}
		return listBooks;
	}

	public synchronized List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}

		List<BookStoreBook> listAllEditorPicks = new ArrayList<BookStoreBook>();
		List<Book> listEditorPicks = new ArrayList<Book>();
		Iterator<Entry<Integer, BookStoreBook>> it = bookMap.entrySet().iterator();
		BookStoreBook book;

		// Get all books that are editor picks
		while (it.hasNext()) {
			Entry<Integer, BookStoreBook> pair = (Entry<Integer, BookStoreBook>) it.next();
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
			throw new BookStoreException("Only " + rangePicks + " editor picks are available.");
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
		return listEditorPicks;

	}

	@Override
	public void clear() {
		this.bookMap = new HashMap<Integer, BookStoreBook>();
	}

	@Override
	public synchronized List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		List<BookStoreBook> books = new ArrayList<BookStoreBook>();
		for(BookStoreBook book : this.bookMap.values()) {
			if(book.getTimesRated() > 0) books.add(book);
		}

		if(numBooks <= 0 || numBooks >= books.size()) {
			throw new BookStoreException("Bad client input to getTopRatedBooks: " + numBooks);
		}
		
		Collections.sort(books, this.avgRatingDescComparator);
		List<Book> out = new ArrayList<Book>();
		for(Iterator<BookStoreBook> it = books.iterator(); it.hasNext() && numBooks > 0; numBooks--) {
			out.add(it.next().immutableBook());
		}
		return out;
	}

	@Override
	public synchronized List<StockBook> getBooksInDemand() throws BookStoreException {
		List<StockBook> out = new ArrayList<StockBook>();
		CertainBookStore.getInstance();
		for(BookStoreBook book : this.bookMap.values()) {
			if(book.hadSaleMiss()) out.add(book.immutableStockBook());
		}
		return out;
	}

	
	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		for (BookRating rating : bookRating) {
			if(BookStoreUtility.isInvalidISBN(rating.getISBN())) {
				throw new BookStoreException(BookStoreConstants.ISBN + rating.getISBN() + BookStoreConstants.INVALID);
			}
			if(!bookMap.containsKey(rating.getISBN())) {
				throw new BookStoreException(BookStoreConstants.ISBN + rating.getISBN() + BookStoreConstants.NOT_AVAILABLE);
			}
			if(BookStoreUtility.isInvalidRating(rating.getRating())) {
				throw new BookStoreException(BookStoreConstants.RATING + rating.getRating() + BookStoreConstants.INVALID);
			}
		}
		
		synchronized(this) {
			for(BookRating rating : bookRating) {
				BookStoreBook book = this.bookMap.get(rating.getISBN());
				book.addRating(rating.getRating());
			}
		}
	}

}
