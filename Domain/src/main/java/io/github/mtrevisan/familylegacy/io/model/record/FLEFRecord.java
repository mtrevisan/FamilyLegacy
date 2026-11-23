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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Represents a FLEF record (header or data record).
 * A record has an ID (optional), a type, a list of children, and can have a tag/value.
 */
public class FLEFRecord{

	private static final Logger LOGGER = LoggerFactory.getLogger(FLEFRecord.class);


	private static final Map<String, Integer> RESERVED_IDS = new ConcurrentHashMap<>();

	/** Who is told that a record has more than one child where one is expected (the application shows it to the user). */
	private static volatile Consumer<FLEFRecord> multipleChildrenReporter = record -> LOGGER.error("Record with more than one child: {}", record);


	private String tag;

	// For main records:
	private String id;
	// For children:
	private String value;

	//the children are an array of exactly their number, or null when there is none: most of the records (the leaves) have none, and the
	//others have few, so that a list (32 bytes, and a second object for what it holds) for each of the millions of records is too much
	private FLEFRecord[] kids;
	/** The record whose list of children this record was put in (the first one, if it is in many: see {@link TrackedList}), {@code null} for a top-level record. */
	FLEFRecord parent;
	/** The parent of a record of a model (see {@link TrackedList}): not a record, the mark of the top level. */
	static final FLEFRecord TOP_LEVEL = new FLEFRecord();


	/**
	 * Sets what to do when {@link #getTheOnlyChild(String)} finds more than one child (by default it is logged).
	 *
	 * @param reporter	The reporter, called with the record.
	 */
	public static void onMultipleChildren(final Consumer<FLEFRecord> reporter){
		multipleChildrenReporter = (reporter != null? reporter: record -> {});
	}

	public static FLEFRecord createMainRecord(final String id, final String type){
		final FLEFRecord record = createEmpty();
		record.setId(id);
		record.setTag(type);
		return record;
	}

	public static FLEFRecord createMainRecord(final String type, final String prefix, final RecordLookup model){
		final FLEFRecord record = createEmpty();
		record.setId(generateNewId(type, prefix, model));
		record.setTag(type);
		return record;
	}

	/**
	 * Generates and reserves a new unique ID.
	 * <p>
	 * The generated ID is guaranteed to be unique among both persisted records and IDs already reserved by open dialogs.
	 *
	 * @return	A new unique ID.
	 */
	private static String generateNewId(final String type, final String prefix, final RecordLookup model){
		Integer next = RESERVED_IDS.get(prefix);
		if(next == null)
			next = model.getRecordsByType(type).stream()
				.map(FLEFRecord::getId)
				.filter(Objects::nonNull)
				.filter(id -> id.startsWith(prefix))
				.mapToInt(id -> {
					try{
						return Integer.parseInt(id.substring(prefix.length()));
					}
					catch(final NumberFormatException ignored){
						return 0;
					}
				})
				.max()
				.orElse(0);
		next ++;

		RESERVED_IDS.put(prefix, next);

		return prefix + next;
	}

	/**
	 * Releases a previously reserved ID for a given prefix.
	 * This should be called when a record creation is canceled or discarded.
	 *
	 * @param prefix the ID prefix of the record type
	 */
	public static void releaseReservedId(final String prefix){
		if(prefix != null){
			try{
				RESERVED_IDS.compute(prefix,
					(k, current) -> (current == null || current <= 0? 0: current - 1));
			}
			catch(final UnsupportedOperationException ignored){}
		}
	}

	public static FLEFRecord createChildWithTagAndValue(final String tag, final String value){
		final FLEFRecord record = createChildWithTag(tag);
		record.setValue(value);
		return record;
	}

	public static FLEFRecord createChildWithTag(final String tag){
		final FLEFRecord record = createEmpty();
		record.setTag(tag);
		return record;
	}


	public static FLEFRecord createEmpty(){
		return new FLEFRecord();
	}


	private FLEFRecord(){}


	/** Tells that this record, and so every record that contains it, has changed. */
	void markModified(){
		final long stamp = Modifications.touch();
		//(the limit only stops a loop of parents, which is an error of whoever built it)
		int guard = 0;
		FLEFRecord root = this;
		while(root.parent != null && root.parent != TOP_LEVEL && guard ++ < 100_000)
			root = root.parent;
		Modifications.log(root, stamp);
	}

	public String getId(){
		return id;
	}

	public String getFormattedId(){
		return id;
	}

	public FLEFRecord setId(final String id){
		markModified();
		this.id = id;

		return this;
	}

	public String getTag(){
		return tag;
	}

	public FLEFRecord setTag(final String tag){
		markModified();
		this.tag = (tag != null? tag.toLowerCase(Locale.ROOT): null);

		return this;
	}

