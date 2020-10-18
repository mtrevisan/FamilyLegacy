/**
 *    Copyright 2015 Thomas Naeff (github.com/thnaeff)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.thn.datatree.core;

import java.util.Collection;
import java.util.Comparator;

import com.google.common.collect.TreeMultimap;

import ch.thn.datatree.DataTreeUtil;


/**
 * 
 * 
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 * @param <K>
 * @param <V>
 * @param <N>
 */
public abstract class GenericKeySetTreeNode<K, V, N extends GenericKeySetTreeNode<K, V, N>> 
	extends GenericMapTreeNode<K, V, N, Collection<N>> 
	implements SetTreeNodeInterface<V, N> {
	
	private Comparator<? super K> keyComparator = null; 
	private Comparator<? super N> valueComparator = null;
	
	/**
	 * Creates a new key set tree node which sorts its keys and nodes by comparing the toString 
	 * result of two nodes.
	 * 
	 * @param key
	 * @param value
	 */
	public GenericKeySetTreeNode(K key, V value) {
		this(null, null, key, value);
	}
	
	/**
	 * Creates a new key set tree node which uses the given comparator to sort the 
	 * key and nodes. The comparators are also used to check for equality, which means 
	 * that since this is a set, a new entry is not added if an equal one is already 
	 * present. An entry is equal when both the key and the value comparison is equal.
	 * 
	 * @param keyComparator The comparator to use, or <code>null</code> to use an 
	 * internally created comparator which compares the toString result of two node keys.
	 * @param valueComparator The comparator to use, or <code>null</code> to use an 
	 * internally created comparator which compares the toString result of two node values.
	 * @param key
	 * @param value
	 */
	public GenericKeySetTreeNode(Comparator<? super K> keyComparator, 
			Comparator<? super N> valueComparator, K key, V value) {
		super(null, key, value);
		
		if (keyComparator == null) {
			keyComparator = DataTreeUtil.<K>getDefaultKeyComparator();
		}
		
		if (valueComparator == null) {
			valueComparator = DataTreeUtil.<N>getDefaultValueComparator();
		}
		
		this.keyComparator = keyComparator;
		this.valueComparator = valueComparator;
		
		//It somehow does not work to use TreeMultimap.create() in the constructor
		//-> workaround
		TreeMultimap<K, N> childrenMap = TreeMultimap.create(keyComparator, valueComparator);
		internalSetChildrenMap(childrenMap);
		
		//Sets the internal children collection after creating the map, since 
		//the values of the map are the children
		internalSetChildrenCollection(internalGetMap().values());
	}
	
	/**
	 * Returns the comparator which is used for the sorting of the keys
	 * 
	 * @return
	 */
	public Comparator<? super K> getKeyComparator() {
		return keyComparator;
	}
	
	/**
	 * Returns the comparator which is used for the sorting of the nodes
	 * 
	 * @return
	 */
	public Comparator<? super N> getValueComparator() {
		return valueComparator;
	}

}
