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
package io.github.mtrevisan.familylegacy.v2.ui.bindings;

import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * Thread-safe debouncer utility based on {@link ScheduledExecutorService}.
 * Consolidates rapid sequential calls for a given key into a single delayed execution.
 *
 * @param <T> the type of the debounce key
 */
public class Debouncer<T>{

	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		final Thread thread = new Thread(r, "Debouncer-Worker");
		thread.setDaemon(true);
		return thread;
	});


	private final ConcurrentHashMap<T, ScheduledFuture<?>> delayedMap = new ConcurrentHashMap<>();
	private final Consumer<T> callback;
	private final int intervalMs;


	public Debouncer(final Consumer<T> callback, final int intervalMs){
		this.callback = Objects.requireNonNull(callback, "Callback cannot be null");
		this.intervalMs = intervalMs;
	}


	/**
	 * Schedules or postpones the execution of the callback for the specified key.
	 *
	 * @param key the debounce key
	 */
	public void call(final T key){
		if(key == null)
			return;

		delayedMap.compute(key, (k, existingFuture) -> {
			if(existingFuture != null)
				existingFuture.cancel(false);

			return SCHEDULER.schedule(() -> executeTask(k), intervalMs, TimeUnit.MILLISECONDS);
		});
	}

	/**
	 * Cancels any pending scheduled task for the specified key without triggering the callback.
	 *
	 * @param key the debounce key to terminate
	 */
	public void terminate(final T key){
		if(key == null)
			return;

		final ScheduledFuture<?> future = delayedMap.remove(key);
		if(future != null)
			future.cancel(false);
	}

	/**
	 * Cancels all pending tasks managed by this debouncer instance.
	 */
	public void terminate(){
		delayedMap.values()
			.forEach(future -> future.cancel(false));
		delayedMap.clear();
	}

	private void executeTask(final T key){
		try{
			if(SwingUtilities.isEventDispatchThread())
				callback.accept(key);
			else
				SwingUtilities.invokeLater(() -> callback.accept(key));
		}
		finally{
			delayedMap.remove(key);
		}
	}

}
