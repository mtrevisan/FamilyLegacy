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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * FLEF data model.
 * Contains the header and a list of records.
 */
public class FLEFModel{

	private FLEFRecord header;
	private final List<FLEFRecord> records = new ArrayList<>();
	private final Map<String, List<FLEFRecord>> recordsByType = new HashMap<>();
	private final Map<String, FLEFRecord> recordsById = new HashMap<>();


	public FLEFRecord getHeader(){
		return header;
	}

	public void setHeader(final FLEFRecord header){
		this.header = header;
	}

	public List<FLEFRecord> getRecords(){
		return records;
	}

	public void addRecord(final FLEFRecord record){
		records.add(record);
		// Indexing by type
		final String type = record.getTag();
		recordsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(record);
		// Indexing by ID
		if(record.getId() != null)
			recordsById.put(FLEFRecordUtils.extractXRef(record.getId()), record);
	}

	public List<FLEFRecord> getRecordsByType(final String type){
		return recordsByType.getOrDefault(type, List.of());
	}

	public FLEFRecord getRecordById(final String id){
		return recordsById.get(FLEFRecordUtils.extractXRef(id));
	}

	public void removeRecord(final String id){
		final FLEFRecord record = recordsById.remove(id);
		if(record != null){
			records.remove(record);
			final String type = record.getTag();
			final List<FLEFRecord> list = recordsByType.get(type);
			if(list != null){
				list.remove(record);
				if(list.isEmpty())
					recordsByType.remove(type);
			}
		}
	}

	public boolean hasRecord(final String id){
		return recordsById.containsKey(id);
	}

	public int getRecordCount(){
		return records.size();
	}

	/**
	 * Returns the set of record types present in this model.
	 *
	 * @return a set of record type names (e.g., "INDIVIDUAL", "FAMILY", etc.)
	 */
	public Set<String> getRecordTypes(){
		return recordsByType.keySet();
	}

	@Override
	public String toString(){
		return "FLEFModel{" +
			"header=" + header +
			", records=" + records.size() +
			'}';
	}

}
