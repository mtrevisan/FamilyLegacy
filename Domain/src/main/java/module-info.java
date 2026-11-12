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
open module io.github.mtrevisan.familylegacy.domain {
	requires java.desktop;
	requires java.net.http;
	requires io.github.mtrevisan.calendars;
	requires io.github.mtrevisan.reports;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	requires org.apache.commons.io;
	requires org.slf4j;
	requires com.ibm.icu;

	exports io.github.mtrevisan.familylegacy.chronomap;
	exports io.github.mtrevisan.familylegacy.comparison;
	exports io.github.mtrevisan.familylegacy.consanguinity;
	exports io.github.mtrevisan.familylegacy.dossier;
	exports io.github.mtrevisan.familylegacy.ego;
	exports io.github.mtrevisan.familylegacy.events;
	exports io.github.mtrevisan.familylegacy.groups;
	exports io.github.mtrevisan.familylegacy.i18n;
	exports io.github.mtrevisan.familylegacy.importer;
	exports io.github.mtrevisan.familylegacy.individual;
	exports io.github.mtrevisan.familylegacy.io;
	exports io.github.mtrevisan.familylegacy.io.grammar;
	exports io.github.mtrevisan.familylegacy.io.grammar.contraints;
	exports io.github.mtrevisan.familylegacy.io.grammar.typedefinitions;
	exports io.github.mtrevisan.familylegacy.io.merger;
	exports io.github.mtrevisan.familylegacy.io.merger.manual;
	exports io.github.mtrevisan.familylegacy.io.model;
	exports io.github.mtrevisan.familylegacy.io.model.readers;
	exports io.github.mtrevisan.familylegacy.io.model.readers.date;
	exports io.github.mtrevisan.familylegacy.io.model.record;
	exports io.github.mtrevisan.familylegacy.places;
	exports io.github.mtrevisan.familylegacy.preferences;
	exports io.github.mtrevisan.familylegacy.privacy;
	exports io.github.mtrevisan.familylegacy.quality;
	exports io.github.mtrevisan.familylegacy.reports;
	exports io.github.mtrevisan.familylegacy.reports.i18n;
	exports io.github.mtrevisan.familylegacy.reports.index;
	exports io.github.mtrevisan.familylegacy.reports.renderers;
	exports io.github.mtrevisan.familylegacy.research;
	exports io.github.mtrevisan.familylegacy.search;
	exports io.github.mtrevisan.familylegacy.search.strategies;
	exports io.github.mtrevisan.familylegacy.social;
	exports io.github.mtrevisan.familylegacy.sources;
	exports io.github.mtrevisan.familylegacy.temporal;
	exports io.github.mtrevisan.familylegacy.tree;
	exports io.github.mtrevisan.familylegacy.tree.layout;
	exports io.github.mtrevisan.familylegacy.tree.pedigree;
	exports io.github.mtrevisan.familylegacy.util;
	exports io.github.mtrevisan.familylegacy.util.math;

	// The user-visible text of a record is given by whoever knows the editors of the application (the UI provides it)
	uses io.github.mtrevisan.familylegacy.io.model.RecordDisplay;

	// The formats of the files that can be imported are found as providers, all of them live in modules of their own (plugins)
	uses io.github.mtrevisan.familylegacy.importer.FileImporter;

	// The formats of the reports are found as providers, the heavy ones (PDF, Word) live in modules of their own
	uses io.github.mtrevisan.reports.ReportRenderer;
	provides io.github.mtrevisan.reports.ReportRenderer
		with io.github.mtrevisan.familylegacy.reports.renderers.TextReportRenderer,
			io.github.mtrevisan.familylegacy.reports.renderers.MarkdownReportRenderer,
			io.github.mtrevisan.familylegacy.reports.renderers.HtmlReportRenderer;
}
