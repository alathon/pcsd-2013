package com.acertainbookstore.client.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.BookStoreBook;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

public class BookStoreHTTPProxyTest extends ProxyTest {
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

	@Test(expected = BookStoreException.class)
	public void testBuyBooksInvalidISBN() throws BookStoreException {
		Set<BookCopy> invalidCopies = new HashSet<BookCopy>();
		invalidCopies.add(new BookCopy(10101010, 3));
		client.buyBooks(invalidCopies);
	}
	
	@Test(expected = BookStoreException.class)
	public void testBuyBooksNotEnoughCopies() throws BookStoreException {
		Set<BookCopy> invalidCopies = new HashSet<BookCopy>();
		invalidCopies.add(new BookCopy(book1.getISBN(), 100));
		client.buyBooks(invalidCopies);
	}
	
	@Test
	public void testBuyBooks() throws BookStoreException {
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(bookCopy1);
		client.buyBooks(booksToBuy);
		List<StockBook> books = storeManager.getBooks();
		StockBook book = this.getByISBN(books, bookCopy1.getISBN());
		assertNotNull(book);
		assertThat(book.getNumCopies(), is(3));
	}
	
	@Test
	public void testRateBooks() throws BookStoreException {
		Set<BookRating> ratings = new HashSet<BookRating>();
		ratings.add(bookRating1);
		ratings.add(bookRating2);
		ratings.add(bookRating21);
		client.rateBooks(ratings);
		List<StockBook> books = storeManager.getBooks();
		
		StockBook book = this.getByISBN(books, bookRating1.getISBN());
		assertNotNull(book);
		assertThat(book.getTimesRated(), is(1l));
		assertThat(book.getTotalRating(), is(3l));
		assertThat(book.getAverageRating(), is(3f));
		
		book = this.getByISBN(books, bookRating2.getISBN());
		assertNotNull(book);
		assertThat(book.getTimesRated(), is(2l));
		assertThat(book.getTotalRating(), is(8l));
		assertThat(book.getAverageRating(), is(4f));
	}
	
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<Integer> isbn = new HashSet<Integer>();
		isbn.add(book1.getISBN());
		isbn.add(book2.getISBN());
		
		List<Book> books = client.getBooks(isbn);
		assertThat(books.size(), is(2));
		for(Book book : books) {
			assertTrue(isbn.contains(book.getISBN()));
		}
	}
	
	@Test
	public void testGetTopRatedBooks() throws BookStoreException {
		Set<BookRating> ratings = new HashSet<BookRating>();
		ratings.add(bookRating1);
		ratings.add(bookRating2);
		ratings.add(bookRating21);
		client.rateBooks(ratings);
		List<Book> topRatedBooks = client.getTopRatedBooks(1);
		assertThat(topRatedBooks.size(), is(1));
		assertThat(topRatedBooks.get(0).getISBN(), is(book2.getISBN()));
	}
	
	@Test
	public void testGetEditorPicks() throws BookStoreException {
		List<Book> editorPicks = client.getEditorPicks(1);
		assertThat(editorPicks.size(), is(1));
		assertThat(editorPicks.get(0).getISBN(), is(book3.getISBN()));
	}
}
