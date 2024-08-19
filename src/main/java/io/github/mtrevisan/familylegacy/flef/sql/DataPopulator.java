/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.sql;

import io.github.mtrevisan.familylegacy.flef.helpers.StringHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


class DataPopulator{

	private static final String FIELD_SEPARATOR = "|";
	private static final char ESCAPE = '\\';
	private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^([A-Z_]+)$");
	private static final Pattern HEADER_PATTERN = Pattern.compile("\\|?([^|]+)\\|?");


	final void populate(final Map<String, GenericTable> tables, final String filePath) throws IOException{
		try(final BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))){
			GenericTable currentTable = null;
			List<String> currentTableHeaders = new ArrayList<>(0);
			String[] currentTableData = null;

			String line;
			while((line = reader.readLine()) != null){
				line = line.trim();

				if(line.isEmpty())
					continue;

				final Matcher matcher = TABLE_NAME_PATTERN.matcher(line);
				if(matcher.find()){
					if(currentTableData != null)
						currentTable.addRecord(GenericRecord.create(currentTableData));

					final String currentTableName = matcher.group(1)
						.toLowerCase(Locale.ROOT);
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
						final Collection<String> readColumnNames = new HashSet<>(currentTableHeaders);
						if(!readColumnNames.containsAll(columnNames) && !columnNames.containsAll(readColumnNames))
							throw new IllegalArgumentException("Number of columns mismatched, expected " + currentTable.getColumns()
								+ ", found " + currentTableHeaders);
					}
					else{
						if(currentTableData != null)
							currentTable.addRecord(GenericRecord.create(currentTableData));

						//assume this is a data line
						currentTableData = StringHelper.split(line, FIELD_SEPARATOR.charAt(0), ESCAPE);
					}
				}
			}

			if(currentTableData != null)
				currentTable.addRecord(GenericRecord.create(currentTableData));
		}

		//TODO validateNotNull(tables);
		//TODO validateDataType(tables);
		validateForeignKeys(tables);
	}

	private static List<String> parseHeaders(final String line){
		final List<String> headers = new ArrayList<>();
		final Matcher matcher = HEADER_PATTERN.matcher(line);
		while(matcher.find()){
			final String header = matcher.group(1)
				.toLowerCase(Locale.ROOT)
				.trim();
			headers.add(header);
		}

		return headers;
	}

	private void validateForeignKeys(final Map<String, GenericTable> tables){
		for(final GenericTable table : tables.values()){
			final String tableName = table.getName();
			final Set<ForeignKey> foreignKeys = table.getForeignKeys();

			final Map<GenericKey, GenericRecord> records = table.getRecords();
			for(final Map.Entry<GenericKey, GenericRecord> record : records.entrySet()){
				final GenericRecord recordRow = record.getValue();

				for(final ForeignKey foreignKey : foreignKeys){
					final String[] tableColumnName = foreignKey.columnName();
					final String referencedTableName = foreignKey.foreignTable();
					final String[] referencedTableColumn = foreignKey.foreignColumn();

					final Object[] externalKeyValue = new Object[tableColumnName.length];
					int index = 0;
					for(int i = 0, length = tableColumnName.length; i < length; i ++)
						externalKeyValue[index++] = table.getValueForColumn(recordRow, tableColumnName[i]);
					if(Arrays.stream(externalKeyValue).allMatch(Objects::isNull))
						continue;

					final GenericKey externalKey = new GenericKey(externalKeyValue);

					final GenericTable referencedTable = tables.get(referencedTableName);
					if(!referencedTable.hasRecord(referencedTableColumn, externalKey))
						throw new IllegalArgumentException("Table " + referencedTableName + " does not have record "
							+ Arrays.toString(externalKeyValue) + " referenced by " + tableName + "." + Arrays.toString(tableColumnName));
				}
			}
		}
	}

}
