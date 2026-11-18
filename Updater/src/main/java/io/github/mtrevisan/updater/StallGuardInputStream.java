/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
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
