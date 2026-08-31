package io.github.mtrevisan.familylegacy.v2.ui.helpers;

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
