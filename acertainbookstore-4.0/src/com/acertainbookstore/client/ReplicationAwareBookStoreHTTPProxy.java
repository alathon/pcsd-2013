/**
 * 
 */
package com.acertainbookstore.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * 
 * ReplicationAwareBookStoreHTTPProxy implements the client level synchronous
 * CertainBookStore API declared in the BookStore class. It keeps retrying the
 * API until a consistent reply is returned from the replicas
 * 
 */
public class ReplicationAwareBookStoreHTTPProxy implements BookStore {
	private HttpClient client;
	private List<String> slaveAddresses;
	private String masterAddress;
	private String filePath = "proxy.properties";
	private volatile long snapshotId = 0;
	private volatile int nextSlave = 0;
	private static int maxTries = 5;
	
	public long getSnapshotId() {	
		return snapshotId;
	}

	public void setSnapshotId(long snapShotId) {
		this.snapshotId = snapShotId;
	}

	/**
	 * Initialize the client object
	 */
	public ReplicationAwareBookStoreHTTPProxy() throws Exception {
		initializeReplicationAwareMappings();
		client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setMaxConnectionsPerAddress(BookStoreClientConstants.CLIENT_MAX_CONNECTION_ADDRESS); // max
																									// concurrent
																									// connections
																									// to
																									// every
																									// address
		client.setThreadPool(new QueuedThreadPool(
				BookStoreClientConstants.CLIENT_MAX_THREADSPOOL_THREADS)); // max
																			// threads
		client.setTimeout(BookStoreClientConstants.CLIENT_MAX_TIMEOUT_MILLISECS); // seconds
																					// timeout;
																					// if
																					// no
																					// server
																					// reply,
																					// the
																					// request
																					// expires
		client.start();
	}

	private void initializeReplicationAwareMappings() throws IOException {

		Properties props = new Properties();
		slaveAddresses = new LinkedList<String>();
		
		InputStream inpStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
		props.load(inpStream);
		this.masterAddress = props
				.getProperty(BookStoreConstants.KEY_MASTER);
		if (!this.masterAddress.toLowerCase().startsWith("http://")) {
			this.masterAddress = new String("http://" + this.masterAddress);
		}

		String slaveAddresses = props
				.getProperty(BookStoreConstants.KEY_SLAVE);
		for (String slave : slaveAddresses
				.split(BookStoreConstants.SPLIT_SLAVE_REGEX)) {
			if (!slave.toLowerCase().startsWith("http://")) {
				slave = new String("http://" + slave);
			}
			this.slaveAddresses.add(slave);
		}
	}

	public String getReplicaAddress(int tries) {
		if(tries > 3) return this.masterAddress;
		
		String slave = this.slaveAddresses.get(this.nextSlave);
		this.nextSlave = (++this.nextSlave) % this.slaveAddresses.size();
		return slave;
	}

	public String getMasterServerAddress() {
		return this.masterAddress;
	}

	public void buyBooks(Set<BookCopy> isbnSet) throws BookStoreException {

		String listISBNsxmlString = BookStoreUtility
				.serializeObjectToXMLString(isbnSet);
		Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);

		BookStoreResult result = null;

		ContentExchange exchange = new ContentExchange();
		String urlString = getMasterServerAddress() + "/"
				+ BookStoreMessageTag.BUYBOOKS;
		exchange.setMethod("POST");
		exchange.setURL(urlString);
		exchange.setRequestContent(requestContent);
		result = BookStoreUtility.SendAndRecv(this.client, exchange);
		this.setSnapshotId(result.getSnapshotId());
	}

	@SuppressWarnings("unchecked")
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {

		String listISBNsxmlString = BookStoreUtility
				.serializeObjectToXMLString(isbnSet);
		Buffer requestContent = new ByteArrayBuffer(listISBNsxmlString);

		BookStoreResult result = null;
		int tries = 0;
		do {
			ContentExchange exchange = new ContentExchange();
			String urlString = getReplicaAddress(tries) + "/"
					+ BookStoreMessageTag.GETBOOKS;
			exchange.setMethod("POST");
			exchange.setURL(urlString);
			exchange.setRequestContent(requestContent);
			try {
				result = BookStoreUtility.SendAndRecv(this.client, exchange);
			} catch (BookStoreException ex) {
				if(ex.getMessage().equals(BookStoreClientConstants.strERR_CLIENT_REQUEST_TIMEOUT)) {
					tries++;
					continue;
				} else {
					throw ex;
				}
			}
		} while (result.getSnapshotId() < this.getSnapshotId()
				&& tries < ReplicationAwareBookStoreHTTPProxy.maxTries);
		
		if(tries == ReplicationAwareBookStoreHTTPProxy.maxTries) {
			throw new BookStoreException(BookStoreClientConstants.strERR_CLIENT_FAILURE);
		}

		this.setSnapshotId(result.getSnapshotId());
		return (List<Book>) result.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		ContentExchange exchange = new ContentExchange();
		String urlEncodedNumBooks = null;

		try {
			urlEncodedNumBooks = URLEncoder.encode(Integer.toString(numBooks),
					"UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new BookStoreException("unsupported encoding of numbooks", ex);
		}

		BookStoreResult result = null;
		int tries = 0;
		do {
			String urlString = getReplicaAddress(tries) + "/"
					+ BookStoreMessageTag.EDITORPICKS + "?"
					+ BookStoreConstants.BOOK_NUM_PARAM + "="
					+ urlEncodedNumBooks;
			exchange.setURL(urlString);
			try {
				result = BookStoreUtility.SendAndRecv(this.client, exchange);
			} catch (BookStoreException ex) {
				if(ex.getMessage().equals(BookStoreClientConstants.strERR_CLIENT_REQUEST_TIMEOUT)) {
					tries++;
					continue;
				} else {
					throw ex;
				}
			}
		} while (result.getSnapshotId() < this.getSnapshotId()
				&& tries < ReplicationAwareBookStoreHTTPProxy.maxTries);
		
		if(tries == ReplicationAwareBookStoreHTTPProxy.maxTries) {
			throw new BookStoreException(BookStoreClientConstants.strERR_CLIENT_FAILURE);
		}
		this.setSnapshotId(result.getSnapshotId());

		return (List<Book>) result.getResultList();
	}

	public void stop() {
		try {
			client.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();

	}

	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		// TODO Auto-generated method stub
		throw new BookStoreException();
	}

}
