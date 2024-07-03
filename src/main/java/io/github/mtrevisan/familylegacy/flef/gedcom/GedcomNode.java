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
package io.github.mtrevisan.familylegacy.flef.gedcom;

import io.github.mtrevisan.familylegacy.flef.helpers.StringHelper;
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
import java.util.Objects;
import java.util.regex.Pattern;


public abstract class GedcomNode implements Cloneable{

	static final String NEW_LINE = "\\n";

	private static final Pattern PATTERN_ID = RegexHelper.pattern("[^0-9]");


	protected int level;
	private String id;
	protected String tag;
	private String xref;
	protected String value;

	protected List<GedcomNode> children;
	private boolean custom;


	protected GedcomNode(){}

	public final boolean isEmpty(){
		return (tag == null || id == null && xref == null && value == null && (children == null || children.isEmpty()));
	}


	private void setLevel(final int level){
		if(level < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + level);

		this.level = level;
	}

	public final GedcomNode withLevel(final int level){
		if(level < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + level);

		this.level = level;

		return this;
	}

	public final GedcomNode withLevel(final String level){
		final int newLevel = (level.length() == 1 && level.charAt(0) == 'n'? 0: Integer.parseInt(level));
		if(newLevel < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + newLevel);

		this.level = newLevel;

		return this;
	}


	public final int getLevel(){
		return level;
	}

	public final String getID(){
		return id;
	}

	public final GedcomNode withID(final String id){
		if(id != null && !id.isEmpty())
			this.id = id;
		return this;
	}


	public final boolean isCustomTag(){
		return (tag != null && tag.charAt(0) == '_');
	}

	public final String getTag(){
		return tag;
	}

	public final GedcomNode withTag(final String tag){
		if(tag != null && !tag.isEmpty())
			this.tag = tag.toUpperCase(Locale.ROOT);
		return this;
	}


	public final String getXRef(){
		return xref;
	}

	public final GedcomNode withXRef(final String xref){
		if(xref != null && !xref.isEmpty())
			this.xref = xref;
		return this;
	}

	public final GedcomNode clearXRef(){
		xref = null;
		return this;
	}


	/**
	 * Returns the value associated with this node.
	 */
	public final String getRawValue(){
		return StringUtils.replace(value, "@", "@@");
	}

	/**
	 * Returns the value associated with this node.
	 * <p>If the value is composed of multiple [CONT|CONTINUATION]|CONC tags, then the concatenation/continuation is returned.</p>
	 */
	public abstract String getValue();

	public final String getValueOrDefault(final String defaultValue){
		final String v = getValue();
		return (v != null? v: defaultValue);
	}

	public abstract GedcomNode withValue(final String value);

	protected final void addValue(final String childTag, final String subValue){
		if(value == null)
			value = subValue;
		else{
			final GedcomNode conNode = createNewNodeWithTag(childTag);
			conNode.value = subValue;
			addChildInner((children != null? children.size(): 0), conNode);
		}
	}


	public final boolean hasChildren(){
		return (children != null && !children.isEmpty());
	}

	public final List<GedcomNode> getChildren(){
		return (children != null? children: Collections.emptyList());
	}

	public final GedcomNode addChildReference(final String tag, final String xref){
		addChild(createNewNodeWithTag(tag)
			.withXRef(xref));
		return this;
	}

	public final GedcomNode addChildValue(final String tag, final String value){
		if(StringUtils.isNotBlank(value))
			addChild(createNewNodeWithTag(tag)
				.withValue(value));
		return this;
	}

