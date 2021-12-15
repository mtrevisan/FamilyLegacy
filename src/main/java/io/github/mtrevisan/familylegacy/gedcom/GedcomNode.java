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

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


public abstract class GedcomNode{

	static final char NEW_LINE = '\n';

	private static final Pattern PATTERN_ID = RegexHelper.pattern("[^0-9]");


	protected int level;
	private String id;
	protected String tag;
	private String xref;
	protected String value;

	protected List<GedcomNode> children;
	private boolean custom;


	protected GedcomNode(){}

	public boolean isEmpty(){
		return (tag == null || id == null && xref == null && value == null && (children == null || children.isEmpty()));
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
		return (tag != null && tag.charAt(0) == '_');
	}

	public String getTag(){
		return tag;
	}

	public GedcomNode withTag(final String tag){
		if(tag != null && !tag.isEmpty())
			this.tag = tag.toUpperCase(Locale.ROOT);
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

	public GedcomNode clearXRef(){
		xref = null;
		return this;
	}


	/**
	 * Returns the value associated with this node.
	 */
	public String getRawValue(){
		return StringUtils.replace(value, "@", "@@");
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

	public GedcomNode addChildReference(final String tag, final String xref){
		addChild(createNewNodeWithTag(tag)
			.withXRef(xref));
		return this;
	}

	public GedcomNode addChildValue(final String tag, final String value){
		if(StringUtils.isNotBlank(value))
			addChild(createNewNodeWithTag(tag)
				.withValue(value));
		return this;
	}

	/** Replaces first occurrence of the given tag with the given value. */
	public GedcomNode replaceChildValue(final String tag, final String value){
		if(StringUtils.isBlank(value))
			removeChildrenWithTag(tag);
		else if(children != null){
			boolean found = false;
			for(final GedcomNode child : children)
				if(child.tag.equals(tag)){
					child.withValue(value);
					found = true;
					break;
				}

			if(!found)
				//create child
				addChild(createNewNodeWithTag(tag)
					.withValue(value));
		}
		else
			addChildValue(tag, value);
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
		if(!getChildrenWithTag(tag).contains(child)){
			if(children == null)
				children = new ArrayList<>(1);

			if(child.level != level + 1){
				int currentLevel = level + 1;
				child.setLevel(currentLevel ++);
				final Deque<GedcomNode> stack = new LinkedList<>();
				stack.push(child);
				final Deque<GedcomNode> childrenStack = new LinkedList<>();
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
		}
		return this;
	}

	public GedcomNode addChild(final GedcomNode child){
		final int index = (children != null? children.size(): 0);
		return addChild(index, child);
	}

	public GedcomNode forceAddChild(final GedcomNode child){
		final int index = (children != null? children.size(): 0);
		return addChildInner(index, child);
	}

	public GedcomNode addChildren(final Iterable<GedcomNode> children){
		if(children != null)
			for(final GedcomNode child : children)
				addChild(child);
		return this;
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

	public GedcomNode removeChildrenWithTag(final String... tags){
		if(children != null){
			final Iterator<GedcomNode> itr = children.iterator();
			while(itr.hasNext())
				if(ArrayUtils.contains(tags, itr.next().tag))
					itr.remove();
		}
		return this;
	}


	public static int compareID(final String id1, final String id2){
		return Integer.compare(extractNumberFromID(id1), extractNumberFromID(id2));
	}

	private static int extractNumberFromID(final String id){
		return Integer.parseInt(PATTERN_ID.matcher(id).replaceAll(StringUtils.EMPTY));
	}


	public boolean isCustom(){
		return custom;
	}

	public void setCustom(){
		custom = true;
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final GedcomNode rhs = (GedcomNode)obj;
		return (/*id.equals(rhs.id)
			&&*/ tag.equals(rhs.tag)
			&& xref.equals(rhs.xref)
			&& value.equals(rhs.value)
			&& children.equals(rhs.children));
	}

	@Override
	public int hashCode(){
		int result = 0/*id.hashCode()*/;
		if(tag != null)
			result = 31 * result + tag.hashCode();
		if(xref != null)
			result = 31 * result + xref.hashCode();
		if(value != null)
			result = 31 * result + value.hashCode();
		if(children != null)
			result = 31 * result + children.hashCode();
		return result;
	}

	@Override
	public String toString(){
		final StringBuilder builder = new StringBuilder();
		if(id != null)
			builder.append("id: ").append(id);
		if(tag != null)
			builder.append(!builder.isEmpty()? ", ": StringUtils.EMPTY).append("tag: ").append(tag);
		if(xref != null)
			builder.append(!builder.isEmpty()? ", ": StringUtils.EMPTY).append("ref: ").append(xref);
		if(value != null)
			builder.append(!builder.isEmpty()? ", ": StringUtils.EMPTY).append("value: ").append(value);
		if(children != null){
			builder.append(!builder.isEmpty()? ", ": StringUtils.EMPTY).append("children: [");
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