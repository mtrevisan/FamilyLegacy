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


/**
 * Represents a FLEF record (header or data record).
 * A record has an ID (optional), a type, a list of children, and can have a level (for children) and a tag/value.
 */
public class FLEFRecord{

	// For main records
	private String id;
//	private String tag;

	// For children
	private String tag;
	private String value;

	private final List<FLEFRecord> children = new ArrayList<>();
	private int lineCount;


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
		return FLEFRecordUtils.extractXRef(id);
	}

	public String getFormattedId(){
		return id;
	}

	public void setId(final String id){
		this.id = FLEFRecordUtils.formatXRef(id);
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

	public void setValue(final String value){
		this.value = value;
	}

	public List<FLEFRecord> getChildren(){
		return children;
	}

	public FLEFRecord addChild(final FLEFRecord child){
		children.add(child);

		return this;
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

	public boolean hasData(){
		return (id != null && !id.isEmpty() || value != null && !value.isEmpty() || !children.isEmpty());
	}

	public int getLineCount(){
		return lineCount;
	}

	public void setLineCount(final int lineCount){
		this.lineCount = lineCount;
	}

	/**
	 * Find the first child with a given tag.
	 */
	public FLEFRecord findChild(final String tag){
		for(final FLEFRecord child : children){
			if(tag.equals(child.getTag())){
				return child;
			}
		}
		return null;
	}

	/**
	 * Find all children with a given tag.
	 */
	public List<FLEFRecord> findChildren(final String tag){
		final List<FLEFRecord> result = new ArrayList<>();
		for(final FLEFRecord child : children){
			if(tag.equals(child.getTag())){
				result.add(child);
			}
		}
		return result;
	}

	/**
	 * Returns the value of a child with a given tag, or null.
	 */
	public String getChildValue(final String tag){
		final FLEFRecord child = findChild(tag);
		return child != null? child.getValue(): null;
	}

	public void copyChildrenFrom(final FLEFRecord record){
		for(final FLEFRecord child : record.getChildren())
			addChild(child);
	}

	/**
	 * Checks whether this record's value is a reference to another record
	 * (i.e. wrapped in @...@, but not the special @VOID@ constant).
	 */
	public boolean isReference(){
		return FLEFRecordUtils.isReference(value);
	}

	/**
	 * Checks whether this record's value is the special @VOID@ constant.
	 */
	public boolean isVoid(){
		return FLEFRecordUtils.isVoidReference(value);
	}

	/**
	 * If this record's value is a reference, returns the referenced ID
	 * (without the surrounding @ symbols). Otherwise returns {@code null}.
	 */
	public String getReferenceId(){
		return FLEFRecordUtils.extractXRef(value);
	}

	@Override
	public String toString(){
		if(id != null)
			return tag + StringUtils.SPACE + id;
		else if(tag != null)
			return tag + (value != null? StringUtils.SPACE + value: StringUtils.EMPTY);
		else
			return null;
	}

}
