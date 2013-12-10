package com.acertainbookstore.locking;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a reentrant read/write lock, with preference towards
 * writes, as we assume those are generally short and less frequent than reads.
 * 
 * Based upon the many (amazing) tutorials written by Jakob Jenkov, available
 * at {@link <a href="http://tutorials.jenkov.com/">Jenkov Tutorials</a>}
 * 
 * The readers map maps reader threads to the number of read holds each thread has.
 * As the write lock is eXclusive, there is a writer variable to keep track of the thread,
 * if any, that has the write lock. To support re-entrant locking for writes, the writeAccess
 * variable keeps track of the number of times a write-lock has been secured, while the writeRequests
 * variable keeps track of how many write requesters are waiting, in order to up-prioritize writes when
 * a read lock is released.
 * @author Martin Grunbaum
 *
 */
public class ReentrantRWLock {
	private Map<Thread, Integer> readers = new HashMap<Thread, Integer>();
	private Thread writer;
	private int writeAccess = 0;
	private int writeRequests = 0;

	/**
	 * Establishes a read lock, once possible, as per the conditions of
	 * {@link ReentrantRWLock}'s couldRead() method.
	 * @throws InterruptedException
	 */
	public synchronized void readLock() throws InterruptedException {
		Thread current = Thread.currentThread();
		while(!this.couldRead(current)) {
			wait();
		}
		
		this.readers.put(current, this.getReadHoldCount(current) + 1);
	}

	/**
	 * Establish a write lock. Any attempt to do so triggers an increment in the
	 * number of requests, and then a wait until the write lock is available as per
	 * the conditions of {@link ReentrantRWLock}'s couldWrite() method.
	 * @throws InterruptedException
	 */
	public synchronized void writeLock() throws InterruptedException {
		this.writeRequests += 1;
		Thread current = Thread.currentThread();
		while(!this.couldWrite(current)) {
			wait();
		}
		this.writeRequests -= 1;
		this.writeAccess += 1;
		this.writer = current;
	}

	/**
	 * Will unlock the write lock, if the calling thread is in fact the writer.
	 * If the writer has re-entered the lock multiple times, the lock will only be cleared
	 * once {@link ReentrantRWLock}'s writeAccess is decremented to zero.
	 * @throws InterruptedException
	 */
	public synchronized void writeUnlock() throws InterruptedException {
		Thread current = Thread.currentThread();
		if(!this.isWriter(current)) {
			throw new IllegalMonitorStateException(current + " tried to unlock a Write lock it does not have.");
		}
		
		if(--this.writeAccess == 0) {
			this.writer = null;
		}
		
		notifyAll();
	}

	/**
	 * Will unlock the read lock, if the calling thread is in fact
	 * a reader. For readers that have re-entered the lock, the lock is only
	 * removed once the reader has no lock counts left.
	 * @throws InterruptedException
	 */
	public synchronized void readUnlock() throws InterruptedException {
		Thread current = Thread.currentThread();
		if(!this.isReader(current)) {
			throw new IllegalMonitorStateException(current + " tried to unlock a Read lock it does not have.");
		}
		
		int reads = this.getReadHoldCount(current);
		if(--reads == 0) {
			this.readers.remove(current);
		} else {
			this.readers.put(current, reads);
		}
		
		notifyAll();
	}

	
	public int getReadHoldCount(Thread caller) {
		if(this.readers.containsKey(caller)) {
			return this.readers.get(caller);
		} else {
			return 0;
		}
	}
	
	private boolean couldWrite(Thread caller) {
		// If we're the only reader already.
		if(this.isOnlyReader(caller)) return true;
		// If there are others, then we can't get an exclusive lock, so no go.
		if(this.hasAnyReaders()) return false;
		// There isn't currently a writer. Go ahead and grab a write lock.
		if(!this.hasWriter()) return true;
		// There is a writer, but its not you. No go.
		if(!this.isWriter(caller)) return false;
		return true;
	}

	private boolean couldRead(Thread caller) {
		// Writer can always obtain a read lock.
		if(this.isWriter(caller)) return true;
		// If there is a writer and its not you, you can't read.
		if(this.hasWriter()) return false;
		// If you're already a reader, go ahead.
		if(this.isReader(caller)) return true;
		// If there are write requests pending, don't allow.
		// as we'd rather let them get the lock.
		if(this.hasWriteRequests()) return false;
		return true;
	}
	
	/* Utility methods */
	private boolean hasAnyReaders() {
		return this.readers.size() > 0;
	}
	
	private boolean hasWriter() {
		return this.writer != null;
	}
	
	private boolean isReader(Thread caller) {
		return this.readers.get(caller) != null;
	}
	
	private boolean isWriter(Thread caller) {
		return this.writer == caller;
	}
	
	private boolean isOnlyReader(Thread caller) {
		return this.isReader(caller) && this.readers.size() == 1;
	}
	
	private boolean hasWriteRequests() {
		return this.writeRequests > 0;
	}
}
