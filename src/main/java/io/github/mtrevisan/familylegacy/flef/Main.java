/**
 * Copyright (c) 2021 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Main{

	public static void main(final String[] args) throws IOException{
		SQLFileParser parser = new SQLFileParser();
		parser.parse("src/main/resources/gedg/treebard/FLeF.sql");
		Map<String, GenericTable> tables = parser.getTables();

		DataPopulator populator = new DataPopulator(tables);
		populator.populate("src/main/resources/gedg/treebard/FLeF.data");
	}

//	public static void main(final String[] args){
//		final String filePath = "src/main/resources/ged/small.newflef.ged";
//
//		final Map<String, List<Map<String, String>>> tables = readTables(filePath);
//
//		for(final String tableName : tables.keySet()){
//			final List<Map<String, String>> rows = tables.get(tableName);
//			if(rows.isEmpty())
//				continue;
//
//			System.out.println("table: " + tableName);
//			for(final Map<String, String> row : rows)
//				System.out.println(row);
//		}
//	}

	private static Map<String, List<Map<String, String>>> readTables(final String filePath){
		final Map<String, List<Map<String, String>>> tables = new HashMap<>();

		try(final BufferedReader br = new BufferedReader(new FileReader(filePath))){
			String tableName = null;
			String[] columns = null;
			List<Map<String, String>> table = null;

			String line;
			while((line = br.readLine()) != null){
				line = line.trim();

				if(line.isEmpty())
					continue;

				if(line.endsWith(":") && !line.contains("|")){
					tableName = line.substring(0, line.length() - 1);

					line = br.readLine();
					columns = line.split("\\|");

					table = new ArrayList<>();
					tables.put(tableName, table);
				}
				else if(tableName != null){
					final String[] values = line.split("\\|");

					final Map<String, String> row = new LinkedHashMap<>();
					for(int i = 0; i < columns.length; i ++)
						row.put(columns[i], (i < values.length? values[i]: null));

					table.add(row);
				}
			}
		}
		catch(final IOException ioe){
			ioe.printStackTrace();
		}

		return tables;
	}

}
