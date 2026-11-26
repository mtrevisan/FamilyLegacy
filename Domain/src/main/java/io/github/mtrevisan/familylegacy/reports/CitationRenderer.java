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

import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Renders the "extra info" blocks attached to any record: notes, source
 * citations, evidence qualifiers, privacy block and the audit trail.
 *
 * <p>Every method respects the report's {@link PrivacyPolicy} — including
 * the privacy declared on the citation node itself — and the
 * {@link ReportConfig} flags.</p>
 */
public final class CitationRenderer{

	/* ======================================================================
	 *                          Tags
	 * ====================================================================== */

	private static final String TAG_SOURCE = SourceCitationReader.TAG_SOURCE;
	private static final String TAG_NOTE = EventParticipationReader.TAG_NOTE;
	private static final String TAG_EVIDENCE = EvidenceQualifiersReader.TAG_EVIDENCE;

	private static final String TAG_AUDIT = AuditReader.TAG_AUDIT;

	private static final String TAG_PRIVACY = EventReader.TAG_PRIVACY;


	/** How many characters a line of the report holds, to tell how many lines a text takes. */
	private static final int LINE_WIDTH = 90;
	/** The level of the heading of a story of the person the chapter is about: a section of the chapter. */
	private static final int OWN_STORY_LEVEL = 2;
	/** The level of the heading of a story of something in the chapter (an event, a relationship...): under the section it is in. */
	private static final int NESTED_STORY_LEVEL = 3;

	/** A link of the text of a note, to a record ({@code [text](flef:I12)}) or the marker of confidential text; the target is the second group. */
	private static final Pattern TEXT_LINK = Pattern.compile("\\[([^\\]]*)\\]\\(flef:([^)\\s]+)\\)");
	/** The target of a link that marks confidential text. */
	private static final String CONFIDENTIAL_TARGET = "confidential";
	/** What stands for a record that the privacy policy hides. */
	private static final String OMITTED = "[omitted]";


	private final ReportContext ctx;
	/** Whether the texts that the options send to a section of their own are kept for it (only the chapter of a person has such a section). */
	private boolean deferring;
	private int deferredNoteCount;
	private int deferredDetailCount;
	private final List<ReportSection> deferredNotes = new ArrayList<>();
	private final List<ReportSection> deferredStories = new ArrayList<>();
	private final List<String> deferredDetails = new ArrayList<>();


	public CitationRenderer(final ReportContext ctx){
		this.ctx = ctx;
	}


	/* ======================================================================
	 *                          Texts of the section
	 * ====================================================================== */

	/**
	 * From now on the notes, and the text of the sources, that the placement of the report sends to a section of their own are not written
	 * where they belong (a reference is), but kept for {@link #takeDeferredNotes()} and {@link #takeDeferredDetails()}. A section that
	 * does not write them must not call this: they would be lost.
	 */
	void startDeferring(){
		deferring = true;
		deferredNoteCount = 0;
		deferredDetailCount = 0;
		deferredNotes.clear();
		deferredStories.clear();
		deferredDetails.clear();
	}

	/** Ends what {@link #startDeferring()} started: the sections after the one that wrote the texts have no place for them. */
	void stopDeferring(){
		deferring = false;
		deferredNotes.clear();
		deferredStories.clear();
		deferredDetails.clear();
	}

	/** @return	The notes of a record, all of them where they are told, whatever the placement: they are the notes of the section itself. */
	List<ReportSection> notesInPlace(final FLEFRecord rec){
		final boolean was = deferring;
		deferring = false;
		try{
			return notes(rec, false);
		}
		finally{
			deferring = was;
		}
	}

	/** @return	The stories of the events, the attributes and the relationships that were kept for the section of the stories. */
	List<ReportSection> takeDeferredStories(){
		final List<ReportSection> out = List.copyOf(deferredStories);
		deferredStories.clear();
		return out;
	}

