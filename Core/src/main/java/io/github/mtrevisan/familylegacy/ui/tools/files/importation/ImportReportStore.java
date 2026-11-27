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
package io.github.mtrevisan.familylegacy.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.importer.ImportIssue;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.preferences.ProjectMeta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Keeps the last import report next to the file the import created ({@code import-report.properties} in its {@code .meta} folder), so
 * that it can be read again after the dialog is closed, and travels with the file.
 */
public final class ImportReportStore{

	private static final String FILE_NAME = "import-report.properties";
	private static final String KEY_SOURCE = "source";
	private static final String KEY_DATE = "date";
	private static final String KEY_OVERFLOW = "overflow";
	private static final String KEY_COUNTS = "counts";
	private static final String KEY_ISSUES = "issues";


	/** A report read back, with what it came from. */
	public record Stored(ImportReport report, String source, String date){}


	private ImportReportStore(){}


	/**
	 * Writes the report (nothing if it is empty: there is nothing to read again).
	 *
	 * @param documentPath	The path of the FLEF file the import created.
	 * @param source	The name of the file that was imported.
	 */
	public static void save(final String documentPath, final ImportReport report, final String source){
		if(documentPath == null || report.isEmpty())
			return;

		final Properties props = new Properties();
		props.setProperty(KEY_SOURCE, (source != null? source: ""));
		props.setProperty(KEY_DATE, LocalDateTime.now()
			.truncatedTo(ChronoUnit.MINUTES)
			.toString());
		props.setProperty(KEY_OVERFLOW, Integer.toString(report.overflow()));

		final Map<String, Integer> counts = report.countsByReason();
		props.setProperty(KEY_COUNTS, Integer.toString(counts.size()));
		int i = 0;
		for(final Map.Entry<String, Integer> entry : counts.entrySet()){
			props.setProperty(KEY_COUNTS + "." + i + ".reason", entry.getKey());
			props.setProperty(KEY_COUNTS + "." + i + ".count", Integer.toString(entry.getValue()));
			i ++;
		}

		final List<ImportIssue> issues = report.issues();
		props.setProperty(KEY_ISSUES, Integer.toString(issues.size()));
		for(int j = 0; j < issues.size(); j ++){
			final ImportIssue issue = issues.get(j);
			props.setProperty(KEY_ISSUES + "." + j + ".reason", issue.reasonKey());
			props.setProperty(KEY_ISSUES + "." + j + ".item", issue.item());
			props.setProperty(KEY_ISSUES + "." + j + ".where", issue.where());
		}

		ProjectMeta.write(ProjectMeta.file(documentPath, FILE_NAME), props, "import report");
	}

	/** @return	Whether the file has a report to read. */
	public static boolean exists(final String documentPath){
		return (documentPath != null && Files.exists(ProjectMeta.file(documentPath, FILE_NAME)));
	}

	/** @return	The report, {@code null} if there is none (or it cannot be read). */
	public static Stored load(final String documentPath){
		if(!exists(documentPath))
			return null;

		final Path file = ProjectMeta.file(documentPath, FILE_NAME);
		final Properties props = ProjectMeta.read(file);
		try{
			final Map<String, Integer> counts = new LinkedHashMap<>();
			final int countSize = Integer.parseInt(props.getProperty(KEY_COUNTS, "0"));
			for(int i = 0; i < countSize; i ++)
				counts.put(props.getProperty(KEY_COUNTS + "." + i + ".reason"),
					Integer.parseInt(props.getProperty(KEY_COUNTS + "." + i + ".count")));

			final List<ImportIssue> issues = new ArrayList<>();
			final int issueSize = Integer.parseInt(props.getProperty(KEY_ISSUES, "0"));
			for(int j = 0; j < issueSize; j ++)
				issues.add(new ImportIssue(props.getProperty(KEY_ISSUES + "." + j + ".reason"),
					props.getProperty(KEY_ISSUES + "." + j + ".item", ""), props.getProperty(KEY_ISSUES + "." + j + ".where", "")));

			if(counts.isEmpty())
				return null;
			return new Stored(ImportReport.restored(issues, counts, Integer.parseInt(props.getProperty(KEY_OVERFLOW, "0"))),
				props.getProperty(KEY_SOURCE, ""), props.getProperty(KEY_DATE, ""));
		}
		catch(final RuntimeException ignored){
			//a damaged file is read as no report
			return null;
		}
	}

}
