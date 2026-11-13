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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.Tile;
import org.jxmapviewer.viewer.TileFactoryInfo;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * A tile factory whose requests that have not started yet can be cancelled, because the view moved on.
 * <p>
 * The factory of the library queues every tile it is asked for and cannot forget it: after a few pans (or a drag of the timeline) the
 * network is still busy with tiles that are no longer on the screen. Here the tiles wait in a queue of this class, the newest first,
 * and are handed to the library only as the loading threads become free, so that at most one tile per thread is waiting there.
 * {@link #cancelPending()} drops the ones still waiting here: a dropped tile is requested again, by the next paint, if it is still
 * on the screen. The requests already running cannot be interrupted, but they are no more than the threads of the pool.
 */
public final class CancellableTileFactory extends DefaultTileFactory{

	private static final long PUMP_INTERVAL_MS = 50;


	private final int threads;
	/** Protects the queue and the pump: it is not the monitor of the factory, which the library holds while it loads. */
	private final Object lock = new Object();
	/** The tiles waiting to be requested: the last one is the newest, that is the one most likely to be on the screen. */
	private final Deque<Tile> waiting = new ArrayDeque<>();
	/** The tiles that were dropped, and not asked for again yet. */
	private final Set<Tile> cancelled = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	private ScheduledExecutorService pump;
	/** There is at most one pump waiting to run: more of them would only compete for the same work. */
	private boolean pumpScheduled;


	public CancellableTileFactory(final TileFactoryInfo info, final int threads){
		super(info);

		this.threads = threads;
		setThreadPoolSize(threads);
	}


	@Override
	public Tile getTile(final int x, final int y, final int zoom){
		final Tile tile = super.getTile(x, y, zoom);

		//a tile that was dropped and is on the screen again is requested again
		if(tile != null && !tile.isLoaded() && cancelled.remove(tile))
			enqueue(tile);

		return tile;
	}

	@Override
	protected void startLoading(final Tile tile){
		enqueue(tile);
	}

	private void enqueue(final Tile tile){
		synchronized(lock){
			waiting.addLast(tile);

			schedulePump();
		}
	}

	/** Must be called holding the lock. */
	private void schedulePump(){
		if(pumpScheduled)
			return;

		if(pump == null)
			pump = Executors.newSingleThreadScheduledExecutor(runnable -> {
				final Thread thread = new Thread(runnable, "tile-queue");
				thread.setDaemon(true);
				return thread;
			});

		pumpScheduled = true;
		pump.schedule(this::pump, PUMP_INTERVAL_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Hands the newest tiles to the library, as long as its loading threads have not got a tile waiting already. The library is called
	 * without holding the lock, so that the interface thread, which only needs the lock to add or drop tiles, is never made to wait.
	 */
	private void pump(){
		while(true){
			final Tile tile;
			synchronized(lock){
				if(waiting.isEmpty()){
					pumpScheduled = false;

					return;
				}
				if(getPendingTiles() >= threads){
					pumpScheduled = false;
					schedulePump();

					return;
				}

				tile = waiting.removeLast();
			}

			super.startLoading(tile);
		}
	}

	/** Forgets the tiles that were not requested yet: they are asked for again if they are still needed. */
	public void cancelPending(){
		synchronized(lock){
			cancelled.addAll(waiting);
			waiting.clear();
		}
	}

	@Override
	public void dispose(){
		synchronized(lock){
			waiting.clear();
			if(pump != null){
				pump.shutdownNow();
				pump = null;
			}
			pumpScheduled = false;
		}

		super.dispose();
	}

}
