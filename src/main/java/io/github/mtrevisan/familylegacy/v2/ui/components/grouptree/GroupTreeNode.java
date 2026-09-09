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
package io.github.mtrevisan.familylegacy.v2.ui.components.grouptree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Represents a single node within a non-biological group or entity tree.
 */
public final class GroupTreeNode{

	private final FLEFRecord record;

	private final List<GroupTreeNode> parents = new ArrayList<>();

	private final List<GroupTreeNode> members = new ArrayList<>();

	private final int generation;


	public GroupTreeNode(final FLEFRecord record, final int generation){
		this.record = record;

		this.generation = generation;
	}


	public FLEFRecord getRecord(){
		return record;
	}

	public String getRecordId(){
		return (record != null? record.getId(): null);
	}

	public int getGeneration(){
		return generation;
	}

	public List<GroupTreeNode> getParents(){
		return Collections.unmodifiableList(parents);
	}

	public void addParent(final GroupTreeNode parentNode){
		if(parentNode != null)
			parents.add(parentNode);
	}

	public List<GroupTreeNode> getMembers(){
		return Collections.unmodifiableList(members);
	}

	public void addMember(final GroupTreeNode memberNode){
		if(memberNode != null)
			members.add(memberNode);
	}

}
