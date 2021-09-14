/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class Debouncer<T>{

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>(0);

	private final Consumer<T> callback;
	private final int interval;


	public Debouncer(final Consumer<T> callback, final int interval){
		Objects.requireNonNull(callback);

		this.callback = callback;
		this.interval = interval;
	}

	public void call(final T key){
		final TimerTask task = new TimerTask(key);

		TimerTask prev;
		do{
			prev = delayedMap.putIfAbsent(key, task);
			if(prev == null)
				executor.schedule(task, interval, TimeUnit.MILLISECONDS);
			//exit only if new task was added to map, or existing task was extended successfully
		}while(prev != null && !prev.extend());
	}

	public void terminate(){
		executor.shutdownNow();
	}


	//The task that wakes up when the wait time elapses
	private final class TimerTask implements Runnable{

		private final T key;
		private long dueTime;
		private final Object lock = new Object();


		TimerTask(final T key){
			this.key = key;

			extend();
		}

		public boolean extend(){
			synchronized(lock){
				//task has been shut down
				if(dueTime < 0)
					return false;

				dueTime = System.currentTimeMillis() + interval;
				return true;
			}
		}

		@Override
		public void run(){
			synchronized(lock){
				final long remaining = dueTime - System.currentTimeMillis();
				//re-schedule task
				if(remaining > 0)
					executor.schedule(this, remaining, TimeUnit.MILLISECONDS);
					//mark as terminated and invoke callback
				else{
					dueTime = -1;
					try{
						callback.accept(key);
					}
					finally{
						delayedMap.remove(key);
					}
				}
			}
		}
	}

}
