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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Application-wide undo/redo history of a {@link FLEFModel}.
 * <p>
 * The model is mutated in place from many places, so instead of recording each operation the history stores <em>snapshots</em>:
 * {@link #checkpoint(Origin)} compares the model with the last snapshot and, if something changed, pushes the previous state on
 * the undo stack. Unchanged records are shared between consecutive snapshots, so the memory cost of a step is proportional to
 * what was modified. Snapshot records are never exposed, hence never mutated.
 */
public final class ModelHistory{

	/** What produced a change. */
	public enum Origin{
		/** A direct edit (menu, tree, ...). */
		EDIT,
		/** A record dialog that has been closed saving its content. */
		DIALOG
	}


	/** How a record was affected by a change. */
	public enum Kind{ADDED, MODIFIED, REMOVED}

	/**
	 * A record touched by a step.
	 *
	 * @param id	The id of the record ({@code null} for the header).
	 * @param tag	The tag of the record.
	 * @param kind	How the record was affected.
	 * @param before	The record before the change ({@code null} if added).
	 * @param after	The record after the change ({@code null} if removed).
	 * 	Both belong to the history and <b>must not be modified</b>, nor looked up in the model.
	 */
	public record Change(String id, String tag, Kind kind, FLEFRecord before, FLEFRecord after){

		/** @return	The record as it was after the change (before it, if removed). */
		public FLEFRecord record(){
			return (after != null? after: before);
		}

	}

	/**
	 * A step as shown to the user.
	 *
	 * @param time	When the change was recorded.
	 * @param origin	What produced the change.
	 * @param changes	The records touched.
	 * @param undone	Whether the step is currently reverted (it can be redone).
	 */
	public record Entry(Instant time, Origin origin, List<Change> changes, boolean undone){}


	private static final int MAX_DEPTH = 100;
	private static final String HEADER_TAG = "HEAD";


	private record Snapshot(FLEFRecord header, List<FLEFRecord> records, Map<String, FLEFRecord> recordsById){}

	/** A state together with the origin of the change that moved away from it (or to it, on the redo stack). */
	private record Step(Snapshot state, Origin origin, Instant time, List<Change> changes){}


	private FLEFModel model;
	private Snapshot baseline;
	/** The state last written to a file, or loaded from it ({@code null}: what is in memory is not on any file). */
	private Snapshot saved;
	/** What {@link #readOnlyView()} gave last, and the snapshot it was built from ({@code null} until it is asked for). */
	private FLEFModel view;
	private Snapshot viewSource;
	private final Deque<Step> undoStack = new ArrayDeque<>();
	private final Deque<Step> redoStack = new ArrayDeque<>();
	/** Told when the model changes: a step recorded, an undo or a redo done, the model replaced. */
	private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();


	public ModelHistory(final FLEFModel model){
		reset(model);
	}


	/**
	 * A copy of the model as it is now, that nobody changes: a background thread can read it while the user goes on editing the real
	 * model (the model is changed in place, from many places, and a reader that is not on the thread of the edits would see it half
	 * changed).
	 * <p>
	 * It costs what the model has changed since the last call, not its size: the records that did not change are the ones of the
	 * snapshots the history already keeps (which are never changed), and while the model does not change the same view is given back,
	 * so every reader shares it. No step of the history is recorded, so an edit that is under way does not become one.
	 * </p>
	 * <p>
	 * Call it from the thread that edits the model (the event dispatch thread). The result must only be <b>read</b>: it is not the model
	 * of the file, so nothing done to it reaches the file, and it is not kept up to date (ask again after a change).
	 * </p>
	 *
	 * @return	A model with the same header and records as the one of the history, that is never modified.
	 */
	public FLEFModel readOnlyView(){
		if(view != null && isUnchanged(viewSource, model))
			return view;

		final Snapshot current = (isUnchanged(baseline, model)? baseline: capture(model, (viewSource != null? viewSource: baseline)));
		final FLEFModel copy = new FLEFModel();
		copy.setHeader(current.header());
		for(final FLEFRecord record : current.records())
			copy.addRecord(record);
		viewSource = current;
		view = copy;
		return copy;
	}

	/** Binds the history to a (new) model, discarding every step. */
	public void reset(final FLEFModel model){
		this.model = model;
		view = null;
		viewSource = null;
		undoStack.clear();
		redoStack.clear();
		baseline = capture(model, null);
		//a model that has just been bound is the one of the file it was read from
		saved = baseline;
		fireChanged();
	}

	/** Tells that the content of the model is now on a file: from here on, {@link #isModified()} compares with it. */
	public void markSaved(){
		saved = capture(model, baseline);
	}

	/**
	 * What a write of the model to a file needs: a copy of the model that nobody changes (so that it can be written from a thread other
	 * than the one that edits), and the mark to give to the history once it is written, so that edits made in the meantime are not
	 * taken for saved.
	 */
	public static final class SaveTicket{

		private final FLEFModel view;
		private final Snapshot snapshot;


		private SaveTicket(final FLEFModel view, final Snapshot snapshot){
			this.view = view;
			this.snapshot = snapshot;
		}


		/** @return	The model as it was when the ticket was made, to be only read. */
		public FLEFModel view(){
			return view;
		}

	}

	/**
	 * Prepares the write of the model as it is now. Call it from the thread that edits the model; give the ticket back to
	 * {@link #markSaved(SaveTicket)} when the file has been written.
	 *
	 * @return	The ticket.
	 */
	public SaveTicket prepareSave(){
		final FLEFModel copy = readOnlyView();
		return new SaveTicket(copy, viewSource);
	}

	/** Tells that the content the ticket was made of is now on a file (what was changed after it is not). */
	public void markSaved(final SaveTicket ticket){
		saved = ticket.snapshot;
	}

	/** Tells that what is in memory is not on any file (for instance, a backup that has just been restored). */
	public void markUnsaved(){
		saved = null;
	}

	/** @return	{@code true} if the model differs from what was last saved or loaded (also if an edit has not become a step yet). */
	public boolean isModified(){
		return (saved == null || !isUnchanged(saved, model));
	}

	public void checkpoint(){
		checkpoint(Origin.EDIT);
	}

	/** Records the changes made to the model since the last checkpoint, if any, as a single step. */
	public void checkpoint(final Origin origin){
		if(isUnchanged(baseline, model))
			return;

		final Snapshot previous = baseline;
		baseline = capture(model, previous);
		undoStack.push(new Step(previous, origin, Instant.now(), diff(previous, baseline)));
		while(undoStack.size() > MAX_DEPTH)
			undoStack.removeLast();
		redoStack.clear();
		fireChanged();
	}

	/**
	 * @param listener	told, on the thread that changed the model, every time the model changes (a step of the history is recorded, an
	 * 			undo or a redo is done, the model is replaced): it must be quick, and it must not change the model
	 */
	public void addChangeListener(final Runnable listener){
		changeListeners.add(listener);
	}

	private void fireChanged(){
		for(final Runnable listener : changeListeners)
			listener.run();
	}

	public boolean canUndo(){
		checkpoint();

		return !undoStack.isEmpty();
	}

	public boolean canRedo(){
		checkpoint();

		return !redoStack.isEmpty();
	}

	/** Returns the origin of the change that {@link #undo()} would revert, or {@code null} if there is none. */
	public Origin peekUndoOrigin(){
		checkpoint();

		return (undoStack.isEmpty()? null: undoStack.peek().origin());
	}

	/** @return	Whether the model has been changed. */
	public boolean undo(){
		checkpoint();
		if(undoStack.isEmpty())
			return false;

		final Step step = undoStack.pop();
		redoStack.push(new Step(baseline, step.origin(), step.time(), step.changes()));
		baseline = step.state();
		apply(baseline);
		fireChanged();
		return true;
	}

	/** @return	Whether the model has been changed. */
	public boolean redo(){
		checkpoint();
		if(redoStack.isEmpty())
			return false;

		final Step step = redoStack.pop();
		undoStack.push(new Step(baseline, step.origin(), step.time(), step.changes()));
		baseline = step.state();
		apply(baseline);
		fireChanged();
		return true;
	}

	/** @return	The recorded steps, the most recent first (undone ones included, flagged as such). */
	public List<Entry> recentChanges(){
		checkpoint();

		final List<Entry> entries = new ArrayList<>(undoStack.size() + redoStack.size());
		for(final Step step : redoStack)
			entries.add(new Entry(step.time(), step.origin(), step.changes(), true));
		for(final Step step : undoStack)
			entries.add(new Entry(step.time(), step.origin(), step.changes(), false));
		return entries;
	}


	/** Lists the records that differ between two snapshots. */
	private static List<Change> diff(final Snapshot before, final Snapshot after){
		final List<Change> changes = new ArrayList<>();
		if(!sameStructure(before.header(), after.header()))
			changes.add(new Change(null, HEADER_TAG, Kind.MODIFIED, before.header(), after.header()));

		final Set<String> currentIds = new HashSet<>();
		for(final FLEFRecord record : after.records()){
			final String id = record.getId();
			if(id == null)
				continue;

			currentIds.add(id);
			final FLEFRecord old = before.recordsById().get(id);
			if(old == null)
				changes.add(new Change(id, record.getTag(), Kind.ADDED, null, record));
			else if(!sameStructure(old, record))
				changes.add(new Change(id, record.getTag(), Kind.MODIFIED, old, record));
		}
		for(final FLEFRecord old : before.records())
			if(old.getId() != null && !currentIds.contains(old.getId()))
				changes.add(new Change(old.getId(), old.getTag(), Kind.REMOVED, old, null));
		return changes;
	}


	private void apply(final Snapshot snapshot){
		final FLEFRecord header = model.getHeader();
		header.clear();
		snapshot.header()
			.deepCopyTo(header);

		final Map<String, FLEFRecord> live = new HashMap<>();
		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records)
			if(record.getId() != null)
				live.put(record.getId(), record);

		final List<FLEFRecord> filteredRecords = new ArrayList<>(snapshot.records().size());
		for(final FLEFRecord record : snapshot.records()){
			final FLEFRecord current = live.get(record.getId());
			filteredRecords.add(current != null && sameStructure(current, record)? current: copy(record));
		}
		model.replaceRecords(filteredRecords);
	}

	private static Snapshot capture(final FLEFModel model, final Snapshot previous){
		final FLEFRecord header = copy(model.getHeader());

		List<FLEFRecord> records = model.getRecords();
		final List<FLEFRecord> filteredRecords = new ArrayList<>(records.size());
		final Map<String, FLEFRecord> recordsById = new HashMap<>();
		for(final FLEFRecord record : records){
			final FLEFRecord old = (previous != null && record.getId() != null? previous.recordsById().get(record.getId()): null);
			final FLEFRecord snapshot = (old != null && sameStructure(old, record)? old: copy(record));
			filteredRecords.add(snapshot);
			if(snapshot.getId() != null)
				recordsById.put(snapshot.getId(), snapshot);
		}
		return new Snapshot(header, filteredRecords, recordsById);
	}

	private static boolean isUnchanged(final Snapshot snapshot, final FLEFModel model){
		final List<FLEFRecord> records = model.getRecords();
		if(records.size() != snapshot.records().size() || !sameStructure(snapshot.header(), model.getHeader()))
			return false;

		for(int i = 0, size = records.size(); i < size; i ++)
			if(!sameStructure(snapshot.records().get(i), records.get(i)))
				return false;
		return true;
	}

	private static FLEFRecord copy(final FLEFRecord record){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		record.deepCopyTo(copy);
		return copy;
	}

	/** Strict comparison, children order included (unlike {@link FLEFRecord#equals(Object)}). */
	private static boolean sameStructure(final FLEFRecord a, final FLEFRecord b){
		if(a == b)
			return true;
		if(!Objects.equals(a.getTag(), b.getTag()) || !Objects.equals(a.getId(), b.getId())
				|| !Objects.equals(a.getValue(), b.getValue()))
			return false;

		final List<FLEFRecord> childrenA = a.getChildren();
		final List<FLEFRecord> childrenB = b.getChildren();
		final int size = childrenA.size();
		if(size != childrenB.size())
			return false;
		for(int i = 0; i < size; i ++)
			if(!sameStructure(childrenA.get(i), childrenB.get(i)))
				return false;
		return true;
	}

}
