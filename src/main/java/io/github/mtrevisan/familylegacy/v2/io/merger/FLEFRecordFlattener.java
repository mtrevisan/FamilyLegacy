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
package io.github.mtrevisan.familylegacy.v2.io.merger;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Flattens a hierarchical FLEFRecord into a flat map of key‑value pairs.
 * This is used to extract fields for similarity computation.
 */
public class FLEFRecordFlattener{

	/**
	 * Extracts all relevant fields from a record.
	 *
	 * @param record the record
	 * @return a map of field names to string values
	 */
	public static Map<String, String> extractFields(final FLEFRecord record){
		final Map<String, String> fields = new LinkedHashMap<>();

		// Basic fields
		fields.put("tag", record.getTag());
		if(record.getId() != null)
			fields.put("id", record.getId());
		if(record.getValue() != null)
			fields.put("value", record.getValue());

		// Tag‑specific extraction
		final String tag = record.getTag();
		if(tag == null)
			return fields;

		switch(tag.toLowerCase()){
			case "individual" -> extractIndividual(record, fields);
			case "group" -> extractGroup(record, fields);
			case "event" -> extractEvent(record, fields);
			case "place" -> extractPlace(record, fields);
			case "source" -> extractSource(record, fields);
			case "document" -> extractDocument(record, fields);
			default -> extractGeneric(record, fields);
		}

		return fields;
	}

	private static void extractIndividual(final FLEFRecord record, final Map<String, String> fields){
		// Extract name parts
		for(final FLEFRecord child : record.getChildren()){
			if("name".equalsIgnoreCase(child.getTag())){
				for(final FLEFRecord part : child.getChildren()){
					if("part".equalsIgnoreCase(part.getTag())){
						final String type = FLEFRecordHelper.getChildValue(part, "type");
						final String value = FLEFRecordHelper.getChildValue(part, "value");
						if(type != null && value != null)
							fields.put("name_" + type, value);
					}
				}
			}
			if("sex".equalsIgnoreCase(child.getTag()))
				fields.put("sex", child.getValue());
		}
		// Optionally extract birth date from events
		final String birthDate = extractEventDate(record, "birth");
		if(birthDate != null)
			fields.put("birth_date", birthDate);
		final String deathDate = extractEventDate(record, "death");
		if(deathDate != null)
			fields.put("death_date", deathDate);
	}

	private static void extractGroup(final FLEFRecord record, final Map<String, String> fields){
		for(final FLEFRecord child : record.getChildren()){
			if("name".equalsIgnoreCase(child.getTag())){
				final String name = child.getValue();
				if(name != null)
					fields.put("name", name);
				// if name has a value child
				final String val = FLEFRecordHelper.getChildValue(child, "value");
				if(val != null)
					fields.put("name", val);
			}
			if("type".equalsIgnoreCase(child.getTag()))
				fields.put("group_type", child.getValue());
		}
	}

	private static void extractEvent(final FLEFRecord record, final Map<String, String> fields){
		for(final FLEFRecord child : record.getChildren()){
			if("type".equalsIgnoreCase(child.getTag()))
				fields.put("event_type", child.getValue());
			if("description".equalsIgnoreCase(child.getTag()))
				fields.put("description", child.getValue());
			if("date".equalsIgnoreCase(child.getTag())){
				final String dateVal = FLEFRecordHelper.getChildValue(child, "value.point.full_date.value");
				if(dateVal != null)
					fields.put("date", dateVal);
			}
			if("place".equalsIgnoreCase(child.getTag())){
				final String place = child.getValue();
				if(place != null)
					fields.put("place", place);
				final String orig = FLEFRecordHelper.getChildValue(child, "original_text");
				if(orig != null)
					fields.put("place", orig);
			}
			if("agency".equalsIgnoreCase(child.getTag()))
				fields.put("agency", child.getValue());
		}
	}

	private static void extractPlace(final FLEFRecord record, final Map<String, String> fields){
		for(final FLEFRecord child : record.getChildren()){
			if("name".equalsIgnoreCase(child.getTag())){
				final String name = child.getValue();
				if(name != null)
					fields.put("place_name", name);
				final String val = FLEFRecordHelper.getChildValue(child, "value");
				if(val != null)
					fields.put("place_name", val);
			}
			if("type".equalsIgnoreCase(child.getTag()))
				fields.put("place_type", child.getValue());
			if("map".equalsIgnoreCase(child.getTag())){
				final String coord = FLEFRecordHelper.getChildValue(child, "coordinates");
				if(coord != null)
					fields.put("coordinates", coord);
			}
		}
	}

	private static void extractSource(final FLEFRecord record, final Map<String, String> fields){
		for(final FLEFRecord child : record.getChildren()){
			if("title".equalsIgnoreCase(child.getTag())){
				final String val = FLEFRecordHelper.getChildValue(child, "value");
				if(val != null)
					fields.put("title", val);
			}
			if("author".equalsIgnoreCase(child.getTag()))
				fields.put("author", child.getValue());
			if("publisher".equalsIgnoreCase(child.getTag()))
				fields.put("publisher", child.getValue());
			if("date".equalsIgnoreCase(child.getTag())){
				final String dateVal = FLEFRecordHelper.getChildValue(child, "value.point.full_date.value");
				if(dateVal != null)
					fields.put("date", dateVal);
			}
			if("media_type".equalsIgnoreCase(child.getTag()))
				fields.put("media_type", child.getValue());
		}
	}

	private static void extractDocument(final FLEFRecord record, final Map<String, String> fields){
		for(final FLEFRecord child : record.getChildren()){
			if("uri".equalsIgnoreCase(child.getTag()))
				fields.put("uri", child.getValue());
			if("description".equalsIgnoreCase(child.getTag()))
				fields.put("description", child.getValue());
			if("mapping".equalsIgnoreCase(child.getTag()))
				fields.put("mapping", child.getValue());
		}
	}

	private static void extractGeneric(final FLEFRecord record, final Map<String, String> fields){
		for(final FLEFRecord child : record.getChildren())
			if(child.getValue() != null)
				fields.put(child.getTag(), child.getValue());
	}

	private static String extractEventDate(final FLEFRecord record, final String eventType){
		for(final FLEFRecord child : record.getChildren())
			if("event".equalsIgnoreCase(child.getTag())){
				final String type = FLEFRecordHelper.getChildValue(child, "type");
				if(eventType.equalsIgnoreCase(type)){
					final FLEFRecord date = FLEFRecordHelper.findChild(child, "date");
					if(date != null)
						return FLEFRecordHelper.getChildValue(date, "value.point.full_date.value");
				}
			}
		return null;
	}

}
