package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


class DataPopulator{

	private static final String FIELD_SEPARATOR = "|";
	private static final String SPACE = " ";
	private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^([A-Z_]+)$");
	private static final Pattern HEADER_PATTERN = Pattern.compile("\\|?([^|]+)\\|?");


	DataPopulator(){}


	void populate(final Map<String, GenericTable> tables, final String filePath) throws IOException{
		try(final BufferedReader reader = new BufferedReader(new FileReader(filePath))){
			GenericTable currentTable = null;
			List<String> currentTableHeaders = new ArrayList<>(0);
			String[] currentTableData = null;

			String line;
			while((line = reader.readLine()) != null){
				line = line.trim();

				if(line.isEmpty())
					continue;

				//add trailing space to ensure split works correctly
				if(line.endsWith(FIELD_SEPARATOR))
					line += SPACE;

				Matcher matcher = TABLE_NAME_PATTERN.matcher(line);
				if(matcher.find()){
					if(currentTableData != null)
						currentTable.addRecord(GenericRecord.create(currentTableData));

					final String currentTableName = matcher.group(1);
					currentTable = tables.get(currentTableName);
					if(currentTable == null)
						throw new IllegalArgumentException("Table " + currentTableName + " not found");

					currentTableHeaders.clear();
					currentTableData = null;
				}
				else if(currentTable != null){
					if(currentTableHeaders.isEmpty()){
						//assume this is the header line
						currentTableHeaders = parseHeaders(line);

						//perform schema validation
						final Set<String> columnNames = currentTable.getColumns().stream()
							.map(GenericColumn::getName)
							.collect(Collectors.toSet());
						final Set<String> readColumnNames = new HashSet<>(currentTableHeaders);
						if(!readColumnNames.containsAll(columnNames) && !columnNames.containsAll(readColumnNames))
							throw new IllegalArgumentException("Number of columns mismatched, expected " + currentTable.getColumns()
								+ ", found " + currentTableHeaders);
					}
					else{
						if(currentTableData != null)
							currentTable.addRecord(GenericRecord.create(currentTableData));

						//assume this is a data line
						currentTableData = parseDataRow(line);
					}
				}
			}
		}

		//TODO validateDataType(tables);
		validateForeignKeys(tables);
	}

	private static List<String> parseHeaders(final String line){
		final List<String> headers = new ArrayList<>();
		final Matcher matcher = HEADER_PATTERN.matcher(line);
		while(matcher.find())
			headers.add(matcher.group(1).trim());

		return headers;
	}

	private static String[] parseDataRow(final String line){
		final StringTokenizer tokenizer = new StringTokenizer(line, FIELD_SEPARATOR);
		final String[] rowData = new String[tokenizer.countTokens()];
		int index = 0;
		while(tokenizer.hasMoreTokens())
			rowData[index ++] = tokenizer.nextToken();
		return rowData;
	}

	private void validateForeignKeys(final Map<String, GenericTable> tables){
		for(final GenericTable table : tables.values()){
			final String tableName = table.getName();
			final Set<ForeignKey> foreignKeys = table.getForeignKeys();

			final Map<GenericKey, GenericRecord> records = table.getRecords();
			for(final Map.Entry<GenericKey, GenericRecord> record : records.entrySet()){
				final GenericKey recordKey = record.getKey();
				final GenericRecord recordRow = record.getValue();

				for(final ForeignKey foreignKey : foreignKeys){
					final String[] tableColumnName = foreignKey.columnName();
					final String referencedTableName = foreignKey.foreignTable();

					for(int i = 0, length = tableColumnName.length; i < length; i ++){
						String column = tableColumnName[i];
						final Object key = table.getValueForColumn(recordRow, column);
						if(key != GenericTable.NO_KEY){
							final GenericTable referencedTable = tables.get(referencedTableName);
							if(!referencedTable.hasRecord(recordKey)){
								referencedTable.hasRecord(recordKey);
								throw new IllegalArgumentException("Table " + referencedTableName + " does not have record " + key
									+ " referenced by " + tableName + "." + column);
							}
						}
					}
				}
			}
		}
	}

}
