package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.acertainbookstore.business.BookStoreBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {
	private Random random = new Random();

	private String getRandomAlphaString() {
		final String alphabet = "0123456789ABCDE";
	    final int N = alphabet.length();
	    
	    String out = "";
	    for(int i = 0; i < 20; i++) {
	    	out += alphabet.charAt(random.nextInt(N));
	    }
	    return out;
	}

	private BookStoreBook generateRandomBook(int isbn) {
		String author = getRandomAlphaString();
		String title = getRandomAlphaString();
		float price = random.nextFloat() * 1000;
		int copies = random.nextInt(100) + 50;
		return new BookStoreBook(isbn, title, author, price, copies);
	}
	
	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
		List<Integer> shuffled = new ArrayList<Integer>(isbns);
		Set<Integer> out = new HashSet<Integer>();
		Collections.shuffle(shuffled);
		for(int i = 0; i < num; i++) {
			out.add(shuffled.get(i));
		}
		return out;
	}

	private Set<Integer> generateRandomInts(int num) {
		Set<Integer> out = new HashSet<Integer>();
		while(out.size() < num) {
			Integer next = 1 + random.nextInt(Integer.MAX_VALUE - 1);
			out.add(next);
		}
		return out;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) {
		Set<StockBook> out = new HashSet<StockBook>();
		Set<Integer> isbns = this.generateRandomInts(num);
		for(Integer isbn : isbns) {
			out.add(this.generateRandomBook(isbn).immutableStockBook());
		}
		return out;
	}

}
