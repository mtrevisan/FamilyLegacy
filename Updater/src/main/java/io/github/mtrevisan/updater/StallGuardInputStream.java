package io.github.mtrevisan.updater;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Closes the underlying stream if a single read waits longer than a timeout, so that a stalled connection fails (with an
 * {@link IOException}) instead of blocking forever: the timeout of the HTTP request only covers the headers.
 */
final class StallGuardInputStream extends FilterInputStream{

	private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
		final Thread thread = new Thread(task, "updater-stall-guard");
		thread.setDaemon(true);
		return thread;
	});


	private final long timeoutMillis;
	private volatile boolean stalled;


	StallGuardInputStream(final InputStream in, final Duration timeout){
		super(in);

		timeoutMillis = timeout.toMillis();
	}

	@Override
	public int read() throws IOException{
		final ScheduledFuture<?> guard = arm();
		try{
			return super.read();
		}
		catch(final IOException e){
			throw translate(e);
		}
		finally{
			guard.cancel(false);
		}
	}

	@Override
	public int read(final byte[] buffer, final int offset, final int length) throws IOException{
		final ScheduledFuture<?> guard = arm();
		try{
			return super.read(buffer, offset, length);
		}
		catch(final IOException e){
			throw translate(e);
		}
		finally{
			guard.cancel(false);
		}
	}

	private ScheduledFuture<?> arm(){
		return WATCHDOG.schedule(() -> {
			stalled = true;
			try{
				in.close();
			}
			catch(final IOException ignored){
				//nothing more to do
			}
		}, timeoutMillis, TimeUnit.MILLISECONDS);
	}

	private IOException translate(final IOException e){
		if(!stalled)
			return e;

		final InterruptedIOException timeout = new InterruptedIOException("The connection stalled for more than " + timeoutMillis + " ms");
		timeout.initCause(e);
		return timeout;
	}

}