	/** Replaces first occurrence of the given tag with the given value. */
	public final GedcomNode replaceChildValue(final String tag, final String value){
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

	public final GedcomNode addChild(final int index, final GedcomNode child){
		if(child.isEmpty())
			return this;

		return addChildInner(index, child);
	}

	public final GedcomNode addClosingChild(final String tag){
		return addChildInner((children != null? children.size(): 0), createNewNodeWithTag(tag));
	}

	private GedcomNode addChildInner(final int index, final GedcomNode child){
		if(tag == null || !getChildrenWithTag(tag).contains(child)){
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

	public final GedcomNode addChild(final GedcomNode child){
		final int index = (children != null? children.size(): 0);
		return addChild(index, child);
	}

	public final GedcomNode forceAddChild(final GedcomNode child){
		final int index = (children != null? children.size(): 0);
		return addChildInner(index, child);
	}

	public final GedcomNode addChildren(final Iterable<GedcomNode> children){
		if(children != null)
			for(final GedcomNode child : children)
				addChild(child);
		return this;
	}

	/**
	 * Return a list of children of the current node given a tag.
	 *
	 * @param tag	Tag used to retrieve the corresponding children. It can be composed with a dot to denote sub-children.
	 */
	public final List<GedcomNode> getChildrenWithTag(final String tag){
		if(StringUtils.contains(tag, '.')){
			List<GedcomNode> subChildren = new ArrayList<>(1);
			subChildren.add(this);
			final String[] subtags = StringHelper.split(tag, '.');
			for(int i = 0; i < subtags.length; i ++)
				subChildren = getChildrenWithTag(subChildren, subtags[i]);
			return subChildren;
		}

		return getChildrenWithTag(Collections.singletonList(this), tag);
	}

	private static List<GedcomNode> getChildrenWithTag(final List<GedcomNode> root, final String tag){
		final List<GedcomNode> taggedChildren;
		if(!root.isEmpty()){
			taggedChildren = new ArrayList<>(0);
			for(int i = 0; i < root.size(); i ++){
				final List<GedcomNode> cc = root.get(i).children;
				if(cc != null){
					for(final GedcomNode child : cc)
						if(tag.equals(child.tag))
							taggedChildren.add(child);
				}
			}
		}
		else
			taggedChildren = Collections.emptyList();
		return taggedChildren;
	}

	public final List<GedcomNode> getChildrenWithTag(final String... tags){
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

	public final void removeChild(final GedcomNode node){
		if(children != null){
			final Iterator<GedcomNode> itr = children.iterator();
			while(itr.hasNext()){
				final GedcomNode currentNode = itr.next();
				if(currentNode.xref != null && currentNode.xref.equals(node.id) || currentNode.equals(node)){
					itr.remove();

					if(children.isEmpty())
						children = null;

					break;
				}
			}
		}
	}

	public final GedcomNode removeChildrenWithTag(final String tag){
		if(children != null)
			children.removeIf(gedcomNode -> tag.equals(gedcomNode.tag));

		return this;
	}

	public final GedcomNode removeChildrenWithTag(final String... tags){
		if(children != null)
			children.removeIf(gedcomNode -> ArrayUtils.contains(tags, gedcomNode.tag));

		return this;
	}

	public final void clearAll(){
		xref = null;
		value = null;

		if(children != null){
			final Iterator<GedcomNode> itr = children.iterator();
			while(itr.hasNext()){
				itr.next();
				itr.remove();
			}
			children = null;
		}
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public final GedcomNode clone(){
		final GedcomNode copy = createNewNodeWithTag(tag)
			.withLevel(level)
			.withID(id)
			.withXRef(xref)
			.withValue(value);
		if(children != null)
			for(final GedcomNode child : children)
				copy.addChild(child.clone());
		copy.custom = custom;
		return copy;
	}

	public final void replaceWith(final GedcomNode node){
		clearAll();

		withTag(node.tag);
		withLevel(node.level);
		withID(node.id);
		withXRef(node.xref);
		withValue(node.value);
		if(node.children != null)
			for(final GedcomNode child : node.children)
				addChild(child.clone());
		custom = node.custom;
	}


	public static int compareID(final String id1, final String id2){
		return Integer.compare(extractNumberFromID(id1), extractNumberFromID(id2));
	}

	private static int extractNumberFromID(final String id){
		return Integer.parseInt(PATTERN_ID.matcher(id).replaceAll(StringUtils.EMPTY));
	}


	public final boolean isCustom(){
		return custom;
	}

	public final void setCustom(){
		custom = true;
	}


	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final GedcomNode rhs = (GedcomNode)obj;
		return (/*id.equals(rhs.id)
			&&*/ Objects.equals(tag, rhs.tag)
			&& Objects.equals(xref, rhs.xref)
			&& Objects.equals(value, rhs.value)
			&& Objects.equals(children, rhs.children));
	}

	@Override
	public final int hashCode(){
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
	public final String toString(){
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