	/** @return	The notes that were kept for the section of the notes, numbered as their references are. */
	List<ReportSection> takeDeferredNotes(){
		final List<ReportSection> out = List.copyOf(deferredNotes);
		deferredNotes.clear();
		return out;
	}

	/** @return	The texts of the sources that were kept for the section of the citations, numbered as their references are. */
	List<String> takeDeferredDetails(){
		final List<String> out = List.copyOf(deferredDetails);
		deferredDetails.clear();
		return out;
	}

	/**
	 * @param text	A text.
	 * @return	How many lines the text takes: the ones it has, a long one counted as many as it takes to write it.
	 */
	static int lines(final String text){
		if(text == null || text.isBlank())
			return 0;

		int lines = 0;
		for(final String line : StringUtils.splitPreserveAllTokens(text.strip(), '\n'))
			lines += Math.max(1, (line.length() + LINE_WIDTH - 1) / LINE_WIDTH);
		return lines;
	}


	/* ======================================================================
	 *                          Notes
	 * ====================================================================== */

	/**
	 * Note blocks for a record. Each note is rendered as its text, followed
	 * by:
	 * <ul>
	 *   <li>an optional metadata suffix with the note's {@code mime} and
	 *       {@code locale};</li>
	 *   <li>its title (when present);</li>
	 *   <li>its translations (when present);</li>
	 *   <li>the sources cited by the note itself (when present).</li>
	 * </ul>
	 *
	 * <p>Notes whose privacy level is hidden by the current policy are
	 * silently skipped.</p>
	 */
	public List<ReportSection> notes(final FLEFRecord rec){
		return notes(rec, true);
	}

	/**
	 * @param withStories	Whether the stories of the record are told where the placement of the report puts them (they are told by the
	 * 	section of the record otherwise, with {@link #storiesOf(FLEFRecord)}).
	 */
	private List<ReportSection> notes(final FLEFRecord rec, final boolean withStories){
		final List<ReportSection> out = new ArrayList<>();
		for(final FLEFRecord n : ctx.visibleChildren(rec, TAG_NOTE)){
			if(!ctx.isVisible(n))
				continue;

			//a story is not a note: it is chosen apart, and told as a text with its title
			if(NoteReader.isStory(n)){
				if(withStories)
					storyInPlace(n, out);
				continue;
			}
			if(!ctx.config.notes())
				continue;
			//the research notes are working material of the research: a report can leave them out
			if(!ctx.config.researchNotes() && NoteReader.isResearch(n))
				continue;

			final String text = noteBody(n);
			final String suffix = noteMetadataSuffix(n);
			//the title of a note is its label ("Agency: Ospedale ..."), not a separate line after the text
			final String title = ReportFormatters.noteTitle(n);
			final boolean hasTitle = (title != null && !title.isBlank());
			//a note the placement sends to the section is told there, under a number, and the place of the note has the number only
			final boolean section = (deferring && ctx.config.notesPlacement().inSection(lines(text), ctx.config.longTextLines()));
			final String number = (section? StringUtils.SPACE + (deferredNoteCount + 1): StringUtils.EMPTY);
			final String label = (section? ctx.labels.sections().note() + number + (hasTitle? " — " + ReportFormatters.escape(title): StringUtils.EMPTY)
				: (hasTitle? ReportFormatters.escape(title): ctx.labels.sections().note()));

			final List<ReportSection> block = new ArrayList<>();
			//a text of several paragraphs is several paragraphs: the label opens the first, the metadata closes the last
			final List<String> paragraphs = paragraphsOf(text);
			for(int i = 0; i < paragraphs.size(); i ++)
				block.add(new ReportSection.Paragraph((i == 0? "**" + label + ":** ": StringUtils.EMPTY)
					+ ReportFormatters.escape(paragraphs.get(i)) + (i == paragraphs.size() - 1? suffix: StringUtils.EMPTY)));

			for(final String tr : ReportFormatters.noteTranslations(n, ctx.labels))
				block.add(new ReportSection.Paragraph(tr));

			// Sources cited by the note itself.
			if(ctx.config.sources())
				block.addAll(citations(n));

			if(section){
				deferredNoteCount ++;
				deferredNotes.addAll(block);
				out.add(new ReportSection.Paragraph("*[" + ctx.labels.sections().note() + number + "]*"));
			}
			else
				out.addAll(block);
		}
		return out;
	}

