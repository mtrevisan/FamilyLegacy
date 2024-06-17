package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;


public class DataPopulator{

	private Map<String, GenericTable> tables;

	public DataPopulator(Map<String, GenericTable> tables){
		this.tables = tables;
	}

	public void populate(String filePath) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		String line;
		while((line = reader.readLine()) != null){
			line = line.trim();
			if(! line.isEmpty()){
				String[] parts = line.split(":");
				String tableName = parts[0].trim();
				String recordString = parts[1].trim();
				GenericRecord record = parseRecord(recordString);

				GenericTable table = tables.get(tableName);
				if(table != null){
					table.addRecord(record);
				}
				else{
					System.out.println("Table not found: " + tableName);
				}
			}
		}
		reader.close();
	}

	private GenericRecord parseRecord(String recordString){
		GenericRecord record = new GenericRecord();
		String[] fields = recordString.split(",");
		for(String field : fields){
			String[] keyValue = field.trim().split("=");
			String key = keyValue[0].trim();
			String value = keyValue[1].trim();
			record.setField(key, value);
		}
		return record;
	}

	public static void main(String[] args){
		try{
			SQLFileParser parser = new SQLFileParser();
			parser.parse("path/to/your/sqlfile.sql");

			DataPopulator populator = new DataPopulator(parser.getTables());
			populator.populate("path/to/your/datafile.txt");

			// Stampa le tabelle e i dati per verifica
			parser.getTables().forEach((tableName, table) -> {
				System.out.println("Table: " + tableName);
				System.out.println(table);
			});
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

}
