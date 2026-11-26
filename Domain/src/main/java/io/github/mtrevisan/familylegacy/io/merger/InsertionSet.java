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
package io.github.mtrevisan.familylegacy.io.merger;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;


/**
 * A set of strings that keeps the order of insertion, made for the many small sets of the index of a model (the parents of a person,
 * the places a place is part of, how a source is used): a {@link java.util.LinkedHashSet} costs a table and a node for each element, and
 * most of these sets have one or two.
 * <p>
 * The elements are kept in an array, and looked for one by one while they are few; from {@value #INDEX_THRESHOLD} elements a hash set
 * is built to look them up, so that a big set (the members of a large group) does not become quadratic. Elements are only added.
 */
final class InsertionSet extends AbstractSet<String>{

	/** From this number of elements a hash set is kept to look them up. */
	static final int INDEX_THRESHOLD = 8;

	private static final String[] NONE = new String[0];


	private String[] items = NONE;
	private int size;
	private Set<String> index;


	@Override
	public boolean add(final String element){
		if(contains(element))
			return false;

		if(size == items.length)
			items = Arrays.copyOf(items, size == 0? 2: size + (size >> 1) + 1);
		items[size ++] = element;
		if(index != null)
			index.add(element);
		else if(size > INDEX_THRESHOLD){
			index = new HashSet<>(size * 2);
			index.addAll(Arrays.asList(items).subList(0, size));
		}
		return true;
	}

	@Override
	public boolean contains(final Object element){
		if(index != null)
			return index.contains(element);

		for(int i = 0; i < size; i ++)
			if(Objects.equals(items[i], element))
				return true;
		return false;
	}

	@Override
	public int size(){
		return size;
	}

	@Override
	public boolean isEmpty(){
		return (size == 0);
	}

	@Override
	public Iterator<String> iterator(){
		return new Iterator<>(){

			private int position;


			@Override
			public boolean hasNext(){
				return (position < size);
			}

			@Override
			public String next(){
				if(position >= size)
					throw new NoSuchElementException();

				return items[position ++];
			}

		};
	}

}
