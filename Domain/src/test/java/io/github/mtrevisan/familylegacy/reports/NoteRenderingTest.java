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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** How a note is written in a report: its body (HTML, confidential text, links, paragraphs), and that the narrative does not repeat it. */
class NoteRenderingTest{

	@Test
	void anHtmlNoteLosesItsTags(){
		final FLEFModel model = model("<p>Emigrated in <b>1890</b>.</p>", "text/html");

		final String text = paragraphs(model, PrivacyPolicy.SHOW_ALL);

		Assertions.assertFalse(text.contains("<"), text);
		Assertions.assertTrue(text.contains("Emigrated in 1890."), text);
	}

	@Test
	void confidentialTextIsDroppedWhenThePolicyHidesIt(){
		final FLEFModel model = model("He was [born at home](flef:confidential) near Treviso.", "text/markdown");

		final String hiding = paragraphs(model, PrivacyPolicy.HIDE_CONFIDENTIAL);
		Assertions.assertFalse(hiding.contains("born at home"), hiding);
		Assertions.assertFalse(hiding.contains("flef:"), hiding);
		Assertions.assertTrue(hiding.contains("near Treviso"), hiding);

		final String showing = paragraphs(model, PrivacyPolicy.SHOW_ALL);
		Assertions.assertTrue(showing.contains("He was born at home near Treviso."), showing);
		Assertions.assertFalse(showing.contains("flef:"), showing);
	}

	@Test
	void aLinkToAHiddenRecordIsAPlaceholder(){
		final FLEFModel model = model("Brother of [Mario](flef:I2) and [Anna](flef:I3).", "text/markdown");
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PRIVACY)
				.addChild(FLEFRecord.createChildWithTagAndValue(PrivacyReader.TAG_LEVEL, PrivacyReader.ENUM_LEVEL_CONFIDENTIAL))));
		model.addRecord(FLEFRecord.createMainRecord("I3", IndividualReader.TYPE));

		final String hiding = paragraphs(model, PrivacyPolicy.HIDE_CONFIDENTIAL);
		Assertions.assertFalse(hiding.contains("Mario"), hiding);
		Assertions.assertTrue(hiding.contains("[omitted]"), hiding);
		Assertions.assertTrue(hiding.contains("[Anna](@I3@)"), hiding);

		final String showing = paragraphs(model, PrivacyPolicy.SHOW_ALL);
		Assertions.assertTrue(showing.contains("[Mario](@I2@)"), showing);
	}

	@Test
	void aTextOfSeveralParagraphsIsSeveralParagraphs(){
		final FLEFModel model = model("First paragraph.\n\nSecond paragraph.\r\n\r\nThird.", "text/markdown");

		final List<ReportSection> sections = notes(model, PrivacyPolicy.SHOW_ALL);

		Assertions.assertEquals(3, sections.size());
		Assertions.assertTrue(((ReportSection.Paragraph)sections.get(0)).text().endsWith("First paragraph."));
		Assertions.assertEquals("Second paragraph.", ((ReportSection.Paragraph)sections.get(1)).text());
		Assertions.assertTrue(((ReportSection.Paragraph)sections.get(2)).text().startsWith("Third."));
	}

	@Test
	void theNarrativeDoesNotRepeatTheNotes(){
		final FLEFModel model = model("A note that is not narrated.", "text/markdown");
		final ReportContext ctx = context(model, PrivacyPolicy.SHOW_ALL);

		final String narrative = new IndividualNarrator(ctx.model, ctx.index, ctx.labels)
			.narrate(model.getRecordById("I1"));

		Assertions.assertFalse(narrative.contains("A note that is not narrated."), narrative);
	}


	@Test
	void theResearchNotesCanBeLeftOutWhileTheOtherNotesStay(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, "A plain remark.")))
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, NoteReader.ENUM_KIND_RESEARCH))
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, "Reasoning of the research."))));
		final ReportConfig base = ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL)
			.withLivingPrivacy(LivingPrivacy.SHOW)
			.with(ReportOption.NOTES, true);

		final String with = texts(model, base.with(ReportOption.RESEARCH_NOTES, true));
		Assertions.assertTrue(with.contains("A plain remark.") && with.contains("Reasoning of the research."), with);

		final String without = texts(model, base.with(ReportOption.RESEARCH_NOTES, false));
		Assertions.assertTrue(without.contains("A plain remark."), without);
		Assertions.assertFalse(without.contains("Reasoning of the research."), without);

		//(the research notes are notes: without the notes there are none)
		Assertions.assertEquals("", texts(model, base.with(ReportOption.NOTES, false).with(ReportOption.RESEARCH_NOTES, true)));
	}


	private static FLEFModel model(final String text, final String mime){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text))
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, mime))));
		return model;
	}

	private static ReportContext context(final FLEFModel model, final PrivacyPolicy policy){
		final ReportConfig config = ReportConfig.uniform(false, ReportLanguage.ENGLISH, policy)
			.withLivingPrivacy(LivingPrivacy.SHOW)
			.with(ReportOption.NOTES, true);
		return ReportContext.build(model, model.getRecordById("I1"), config);
	}

	private static List<ReportSection> notes(final FLEFModel model, final PrivacyPolicy policy){
		return context(model, policy).citations()
			.notes(model.getRecordById("I1"));
	}

	private static String texts(final FLEFModel model, final ReportConfig config){
		final StringBuilder sb = new StringBuilder();
		for(final ReportSection section : ReportContext.build(model, model.getRecordById("I1"), config).citations()
				.notes(model.getRecordById("I1")))
			sb.append(((ReportSection.Paragraph)section).text()).append((char)10);
		return sb.toString();
	}

	private static String paragraphs(final FLEFModel model, final PrivacyPolicy policy){
		final StringBuilder sb = new StringBuilder();
		for(final ReportSection section : notes(model, policy))
			sb.append(((ReportSection.Paragraph)section).text()).append('\n');
		return sb.toString();
	}

}
