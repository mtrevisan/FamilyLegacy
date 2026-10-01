/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.tree;

import io.github.mtrevisan.familylegacy.flef.ui.panels.ChildrenPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonPanel;


public class GenealogicalTree{

	public static final int LAST_GENERATION_CHILD = -1;
	public static final int NOT_FOUND = -2;

	private final GroupPanel[] tree;
	private final ChildrenPanel childrenPanel;


	public GenealogicalTree(final int generations, final ChildrenPanel childrenPanel){
		tree = new GroupPanel[1 << (generations - 1)];
		this.childrenPanel = childrenPanel;
	}


	public PersonPanel[] getChildren(){
		return childrenPanel.getChildBoxes();
	}

	public GroupPanel get(final int index){
		return (index >= 0 && index < tree.length? tree[index]: null);
	}


	public void addTo(final int index, final GroupPanel groupPanel){
		tree[index] = groupPanel;
	}

	public static int getParent(final int index){
		return (index > 0? ((index - 1) >> 1): NOT_FOUND);
	}

	public static int getLeftChild(final int index){
		return (index << 1) + 1;
	}

	public static int getRightChild(final int index){
		return (index << 1) + 2;
	}

	public int getIndexOf(final GroupPanel groupPanel){
		for(int i = 0, length = tree.length; i < length; i ++)
			if(tree[i] == groupPanel)
				return i;

		return NOT_FOUND;
	}

	public int getIndexOf(final PersonPanel personPanel){
		final PersonPanel[] childBoxes = childrenPanel.getChildBoxes();
		for(int i = 0, length = childBoxes.length; i < length; i ++)
			if(childBoxes[i] == personPanel)
				return LAST_GENERATION_CHILD;

		for(int i = 0, length = tree.length; i < length; i ++){
			final GroupPanel groupPanel = tree[i];
			if(groupPanel.getPartner1() == personPanel || groupPanel.getPartner2() == personPanel)
				return i;
		}

		return NOT_FOUND;
	}

}
