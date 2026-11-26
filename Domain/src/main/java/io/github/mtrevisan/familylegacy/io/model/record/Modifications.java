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
package io.github.mtrevisan.familylegacy.io.model.record;


import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Counts the changes made to the records and to the lists of records of the models, all of them, wherever they are: it tells that
 * <em>nothing</em> changed since a moment (the value is the same), so that who keeps a copy of a model need not compare it with the
 * model to know that it is still the same. It cannot tell what changed: when the value is different, a comparison is still needed.
 * <p>It is a single counter, written by the thread that edits (the others only read it): two threads editing at the very same time
 * can lose a count, and the model is not made for that.</p>
 */
public final class Modifications{

	private static final AtomicLong count= new AtomicLong(0l);
	private static volatile boolean shared;

	/** How many of the records modified last are remembered (the ones modified before are forgotten, and it is known that they are). */
	private static final int RING = 8192;
	private static final FLEFRecord[] ringRecords = new FLEFRecord[RING];
	private static final long[] ringStamps = new long[RING];
	private static int ringHead;
	private static int ringSize;
	/** The greatest value of the counter at which a record that is not remembered any more was modified. */
	private static long ringForgotten;


	private Modifications(){}


	/**
	 * Tells that something changed.
	 *
	 * @return	The new value of the counter.
	 */
	public static long touch(){
		return count.incrementAndGet();
	}

/** Tells that a top-level record (or something below it) was modified when the counter was at {@code stamp}. */
static synchronized void log(final FLEFRecord root, final long stamp){
	if(ringSize > 0){
		final int last = (ringHead + RING - 1) % RING;
		if(ringRecords[last] == root){
			ringStamps[last] = stamp;

			return;
		}
	}

	if(ringSize == RING)
		ringForgotten = Math.max(ringForgotten, ringStamps[ringHead]);
	else
		ringSize ++;
	ringRecords[ringHead] = root;
	ringStamps[ringHead] = stamp;
	ringHead = (ringHead + 1) % RING;
}

/**
 * The top-level records modified after a moment, so that who keeps a copy of a model looks at those and not at all the records.
 *
 * @param stamp	A value of {@link #current()}.
 * @param modified	Receives the records (a record once), those that are not in any model too.
 * @param limit	The most records worth looking at: if they are more, the caller is better off comparing everything.
 * @return	Whether {@code modified} has all of them: {@code false} if some may have been forgotten (too many were modified), or if they
 * 	are more than {@code limit}. No record is modified without being remembered, unless {@link #sharedNodesSeen()}.
 */
public static synchronized boolean modifiedSince(final long stamp, final List<FLEFRecord> modified, final int limit){
	if(ringForgotten > stamp)
		return false;

	for(int i = 0; i < ringSize; i ++){
		final int index = (ringHead + RING - ringSize + i) % RING;
		if(ringStamps[index] > stamp && !containsIdentity(modified, ringRecords[index])){
			if(modified.size() == limit)
				return false;

			modified.add(ringRecords[index]);
		}
	}
	return true;
}

private static boolean containsIdentity(final List<FLEFRecord> records, final FLEFRecord record){
	for(int i = 0, size = records.size(); i < size; i ++)
		if(records.get(i) == record)
			return true;
	return false;
}

/** Tells that a record was put into two lists: from now on a record that was modified does not tell which records contain it. */
	static void markShared(){
		shared = true;
	}

	/**
	 * @return	Whether a record has been seen in two lists (once, at any moment, in the whole program): the records marked as modified by
	 * 	the records remembered by {@link #modifiedSince(long, java.util.List, int)} are then not all the ones that contain what was modified.
	 */
	public static boolean sharedNodesSeen(){
		return shared;
	}

	/** @return	A value that is the same as the one given before if and only if nothing changed in between. */
	public static long current(){
		return count.get();
	}

}
