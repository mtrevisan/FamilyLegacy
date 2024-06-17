package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class DataPopulator{

	private static final String FIELD_SEPARATOR = "|";
	private static final String SPACE = " ";
	private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^([A-Z_]+)$");
	private static final Pattern HEADER_PATTERN = Pattern.compile("\\|?([^|]+)\\|?");


	private final Map<String, GenericTable> tables;


	public DataPopulator(final Map<String, GenericTable> tables){
		this.tables = tables;
	}


	public void populate(final String filePath) throws IOException{
		try(final BufferedReader reader = new BufferedReader(new FileReader(filePath))){
			String currentTableName = null;
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
					if(currentTableData != null){
						final GenericTable currentTable = tables.get(currentTableName);
						if(currentTable == null)
							throw new IllegalArgumentException("Table " + currentTableName + " not found");
						currentTable.addRecord(GenericRecord.create(currentTableData));
					}

					currentTableName = matcher.group(1);

					currentTableHeaders.clear();
				}
				else if(currentTableName != null){
					if(currentTableHeaders.isEmpty()){
						//assume this is the header line
						currentTableHeaders = parseHeaders(line);

						//perform schema validation
						final GenericTable currentTable = tables.get(currentTableName);
						if(currentTable == null)
							throw new IllegalArgumentException("Table " + currentTableName + " not found");
						final Set<String> columnNames = currentTable.getColumns().stream()
							.map(GenericColumn::getName)
							.collect(Collectors.toSet());
						final Set<String> readColumnNames = new HashSet<>(currentTableHeaders);
						if(!readColumnNames.containsAll(columnNames) && !columnNames.containsAll(readColumnNames))
							throw new IllegalArgumentException("Number of columns mismatched, expected " + currentTable.getColumns()
								+ ", found " + currentTableHeaders);
					}
					else{
						//assume this is a data line
						currentTableData = parseDataRow(line);

						//TODO perform data validation
					}
				}
			}
		}
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


	public static void main(final String[] args) throws IOException{
		final SQLFileParser parser = new SQLFileParser();
		parser.parse("src/main/resources/gedg/treebard/FLeF.sql");

		final DataPopulator populator = new DataPopulator(parser.getTables());
		populator.populate("src/main/resources/gedg/treebard/FLeF.data");

		populator.tables.forEach((tableName, table) -> {
			final List<GenericRecord> records = table.getRecords();
			if(!records.isEmpty()){
				System.out.println(tableName);
				records.forEach(datum -> {
					final List<GenericColumn> columns = table.getColumns();
					final Map<String, Object> result = new LinkedHashMap<>(columns.size());
						final String[] fields = datum.getFields();
					int i = 0;
					for(final GenericColumn column : columns){
						if(i == fields.length)
							break;

						if(fields[i] != null)
							result.put(column.getName(), fields[i]);

						i ++;
					}
					System.out.println(result);
				});
				System.out.println();
			}
		});
	}

}