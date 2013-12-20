package com.acertainbookstore.business;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.acertainbookstore.client.BookStoreClientConstants;
import com.acertainbookstore.interfaces.Replicator;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreResult;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainBookStoreReplicator implements Replicator {
	private HttpClient client;
	private ExecutorService executor;

	public CertainBookStoreReplicator(int maxReplicatorThreads) {
		this.executor = Executors.newFixedThreadPool(maxReplicatorThreads);
		this.client = new HttpClient();
		client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		client.setMaxConnectionsPerAddress(200);
		client.setThreadPool(new QueuedThreadPool(250));
		client.setTimeout(30000);
		try {
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Future<ReplicationResult>> replicate(Set<String> slaveServers,
			ReplicationRequest request) {
		List<Callable<ReplicationResult>> todo = new ArrayList<Callable<ReplicationResult>>(slaveServers.size());
		for(String slave : slaveServers) {
			Callable<ReplicationResult> callable = new CertainBookStoreReplicationTask(this.client, request, slave);
			todo.add(callable);
		}

		try {
			List<Future<ReplicationResult>> result = this.executor.invokeAll(todo);
			return result;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
