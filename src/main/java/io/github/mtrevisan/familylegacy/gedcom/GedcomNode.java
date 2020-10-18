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

import io.github.mtrevisan.familylegacy.gedcom.models.ExtensionContainer;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GedcomNode{

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
	private GedcomTag tag;
	private String xref;
	private String value;

	private ExtensionContainer extensionContainer;

	private GedcomNode parent;
	private List<GedcomNode> children;


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

		//TODO create extensionContainer

		return node;
	}

	public void setLevel(final String level){
		this.level = Integer.parseInt(level);
	}

	public int getLevel(){
		return level;
	}

	private void setID(final String id){
		if(id != null && !id.isEmpty())
			this.id = id;
	}

	public GedcomTag getTag(){
		return tag;
	}

	private void setTag(final String tag){
		if(tag != null && !tag.isEmpty())
			this.tag = GedcomTag.valueOf(tag.toUpperCase());
	}

	private void setXRef(final String xref){
		if(xref != null && !xref.isEmpty())
			this.xref = xref;
	}

	public void setValue(final String value){
		if(value != null && !value.isEmpty())
			this.value = value;
	}

	public GedcomNode getParent(){
		return parent;
	}

	public void setParent(final GedcomNode parent){
		this.parent = parent;
	}

	public List<GedcomNode> getChildren(){
		return (children != null? children: Collections.emptyList());
	}

	public void addChild(final GedcomNode child){
		if(children == null)
			children = new ArrayList<>();

		children.add(child);
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
			.append((parent != null), (rhs.parent != null))
			.append(children, rhs.children);
		if(parent != null)
			builder.append(parent.id, rhs.parent.id)
				.append(parent.tag, rhs.parent.tag)
				.append(parent.xref, rhs.parent.xref)
				.append(parent.value, rhs.parent.value);
		return builder.isEquals();
	}

	@Override
	public int hashCode(){
		final HashCodeBuilder builder = new HashCodeBuilder()
			.append(id)
			.append(tag)
			.append(xref)
			.append(value)
			.append(children);
		if(parent != null)
			builder.append(parent.id)
				.append(parent.tag)
				.append(parent.xref)
				.append(parent.value);
		return builder.hashCode();
	}

	@Override
	public String toString(){
		final StringBuilder builder = new StringBuilder();
		if(id != null)
			builder.append("id: ").append(id);
		if(tag != null)
			builder.append(builder.length() > 0? ", ": "").append("tag: ").append(tag);
		if(xref != null)
			builder.append(builder.length() > 0? ", ": "").append("ref: ").append(xref);
		if(value != null)
			builder.append(builder.length() > 0? ", ": "").append("value: ").append(value);
		if(parent != null){
			final StringBuilder parentBuilder = new StringBuilder();
			if(parent.id != null)
				parentBuilder.append(builder.length() > 0? ", ": "").append("parent: {");
			if(parent.id != null)
				parentBuilder.append("id: ").append(parent.id);
			if(parent.tag != null)
				parentBuilder.append(parentBuilder.length() > 0? ", ": "").append("tag: ").append(parent.tag);
			if(parent.xref != null)
				parentBuilder.append(parentBuilder.length() > 0? ", ": "").append("ref: ").append(parent.xref);
			if(parent.value != null)
				parentBuilder.append(parentBuilder.length() > 0? ", ": "").append("value: ").append(parent.value);
			parentBuilder.append("}");
			builder.append(parentBuilder);
		}
		if(children != null){
			builder.append(builder.length() > 0? ", ": "").append("children: [");
			final int size = children.size();
			for(int i = 0; i < size; i ++){
				final GedcomNode child = children.get(i);
				final StringBuilder childBuilder = new StringBuilder();
				childBuilder.append('{');
				if(child.id != null)
					childBuilder.append("id: ").append(child.id);
				if(child.tag != null)
					childBuilder.append(childBuilder.length() > 1? ", ": "").append("tag: ").append(child.tag);
				if(child.xref != null)
					childBuilder.append(childBuilder.length() > 1? ", ": "").append("ref: ").append(child.xref);
				if(child.value != null)
					childBuilder.append(childBuilder.length() > 1? ", ": "").append("value: ").append(child.value);
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