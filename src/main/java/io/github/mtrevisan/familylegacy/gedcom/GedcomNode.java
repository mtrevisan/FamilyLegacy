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
package io.github.mtrevisan.familylegacy.gedcom;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;


public abstract class GedcomNode{

	protected static final char NEW_LINE = '\n';


	protected int level;
	private String id;
	protected String tag;
	private String xref;
	protected String value;

	protected List<GedcomNode> children;
	private boolean custom;


	protected GedcomNode(){}

	public boolean isEmpty(){
		return (tag == null || id == null && value == null && (children == null || children.isEmpty()));
	}

	private void setLevel(final int level){
		if(level < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + level);

		this.level = level;
	}

	public GedcomNode withLevel(final String level){
		this.level = (level.length() == 1 && level.charAt(0) == 'n'? 0: Integer.parseInt(level));
		if(this.level < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + this.level);

		return this;
	}

	public int getLevel(){
		return level;
	}

	public String getID(){
		return id;
	}

	public GedcomNode withID(final String id){
		if(id != null && !id.isEmpty())
			this.id = id;
		return this;
	}

	public boolean isCustomTag(){
		return (tag.charAt(0) == '_');
	}

	public String getTag(){
		return tag;
	}

	public GedcomNode withTag(final String tag){
		if(tag != null && !tag.isEmpty())
			this.tag = tag.toUpperCase();
		return this;
	}

	public String getXRef(){
		return xref;
	}

	public GedcomNode withXRef(final String xref){
		if(xref != null && !xref.isEmpty())
			this.xref = xref;
		return this;
	}

	/**
	 * Returns the value associated with this node.
	 */
	public String getRawValue(){
		return value;
	}

	/**
	 * Returns the value associated with this node.
	 * <p>If the value is composed of multiple [CONT|CONTINUATION]|CONC tags, then the concatenation/continuation is returned.</p>
	 */
	public abstract String getValue();

	public String getValueOrDefault(final String defaultValue){
		final String v = getValue();
		return (v != null? v: defaultValue);
	}

	public abstract GedcomNode withValue(final String value);

	protected void addValue(final String childTag, final String subValue){
		if(value == null)
			value = subValue;
		else{
			final GedcomNode conNode = createNewNodeWithTag(childTag);
			conNode.value = subValue;
			addChildInner((children != null? children.size(): 0), conNode);
		}
	}

	public boolean hasChildren(){
		return (children != null && !children.isEmpty());
	}

	public List<GedcomNode> getChildren(){
		return (children != null? children: Collections.emptyList());
	}

	public GedcomNode addChildReference(final String tag, final String id){
		addChild(createNewNodeWithTag(tag)
			.withID(id));
		return this;
	}

	public GedcomNode addChildValue(final String tag, final String value){
		if(StringUtils.isNotEmpty(value))
			addChild(createNewNodeWithTag(tag)
				.withValue(value));
		return this;
	}

	protected abstract GedcomNode createNewNodeWithTag(final String tag);

	public GedcomNode addChild(final int index, final GedcomNode child){
		if(child.isEmpty())
			return this;

		return addChildInner(index, child);
	}

	public GedcomNode addClosingChild(final String tag){
		return addChildInner((children != null? children.size(): 0), createNewNodeWithTag(tag));
	}

	private GedcomNode addChildInner(final int index, final GedcomNode child){
		if(children == null)
			children = new ArrayList<>(1);

		if(child.level != level + 1){
			int currentLevel = level + 1;
			child.setLevel(currentLevel ++);
			final Deque<GedcomNode> stack = new ArrayDeque<>();
			stack.push(child);
			final Deque<GedcomNode> childrenStack = new ArrayDeque<>();
			while(!stack.isEmpty()){
				while(!stack.isEmpty()){
					final GedcomNode node = stack.pop();
					for(final GedcomNode c : node.getChildren()){
						c.setLevel(currentLevel);
						childrenStack.push(c);
					}
				}

				while(!childrenStack.isEmpty())
					stack.push(childrenStack.pop());

				currentLevel ++;
			}
		}
		children.add(index, child);
		return this;
	}

	public GedcomNode addChild(final GedcomNode child){
		return addChild((children != null? children.size(): 0), child);
	}

	GedcomNode addChildEvenIfEmpty(final GedcomNode child){
		return addChildInner((children != null? children.size(): 0), child);
	}

	/**
	 * Inserts a child after a given node.
	 * <p>WARNING: THE NODE AFTER MUST BE PRESENT!!!</p>.
	 * <p>WARNING: DOES NOT INSERTS AS FIRST ELEMENT!!!</p>.
	 *
	 * @param child	Node to add.
	 * @param nodeAfter	Node that should be after the inserted node.
	 */
	public void addChildBefore(final GedcomNode child, final GedcomNode nodeAfter){
		if(!child.isEmpty() && (child.value != null || child.hasChildren())){
			if(children == null){
				children = new ArrayList<>(1);
				addChild(child);
			}
			else if(nodeAfter != null){
				final int index = children.indexOf(nodeAfter);
				addChild(index, child);
			}
			else
				addChild(child);
		}
	}

	public GedcomNode addChildren(final Iterable<GedcomNode> children){
		if(children != null)
			for(final GedcomNode child : children)
				addChild(child);
		return this;
	}

	public boolean existChildrenWithID(final String xref){
		if(xref != null && children != null)
			for(final GedcomNode child : children)
				if(xref.equals(child.id))
					return true;
		return false;
	}

	public List<GedcomNode> getChildrenWithTag(final String... tags){
		final List<GedcomNode> taggedChildren;
		if(children != null){
			taggedChildren = new ArrayList<>(0);
			for(final GedcomNode child : children)
				if(ArrayUtils.contains(tags, child.tag))
					taggedChildren.add(child);
		}
		else
			taggedChildren = Collections.emptyList();
		return taggedChildren;
	}

	public boolean isCustom(){
		return custom;
	}

	public void setCustom(){
		custom = true;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final GedcomNode rhs = (GedcomNode)obj;
		final EqualsBuilder builder = new EqualsBuilder()
			.append(id, rhs.id)
			.append(tag, rhs.tag)
			.append(xref, rhs.xref)
			.append(value, rhs.value)
			.append(children, rhs.children);
		return builder.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(id)
			.append(tag)
			.append(xref)
			.append(value)
			.append(children)
			.hashCode();
	}

	@Override
	public String toString(){
		final StringBuilder builder = new StringBuilder();
		if(id != null)
			builder.append("id: ").append(id);
		if(tag != null)
			builder.append(builder.length() > 0? ", ": StringUtils.EMPTY).append("tag: ").append(tag);
		if(xref != null)
			builder.append(builder.length() > 0? ", ": StringUtils.EMPTY).append("ref: ").append(xref);
		if(value != null)
			builder.append(builder.length() > 0? ", ": StringUtils.EMPTY).append("value: ").append(value);
		if(children != null){
			builder.append(builder.length() > 0? ", ": StringUtils.EMPTY).append("children: [");
			final int size = children.size();
			for(int i = 0; i < size; i ++){
				builder.append('{')
					.append(children.get(i))
					.append('}');

				if(i < size - 1)
					builder.append(", ");
			}
			builder.append(']');
		}
		return builder.toString();
	}

}