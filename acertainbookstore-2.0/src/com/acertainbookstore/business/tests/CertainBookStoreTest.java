package com.acertainbookstore.business.tests;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookStoreBook;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Tests for the CertainBookStore singleton.
 * Run by 'cheating' and re-instantiating the bookMap for every test,
 * as the current model of dual-interfaces on a singleton is horrible for testing.
 * 
 * Tests all exceptions for addBooks(), but no exceptions for the other methods, simply out of
 * convenience. The model itself really shouldn't be doing input validation through exceptions in the
 * way it is; that input should be getting validated as an XML validation or as part of a constructor or something else, 
 * so that the rest of the code can just assume valid books, instead of having potentially invalid books floating around
 * as instantiated objects.
 * @author Martin Grunbaum
 *
 */
public class CertainBookStoreTest {
	private static BookStoreBook book1;
	private static StockBook stockBook1;
	private static BookStoreBook book2;
	private static StockBook stockBook2;
	private static BookStoreBook book3;
	private static StockBook stockBook3;
	private static BookStoreBook book4;
	private static StockBook stockBook4;
	
	private static StockBook bookInvalidISBN;
	private static StockBook bookInvalidCopies;
	private static StockBook bookInvalidAuthor;
	private static StockBook bookInvalidTitle;
	
	
	private static BookCopy bookCopy1;
	private static Map<Integer, BookStoreBook> baseMap;
	
	@BeforeClass
	public static void oneTimeSetup() {
		book1 = new BookStoreBook(5050, "Some Title", "Some Author", 500.0f, 5);
		book2 = new BookStoreBook(1111, "Some Other Title", "Some Other Author", 10.0f, 2);
		book3 = new BookStoreBook(2222, "Some Third Title", "Some Third Author", 200f, 200);
		book4 = new BookStoreBook(3333, "My Stock Book", "J. Lewis", 20f, 0);
		book4.setEditorPick(true);
		
		stockBook1 = new ImmutableStockBook(5050, "Some Title", "Some Author", 500.0f, 5, 0l, 0l, 0l, false);
		stockBook2 = new ImmutableStockBook(1111, "Some Other Title", "Some Other Author", 10.0f, 2, 0l, 0l, 0l, false);
		stockBook3 = new ImmutableStockBook(2222, "Some Third Title", "Some Third Author", 200f, 200, 0l, 0l, 0l, false);
		stockBook4 = new ImmutableStockBook(3333, "My Stock Book", "J. Lewis", 20f, 2, 0l, 0l, 0l, true);
		
		baseMap = new HashMap<Integer, BookStoreBook>();
		baseMap.put(book1.getISBN(), book1);
		baseMap.put(book2.getISBN(), book2);
		baseMap.put(book3.getISBN(), book3);

		bookInvalidISBN = new ImmutableStockBook(-3, "Jeepers Creepers", "Some Author Dude", 20f, 2, 0l, 0l, 0l, false);
		bookInvalidAuthor = new ImmutableStockBook(2, "Jeepers Creepers", "", 20f, 2, 0l, 0l, 0l, false);
		bookInvalidTitle = new ImmutableStockBook(2, "", "Some Author Dude", 20f, 2, 0l, 0l, 0l, false);
		bookInvalidCopies = new ImmutableStockBook(2, "Jeepers Creepers", "Some Author Dude", 20f, 0, 0l, 0l, 0l, false);
		
		bookCopy1 = new BookCopy(1111, 100);
	}
	
	@Before
	public void setupBookMap() {
		CertainBookStore.getInstance().bookMap = new HashMap<Integer, BookStoreBook>();
		CertainBookStore.getInstance().bookMap.putAll(CertainBookStoreTest.baseMap);
	}

	@Test
	public void testAddBooks() throws BookStoreException {
		Set<StockBook> books = new HashSet<StockBook>();
		books.add(stockBook4);
		CertainBookStore.getInstance().addBooks(books);
		BookStoreBook bookInMap = CertainBookStore.getInstance().bookMap.get(stockBook4.getISBN());
		assertThat(bookInMap, is(CertainBookStoreTest.book4));
	}
	
	@Test(expected = BookStoreException.class)
	public void testAddBooksInvalidAlreadyExists() throws BookStoreException {
		Set<StockBook> books = new HashSet<StockBook>();
		books.add(stockBook4);
		CertainBookStore.getInstance().addBooks(books);
		CertainBookStore.getInstance().addBooks(books);
	}
	
	@Test(expected = BookStoreException.class)
	public void testAddBooksInvalidISBN() throws BookStoreException {
		Set<StockBook> books = new HashSet<StockBook>();
		books.add(bookInvalidISBN);
		CertainBookStore.getInstance().addBooks(books);
	}

	@Test(expected = BookStoreException.class)
	public void testAddBooksInvalidCopies() throws BookStoreException {
		Set<StockBook> books = new HashSet<StockBook>();
		books.add(bookInvalidCopies);
		CertainBookStore.getInstance().addBooks(books);
	}
	
	@Test(expected = BookStoreException.class)
	public void testAddBooksInvalidAuthor() throws BookStoreException {
		Set<StockBook> books = new HashSet<StockBook>();
		books.add(bookInvalidAuthor);
		CertainBookStore.getInstance().addBooks(books);
	}
	
	@Test(expected = BookStoreException.class)
	public void testAddBooksInvalidTitle() throws BookStoreException {
		Set<StockBook> books = new HashSet<StockBook>();
		books.add(bookInvalidTitle);
		CertainBookStore.getInstance().addBooks(books);
	}
	
	@Test
	public void testAddCopies() throws BookStoreException {
		Set<BookCopy> copiesSet = new HashSet<BookCopy>();
		copiesSet.add(bookCopy1);
		assertThat(CertainBookStore.getInstance().bookMap.get(bookCopy1.getISBN()).areCopiesInStore(2), is(true));
		assertThat(CertainBookStore.getInstance().bookMap.get(bookCopy1.getISBN()).areCopiesInStore(3), is(false));
		CertainBookStore.getInstance().addCopies(copiesSet);
		assertThat(CertainBookStore.getInstance().bookMap.get(bookCopy1.getISBN()).areCopiesInStore(102), is(true));
		assertThat(CertainBookStore.getInstance().bookMap.get(bookCopy1.getISBN()).areCopiesInStore(103), is(false));
	}

	@Test
	public void testGetBooks() {
		List<StockBook> books = CertainBookStore.getInstance().getBooks();
		assertThat(books.size(), is(3));
		assertThat(books.contains(stockBook1), is(true));
		assertThat(books.contains(stockBook2), is(true));
		assertThat(books.contains(stockBook3), is(true));
	}

	/*
	@Test
	public void testUpdateEditorPicks() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testBuyBooks() { 
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetBooksSetOfInteger() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetEditorPicks() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetTopRatedBooks() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetBooksInDemand() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testRateBooks() {
		fail("Not yet implemented"); // TODO
	}
	*/
}
