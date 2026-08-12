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
package io.github.mtrevisan.familylegacy.v2.io.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Represents a FLEF record (header or data record).
 * A record has an ID (optional), a type, a list of children, and can have a tag/value.
 */
public class FLEFRecord{

	private static final String PARAM_ID = "id";


	private String tag;

	// For main records:
	private String id;
	// For children:
	private String value;

	private final List<FLEFRecord> children = new ArrayList<>();


	public static FLEFRecord createMainRecord(final String id, final String type){
		final FLEFRecord record = createEmpty();
		record.setId(id);
		record.setTag(type);
		return record;
	}

	public static FLEFRecord createChildWithValue(final String tag, final String value){
		final FLEFRecord record = createChild(tag);
		record.setValue(value);
		return record;
	}

	public static FLEFRecord createChild(final String tag){
		final FLEFRecord record = createEmpty();
		record.setTag(tag);
		return record;
	}


	public static FLEFRecord createEmpty(){
		return new FLEFRecord();
	}


	private FLEFRecord(){}


	public String getId(){
		return XRefHelper.extractXRef(id);
	}

	public String findRecordId(){
		String id = null;
		for(final FLEFRecord child : children)
			if(PARAM_ID.equals(child.getTag()))
				id = XRefHelper.extractXRef(child.getValue());
		return id;
	}

	public String getFormattedId(){
		return id;
	}

	public FLEFRecord setId(final String id){
		this.id = XRefHelper.formatXRef(id);

		return this;
	}

	public String getTag(){
		return tag;
	}

	public FLEFRecord setTag(final String tag){
		this.tag = tag;

		return this;
	}

	public String getValue(){
		return value;
	}

	public FLEFRecord setValue(final String value){
		this.value = value;

		return this;
	}

	public List<FLEFRecord> getChildren(){
		return children;
	}

	public long countChildren(final String tag){
		return children.stream()
			.filter(c -> tag.equals(c.getTag()))
			.count();
	}

	public FLEFRecord addChild(final FLEFRecord child){
		if(child == null || child.isEmpty())
			return this;

		return forceAddChild(child);
	}

	public FLEFRecord forceAddChild(final FLEFRecord child){
		if(child == null)
			return this;

		children.add(child);

		return this;
	}

	public FLEFRecord addChildren(final List<FLEFRecord> children){
		this.children.addAll(children);

		return this;
	}

	public FLEFRecord addChildWithTag(final String tag, final FLEFRecord child){
		if(child == null || child.isEmpty())
			return this;

		child.setTag(tag);

		return addChild(child);
	}

	public FLEFRecord addChildrenWithTag(final String tag, final List<FLEFRecord> children){
		for(final FLEFRecord child : children)
			if(StringUtils.isEmpty(child.getTag()))
				child.setTag(tag);

		return addChildren(children);
	}

	/**
	 * Removes a specific child record from this record's children list.
	 *
	 * @param child the child record to remove
	 * @return {@code true} if the child was found and removed, {@code false} otherwise
	 */
	public boolean removeChild(final FLEFRecord child){
		return children.remove(child);
	}

	/**
	 * Removes all children with the given tag.
	 *
	 * @param tag the tag of children to remove
	 * @return the list of removed children (empty if none were found)
	 */
	public List<FLEFRecord> removeChildren(final String tag){
		final List<FLEFRecord> removed = new ArrayList<>();
		children.removeIf(child -> {
			if(tag.equals(child.getTag())){
				removed.add(child);
				return true;
			}
			return false;
		});
		return removed;
	}

	public boolean hasChildren(){
		return !children.isEmpty();
	}

	public void clear(){
		value = null;
		children.clear();
	}

	public boolean hasData(){
		return (id != null && !id.isEmpty() || value != null && !value.isEmpty() || hasChildren());
	}

	/**
	 * Checks whether this record's value is a reference to another record
	 * (i.e. wrapped in @...@, but not the special @VOID@ constant).
	 */
	public boolean isReference(){
		return XRefHelper.isReference(value);
	}

	/**
	 * Checks whether this record's value is the special @VOID@ constant.
	 */
	public boolean isVoid(){
		return XRefHelper.isVoidReference(value);
	}

	public boolean isEmpty(){
		return (StringUtils.isEmpty(id) && StringUtils.isEmpty(value) && children.isEmpty());
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final FLEFRecord other = (FLEFRecord)obj;
		return (Objects.equals(id, other.id)
			&& Objects.equals(tag, other.tag) && Objects.equals(value, other.value));
	}

	@Override
	public int hashCode(){
		return Objects.hash(id, tag, value);
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder(tag != null? tag: "--");
		if(id != null)
			sb.append('[').append(id).append(']');
		if(value != null)
			sb.append(" = ").append(value);
		if(hasChildren())
			sb.append(" (").append(children.size()).append(" children)");
		return sb.toString();
	}

}
