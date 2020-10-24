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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class GedcomNode{

	/** NOTE: {@link Pattern#DOTALL} is for unicode line separator. */
	private static final Pattern GEDCOM_LINE = Pattern.compile("^\\s*(\\d)\\s+(@([^@ ]+)@\\s+)?([a-zA-Z_0-9.]+)(\\s+@([^@ ]+)@)?(\\s(.*))?$",
		Pattern.DOTALL);
	private static final int GEDCOM_LINE_LEVEL = 1;
	private static final int GEDCOM_LINE_ID = 3;
	private static final int GEDCOM_LINE_TAG = 4;
	private static final int GEDCOM_LINE_XREF = 6;
	private static final int GEDCOM_LINE_VALUE = 8;


	private int level;
	private String id;
	private String tag;
	private String xref;
	private String value;

	private List<GedcomNode> children;


	public static GedcomNode createEmpty(){
		return new GedcomNode();
	}

	public static GedcomNode parse(final String line){
		final Matcher m = GEDCOM_LINE.matcher(line);
		if(!m.find())
			return null;

		final GedcomNode node = new GedcomNode();
		node.setLevel(m.group(GEDCOM_LINE_LEVEL));
		node.setTag(m.group(GEDCOM_LINE_TAG));
		node.setID(m.group(GEDCOM_LINE_ID));
		node.setXRef(m.group(GEDCOM_LINE_XREF));
		node.setValue(m.group(GEDCOM_LINE_VALUE));

		return node;
	}

	private GedcomNode(){}

	public void setLevel(final String level){
		this.level = Integer.parseInt(level);
	}

	public int getLevel(){
		return level;
	}

	public String getID(){
		return id;
	}

	private void setID(final String id){
		if(id != null && !id.isEmpty())
			this.id = id;
	}

	public boolean isCustomTag(){
		return (tag.charAt(0) == '_');
	}

	public String getTag(){
		return tag;
	}

	private void setTag(final String tag){
		if(tag != null && !tag.isEmpty())
			this.tag = tag.toUpperCase();
	}

	public String getXRef(){
		return xref;
	}

	private void setXRef(final String xref){
		if(xref != null && !xref.isEmpty())
			this.xref = xref;
	}

	public String getValue(){
		return value;
	}

	public void setValue(final String value){
		if(value != null && !value.isEmpty())
			this.value = value;
	}

	public void appendValue(final String value){
		if(this.value == null)
			this.value = value;
		else
			this.value += value;
	}

	public List<GedcomNode> getChildren(){
		return (children != null? children: Collections.emptyList());
	}

	public void addChild(final GedcomNode child){
		if(children == null)
			children = new ArrayList<>(1);

		children.add(child);
	}

	public List<GedcomNode> getChildrenWithTag(final String tag){
		final List<GedcomNode> taggedChildren;
		if(children != null){
			taggedChildren = new ArrayList<>(0);
			for(final GedcomNode child : children)
				if(child.tag.equals(tag))
					taggedChildren.add(child);
		}
		else
			taggedChildren = Collections.emptyList();
		return taggedChildren;
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
			final StringBuilder childBuilder = new StringBuilder();

			builder.append(builder.length() > 0? ", ": StringUtils.EMPTY).append("children: [");
			final int size = children.size();
			for(int i = 0; i < size; i ++){
				final GedcomNode child = children.get(i);
				childBuilder.setLength(0);
				childBuilder.append('{');
				if(child.id != null)
					childBuilder.append("id: ").append(child.id);
				if(child.tag != null)
					childBuilder.append(childBuilder.length() > 1? ", ": StringUtils.EMPTY).append("tag: ").append(child.tag);
				if(child.xref != null)
					childBuilder.append(childBuilder.length() > 1? ", ": StringUtils.EMPTY).append("ref: ").append(child.xref);
				if(child.value != null)
					childBuilder.append(childBuilder.length() > 1? ", ": StringUtils.EMPTY).append("value: ").append(child.value);
				childBuilder.append('}');
				builder.append(childBuilder);
				if(i < size - 1)
					builder.append(", ");
			}
			builder.append(']');
		}
		return builder.toString();
	}

}