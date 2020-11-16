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
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class GedcomNode{

	private static final String NEW_LINE = "\\n";

	/** NOTE: {@link Pattern#DOTALL} is for unicode line separator. */
	private static final Pattern GEDCOM_LINE = Pattern.compile("^\\s*(\\d)\\s+(@([^@ ]+)@\\s+)?([a-zA-Z_0-9.]+)(\\s+@([^@ ]+)@)?(\\s(.*))?$",
		Pattern.DOTALL);
	private static final int GEDCOM_LINE_LEVEL = 1;
	private static final int GEDCOM_LINE_ID = 3;
	private static final int GEDCOM_LINE_TAG = 4;
	private static final int GEDCOM_LINE_XREF = 6;
	private static final int GEDCOM_LINE_VALUE = 8;

	private static final String TAG_CONCATENATION = "CONC";
	private static final String TAG_CONTINUATION = "CONT";
	private static final String[] CONTINUATION_TAGS = {TAG_CONCATENATION, TAG_CONTINUATION};
	static{
		Arrays.sort(CONTINUATION_TAGS);
	}


	private int level;
	private String id;
	private String tag;
	private String xref;
	private String value;

	private List<GedcomNode> children;
	private boolean custom;


	public static GedcomNode createRoot(){
		final GedcomNode root = new GedcomNode();
		root.level = -1;
		return root;
	}

	public static GedcomNode createEmpty(){
		return new GedcomNode();
	}

	public static GedcomNode create(final String tag){
		return new GedcomNode(tag);
	}

	public static GedcomNode create(final String tag, final String id, final String value){
		return new GedcomNode(tag)
			.withID(id)
			.withValue(value);
	}

	public static GedcomNode parse(final CharSequence line){
		final Matcher m = GEDCOM_LINE.matcher(line);
		if(!m.find())
			return null;

		final GedcomNode node = new GedcomNode();
		node.setLevel(m.group(GEDCOM_LINE_LEVEL));
		node.withID(m.group(GEDCOM_LINE_ID));
		node.withTag(m.group(GEDCOM_LINE_TAG));
		node.setXRef(m.group(GEDCOM_LINE_XREF));
		node.withValue(m.group(GEDCOM_LINE_VALUE));

		return node;
	}

	private GedcomNode(){}

	private GedcomNode(final String tag){
		if(tag == null || tag.isEmpty())
			throw new IllegalArgumentException("Tag must be present");

		this.tag = tag;
	}

	/**
	 * NOTE: clear all the fields but the {@link #level} and the {@link #tag}.
	 */
	public GedcomNode clear(){
		id = null;
		xref = null;
		value = null;

		children = null;
		custom = false;

		return this;
	}

	public boolean isEmpty(){
		return (tag == null || id == null && value == null && (children == null || children.isEmpty()));
	}

	public void setLevel(final int level){
		if(level < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + level);

		this.level = level;
	}

	public void setLevel(final String level){
		this.level = (level.length() == 1 && level.charAt(0) == 'n'? 0: Integer.parseInt(level));

		if(this.level < 0)
			throw new IllegalArgumentException("Level must be greater than or equal to zero, was " + this.level);
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

	public void removeID(){
		id = null;
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

	public void setXRef(final String xref){
		if(xref != null && !xref.isEmpty())
			this.xref = xref;
	}

	/**
	 * Returns the value associated with this node.
	 * <p>If the value is composed of multiple CONC|CONT tags, then the concatenation is returned.</p>
	 */
	public String getValue(){
		if(children != null){
			final StringBuilder sb = new StringBuilder();
			if(value != null)
				sb.append(value);
			for(final GedcomNode child : children)
				if(Arrays.binarySearch(CONTINUATION_TAGS, child.tag) >= 0){
					if(child.tag.charAt(3) == 'T')
						sb.append(NEW_LINE);
					if(child.value != null)
						sb.append(child.value);
				}
			return (sb.length() > 0? sb.toString(): null);
		}
		else
			return value;
	}

	public void removeValue(){
		value = null;
	}

	public GedcomNode withValue(final String value){
		this.value = null;
		if(value != null && !value.isEmpty()){
			//split line into CONC|CONT if appliable
			int remainingLength;
			final int length = value.length();
			int offset = 0;
			do{
				remainingLength = Math.min(255 - offset - tag.length() - 4, length - offset);

				final String newTag;
				final int lineFeedIndex = value.indexOf(NEW_LINE, offset);
				if(lineFeedIndex >= 0 && lineFeedIndex < offset + remainingLength){
					remainingLength = lineFeedIndex;
					newTag = TAG_CONTINUATION;
				}
				else{
					while(value.charAt(offset + remainingLength - 1) == ' ')
						remainingLength --;
					newTag = TAG_CONCATENATION;
				}
				final String newValue = value.substring(offset, offset + remainingLength);

				if(this.value != null){
					final GedcomNode child = create(newTag);
					child.value = newValue;
					addChild(child);
				}
				else
					this.value = newValue;

				offset += remainingLength;
				if(newTag.equals(TAG_CONTINUATION))
					offset += NEW_LINE.length();
			}while(offset < length);
		}
		return this;
	}

	public boolean hasChildren(){
		return (children != null && !children.isEmpty());
	}

	public List<GedcomNode> getChildren(){
		return (children != null? children: Collections.emptyList());
	}

	public GedcomNode addChildReference(final String tag, final String id){
		addChild(create(tag)
			.withID(id));
		return this;
	}

	public GedcomNode addChildValue(final String tag, final String value){
		if(StringUtils.isNotEmpty(value))
			addChild(create(tag)
				.withValue(value));
		return this;
	}

	public GedcomNode addChildValueConcatenated(final String tag, final String value){
		if(StringUtils.isNotEmpty(value))
			addChild(create(tag)
				.withValue(value));
		return this;
	}

	public GedcomNode addChild(final int index, final GedcomNode child){
		if(child.isEmpty())
			return this;

		return addChildInner(index, child);
	}

	public GedcomNode addClosingChild(final String tag){
		return addChildInner(0, GedcomNode.create(tag));
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

	public GedcomNode removeChild(final GedcomNode child){
		if(children != null && !child.isEmpty()){
			children.remove(child);

			if(children.isEmpty())
				children = null;
		}
		return this;
	}

	public List<GedcomNode> removeChildren(){
		final List<GedcomNode> originalChildren = children;
		children = null;
		return originalChildren;
	}

	/**
	 * Returns the first child with the given tag, {@code null} if no child was found.
	 *
	 * @param tags	The tag(s) to search the first child.
	 * @return	The first {@link GedcomNode} that matches the given tag, or {@code null}.
	 */
	public GedcomNode getFirstChildWithTag(final String... tags){
		if(children != null)
			for(final GedcomNode child : children)
				if(ArrayUtils.contains(tags, child.tag))
					return child;
		return null;
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

	public GedcomNode getChildWithIDAndTag(final String id, final String... tags){
		if(children != null)
			for(final GedcomNode child : children)
				if(ArrayUtils.contains(tags, child.tag) && id.equals(child.id))
					return child;
		return createEmpty();
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