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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;


/**
 * The list of the records of a model. It tells {@link Modifications} every time it is changed, whoever changes it (the model gives its list
 * away, and it is edited in place from many places): this is how a change of it is noticed without comparing it with what it was. Each record
 * put into it learns that it is a record of a model (its parent is {@link FLEFRecord#TOP_LEVEL}); a record that has another parent is a
 * shared node, which {@link Modifications#sharedNodesSeen()} tells.
 * <p>It is a plain {@link ArrayList} in everything else, and not thread safe.</p>
 *
 * @param <E>	The type of the elements.
 */
public final class TrackedList<E> extends ArrayList<E>{

	/** The value of the counter of the changes at the last change of the list. */
	private long lastChange;


	public TrackedList(final int capacity){
		super(capacity);
	}


	private void changed(){
		lastChange = Modifications.touch();
	}

	/** @return	The value of {@link Modifications#current()} at the last change of this list (not of its elements), 0 if it never changed. */
	public long lastChange(){
		return lastChange;
	}

	/** The element is in this list from now on: it is a record of a model. */
	private void attach(final Object element){
		if(element instanceof final FLEFRecord record){
			if(record.parent != null && record.parent != FLEFRecord.TOP_LEVEL)
				Modifications.markShared();
			record.parent = FLEFRecord.TOP_LEVEL;
		}
	}

	/** The element has been taken out of this list: if it is not in it any more (it may have been put in twice), it is not a record of a model. */
	private void detach(final Object element){
		if(element instanceof final FLEFRecord record && record.parent == FLEFRecord.TOP_LEVEL){
			for(int i = 0, size = size(); i < size; i ++)
				if(get(i) == element)
					return;

			record.parent = null;
		}
	}

	@Override
	public boolean add(final E element){
		changed();
		attach(element);
		return super.add(element);
	}

	@Override
	public void add(final int index, final E element){
		changed();
		attach(element);
		super.add(index, element);
	}

	@Override
	public boolean addAll(final Collection<? extends E> elements){
		changed();
		for(final E element : elements)
			attach(element);
		return super.addAll(elements);
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends E> elements){
		changed();
		for(final E element : elements)
			attach(element);
		return super.addAll(index, elements);
	}

	@Override
	public E set(final int index, final E element){
		changed();
		attach(element);
		final E old = super.set(index, element);
		if(old != element)
			detach(old);
		return old;
	}

	@Override
	public E remove(final int index){
		changed();
		final E old = super.remove(index);
		detach(old);
		return old;
	}

	@Override
	public boolean remove(final Object element){
		changed();
		final boolean removed = super.remove(element);
		if(removed)
			detach(element);
		return removed;
	}

	//(these four of the List interface are not made of the methods above in ArrayList)
	@Override
	public E removeFirst(){
		changed();
		final E old = super.removeFirst();
		detach(old);
		return old;
	}

	@Override
	public E removeLast(){
		changed();
		final E old = super.removeLast();
		detach(old);
		return old;
	}

	@Override
	public void addFirst(final E element){
		changed();
		attach(element);
		super.addFirst(element);
	}

	@Override
	public void addLast(final E element){
		changed();
		attach(element);
		super.addLast(element);
	}

	@Override
	public boolean removeAll(final Collection<?> elements){
		changed();
		return super.removeAll(elements);
	}

	@Override
	public boolean retainAll(final Collection<?> elements){
		changed();
		return super.retainAll(elements);
	}

	@Override
	public boolean removeIf(final Predicate<? super E> filter){
		changed();
		final List<E> removed = new ArrayList<>(0);
		final boolean any = super.removeIf(element -> {
			final boolean remove = filter.test(element);
			if(remove)
				removed.add(element);
			return remove;
		});
		for(final E element : removed)
			detach(element);
		return any;
	}

	@Override
	public void replaceAll(final UnaryOperator<E> operator){
		changed();
		super.replaceAll(operator);
		for(final E element : this)
			attach(element);
	}

	@Override
	public void sort(final Comparator<? super E> comparator){
		changed();
		super.sort(comparator);
	}

	@Override
	public void clear(){
		changed();
		final Object[] removed = toArray();
		super.clear();
		for(final Object element : removed)
			detach(element);
	}

	@Override
	protected void removeRange(final int from, final int to){
		changed();
		super.removeRange(from, to);
	}

	@Override
	public Iterator<E> iterator(){
		return new Itr();
	}

	/** Like the one of {@link ArrayList}, light enough to be used in every loop, and its removals are told. */
	private final class Itr implements Iterator<E>{

		private int cursor;
		private int last = -1;
		private int expectedModCount = modCount;


		@Override
		public boolean hasNext(){
			return (cursor != size());
		}

		@Override
		public E next(){
			if(modCount != expectedModCount)
				throw new ConcurrentModificationException();
			final int index = cursor;
			if(index >= size())
				throw new NoSuchElementException();

			cursor = index + 1;
			last = index;
			return get(index);
		}

		@Override
		public void remove(){
			if(last < 0)
				throw new IllegalStateException();
			if(modCount != expectedModCount)
				throw new ConcurrentModificationException();

			TrackedList.this.remove(last);
			cursor = last;
			last = -1;
			expectedModCount = modCount;
		}

	}

	@Override
	public ListIterator<E> listIterator(){
		return listIterator(0);
	}

	@Override
	public ListIterator<E> listIterator(final int index){
		final ListIterator<E> iterator = super.listIterator(index);
		return new ListIterator<>(){

			@Override
			public boolean hasNext(){
				return iterator.hasNext();
			}

			@Override
			public E next(){
				return iterator.next();
			}

			@Override
			public boolean hasPrevious(){
				return iterator.hasPrevious();
			}

			@Override
			public E previous(){
				return iterator.previous();
			}

			@Override
			public int nextIndex(){
				return iterator.nextIndex();
			}

			@Override
			public int previousIndex(){
				return iterator.previousIndex();
			}

			@Override
			public void remove(){
				changed();
				iterator.remove();
			}

			@Override
			public void set(final E element){
				changed();
				attach(element);
				iterator.set(element);
			}

			@Override
			public void add(final E element){
				changed();
				attach(element);
				iterator.add(element);
			}

		};
	}

	@Override
	public List<E> subList(final int from, final int to){
		final List<E> view = super.subList(from, to);
		return new AbstractList<>(){

			@Override
			public E get(final int index){
				return view.get(index);
			}

			@Override
			public int size(){
				return view.size();
			}

			@Override
			public E set(final int index, final E element){
				changed();
				attach(element);
				return view.set(index, element);
			}

			@Override
			public void add(final int index, final E element){
				changed();
				attach(element);
				view.add(index, element);
			}

			@Override
			public E remove(final int index){
				changed();
				final E old = view.remove(index);
				detach(old);
				return old;
			}

		};
	}

}