	/**
	 * Tells a story of a record where the placement puts it: right after the record, or (where the chapter of a person has a section for
	 * them) kept for that section.
	 */
	private void storyInPlace(final FLEFRecord story, final List<ReportSection> out){
		switch(ctx.config.stories()){
			case EXCLUDE -> {
				//left out
			}
			case INLINE -> out.addAll(storyBlocks(story, NESTED_STORY_LEVEL));
			case SECTION -> {
				if(deferring)
					deferredStories.addAll(storyBlocks(story, NESTED_STORY_LEVEL));
				else
					out.addAll(storyBlocks(story, NESTED_STORY_LEVEL));
			}
		}
	}

	/**
	 * @param rec	A record.
	 * @return	The stories of the record, each with its title as a heading of the level of the sections of a chapter, and its text in normal
	 * 	paragraphs (the privacy policy applies; empty if the report leaves the stories out).
	 */
	List<ReportSection> storiesOf(final FLEFRecord rec){
		final List<ReportSection> out = new ArrayList<>();
		if(ctx.config.stories() == StoryPlacement.EXCLUDE)
			return out;

		for(final FLEFRecord n : ctx.visibleChildren(rec, TAG_NOTE))
			if(NoteReader.isStory(n) && ctx.isVisible(n))
				out.addAll(storyBlocks(n, OWN_STORY_LEVEL));
		return out;
	}

	/**
	 * @param rec	The record a report is about.
	 * @return	Its notes, and then its stories where the report puts them: for the reports that have no chapter of a person, with no
	 * 	deferring (the stories are a section of their own, or follow the notes).
	 */
	List<ReportSection> rootNotes(final FLEFRecord rec){
		final List<ReportSection> out = new ArrayList<>(notesInPlace(rec));
		out.addAll(rootStories(rec));
		return out;
	}

	/**
	 * @param rec	The record a report is about.
	 * @return	Its stories as the report puts them: in a section of their own, with its heading, or without it (inline); nothing if the report
	 * 	leaves the stories out or the record has none.
	 */
	List<ReportSection> rootStories(final FLEFRecord rec){
		final List<ReportSection> own = storiesOf(rec);
		if(own.isEmpty() || ctx.config.stories() != StoryPlacement.SECTION)
			return own;

		final List<ReportSection> out = new ArrayList<>(own.size() + 1);
		out.add(new ReportSection.Heading(OWN_STORY_LEVEL, ctx.labels.sections().stories()));
		out.addAll(own);
		return out;
	}

	/**
	 * A story is written as a document: its title as a heading (if it has one) and then its text as blocks (paragraphs, and for a story in
	 * Markdown the headings, the lists and the rules), without the label and the technical metadata (the media type, the locale) of a note;
	 * then its translations and the sources it cites.
	 */
	private List<ReportSection> storyBlocks(final FLEFRecord story, final int level){
		final List<ReportSection> block = new ArrayList<>();
		final String title = ReportFormatters.noteTitle(story);
		if(title != null)
			block.add(new ReportSection.Heading(level, ReportFormatters.escape(title)));

		final String mime = FLEFRecordHelper.getChildValue(story, NoteReader.TAG_MIME);
		block.addAll(StoryBlocks.parse(noteBody(story), (mime != null && mime.contains("markdown")), level, title != null));
		for(final String translation : ReportFormatters.noteTranslations(story, ctx.labels))
			block.add(new ReportSection.Paragraph(translation));
		if(ctx.config.sources())
			block.addAll(citations(story));
		return block;
	}

