package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Generic asynchronous loader with caching.
 * <p>
 * Runs a {@link Callable} off the EDT, caches its result under a given key, and always resolves
 * the callback on the EDT — whether the value came from cache or was just computed. Knows
 * nothing about what {@code T} is: images, documents, parsed files, anything with a natural
 * string key can go through the same instance.
 * <p>
 * A failure (the {@code Callable} throwing) resolves the callback with {@code fallback} and is
 * never cached, so a transient error on one call does not poison later calls for the same key.
 *
 * @param <T> the type of value being loaded and cached
 */
public final class AsyncResourceLoader<T>{

	private final Map<String, T[]> cache = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<T[]>> inFlight = new ConcurrentHashMap<>();


	/**
	 * Loads (or retrieves from cache) the value for {@code key}, calling {@code callback} on the
	 * EDT with the result.
	 *
	 * @param key      cache key; a cache hit skips {@code task} entirely
	 * @param task     work to run off the EDT if {@code key} is not already cached
	 * @param callback invoked on the EDT with the cached/computed value, or {@code fallback}
	 */
	public void load(final String key, final Callable<T[]> task, final Consumer<T[]> callback){
		final T[] cached = cache.get(key);
		if(cached != null){
			SwingUtilities.invokeLater(() -> callback.accept(cached));

			return;
		}

		// If there is already a task running for this key, it hooks into it.
		inFlight.computeIfAbsent(key, k -> {
			final CompletableFuture<T[]> future = new CompletableFuture<>();
			new SwingWorker<T[], Void>(){
				@Override
				protected T[] doInBackground() throws Exception{
					return task.call();
				}

				@Override
				protected void done(){
					try{
						final T[] result = get();
						if(result != null)
							cache.put(key, result);
						future.complete(result);
					}
					catch(final Exception e){
						future.complete(null);
					}
					finally{
						inFlight.remove(key);
					}
				}
			}.execute();
			return future;
		}).thenAccept(result -> SwingUtilities.invokeLater(() -> callback.accept(result)));
	}

	/**
	 * Drops every cached value. Does not affect loads already in flight.
	 */
	public void clearCache(){
		cache.clear();
	}

	/**
	 * Drops the cached value for {@code key}, if any, so the next {@link #load} for it recomputes.
	 */
	public void invalidate(final String key){
		cache.remove(key);
	}

}