	public String getValue(){
		return value;
	}

	public FLEFRecord setValue(final String value){
		markModified();
		this.value = value;

		return this;
	}

	/**
	 * @return	The children of the record, a list that can be changed. It is a view of the record, made at every call: a loop that only
	 * 	reads is better with {@link #childCount()} and {@link #childAt(int)}.
	 */
	public List<FLEFRecord> getChildren(){
		return new Children(this);
	}

	/** @return	The number of children. */
	public int childCount(){
		return (kids != null? kids.length: 0);
	}

	/** @return	The child at the position. */
	public FLEFRecord childAt(final int index){
		if(kids == null || index < 0 || index >= kids.length)
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + childCount());

		return kids[index];
	}

	private List<FLEFRecord> childrenOrNone(){
		return (kids != null? Arrays.asList(kids): List.of());
	}

	/** The child is a child of this record from now on: its parent is this record, unless it has another one (a shared node). */
	private void attach(final FLEFRecord child){
		if(child.parent == null)
			child.parent = this;
		else if(child.parent != this)
			Modifications.markShared();
	}

	/** The child has been taken out of the children: if it is not there any more (it may have been put in twice), it has no parent. */
	private void detach(final FLEFRecord child){
		if(child.parent == this){
			for(int i = 0, size = childCount(); i < size; i ++)
				if(kids[i] == child)
					return;

			child.parent = null;
		}
	}

	private void insertChild(final int index, final FLEFRecord child){
		final int size = childCount();
		if(index < 0 || index > size)
			throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + size);

		markModified();
		attach(child);
		final FLEFRecord[] grown = new FLEFRecord[size + 1];
		if(kids != null){
			System.arraycopy(kids, 0, grown, 0, index);
			System.arraycopy(kids, index, grown, index + 1, size - index);
		}
		grown[index] = child;
		kids = grown;
	}

	private boolean appendChildren(final Collection<? extends FLEFRecord> added){
		if(added.isEmpty())
			return false;

		//(a copy, because the collection may be the children of this record)
		final FLEFRecord[] elements = added.toArray(new FLEFRecord[0]);
		markModified();
		final int size = childCount();
		final FLEFRecord[] grown = new FLEFRecord[size + elements.length];
		if(kids != null)
			System.arraycopy(kids, 0, grown, 0, size);
		for(int i = 0; i < elements.length; i ++){
			attach(elements[i]);
			grown[size + i] = elements[i];
		}
		kids = grown;
		return true;
	}

	private FLEFRecord setChildAt(final int index, final FLEFRecord child){
		final FLEFRecord old = childAt(index);
		markModified();
		attach(child);
		kids[index] = child;
		if(old != child)
			detach(old);
		return old;
	}

	private FLEFRecord removeChildAt(final int index){
		final FLEFRecord old = childAt(index);
		markModified();
		final int size = kids.length;
		if(size == 1)
			kids = null;
		else{
			final FLEFRecord[] shrunk = new FLEFRecord[size - 1];
			System.arraycopy(kids, 0, shrunk, 0, index);
			System.arraycopy(kids, index + 1, shrunk, index, size - index - 1);
			kids = shrunk;
		}
		detach(old);
		return old;
	}

	private boolean removeChildrenIf(final Predicate<? super FLEFRecord> filter){
		if(kids == null)
			return false;

		final List<FLEFRecord> kept = new ArrayList<>(kids.length);
		final List<FLEFRecord> removed = new ArrayList<>(0);
		for(final FLEFRecord child : kids)
			(filter.test(child)? removed: kept).add(child);
		if(removed.isEmpty())
			return false;

		markModified();
		kids = (kept.isEmpty()? null: kept.toArray(new FLEFRecord[0]));
		for(final FLEFRecord child : removed)
			detach(child);
		return true;
	}

	private void sortChildren(final Comparator<? super FLEFRecord> comparator){
		if(kids != null){
			markModified();
			Arrays.sort(kids, comparator);
		}
	}

	private void clearChildren(){
		if(kids != null){
			markModified();
			final FLEFRecord[] removed = kids;
			kids = null;
			for(final FLEFRecord child : removed)
				detach(child);
		}
	}

	/** The children of a record as a list: the reads see what the record has, and every change is made to the record, and told. */
	private static final class Children extends AbstractList<FLEFRecord> implements RandomAccess{

		private final FLEFRecord owner;


		private Children(final FLEFRecord owner){
			this.owner = owner;
		}


		@Override
		public int size(){
			return owner.childCount();
		}

		@Override
		public FLEFRecord get(final int index){
			return owner.childAt(index);
		}

		@Override
		public FLEFRecord set(final int index, final FLEFRecord element){
			return owner.setChildAt(index, element);
		}

		@Override
		public void add(final int index, final FLEFRecord element){
			modCount ++;
			owner.insertChild(index, element);
		}

		@Override
		public boolean addAll(final Collection<? extends FLEFRecord> elements){
			modCount ++;
			return owner.appendChildren(elements);
		}

		@Override
		public FLEFRecord remove(final int index){
			modCount ++;
			return owner.removeChildAt(index);
		}

		@Override
		public boolean removeIf(final Predicate<? super FLEFRecord> filter){
			modCount ++;
			return owner.removeChildrenIf(filter);
		}

		@Override
		public void sort(final Comparator<? super FLEFRecord> comparator){
			owner.sortChildren(comparator);
		}

		@Override
		public void clear(){
			modCount ++;
			owner.clearChildren();
		}

		@Override
		public Iterator<FLEFRecord> iterator(){
			return new Iterator<>(){

				private int cursor;
				private int last = -1;


				@Override
				public boolean hasNext(){
					return (cursor < owner.childCount());
				}

				@Override
				public FLEFRecord next(){
					if(cursor >= owner.childCount())
						throw new NoSuchElementException();

					last = cursor ++;
					return owner.kids[last];
				}

				@Override
				public void remove(){
					if(last < 0)
						throw new IllegalStateException();

					modCount ++;
					owner.removeChildAt(last);
					cursor = last;
					last = -1;
				}

			};
		}

	}

	public FLEFRecord getTheOnlyChild(){
		return getTheOnlyChild(null);
	}

	public FLEFRecord getTheOnlyChild(final String tag){
		final List<FLEFRecord> taggedChildren = (tag != null? FLEFRecordHelper.findChildren(this, tag): childrenOrNone());
		final int size = taggedChildren.size();
		if(size > 1){
			multipleChildrenReporter.accept(this);

			return null;
		}

		return (size == 1? taggedChildren.getFirst(): null);
	}

	/**
	 * Helper function to extract a referenced ID from a structure's xref tag.
	 *
	 * @param tag the tag to search for
	 * @param referencedType expected record type
	 * @return referenced ID or {@code null}
	 */
	public String extractReferencedId(final String tag, final String referencedType){
		final FLEFRecord referenceParent = FLEFRecordHelper.findChild(this, tag);
		if(referenceParent == null)
			return null;

		final FLEFRecord ref = referenceParent.getTheOnlyChild();
		if(ref == null || !Strings.CI.equals(ref.getTag(), referencedType))
			return null;

		return ref.getValue();
	}

	/**
	 * Extracts a list of referenced ids from the given {@code tag}
	 * children of a record. Each child may wrap its reference one level
	 * deep.
	 *
	 * @param tag the tag to search for
	 * @return the list of referenced IDs or {@code null}
	 */
	public List<String> extractReferenceIds(final String tag){
		final List<String> result = new ArrayList<>();
		final List<FLEFRecord> referenceParents = FLEFRecordHelper.findChildren(this, tag);
		for(final FLEFRecord referenceParent : referenceParents){
			final FLEFRecord reference = referenceParent.getTheOnlyChild();
			final String id = (reference != null && reference.getValue() != null
				? reference.getValue()
				: referenceParent.getValue());
			if(id != null)
				result.add(id);
		}
		return result;
	}

	public long countChildrenWithTag(final String tag){
		long count = 0;
		for(int i = 0, size = childCount(); i < size; i ++)
			if(tag != null && tag.equalsIgnoreCase(kids[i].getTag()))
				count ++;
		return count;
	}

	public FLEFRecord addChild(final FLEFRecord child){
		if(child == null || child.isEmpty())
			return this;

		return forceAddChild(child);
	}

	public FLEFRecord forceAddChild(final FLEFRecord child){
		if(child == null)
			return this;

		insertChild(childCount(), child);

		return this;
	}

	public FLEFRecord addChildren(final List<FLEFRecord> children){
		appendChildren(children);

		return this;
	}

	public FLEFRecord addChildWithTag(final String tag, final FLEFRecord child){
		if(child == null || child.isEmpty())
			return this;

		child.setTag(tag);

		return addChild(child);
	}

	public FLEFRecord addChildrenWithTag(final String tag, final List<FLEFRecord> children){
		for(final FLEFRecord child : children)
			if(StringUtils.isEmpty(child.getTag()))
				child.setTag(tag);

		return addChildren(children);
	}

	/**
	 * Removes a specific child record from this record's children list.
	 *
	 * @param child	the child record to remove
	 * @return {@code true} if the child was found and removed, {@code false} otherwise
	 */
	public boolean removeChild(final FLEFRecord child){
		return removeChildrenIf(c -> c == child);
	}

	/**
	 * Removes all children with the given tag.
	 *
	 * @param tag	the tag of children to remove
	 * @return the list of removed children (empty if none were found)
	 */
	private List<FLEFRecord> removeChildren(final String tag){
		final List<FLEFRecord> removed = new ArrayList<>(0);
		removeChildrenIf(child -> {
			if(tag != null && tag.equalsIgnoreCase(child.getTag())){
				removed.add(child);
				return true;
			}
			return false;
		});
		return removed;
	}

	public boolean hasChildren(){
		return (kids != null);
	}

	public void clear(){
		markModified();
		value = null;
		clearChildren();
	}

	public boolean hasData(){
		return (StringUtils.isNotEmpty(id) || StringUtils.isNotEmpty(value) || hasChildren());
	}

	public boolean isEmpty(){
		return (StringUtils.isEmpty(tag) && StringUtils.isEmpty(id) && StringUtils.isEmpty(value) && !hasChildren());
	}


	/**
	 * Creates a deep copy of this record and its entire subtree.
	 * The copy is independent of the original; modifications to the copy
	 * do not affect the original and vice versa.
	 * <p>
	 * This implementation is iterative and uses a stack to avoid recursion,
	 * preventing stack overflow on deeply nested trees.
	 */
	public void deepCopyTo(final FLEFRecord destination){
		// Map original nodes to their copies to handle shared references
		final Map<FLEFRecord, FLEFRecord> map = new IdentityHashMap<>();
		final Deque<FLEFRecord> stack = new ArrayDeque<>();

		//the destination changes, and the nodes below it are made here and nobody has seen them: one count is enough
		destination.markModified();
		// Create copy of the root
		destination.tag = this.tag;
		destination.id = this.id;
		destination.value = this.value;
		map.put(this, destination);
		stack.push(this);

		// Process all nodes iteratively
		while(!stack.isEmpty()){
			final FLEFRecord original = stack.pop();
			final FLEFRecord copy = map.get(original);
if(original.kids == null)
	continue;

//(the destination may already have children: the copied ones come after them)
final int before = copy.childCount();
final FLEFRecord[] copied = new FLEFRecord[before + original.kids.length];
if(before > 0)
	System.arraycopy(copy.kids, 0, copied, 0, before);
copy.kids = copied;

// Copy all children
for(int i = 0, size = original.kids.length; i < size; i ++){
	final FLEFRecord child = original.kids[i];
	FLEFRecord childCopy = map.get(child);
	if(childCopy == null){
		// Not copied yet: create a new copy
		childCopy = FLEFRecord.createEmpty();
		childCopy.tag = child.tag;
		childCopy.id = child.id;
		childCopy.value = child.value;
		map.put(child, childCopy);
		stack.push(child);
	}
	// Add the (possibly existing) copy to this node's children
	copied[before + i] = childCopy;
	copy.attach(childCopy);
			}
		}
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final FLEFRecord other = (FLEFRecord)obj;
		return (Objects.equals(tag, other.tag)
			&& Objects.equals(id, other.id)
			&& Objects.equals(value, other.value)
			&& areChildrenEqual(childrenOrNone(), other.childrenOrNone()));
	}

	private boolean areChildrenEqual(final List<FLEFRecord> a, final List<FLEFRecord> b){
		if(a == b)
			return true;
		final int size = a.size();
		if(size != b.size())
			return false;

		//the same children in the same order (e.g. a copy) are equal without building anything
		boolean sameOrder = true;
		for(int i = 0; sameOrder && i < size; i ++)
			sameOrder = a.get(i).equals(b.get(i));
		if(sameOrder)
			return true;

		final Map<FLEFRecord, Integer> counts = new HashMap<>();
		for(final FLEFRecord child : a)
			counts.merge(child, 1, Integer::sum);
		for(final FLEFRecord child : b){
			final int remaining = counts.getOrDefault(child, 0);
			if(remaining == 0)
				return false;
			if(remaining == 1)
				counts.remove(child);
			else
				counts.put(child, remaining - 1);
		}
		return counts.isEmpty();
	}

	@Override
	public int hashCode(){
		//the same value of Objects.hash(tag, id, value), without the array
		int hash = 31 * (31 * (31 + Objects.hashCode(tag)) + Objects.hashCode(id)) + Objects.hashCode(value);
		hash = 31 * hash + childrenHash(childrenOrNone());
		return hash;
	}

	private int childrenHash(final List<FLEFRecord> children){
		int result = 0;
		for(final FLEFRecord child : children)
			result ^= child.hashCode();
		return result;
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder(tag != null? tag: "--");
		if(id != null)
			sb.append('[').append(id).append(']');
		if(value != null)
			sb.append(" = ").append(value);
		if(hasChildren())
			sb.append(StringUtils.SPACE)
				.append('(')
				.append(childCount())
				.append(" children)");
		return sb.toString();
	}

}