	/**
	 * The text of a note as the report writes it: an HTML note is turned into text (its tags would be shown as they are), the links that
	 * mark confidential text are dropped when the privacy policy hides it (the text is kept otherwise), and the links to a record that the
	 * policy hides are replaced by a placeholder, so that the name of the record is not leaked; the links to the other records
	 * ({@code flef:Id}) become the internal links of the report ({@code @Id@}).
	 */
	private String noteBody(final FLEFRecord note){
		final String body = ReportFormatters.noteBody(note);
		if(body == null || body.isEmpty())
			return StringUtils.EMPTY;

		final boolean hideConfidential = (ctx.config.privacyPolicy() != PrivacyPolicy.SHOW_ALL);
		final Matcher matcher = TEXT_LINK.matcher(body);
		final StringBuilder sb = new StringBuilder(body.length());
		while(matcher.find()){
			final String id = matcher.group(2);
			final String replacement;
			if(CONFIDENTIAL_TARGET.equalsIgnoreCase(id))
				replacement = (hideConfidential? StringUtils.EMPTY: matcher.group(1));
			else{
				final FLEFRecord target = ctx.model.getRecordById(id);
				replacement = (target != null && !ctx.isVisible(target)
					? OMITTED
					: "[" + matcher.group(1) + "](@" + id + "@)");
			}
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/** @return	The paragraphs of a text (separated by blank lines), at least one (empty if the text is). */
	private static List<String> paragraphsOf(final String text){
		final List<String> paragraphs = new ArrayList<>(1);
		for(final String paragraph : StringUtils.splitByWholeSeparator(Strings.CS.replace(text, "\r\n", "\n"), "\n\n")){
			final String stripped = paragraph.strip();
			if(!stripped.isEmpty())
				paragraphs.add(stripped);
		}
		if(paragraphs.isEmpty())
			paragraphs.add(StringUtils.EMPTY);
		return paragraphs;
	}

	/**
	 * Builds a small suffix such as {@code " — (text/markdown, it)"} that
	 * records the note's declared MIME type and locale. Returns an empty
	 * string when neither field is present.
	 */
	private String noteMetadataSuffix(final FLEFRecord note){
		final String mime = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_MIME);
		final String locale = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_LOCALE);
		final List<String> tags = new ArrayList<>(2);
		if(mime != null && !mime.isBlank()) tags.add(mime.trim());
		if(locale != null && !locale.isBlank()) tags.add(locale.trim());
		if(tags.isEmpty())
			return StringUtils.EMPTY;

		return " — *(" + ReportFormatters.escape(String.join(", ", tags)) + ")*";
	}


	/* ======================================================================
	 *                          Citations
	 * ====================================================================== */

