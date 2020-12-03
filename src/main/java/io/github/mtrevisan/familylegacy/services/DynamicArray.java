/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.services;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;


public final class DynamicArray<T>{

	private static final float DEFAULT_GROWTH_RATE = 1.2f;


	public T[] data;
	public int limit;

	private final float growthRate;


	public static <T> DynamicArray<T> wrap(final T[] array){
		return new DynamicArray<>(array);
	}

	public static <T> DynamicArray<T> create(final Class<T> type){
		return new DynamicArray<>(type, 0, DEFAULT_GROWTH_RATE);
	}

	public static <T> DynamicArray<T> create(final Class<T> type, final int capacity){
		return new DynamicArray<>(type, capacity, DEFAULT_GROWTH_RATE);
	}

	public static <T> DynamicArray<T> create(final Class<T> type, final int capacity, final float growthRate){
		return new DynamicArray<>(type, capacity, growthRate);
	}

	private DynamicArray(final T[] array){
		data = array;
		limit = array.length;

		growthRate = DEFAULT_GROWTH_RATE;
	}

	@SuppressWarnings("unchecked")
	private DynamicArray(final Class<T> type, final int capacity, final float growthRate){
		data = (T[])Array.newInstance(type, capacity);

		this.growthRate = growthRate;
	}

	/**
	 * Appends the specified element to the end of this array.
	 *
	 * @param elem	Element to be appended to the internal array.
	 */
	public void add(final T elem){
		grow(1);

		data[limit ++] = elem;
	}

	/**
	 * Inserts the specified element at the specified position in this list.
	 * <p>Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).</p>
	 *
	 * @param index	Index at which the specified element is to be inserted.
	 * @param elem	Element to be appended to the internal array.
	 * @throws IndexOutOfBoundsException	If the index is out of range ({@code index < 0 || index > size()})
	 */
	public void add(final int index, final T elem){
		if(index < 0 || index > limit)
			throw new IndexOutOfBoundsException("Index: " + index + ", size: " + limit);

		if(limit == data.length){
			final T[] copy = newInstance(limit + 1);
			System.arraycopy(data, 0, copy, 0, index);
			System.arraycopy(data, index, copy, index + 1, limit - index);

			data = copy;
		}
		else
			System.arraycopy(data, index, data, index + 1, limit - index);

		data[index] = elem;
		limit ++;
	}

	/**
	 * Appends the specified element to the end of this array if not {@code null}.
	 *
	 * @param elem	Element to be appended to the internal array.
	 */
	public void addIfNotNull(final T elem){
		if(elem != null)
			add(elem);
	}

	/**
	 * Appends all of the elements in the specified collection to the end of this array.
	 *
	 * @param array	Collection containing elements to be added to this array.
	 */
	public void addAll(final DynamicArray<? extends T> array){
		addAll(array.data, array.limit);
	}

	/**
	 * Appends all of the elements in the specified collection to the end of this array.
	 *
	 * @param array	Collection containing elements to be added to this array.
	 * @param length	Length of the array.
	 */
	public void addAll(final T[] array, final int length){
		grow(length);

		System.arraycopy(array, 0, data, limit, length);
		limit += length;
	}

	/**
	 * Inserts all of the elements in the specified collection into this array at the specified position.
	 * <p>Shifts the element currently at that position (if any) and any subsequent elements to the right
	 * (increases their indices).</p>
	 *
	 * @param index	Index at which to insert the first element from the specified collection.
	 * @param array	Collection containing elements to be added to this array.
	 */
	public void addAll(final int index, final DynamicArray<T> array){
		final int addLength = array.limit;
		if(addLength != 0){
			grow(addLength);

			if(index < limit)
				System.arraycopy(data, index, data, index + addLength, limit - index);
			System.arraycopy(array.data, 0, data, index, addLength);
			limit += addLength;
		}
	}

	/**
	 * Returns whether this array contains the specified element using {@code Objects.equals()}.
	 *
	 * @param elem	Element whose presence in this array is to be tested.
	 * @return	Whether this array contains the specified element.
	 */
	public boolean contains(final T elem){
		return (indexOf(elem, 0) >= 0);
	}

	/**
	 * @param elem	Element whose presence in this array is to be tested.
	 * @return The index of the first occurrence of the specified element in this array, or {@code -1} if this array does not contain
	 * 	 * the element.
	 */
	public int indexOf(final T elem){
		return indexOf(elem, 0);
	}

