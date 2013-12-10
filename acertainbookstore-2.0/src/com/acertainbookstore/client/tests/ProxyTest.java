package com.acertainbookstore.client.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;

public abstract class ProxyTest {
	protected static StockManager	storeManager;
	protected static BookStore	client;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
			client = new BookStoreHTTPProxy("http://localhost:8081");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void tearDownAfterClass() {
		((BookStoreHTTPProxy) client).stop();
		((StockManagerHTTPProxy) storeManager).stop();
	}
}
