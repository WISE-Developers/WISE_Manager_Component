package ca.wise.lib.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ca.wise.lib.INewJobListener;
import ca.wise.lib.JobLists;
import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;

public class SocketListener implements Runnable, Closeable {
	
	private final int port = 32478;
	private ServerSocketChannel serverChannel;
	private boolean close = false;
	private boolean isRunning = false;
	private ThreadPoolExecutor executor;
	private Lock lock = new ReentrantLock();
	private INewJobListener listener;
	private JobLists jobs;
	
	public SocketListener(JobLists jobs, INewJobListener listener) {
		this.listener = listener;
		this.jobs = jobs;
		executor = new ThreadPoolExecutor(2, 32, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
		try {
			serverChannel = ServerSocketChannel.open();
			InetSocketAddress hostAddress = new InetSocketAddress(port);
			serverChannel.bind(hostAddress);
			serverChannel.socket().setSoTimeout((int)5000);
		}
		catch (IOException e) {
			WISELogger.getSpecial(LogName.Backend).error("Unable to open socket.", e);
		}
	}

	@Override
	public void run() {
		lock.lock();
		try {
			while (!close) {
				try {
					Socket channel = serverChannel.socket().accept();
					ConnectionHandler handler = new ConnectionHandler(jobs, channel, listener);
					executor.execute(handler);
				}
				catch (SocketTimeoutException e) { }
				catch (IOException e) {
					if (!close)
						WISELogger.getSpecial(LogName.Backend).error("Error connecting to client socket.", e);
				}
			}
			
			try {
				cleanupResources();
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).error("Unable to close socket.", e);
			}
		}
		finally {
			lock.unlock();
		}
	}
	
	/**
	 * Stop the executing socket listener. This method will block until the socket
	 * listener has shutdown.
	 */
	public void stop() {
		close = true;
		try {
			serverChannel.socket().close();
		}
		catch (Exception e) { }
		lock.lock();
		lock.unlock();
	}

	@Override
	public void close() throws IOException {
		close = true;
		if (!isRunning) {
			cleanupResources();
		}
	}
	
	private void cleanupResources() throws IOException {
		if (serverChannel != null) {
			if (serverChannel.isOpen())
				serverChannel.close();
			serverChannel = null;
		}
	}
}
