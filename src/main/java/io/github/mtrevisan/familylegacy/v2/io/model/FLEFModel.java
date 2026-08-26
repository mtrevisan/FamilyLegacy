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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * FLEF data model.
 * Contains the header and a list of top-level records.
 */
public class FLEFModel{

	private FLEFRecord header;
	private final List<FLEFRecord> records = new ArrayList<>();

	private final Map<String, List<FLEFRecord>> recordsByType = new HashMap<>();
	private final Map<String, FLEFRecord> recordsById = new HashMap<>();


	public FLEFRecord getHeader(){
		if(header == null)
			header = FLEFRecord.createEmpty();
		return header;
	}

	/**
	 * NOTE: Used by {@link io.github.mtrevisan.familylegacy.v2.io.FLEFParser#parse(String)} only.
	 */
	public void setHeader(final FLEFRecord header){
		this.header = header;
	}

	public List<FLEFRecord> getRecords(){
		return records;
	}

	public void addRecord(final FLEFRecord record){
		final String id = record.getId();
		if(id != null){
			record.setId(id);

			if(recordsById.containsKey(id))
				// Optionally remove existing record to allow replacement/update
				removeRecord(id);

			recordsById.put(id, record);
		}

		records.add(record);
		if(record.getTag() != null)
			recordsByType.computeIfAbsent(record.getTag(), k -> new ArrayList<>()).add(record);
	}

	public List<FLEFRecord> getRecordsByType(final String type){
		return recordsByType.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
	}

	public FLEFRecord getRecordById(final String id){
		if(id == null)
			return null;

		return recordsById.get(id);
	}

	public FLEFRecord removeRecord(final String id){
		if(id == null)
			return null;

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

		return record;
	}

	public boolean hasRecord(final String id){
		if(id == null)
			return false;

		return recordsById.containsKey(id);
	}

	public int getRecordCount(){
		return records.size();
	}

	public Set<String> getRecordTypes(){
		return recordsByType.keySet();
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final FLEFModel other = (FLEFModel)obj;
		return (Objects.equals(header, other.header)
			&& Objects.equals(new HashSet<>(records), new HashSet<>(other.records)));
	}

	@Override
	public int hashCode(){
		int hash = Objects.hash(header);
		hash = 31 * hash + childrenHash(records);
		return hash;
	}

	private int childrenHash(final List<FLEFRecord> children){
		int result = 0;
		for(final FLEFRecord child : children)
			result ^= child.hashCode();
		return result;
	}

	@Override
	public String toString(){
		return "FLEFModel{"
			+ "header=" + (header != null? header.getTag(): "null")
			+ ", records=" + records.size()
			+ '}';
	}

}
