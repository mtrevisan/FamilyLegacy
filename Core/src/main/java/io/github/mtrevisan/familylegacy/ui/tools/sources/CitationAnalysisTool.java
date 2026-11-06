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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
		return I18N.t("menu.source_citation.analysis");
	}

	/** What the analysis found: the sources and the documents nobody uses, the assertions with no citation, and the totals. */
	record Analysis(List<FLEFRecord> unusedSources, List<FLEFRecord> unusedDocuments, List<FLEFRecord> uncited, int citedSourceCount,
		int referencedDocumentCount){}


	@Override
	public void run(final ToolContext context){
		new CitationAnalysisDialog(context.owner(), context.model())
			.setVisible(true);
	}

	/** Reads the model: it is called again after a record has been edited, so that the report is what the file is now. */
	static Analysis analyze(final FLEFModel model){
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

		final List<FLEFRecord> uncited = new ArrayList<>();
		for(final String type : ASSERTION_TYPES)
			for(final FLEFRecord record : PrivacyService.visible(model, model.getRecordsByType(type)))
				if(!hasCitation(record))
					uncited.add(record);

		return new Analysis(unusedSources, unusedDocuments, uncited, sourceUsage.size(), docUsage.size());
	}

	/**
	 * Record types that represent genealogical assertions and are
	 * expected to carry at least one source citation when the model is
	 * well supported. The list is deliberately limited to the most
	 * common types to keep the report focused.
	 */
	private static final List<String> ASSERTION_TYPES = List.of(
		IndividualReader.TYPE, EventReader.TYPE, IndividualAttributeReader.TYPE,
		RelationshipReader.TYPE, PlaceReader.TYPE
	);

	private static boolean hasCitation(final FLEFRecord record){
		for(final FLEFRecord child : record.getChildren())
			if(SourceHandler.isOfType(child))
				return true;
		return false;
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		//a file with no source at all has something to report too: its assertions are all uncited
		return (context != null && context.hasModel());
	}

}