	/**
	 * Citation blocks for a record. Each citation shows the source title,
	 * the locator (if any) and the extracted text (if any). Citations whose
	 * own privacy level is hidden by the current policy are skipped, even
	 * when the referenced source is visible.
	 */
	public List<ReportSection> citations(final FLEFRecord rec){
		final List<ReportSection> out = new ArrayList<>();
		final List<String> items = new ArrayList<>();
		final List<FLEFRecord> cites = principal(ctx.visibleChildren(rec, TAG_SOURCE));
		if(cites.isEmpty())
			return out;

		for(final FLEFRecord c : cites){
			// Filter on the citation's own privacy block (item 6).
			if(!ctx.isVisible(c))
				continue;

			final String sid = ReportFormatters.extractSourceId(c);
			if(sid == null)
				continue;
			if(!ctx.isVisible(ctx.model.getRecordById(sid)))
				continue;

			final StringBuilder sb = new StringBuilder();
			if(SourceCitationReader.isContradicting(c))
				sb.append("**[").append(ctx.labels.sections().stanceContradicts()).append("]** ");
			sb.append(ReportFormatters.escape(ctx.sourceTitle(sid)));

			final String title = sb.toString();
			final String locator = FLEFRecordHelper.getChildValue(c, SourceCitationReader.TAG_LOCATOR);
			if(locator != null && ctx.config.sourceLocator())
				sb.append(" — *").append(ctx.labels.sections().sourceLocator()).append(":* ").append(ReportFormatters.escape(locator));

			//what the source says (the text of the extract, its note, its crop): it may go to the section of the citations
			final StringBuilder detail = new StringBuilder();
			final StringBuilder plainDetail = new StringBuilder();

			//the extracts are records of their own, with their own privacy
			final List<FLEFRecord> extracts = ctx.visible(ExtractReader.extractsOf(c, ctx.model));
			final FLEFRecord textExtract = extracts.stream()
				.filter(extract -> ExtractReader.extractText(extract) != null)
				.findFirst()
				.orElse(null);
			final String extractText = (textExtract != null? ExtractReader.extractText(textExtract): null);
			if(extractText != null){
				plainDetail.append(extractText);
				detail.append(" — *").append(ctx.labels.sections().extractText()).append(":* \"")
					.append(ReportFormatters.escape(extractText)).append('"');

				final String extractType = ExtractReader.extractType(textExtract);
				final String extractLocale = ExtractReader.extractLocale(textExtract);
				final List<String> quals = new ArrayList<>();
				if(extractType != null && !extractType.isBlank())
					quals.add(ReportFormatters.orEmpty(
						ctx.labels.enumValue("enum.extract_type", extractType)));
				if(extractLocale != null && !extractLocale.isBlank())
					quals.add(extractLocale);
				if(!quals.isEmpty())
					detail.append(" *(").append(ReportFormatters.escape(
						String.join(", ", quals))).append(")*");
			}

			// Extract note.
			final String extractNote = ReportFormatters.extractNote(extracts);
			if(extractNote != null){
				plainDetail.append('\n').append(extractNote);
				detail.append(" — *").append(ctx.labels.sections().extractNote()).append(":* ")
					.append(ReportFormatters.escape(extractNote));
			}

			// Extract crop.
			final String crop = ReportFormatters.extractCrop(extracts);
			if(crop != null){
				plainDetail.append('\n').append(crop);
				detail.append(" — *").append(ctx.labels.sections().extractCrop()).append(":* ")
					.append(ReportFormatters.escape(crop));
			}

			if(!detail.isEmpty() && deferring && ctx.config.sourcesPlacement().inSection(lines(plainDetail.toString()), ctx.config.longTextLines())){
				//the citation keeps the source and the locator, and the number of the text that is told in the section
				deferredDetailCount ++;
				final String mark = ctx.labels.sections().extractText() + StringUtils.SPACE + deferredDetailCount;
				deferredDetails.add("**" + mark + ":** " + title + detail);
				sb.append(" — *(").append(mark).append(")*");
			}
			else
				sb.append(detail);

			items.add(sb.toString());

			//the evidence of a citation comes right after it: the citations before it make a list of their own
			if(ctx.config.evidence()){
				flush(out, items);
				addEvidence(c, out);
			}
		}
		flush(out, items);
		return out;
	}

	/**
	 * @param citations	The citations of a record.
	 * @return	Every citation, or (if the report cites the principal sources only) the ones that support the record best: of the citations that
	 * 	support it (do not contradict it) the ones with the highest weight, see {@link SourceCitationReader#citationWeight(FLEFRecord)}. A
	 * 	citation that contradicts the record is always kept: leaving out what disagrees would mislead the reader.
	 */
	private List<FLEFRecord> principal(final List<FLEFRecord> citations){
		if(!ctx.config.principalSourcesOnly() || citations.size() < 2)
			return citations;

		int best = Integer.MIN_VALUE;
		for(final FLEFRecord citation : citations)
			best = Math.max(best, SourceCitationReader.citationWeight(citation));
		final List<FLEFRecord> out = new ArrayList<>(citations.size());
		for(final FLEFRecord citation : citations){
			final int weight = SourceCitationReader.citationWeight(citation);
			if(weight == best || weight < 0)
				out.add(citation);
		}
		return out;
	}

	/** Writes the citations collected so far as one list, and starts another. */
	private static void flush(final List<ReportSection> out, final List<String> items){
		if(items.isEmpty())
			return;

		out.add(new ReportSection.BulletList(List.copyOf(items)));
		items.clear();
	}



