package server;

/**
 * This class use lock based concurrency control
 * for replica servers synchronization
 */
public class ReadWriteLock{
	private int readers = 0;
	private int writers = 0;
	private int writeRequests = 0;

	/**
	 * Forces the current thread to wait until
	 * other thread invokes notify() or notifyAll() on the same object.
	 * @throws InterruptedException lock is interrupted
	 */
	public synchronized void lockRead() throws InterruptedException {
		while (writers > 0 || writeRequests > 0) {
			wait();
		}
		readers++;
	}

	/**
	 * Wakes all threads that are waiting on this object's monitor.
	 */
	public synchronized void unlockRead() {
		readers--;
		notifyAll();
	}

	/**
	 * Forces the current thread to wait until
	 * other thread invokes notify() or notifyAll() on the same object.
	 * @throws InterruptedException lock is interrupted
	 */
	public synchronized void lockWrite() throws InterruptedException {
		writeRequests++;
		 
		while (readers > 0 || writers > 0) {
			wait();
		}
		writeRequests--;
		writers++;
	}

	/**
	 * Wakes all threads that are waiting on this object's monitor.
	 */
	public synchronized void unlockWrite() {
		writers--;
		notifyAll();
	}
}