	/**
	 * @param elem	Element whose presence in this array is to be tested.
	 * @param startIndex	Starting index from which to search.
	 * @return The index of the first occurrence of the specified element in this array, or {@code -1} if this array does not contain
	 * 	 * the element.
	 */
	public int indexOf(final T elem, final int startIndex){
		if(elem == null){
			for(int i = startIndex; i < limit; i ++)
				if(data[i] == null)
					return i;
			}
		else{
			for(int i = startIndex; i < limit; i ++)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	public synchronized int lastIndexOf(final T elem, final int startIndex){
		if(elem == null){
			for(int i = startIndex - 1; i >= 0; i --)
				if(data[i] == null)
					return i;
		}
		else{
			for(int i = startIndex - 1; i >= 0; i --)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	public void remove(final T elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			data[-- limit] = null;
		}
	}

	/**
	 * Removes the element at the specified position in this array.
	 * <p>Shifts any subsequent elements to the left (subtracts one from their indices).</p>
	 *
	 * @param index	The index of the element to be removed.
	 * @return	The element that was removed from the array.
	 * @throws IndexOutOfBoundsException	If the index is out of range ({@code index < 0 || index > size()})
	 */
	public T remove(final int index){
		if(index < 0 || index > limit)
			throw new IndexOutOfBoundsException("Index: " + index + ", size: " + limit);

		final T oldValue = data[index];

		if(limit - 1 > index)
			System.arraycopy(data, index + 1, data, index, limit - index - 1);
		data[-- limit] = null;

		return oldValue;
	}

	/**
	 * Removes the element at the specified position in this array.
	 * <p>Shifts any subsequent elements to the left (subtracts one from their indices).</p>
	 *
	 * @param indices	The indices of the element to be removed.
	 * @throws IndexOutOfBoundsException	If the index is out of range ({@code index < 0 || index > size()})
	 */
	public void removeAll(final int... indices){
		if(indices != null && indices.length > 0){
			for(int index : indices)
				if(index < 0 || index > limit)
					throw new IndexOutOfBoundsException("Index: " + index + ", size: " + limit);

			Arrays.sort(indices);

			//create result array
			final T[] result = newInstance(limit - indices.length);
			//index just after last copy
			int end = limit;
			//number of entries so far not copied
			int dest = limit - indices.length;
			for(int i = indices.length - 1; i >= 0; i --){
				final int index = indices[i];
				//same as `cp > 0`
				if(end - index > 1){
					final int cp = end - index - 1;
					dest -= cp;
					System.arraycopy(data, index + 1, result, dest, cp);
				}
				end = index;
			}
			if(end > 0)
				System.arraycopy(data, 0, result, 0, end);

			data = result;
			limit -= indices.length;
		}
	}

	/**
	 * Increases the capacity of the internal array, if necessary, to ensure that it can hold at least the number of elements
	 * specified by the minimum capacity argument.
	 *
	 * @param newCapacity	The desired minimum capacity.
	 */
	public void ensureCapacity(final int newCapacity){
		grow(newCapacity - limit);
	}

	public void filter(final Predicate<? super T> filter){
		reset();
		for(final T elem : data)
			if(filter.test(elem))
				data[limit ++] = elem;
	}

	public void join(final Function<? super T, String> reducer, final StringJoiner joiner){
		for(int i = 0; i < limit; i ++)
			joiner.add(reducer.apply(data[i]));
	}

	private void grow(final int size){
		final int delta = limit - data.length + size;
		if(delta > 0){
			final int newLength = data.length + (int)Math.ceil(delta * growthRate);
			final T[] copy = newInstance(newLength);
			System.arraycopy(data, 0, copy, 0, Math.min(data.length, newLength));
			data = copy;
		}
	}

	/**
	 * Returns whether this array contains no elements.
	 *
	 * @return	Whether this array contains no elements.
	 */
	public boolean isEmpty(){
		return (limit == 0);
	}

	/** Removes all of the elements from this array. */
	private void reset(){
		limit = 0;
	}

	/**
	 * Removes all of the elements from this array.
	 * <p>The array will be emptied after this call returns.</p>
	 */
	public void clear(){
		data = null;
		limit = -1;
	}

	/**
	 * NOTE: this method should be called the least possible because it is inefficient.
	 *
	 * @return	A copy of the array.
	 */
	public T[] extractCopyOrNull(){
		if(isEmpty())
			return null;

		final Class<?> type = getDataType();
		@SuppressWarnings("unchecked")
		final T[] copy = (T[])Array.newInstance(type, limit);
		System.arraycopy(data, 0, copy, 0, limit);
		return copy;
	}

	@SuppressWarnings("unchecked")
	private T[] newInstance(final int size){
		final Class<?> type = getDataType();
		return (T[])Array.newInstance(type, size);
	}

	private Class<?> getDataType(){
		return data.getClass().getComponentType();
	}

	@Override
	public String toString(){
		return Arrays.toString(data);
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final DynamicArray<?> rhs = (DynamicArray<?>)obj;
		return (limit == rhs.limit && Arrays.equals(data, rhs.data));
	}

	@Override
	public int hashCode(){
		return (Integer.hashCode(limit) ^ Arrays.hashCode(data));
	}

}
