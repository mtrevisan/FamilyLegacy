package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Reports on how sources, repositories, and documents are used in the
 * model.
 * <p>
 * Three things are checked:
 * <ul>
 *   <li><b>Unused sources</b> — sources that are never cited by any
 *       record. They are candidates for cleanup or for reuse;</li>
 *   <li><b>Unused documents</b> — documents that are not referenced by
 *       any source or extract. They may be leftovers from imports;</li>
 *   <li><b>Uncited assertions</b> — individuals, events, and other
 *       records that carry no source citation. These are the places
 *       where the model is least supported by evidence.</li>
 * </ul>
 * The report is informational: it does not modify the model.
 */
public final class CitationAnalysisTool implements ToolOperation{


	@Override
	public String getName(){
		return "Citation Analysis…";
	}

	@Override
	public void run(final ToolContext context){
		final FLEFModel model = context.model();
		final Map<String, Integer> sourceUsage = SourceHelper.countCitationsPerSource(model);
		final Map<String, Integer> docUsage = SourceHelper.countDocumentReferences(model);

		final List<FLEFRecord> unusedSources = new ArrayList<>();
		for(final FLEFRecord source : SourceHelper.listAllSources(model))
			if(sourceUsage.getOrDefault(source.getId(), 0) == 0)
				unusedSources.add(source);

		final List<FLEFRecord> unusedDocuments = new ArrayList<>();
		for(final FLEFRecord doc : SourceHelper.listAllDocuments(model))
			if(docUsage.getOrDefault(doc.getId(), 0) == 0)
				unusedDocuments.add(doc);

		final Map<String, Integer> uncited = new TreeMap<>();
		for(final String type : ASSERTION_TYPES){
			int count = 0;
			for(final FLEFRecord record : model.getRecordsByType(type))
				if(!hasCitation(record))
					count++;
			if(count > 0)
				uncited.put(type, count);
		}

		ReportDialog.showHtml(context.owner(), "Citation Analysis",
			buildReport(unusedSources, unusedDocuments, uncited,
				sourceUsage.size(), docUsage.size()));
	}


	/**
	 * Record types that represent genealogical assertions and are
	 * expected to carry at least one source citation when the model is
	 * well supported. The list is deliberately limited to the most
	 * common types to keep the report focused.
	 */
	private static final List<String> ASSERTION_TYPES = List.of(
		"individual", "event", "individual_attribute",
		"relationship", "place"
	);

	private static boolean hasCitation(final FLEFRecord record){
		for(final FLEFRecord child : record.getChildren())
			if(SourceHelper.TAG_SOURCE.equalsIgnoreCase(child.getTag()))
				return true;
		return false;
	}

	private static String buildReport(final List<FLEFRecord> unusedSources,
		final List<FLEFRecord> unusedDocuments,
		final Map<String, Integer> uncited,
		final int citedSourceCount, final int referencedDocumentCount){
		final StringBuilder body = new StringBuilder();
		body.append("<h1>Citation Analysis</h1>");

		// Unused sources.
		body.append("<h2>Unused sources</h2>");
		if(unusedSources.isEmpty())
			body.append("<p class='ok'>Every source is cited at least once.</p>");
		else{
			body.append("<p class='warn'><b>").append(unusedSources.size())
				.append("</b> sources are not cited by any record.</p>");
			body.append("<table>");
			body.append("<tr><th>Source</th><th>Title</th></tr>");
			for(final FLEFRecord source : unusedSources){
				body.append("<tr>");
				body.append("<td>").append(ReportDialog.escape(source.getId())).append("</td>");
				body.append("<td>").append(ReportDialog.escape(SourceHelper.sourceTitle(source))).append("</td>");
				body.append("</tr>");
			}
			body.append("</table>");
		}

		// Unused documents.
		body.append("<h2>Unused documents</h2>");
		if(unusedDocuments.isEmpty())
			body.append("<p class='ok'>Every document is referenced at least once.</p>");
		else{
			body.append("<p class='warn'><b>").append(unusedDocuments.size())
				.append("</b> documents are not referenced by any source or extract.</p>");
			body.append("<table>");
			body.append("<tr><th>Document</th><th>URI</th></tr>");
			for(final FLEFRecord doc : unusedDocuments){
				body.append("<tr>");
				body.append("<td>").append(ReportDialog.escape(doc.getId())).append("</td>");
				final String uri = SourceHelper.documentUri(doc);
				body.append("<td>").append(ReportDialog.escape(uri != null? uri: "")).append("</td>");
				body.append("</tr>");
			}
			body.append("</table>");
		}

		// Uncited assertions.
		body.append("<h2>Uncited assertions</h2>");
		if(uncited.isEmpty())
			body.append("<p class='ok'>Every assertion of the analysed types carries at least one citation.</p>");
		else{
			body.append("<p class='warn'>The following record types contain assertions with no citation:</p>");
			body.append("<table>");
			body.append("<tr><th>Type</th><th>Uncited records</th></tr>");
			for(final Map.Entry<String, Integer> e : uncited.entrySet()){
				body.append("<tr>");
				body.append("<td>").append(ReportDialog.escape(e.getKey())).append("</td>");
				body.append("<td>").append(e.getValue()).append("</td>");
				body.append("</tr>");
			}
			body.append("</table>");
		}

		// Summary.
		body.append("<h2>Summary</h2>");
		body.append("<table>");
		body.append("<tr><td>Sources cited</td><td>").append(citedSourceCount).append("</td></tr>");
		body.append("<tr><td>Documents referenced</td><td>").append(referencedDocumentCount).append("</td></tr>");
		body.append("</table>");

		return ReportDialog.document(body.toString());
	}

}
