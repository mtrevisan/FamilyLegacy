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

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a FLEF record (header or data record).
 * A record has an ID (optional), a type, a list of children, and can have a level (for children) and a tag/value.
 */
public class FLEFRecord{

	// For main records (level 0)
	private String id;
	private String type;

	// For children (level > 0)
	private int level;
	private String tag;
	private String value;

	private final List<FLEFRecord> children = new ArrayList<>();
	private int lineCount;


	public String getId(){
		return id;
	}

	public void setId(final String id){
		this.id = id;
	}

	public String getType(){
		return type;
	}

	public void setType(final String type){
		this.type = type;
	}

	public int getLevel(){
		return level;
	}

	public void setLevel(final int level){
		this.level = level;
	}

	public String getTag(){
		return tag;
	}

	public void setTag(final String tag){
		this.tag = tag;
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

	public void addChild(final FLEFRecord child){
		children.add(child);
	}

	public boolean hasChildren(){
		return !children.isEmpty();
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

	@Override
	public String toString(){
		if(id != null){
			return type + " " + id;
		}
		else if(tag != null){
			return level + " " + tag + (value != null? " " + value: "");
		}
		else{
			return type != null? type: "Record";
		}
	}
}
