package com.acertainbookstore.business.tests;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Before;
import org.junit.Test;

import com.acertainbookstore.business.BookStoreBook;
import com.acertainbookstore.business.ImmutableBook;
import com.acertainbookstore.business.StockBook;

public class BookStoreBookTest {
	private BookStoreBook book1;
	private BookStoreBook book2;
	
	private int book1Copies = 5;
	
	@Before
	public void createBooks() {
		book1 = new BookStoreBook(5050, "Some Title", "Some Author", 500.0f, book1Copies);
		book2 = new BookStoreBook(1111, "Some Other Title", "Some Other Author", 10.0f, 0);
	}
	
	@Test
	public void testAreCopiesInStore() {
		assertThat(book1.areCopiesInStore(5), is(true));
		assertThat(book1.areCopiesInStore(6), is(false));
		assertThat(book2.areCopiesInStore(1), is(false));
		assertThat(book2.areCopiesInStore(0), is(true));
	}
	
	@Test
	public void testGetAverageRating() {
		book1.addRating(5);
		book1.addRating(5);
		book1.addRating(5);
		book1.addRating(3);
		book1.addRating(1);
		book1.addRating(2);
		
		// Note that the rating is actually 3.5 (21/6), but the method does integer division and converts to float
		// at the end, effectively truncating the rating.
		assertThat(book1.getAverageRating(),is(3f));
		assertThat(book2.getAverageRating(), is(-1f));
	}

	@Test
	public void testBuyCopies() {
		assertThat(book1.getNumCopies(), is(this.book1Copies));
		book1.buyCopies(2);
		assertThat(book1.getNumCopies(), is(this.book1Copies - 2));
	}

	@Test
	public void testAddCopies() {
		assertThat(book1.getNumCopies(), is(this.book1Copies));
		book1.addCopies(2);
		assertThat(book1.getNumCopies(), is(this.book1Copies + 2));
	}

	@Test
	public void testAddSaleMiss() {
		assertThat(book1.getSaleMisses(), is(0l));
		book1.addSaleMiss();
		assertThat(book1.getSaleMisses(),  is(1l));
	}

	@Test
	public void testAddRating() {
		book1.addRating(3);
		assertThat(book1.getAverageRating(), is(3f));
		book1.addRating(5);
		assertThat(book1.getAverageRating(), is(4f));
	}

	@Test
	public void testHadSaleMiss() {
		assertThat(book1.getSaleMisses(), is(0l));
		book1.addSaleMiss();
		assertThat(book1.hadSaleMiss(), is(true));
	}

	@Test
	public void testImmutableBook() {
		ImmutableBook book = book1.immutableBook();
		assertThat(book1.getAuthor(), is(book.getAuthor()));
		assertThat(book1.getISBN(), is(book.getISBN()));
		assertThat(book1.getPrice(), is(book.getPrice()));
		assertThat(book1.getTitle(), is(book.getTitle()));
	}

	@Test
	public void testImmutableStockBook() {
		StockBook book = book1.immutableStockBook();
		assertThat(book1.getAuthor(), is(book.getAuthor()));
		assertThat(book1.getISBN(), is(book.getISBN()));
		assertThat(book1.getPrice(), is(book.getPrice()));
		assertThat(book1.getTitle(), is(book.getTitle()));
		assertThat(book1.getNumCopies(), is(book.getNumCopies()));
		assertThat(book1.getAverageRating(), is(book.getAverageRating()));
		assertThat(book1.getSaleMisses(), is(book.getSaleMisses()));
		assertThat(book1.getTimesRated(), is(book.getTimesRated()));
		assertThat(book1.getTotalRating(), is(book.getTotalRating()));
	}
}
