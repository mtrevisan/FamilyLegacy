package io.github.mtrevisan.familylegacy.v2.iov3.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		return header;
	}

	public void setHeader(final FLEFRecord header){
		this.header = header;
	}

	public List<FLEFRecord> getRecords(){
		return records;
	}

	public void addRecord(final FLEFRecord record){
		if(record == null)
			return;

		final String cleanId = record.getId(); // Already extracts XRef via FLEFRecordUtils
		if(cleanId != null){
			if(recordsById.containsKey(cleanId))
				// Optionally remove existing record to allow replacement/update
				removeRecord(cleanId);
			recordsById.put(cleanId, record);
		}

		records.add(record);
		if(record.getTag() != null)
			recordsByType.computeIfAbsent(record.getTag(), k -> new ArrayList<>()).add(record);
	}

	public List<FLEFRecord> getRecordsByType(final String type){
		return recordsByType.getOrDefault(type, List.of());
	}

	public FLEFRecord getRecordById(final String id){
		if(id == null)
			return null;

		return recordsById.get(FLEFRecordUtils.extractXRef(id));
	}

	public void removeRecord(final String id){
		if(id == null)
			return;

		final String cleanId = FLEFRecordUtils.extractXRef(id);
		final FLEFRecord record = recordsById.remove(cleanId);

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
		if(id == null)
			return false;

		return recordsById.containsKey(FLEFRecordUtils.extractXRef(id));
	}

	public int getRecordCount(){
		return records.size();
	}

	public Set<String> getRecordTypes(){
		return recordsByType.keySet();
	}

	@Override
	public String toString(){
		return "FLEFModel{" +
			"header=" + (header != null ? header.getTag() : "null") +
			", records=" + records.size() +
			'}';
	}

}
