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


module io.github.mtrevisan.familylegacy.core {
	requires java.base;
	requires io.github.mtrevisan.familylegacy.domain;
	requires io.github.mtrevisan.calendars;
	requires io.github.mtrevisan.reports;
	requires org.apache.commons.lang3;
	requires java.desktop;
	requires com.miglayout.swing;
	requires org.slf4j;
	requires org.apache.commons.text;
	requires com.ibm.icu;
	requires org.apache.commons.io;
	requires org.apache.tika.core;
	requires java.net.http;
	requires java.prefs;
	requires org.jxmapviewer.jxmapviewer2;
	requires net.coobird.thumbnailator;
	requires org.apache.pdfbox;

	// Assistants that plug into the Research Assistant
	uses io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;

	// The main window finds the Research Assistant, it does not depend on it
	uses io.github.mtrevisan.familylegacy.ui.shell.ResearchAssistantPort;
	provides io.github.mtrevisan.familylegacy.ui.shell.ResearchAssistantPort
		with io.github.mtrevisan.familylegacy.ui.assistant.app.FrameAssistantPort;

	// The handlers create the editors through a registry that the catalog of the editors fills
	uses io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogProvider;
	provides io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogProvider
		with io.github.mtrevisan.familylegacy.ui.dialogs.registry.RecordDialogCatalog;

	// The reports (of the domain) name the records as the handlers do, without depending on them
	provides io.github.mtrevisan.familylegacy.io.model.RecordDisplay
		with io.github.mtrevisan.familylegacy.ui.handlers.HandlerRecordDisplay;

	// The selection dialog finds the views of the projections it uses
	uses io.github.mtrevisan.familylegacy.ui.components.searches.service.SelectionViewProvider;
	provides io.github.mtrevisan.familylegacy.ui.components.searches.service.SelectionViewProvider
		with io.github.mtrevisan.familylegacy.ui.components.projections.selection.ProjectionSelectionViews;
}
