package com.acertainbookstore.business.tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookStoreBook;
import com.acertainbookstore.business.ConcurrentCertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentCertainBookStoreTest {
	protected static final int GETBOOKS_ITERATIONS = 50;

	private static Map<Integer, BookStoreBook> baseMap;
	private static Map<Integer, BookCopy> buyMap;
	private static Set<BookCopy> booksToBuy;
	
	private static StockManager manager;
	private static BookStore client;

	private static Random random = new Random();
	
	ExecutorService executor = Executors.newFixedThreadPool(2);
	
	private static BookStoreBook generateRandomBook(int isbn) {
		String author = UUID.randomUUID().toString();
		String title = UUID.randomUUID().toString();
		float price = random.nextFloat() * 1000;
		int copies = random.nextInt(100) + 5;
		return new BookStoreBook(isbn, title, author, price, copies);
	}
	
	@BeforeClass
	public static void oneTimeSetup() {
		buyMap = new HashMap<Integer, BookCopy>();
		baseMap = new HashMap<Integer, BookStoreBook>();
		booksToBuy = new HashSet<BookCopy>();
		
		for(int i = 0; i < 100000; i++) {
			BookStoreBook book = ConcurrentCertainBookStoreTest.generateRandomBook(i+1);
			baseMap.put(book.getISBN(), book);
			if(random.nextFloat() < 0.75) {
				BookCopy copy = new BookCopy(book.getISBN(), random.nextInt(3) + 1);
				buyMap.put(copy.getISBN(), copy);
				booksToBuy.add(copy);
			}
		}
	
		manager = ConcurrentCertainBookStore.getInstance();
		client = ConcurrentCertainBookStore.getInstance();
	}
	
	@Before
	public void setupBookMap() {
		ConcurrentCertainBookStore.getInstance().bookMap = new HashMap<Integer, BookStoreBook>();
		for(BookStoreBook book : baseMap.values()) {
			BookStoreBook copy = new BookStoreBook(book.getISBN(), book.getTitle(), book.getAuthor(), book.getPrice(), book.getNumCopies());
			ConcurrentCertainBookStore.getInstance().bookMap.put(book.getISBN(), copy);
		}
	}

	@Test
	public void test1() throws Throwable {
		Runnable c1 = new Runnable() {
			@Override
			public void run() {
				try {
					client.buyBooks(ConcurrentCertainBookStoreTest.booksToBuy);
				} catch (BookStoreException ex) {
					ex.printStackTrace();
					fail();
				}
			}
		};
		
		Runnable c2 = new Runnable() {
			@Override
			public void run() {
				try {
					manager.addCopies(ConcurrentCertainBookStoreTest.booksToBuy);
				} catch (BookStoreException ex) {
					ex.printStackTrace();
					fail();
				}
			}
		};
		
		Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					List<StockBook> books = manager.getBooks();
					for(StockBook book : books) {
						int expectedCopies = ConcurrentCertainBookStoreTest.baseMap.get(book.getISBN()).getNumCopies();
						assertThat(book.getNumCopies(), is(expectedCopies));
					}
				} catch (BookStoreException e) {
					e.printStackTrace();
					fail();
				}
			}
		};

		for(int i = 0; i < 10; i++) {
			Future<?> f1 = executor.submit(c1);
			Future<?> f2 = executor.submit(c2);
			
			try {
				f1.get();
				f2.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				fail();
			}
			
			Future<?> f3 = executor.submit(test);
			try {
				f3.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				fail();
			}
		}
	}
	
	@Test
	public void test2() {
		final Runnable c1 = new Runnable() {
			@Override
			public void run() {
				while(true) {
					if(Thread.interrupted()) break;
					try {
						client.buyBooks(booksToBuy);
						manager.addCopies(booksToBuy);
					} catch (BookStoreException ex) {
						ex.printStackTrace();
						fail();
					}
				}
			}
		};
		
		final Runnable c2 = new Runnable() {
			@Override
			public void run() {
				try {
					for(int i = 0; i < ConcurrentCertainBookStoreTest.GETBOOKS_ITERATIONS; i++) {
						List<StockBook> books = manager.getBooks();
						for(StockBook book : books) {
							int origAmt = ConcurrentCertainBookStoreTest.baseMap.get(book.getISBN()).getNumCopies();
							BookCopy copy = ConcurrentCertainBookStoreTest.buyMap.get(book.getISBN());
							if(copy != null) {
								assertTrue(book.getNumCopies() + ":" + origAmt + ":" + copy.getNumCopies(), book.getNumCopies() == origAmt || book.getNumCopies() == origAmt - copy.getNumCopies());
							}
						}
						
						Thread.sleep(50);
					}
				} catch (BookStoreException ex) {
					ex.printStackTrace();
					fail();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		Future<?> f1 = executor.submit(c1);
		Future<?> f2 = executor.submit(c2);
		
		try {
			f2.get();
			f1.cancel(true);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			fail();
		}
	}

}