	/* ======================================================================
	 *                          Evidence
	 * ====================================================================== */

	public void addEvidence(final FLEFRecord owner, final List<ReportSection> out){
		final FLEFRecord ev = FLEFRecordHelper.findChild(owner, TAG_EVIDENCE);
		if(ev == null)
			return;

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceSourceType(),
			FLEFRecordHelper.getChildValue(ev, EvidenceQualifiersReader.TAG_SOURCE_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceInformationType(),
			FLEFRecordHelper.getChildValue(ev, EvidenceQualifiersReader.TAG_INFORMATION_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceType(),
			FLEFRecordHelper.getChildValue(ev, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE));

		if(!rows.isEmpty())
			out.add(new ReportSection.Paragraph(
				"*" + ctx.labels.sections().evidence() + ":* " + String.join(", ", rows)));
	}


	/* ======================================================================
	 *                          Participation details
	 * ====================================================================== */

	public List<ReportSection> participationDetails(final FLEFRecord participation){
		final List<ReportSection> out = new ArrayList<>();
		if(participation == null)
			return out;

		//the notes of the participation, independent from the event, are shown once, as every other note (the ones hidden by the privacy
		//policy are not)
		if(ctx.writesNotes())
			out.addAll(notes(participation));
		if(ctx.config.sources())
			out.addAll(citations(participation));
		if(ctx.config.evidence())
			addEvidence(participation, out);
		out.addAll(audit(participation));
		out.addAll(privacyInfo(participation));
		return out;
	}


	/* ======================================================================
	 *                          Privacy details
	 * ====================================================================== */

	public List<ReportSection> privacyInfo(final FLEFRecord rec){
		if(!ctx.config.showPrivacyDetails())
			return List.of();

		final FLEFRecord privacy = FLEFRecordHelper.findChild(rec, TAG_PRIVACY);
		if(privacy == null)
			return List.of();

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().privacyLevel(),
			PrivacyReader.extractLevel(privacy));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().privacyReason(),
			PrivacyReader.extractReason(privacy));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().privacyExpires(),
			PrivacyReader.extractExpires(privacy));
		if(rows.isEmpty())
			return List.of();

		return List.of(new ReportSection.Paragraph(
			"*" + ctx.labels.sections().privacy() + ":* " + String.join(", ", rows)));
	}


	/* ======================================================================
	 *                          Audit
	 * ====================================================================== */

	public List<ReportSection> audit(final FLEFRecord rec){
		if(!ctx.config.audit() || ctx.isRedacted(rec))
			return List.of();

		final FLEFRecord auditNode = FLEFRecordHelper.findChild(rec, TAG_AUDIT);
		if(auditNode == null)
			return List.of();

		final List<String> rows = new ArrayList<>();

		final FLEFRecord creation = FLEFRecordHelper.findChild(auditNode, AuditReader.TAG_CREATION);
		if(creation != null)
			appendAuditRow(rows, ctx.labels.sections().auditCreated(), creation);

		for(final FLEFRecord update : FLEFRecordHelper.findChildren(auditNode, AuditReader.TAG_UPDATE))
			appendAuditRow(rows, ctx.labels.sections().auditUpdated(), update);

		if(rows.isEmpty())
			return List.of();
		return List.of(new ReportSection.BulletList(rows));
	}

	private void appendAuditRow(final List<String> rows, final String label,
		final FLEFRecord entry){
		final String date = FLEFRecordHelper.getChildValue(entry, AuditReader.TAG_DATE);
		if(StringUtils.isEmpty(date))
			return;

		final StringBuilder sb = new StringBuilder("**").append(label).append(":** ")
			.append(ReportFormatters.escape(date.trim()));
		final String comment = FLEFRecordHelper.getChildValue(entry, AuditReader.TAG_COMMENT);
		if(comment != null && !comment.isBlank())
			sb.append(" — ").append(ReportFormatters.escape(comment.trim()));
		rows.add(sb.toString());
	}

}
