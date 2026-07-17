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
package io.github.mtrevisan.familylegacy.v2.ui.utils;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Utility methods for working with FLEFRecord objects.
 * Provides common operations for finding, adding, and updating child records.
 */
public final class FLEFRecordUtils{

	private FLEFRecordUtils(){}


	/**
	 * Finds the value of the first child with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag to search for.
	 * @return	The value of the first matching child, or {@code null} if not found.
	 */
	public static String getChildValue(final FLEFRecord parent, final String tag){
		if(parent == null)
			return null;

		for(final FLEFRecord child : parent.getChildren())
			if(tag.equals(child.getTag()))
				return child.getValue();
		return null;
	}

	/**
	 * Finds the first child with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag to search for.
	 * @return	The first matching child record, or {@code null} if not found.
	 */
	public static FLEFRecord findChild(final FLEFRecord parent, final String tag){
		if(parent == null)
			return null;

		for(final FLEFRecord child : parent.getChildren())
			if(tag.equals(child.getTag()))
				return child;
		return null;
	}

	/**
	 * Finds all children with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag to search for.
	 * @return	A list of matching child records.
	 */
	public static List<FLEFRecord> findChildren(final FLEFRecord parent, final String tag){
		final List<FLEFRecord> result = new ArrayList<>();
		if(parent == null)
			return result;

		for(final FLEFRecord child : parent.getChildren())
			if(tag.equals(child.getTag()))
				result.add(child);
		return result;
	}

	/**
	 * Collects values of all children with the given tag as a comma-separated string.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag to search for.
	 * @return	A comma-separated string of values, or empty string if none found.
	 */
	public static String getChildValuesAsString(final FLEFRecord parent, final String tag){
		if(parent == null)
			return StringUtils.EMPTY;

		final StringBuilder sb = new StringBuilder();
		for(final FLEFRecord child : parent.getChildren())
			if(tag.equals(child.getTag()) && child.getValue() != null && !child.getValue().isEmpty()){
				if(!sb.isEmpty())
					sb.append(",");
				sb.append(child.getValue());
			}
		return sb.toString();
	}

	/**
	 * Updates or creates a child with the given tag and value.
	 * If the value is null or empty, the child is removed.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag to update.
	 * @param value	The new value ({@code null} or empty to remove).
	 */
	public static void updateChildValue(final FLEFRecord parent, final String tag, final String value){
		if(parent == null)
			return;

		if(value == null || value.isEmpty()){
			parent.getChildren().removeIf(c -> tag.equals(c.getTag()));
			return;
		}

		final FLEFRecord existing = findChild(parent, tag);
		if(existing != null)
			existing.setValue(value);
		else{
			final FLEFRecord newChild = FLEFRecord.createChildWithValue(1, tag, value);
			parent.addChild(newChild);
		}
	}

	/**
	 * Adds a single child with the given tag and value, if value is not empty.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag for the new child.
	 * @param level	The level for the new child.
	 * @param value	The value to set (ignored if {@code null} or empty).
	 */
	public static void addChild(final FLEFRecord parent, final String tag, final int level, final String value){
		if(parent == null || value == null || value.isEmpty())
			return;

		final FLEFRecord child = FLEFRecord.createChildWithValue(level, tag, value);
		parent.addChild(child);
	}

	/**
	 * Adds multiple children from a comma-separated string of values.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag for the new children.
	 * @param values	Comma-separated string of values (e.g., "I1,I2,I3").
	 */
	public static void addChildrenFromString(final FLEFRecord parent, final String tag, final String values){
		if(parent == null || values == null || values.isEmpty())
			return;

		for(final String val : values.split(",")){
			final String trimmed = val.trim();
			if(!trimmed.isEmpty()){
				final FLEFRecord child = FLEFRecord.createChildWithValue(1, tag, trimmed);
				parent.addChild(child);
			}
		}
	}

	/**
	 * Removes all children with the given tag.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag to remove.
	 */
	public static void removeChildren(final FLEFRecord parent, final String tag){
		if(parent == null)
			return;

		parent.getChildren().removeIf(c -> tag.equals(c.getTag()));
	}

	/**
	 * Copies a record and all its children, adjusting the level offset.
	 *
	 * @param source   the source record to copy
	 * @param newLevel the new level for the root of the copied tree
	 * @return a deep copy of the record with adjusted levels
	 */
	public static FLEFRecord copyRecordWithLevel(FLEFRecord source, int newLevel){
		if(source == null) return null;
		FLEFRecord copy = new FLEFRecord();
		copy.setLevel(newLevel);
		copy.setTag(source.getTag());
		copy.setValue(source.getValue());
		for(FLEFRecord child : source.getChildren()){
			copy.addChild(copyRecordWithLevel(child, newLevel + 1));
		}
		return copy;
	}

	/**
	 * Generates a new unique ID for a record type.
	 *
	 * @param model	The FLEF model.
	 * @param type	The record type (e.g., "INDIVIDUAL", "FAMILY", "EVENT").
	 * @param prefix	The ID prefix (e.g., "I", "F", "E").
	 * @return	A new unique ID.
	 */
	public static String generateNewId(final FLEFModel model, final String type, final String prefix){
		int max = 0;
		for(final FLEFRecord rec : model.getRecordsByType(type)){
			final String id = rec.getId();
			if(id != null && id.startsWith(prefix)){
				try{
					final int num = Integer.parseInt(id.substring(1));
					if(num > max)
						max = num;
				}
				catch(final NumberFormatException ignored){}
			}
		}
		return prefix + (max + 1);
	}

}
