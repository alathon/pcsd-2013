package com.acertainbookstore.client.tests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.BookStoreBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

public class StockManagerHTTPProxyTest extends ProxyTest {
	private static BookStoreBook book1;
	private static StockBook stockBook1;
	private static BookStoreBook book2;
	private static StockBook stockBook2;
	private static BookStoreBook book3;
	private static StockBook stockBook3;
	private static BookStoreBook book4;
	
	private static BookRating bookRating1;
	private static BookRating bookRating2;
	private static BookRating bookRating21;
	private static BookCopy bookCopy1;
	


	@BeforeClass
	public static void oneTimeSetup() {
		book1 = new BookStoreBook(5050, "Some Title", "Some Author", 500.0f, 5);
		book2 = new BookStoreBook(1111, "Some Other Title", "Some Other Author", 10.0f, 2);
		book3 = new BookStoreBook(2222, "Some Third Title", "Some Third Author", 200f, 200);
		book3.setEditorPick(true);
		book4 = new BookStoreBook(3333, "My Stock Book", "J. Lewis", 20f, 0);
		book4.setEditorPick(true);
		
		stockBook1 = new ImmutableStockBook(5050, "Some Title", "Some Author", 500.0f, 5, 0l, 0l, 0l, false);
		stockBook2 = new ImmutableStockBook(1111, "Some Other Title", "Some Other Author", 10.0f, 2, 0l, 0l, 0l, false);
		stockBook3 = new ImmutableStockBook(2222, "Some Third Title", "Some Third Author", 200f, 200, 0l, 0l, 0l, true);
		
		bookCopy1 = new BookCopy(book1.getISBN(), 2);
		bookRating1 = new BookRating(book1.getISBN(), 3);
		bookRating2 = new BookRating(book2.getISBN(), 3);
		bookRating21 = new BookRating(book2.getISBN(), 5);
	}
	
	@Before
	public void readyBooks() throws BookStoreException {
		Set<StockBook> basicBooks = new HashSet<StockBook>();
		basicBooks.add(stockBook1);
		basicBooks.add(stockBook2);
		basicBooks.add(stockBook3);
		storeManager.clear();
		storeManager.addBooks(basicBooks);
	}
	
	private StockBook getByISBN(List<StockBook> books, int ISBN) {
		for(StockBook book : books) {
			if(book.getISBN() == ISBN) return book;
		}
		return null;
	}
	
	/*
	@Test
	public void testAddBooks() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testAddCopies() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetBooks() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testUpdateEditorPicks() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetBooksInDemand() {
		fail("Not yet implemented"); // TODO
	}
	*/
}
