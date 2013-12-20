package com.acertainbookstore.business;

import java.util.concurrent.Callable;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteArrayBuffer;

import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * CertainBookStoreReplicationTask performs replication to a slave server. It
 * returns the result of the replication on completion using ReplicationResult
 */
public class CertainBookStoreReplicationTask implements
		Callable<ReplicationResult> {
	
	private final ReplicationRequest request;
	private final String slave;
	private final HttpClient client;
	
	protected CertainBookStoreReplicationTask(HttpClient client, ReplicationRequest request, String slave) {
		this.request = request;
		this.slave = slave;
		this.client = client;
	}

	@Override
	public ReplicationResult call() throws Exception {
		ContentExchange exchange = new ContentExchange();
		String url = this.slave + "/" + request.getMessageType();
		String serialData = BookStoreUtility.serializeObjectToXMLString(request.getDataSet());
		exchange.setMethod("POST");
		exchange.setURL(url);
		exchange.setRequestContent(new ByteArrayBuffer(serialData));

		BookStoreResult result = BookStoreUtility.SendAndRecv(this.client, exchange);
		// TODO: Verify that result is in fact correct instead of simply setting
		// to true.
		ReplicationResult res = new ReplicationResult(this.slave, true);
		return res;
	}

}
