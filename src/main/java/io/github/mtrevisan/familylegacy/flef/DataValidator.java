package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class DataValidator{

	private final Map<String, List<GenericRecord>> tables;


	public DataValidator(final Map<String, List<GenericRecord>> tables){
		this.tables = tables;
	}


	public void validate(String filePath) throws IOException{
		final BufferedReader reader = new BufferedReader(new FileReader(filePath));
		String line;
		while((line = reader.readLine()) != null){
			line = line.trim();
			validateLine(line);
		}
		reader.close();
	}

	private void validateLine(final String line){
		final String[] parts = line.split(":");
		final String tableName = parts[0].trim();
		final String recordString = parts[1].trim();
		final GenericRecord record = parseRecord(recordString);

		if(tables.containsKey(tableName)){
			final List<GenericRecord> records = tables.get(tableName);
			if(records.contains(record))
				System.out.println("Record validated: " + record);
			else
				System.out.println("Record not found: " + record);
		}
		else
			System.out.println("Table not found: " + tableName);
	}

	private GenericRecord parseRecord(final String recordString){
		final GenericRecord record = new GenericRecord();
		final String[] fields = recordString.split(",");
		for(final String field : fields){
			final String[] keyValue = field.trim()
				.split("=");
			final String key = keyValue[0].trim();
			final String value = keyValue[1].trim();
			record.setField(key, value);
		}
		return record;
	}

}
