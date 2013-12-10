package com.acertainbookstore.server;

import java.io.IOException;
import java.util.Set;

import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreResponse;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * The BookStoreResponseCrafter class is used by {@link BookStoreHTTPMessageHandler} as
 * an intermediary object, which accepts (potentially) input and will create a {@link BookStoreResponse}
 * based on what {@link CertainBookStore} responds with, that it can then serialize again. This provides
 * a cleaner abstraction level between path-handling, response generation and actual API calls, and also
 * provides for code that is much easier to test. In particular because this class doesn't know about what
 * StockManager or BookStore happens to be backing the request, so we can proxy or mock those in testing.
 * @author Martin Grunbaum
 *
 */
public class BookStoreResponseCrafter {
	@SuppressWarnings("unchecked")
	public static BookStoreResponse addBooks(StockManager stock, String xml) throws IOException {
		Set<StockBook> bookSet = (Set<StockBook>) BookStoreUtility.deserializeXMLStringToObject(xml);

		BookStoreResponse bookStoreresponse = new BookStoreResponse();
		try {
			stock.addBooks(bookSet);
		} catch (BookStoreException ex) {
			bookStoreresponse.setException(ex);
		}
		return bookStoreresponse;
	}
	
	@SuppressWarnings("unchecked")
	public static BookStoreResponse addCopies(StockManager stock, String xml) throws IOException {
		Set<BookCopy> listBookCopies = (Set<BookCopy>) BookStoreUtility.deserializeXMLStringToObject(xml);
		BookStoreResponse resp = new BookStoreResponse();
		try {
			stock.addCopies(listBookCopies);
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	public static BookStoreResponse listBooks(StockManager stock) {
		BookStoreResponse resp = new BookStoreResponse();
		try {
			resp.setList(stock.getBooks());
		} catch (BookStoreException e) {
			resp.setException(e);
		}
		return resp;
	}

	@SuppressWarnings("unchecked")
	public static BookStoreResponse updateEditorPicks(StockManager stock, String xml) throws IOException {
		BookStoreResponse resp = new BookStoreResponse();
		try {
			Set<BookEditorPick> mapEditorPicksValues = (Set<BookEditorPick>) BookStoreUtility.deserializeXMLStringToObject(xml);
			stock.updateEditorPicks(mapEditorPicksValues);
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	@SuppressWarnings("unchecked")
	public static BookStoreResponse buyBooks(BookStore store, String xml) throws IOException {
		Set<BookCopy> bookCopiesToBuy = (Set<BookCopy>) BookStoreUtility.deserializeXMLStringToObject(new String(xml));

		// Make the purchase
		BookStoreResponse resp = new BookStoreResponse();
		try {
			store.buyBooks(bookCopiesToBuy);
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	@SuppressWarnings("unchecked")
	public static BookStoreResponse getBooks(BookStore store, String xml) throws IOException {
		Set<Integer> isbnSet = (Set<Integer>) BookStoreUtility.deserializeXMLStringToObject(xml);

		BookStoreResponse resp = new BookStoreResponse();
		try {
			resp.setList(store.getBooks(isbnSet));
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	public static BookStoreResponse editorPicks(BookStore store, String numBooksStr) throws IOException {
		BookStoreResponse resp = new BookStoreResponse();
		try {
			int numBooks = BookStoreUtility.convertStringToInt(numBooksStr);
			resp.setList(store.getEditorPicks(numBooks));
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	@SuppressWarnings("unchecked")
	public static BookStoreResponse rateBooks(BookStore store, String xml) throws IOException {
		Set<BookRating> ratings = (Set<BookRating>) BookStoreUtility.deserializeXMLStringToObject(xml);
		
		BookStoreResponse resp = new BookStoreResponse();
		try {
			store.rateBooks(ratings);
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	public static BookStoreResponse getTopRated(BookStore store, String numBooksStr) throws IOException {
		BookStoreResponse resp = new BookStoreResponse();
		try {
			int numBooks = BookStoreUtility.convertStringToInt(numBooksStr);
			resp.setList(store.getTopRatedBooks(numBooks));
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	public static BookStoreResponse getInDemand(StockManager stock) throws IOException {
		BookStoreResponse resp = new BookStoreResponse();
		try {
			resp.setList(stock.getBooksInDemand());
		} catch (BookStoreException ex) {
			resp.setException(ex);
		}
		return resp;
	}

	public static BookStoreResponse clear(CertainBookStore instance) {
		instance.clear();
		return new BookStoreResponse();
	}
}
