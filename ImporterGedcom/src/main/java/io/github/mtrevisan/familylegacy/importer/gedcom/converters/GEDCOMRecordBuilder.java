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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.PendingFile;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMImportContext;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class GEDCOMRecordBuilder{

	/** A name followed by its phonemic transcription between slashes, e.g. {@code Dorato /do.ˈra.to/}. */
	private static final Pattern NAME_WITH_PHONETIC = Pattern.compile("^(?<name>[^/]+?)\\s*(?<phonetic>/[^/]+/)$");


	/** The types of the events covered by the sources (DATA.EVEN), by id of the source. */
	private static final Map<String, Set<String>> SOURCE_EVENT_TYPES = new HashMap<>();


	private GEDCOMRecordBuilder(){}


	public static void transferValue(FLEFRecord parent, String tag, GEDCOMNode node){
		if(node != null && StringUtils.isNotEmpty(node.getValue()))
			parent.addChild(FLEFRecord.createChildWithTagAndValue(tag, node.getValue()));
	}


	/*
	NOTE_STRUCTURE :=
	[
	n NOTE @<XREF:NOTE>@    {1:1}
	|
	n NOTE [ <SUBMITTER_TEXT> | <NULL> ]    {1:1}
	  +1 [ CONC | CONT ] <SUBMITTER_TEXT>    {0:M}
	]
	*/
	public static void attachNote(FLEFRecord parent,
			FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap){
		attachNote(parent, model, node, noteRawMap, null);
	}

	/**
	 * Attaches a NoteStructure built from a NOTE_STRUCTURE (or from any GEDCOM line whose text is a note).
	 *
	 * @param title	The title of the note, may be {@code null}.
	 */
	public static void attachNote(FLEFRecord parent,
			FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, String title){
		if(parent == null || node == null || StringUtils.isBlank(node.getValue()))
			return;

		final GEDCOMNode noteNode = resolveNote(node, noteRawMap);
		if(noteNode == null)
			return;

		final FLEFRecord note = NoteBuilder.createNoteStruct(title, GEDCOMHelper.extractFullText(noteNode), noteNode);
		if(note == null)
			return;

		attachNoteFormat(note, noteNode);
		attachNoteSources(note, model, noteNode, noteRawMap);

		final String noteSig = Deduplicator.computeSignature(note);
		for(final FLEFRecord child : parent.getChildren()){
			if(NoteReader.TAG_NOTE.equalsIgnoreCase(child.getTag()) && Deduplicator.computeSignature(child).equals(noteSig))
				return;
		}

		parent.addChild(note);
	}

	/**
	 * Attaches the source citations of a NOTE (record or structure) to the NoteStructure, before its audit.
	 * <p>
	 * A citation of a source record not (yet) in the model is skipped, to not create a placeholder that the conversion of the
	 * source would then clash with (it happens for the notes of the records converted before the sources).
	 */
	private static void attachNoteSources(final FLEFRecord note, final FLEFModel model, final GEDCOMNode noteNode,
			final Map<String, GEDCOMNode> noteRawMap){
		if(model == null)
			return;

		final FLEFRecord citations = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE);
		for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(noteNode, GEDCOMMapper.TAG_SOURCE)){
			final String sourceId = GEDCOMHelper.pointer(sourNode.getValue());
			if(sourceId == null || model.hasRecord(sourceId))
				attachSource(citations, model, sourNode, noteRawMap);
		}
		// a note cannot have notes: those of the citations (see attachSource) are appended to its text
		final List<FLEFRecord> citationNotes = FLEFRecordHelper.findChildren(citations, NoteReader.TAG_NOTE);
		if(!citationNotes.isEmpty()){
			citations.getChildren()
				.removeAll(citationNotes);
			final FLEFRecord textRecord = FLEFRecordHelper.findChild(note, NoteReader.TAG_TEXT);
			final StringBuilder text = new StringBuilder(textRecord.getValue());
			for(final FLEFRecord citationNote : citationNotes)
				text.append(StringUtils.LF).append(FLEFRecordHelper.getChildValue(citationNote, NoteReader.TAG_TEXT));
			textRecord.setValue(text.toString());
		}
		note.getChildren()
			.addAll(note.getChildren().size() - 1, citations.getChildren());
	}

	/**
	 * Returns the NOTE record a NOTE structure points to, or the structure itself when it holds the text.
	 *
	 * @return	The node holding the text of the note, or {@code null} if the record does not exist.
	 */
	public static GEDCOMNode resolveNote(final GEDCOMNode node, final Map<String, GEDCOMNode> noteRawMap){
		final String noteId = (node != null? GEDCOMHelper.pointer(node.getValue()): null);
		if(noteId == null)
			return node;

		return (noteRawMap != null? noteRawMap.get(noteId): null);
	}

	/*
	(GEDCOM 7)
	n NOTE <Text>
	  +1 MIME <MediaType>    {0:1}
	  +1 LANG <Language>    {0:1}
	  +1 TRAN <Text>    {0:M}
	    +2 MIME <MediaType>    {0:1}
	    +2 LANG <Language>    {0:1}
	*/
	/** Attaches the media type, the language, and the translations of a note, before its audit. */
	private static void attachNoteFormat(final FLEFRecord note, final GEDCOMNode noteNode){
		final List<FLEFRecord> format = new ArrayList<>();
		final String mime = GEDCOMHelper.findFirstValue(noteNode, "MIME");
		if(mime != null)
			format.add(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, mime));
		final String locale = GEDCOMHelper.findFirstValue(noteNode, GEDCOMMapper.TAG_LANGUAGE);
		if(locale != null)
			format.add(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_LOCALE, locale));
		for(final GEDCOMNode tranNode : GEDCOMHelper.findChildren(noteNode, GEDCOMMapper.TAG_TRANSLATION)){
			final String text = StringUtils.trimToNull(GEDCOMHelper.extractFullText(tranNode));
			if(text == null)
				continue;

			final FLEFRecord translation = FLEFRecord.createChildWithTag(NoteReader.TAG_TRANSLATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text));
			final String translationLocale = GEDCOMHelper.findFirstValue(tranNode, GEDCOMMapper.TAG_LANGUAGE);
			if(translationLocale != null)
				translation.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_LOCALE, translationLocale));
			format.add(translation);
		}
		note.getChildren()
			.addAll(note.getChildren().size() - 1, format);
	}

	/*
	PERSONAL_NAME_STRUCTURE :=
	n NAME <NAME_PERSONAL>    {1:1}
	  +1 TYPE <NAME_TYPE>    {0:1}
	  +1 <<PERSONAL_NAME_PIECES>>    {0:1}
	  +1 FONE <NAME_PHONETIC_VARIATION>    {0:M}
	    +2 TYPE <PHONETIC_TYPE>    {1:1}
	    +2 <<PERSONAL_NAME_PIECES>>    {0:1}
	  +1 ROMN <NAME_ROMANIZED_VARIATION>    {0:M}
	    +2 TYPE <ROMANIZED_TYPE>    {1:1}
	    +2 <<PERSONAL_NAME_PIECES>>    {0:1}

	PERSONAL_NAME_PIECES :=
	  n NPFX, n GIVN, n NICK, n SPFX, n SURN, n NSFX    {0:1}
	  n <<NOTE_STRUCTURE>>, n <<SOURCE_CITATION>>    {0:M}
	*/
	/**
	 * Parses a PersonalNameStructure (for individuals) extracting all sub-tags
	 * (NPFX, GIVN, NICK, SPFX, SURN, NSFX), phonetic and romanized variations, inline sources, and notes.
	 */
	public static void attachPersonalNameStructure(FLEFRecord parent,
			GEDCOMNode nameNode, FLEFModel model, Map<String, GEDCOMNode> noteRawMap){
		if(nameNode == null)
			return;

		final String nameType = GEDCOMHelper.findFirstValue(nameNode, GEDCOMMapper.TAG_TYPE);
		final String mappedNameType = GEDCOMMapper.mapNameType(nameType);
		final FLEFRecord name = createPersonalName(nameNode.getValue(), nameNode, mappedNameType);
		if(name == null)
			return;

		for(GEDCOMNode foneNode : GEDCOMHelper.findChildren(nameNode, "FONE"))
			attachNameVariant(name, foneNode, TextValueVariantReader.TAG_PHONETIC, GedcomTexts.t("title.variation_phonetic"));
		for(GEDCOMNode romnNode : GEDCOMHelper.findChildren(nameNode, "ROMN"))
			attachNameVariant(name, romnNode, TextValueVariantReader.TAG_TRANSCRIPTION, GedcomTexts.t("title.variation_romanized"));
		// GEDCOM 7 replaces FONE and ROMN with translations, distinguished by their language
		for(GEDCOMNode tranNode : GEDCOMHelper.findChildren(nameNode, GEDCOMMapper.TAG_TRANSLATION))
			attachNameVariant(name, tranNode, TextValueVariantReader.TAG_TRANSCRIPTION, GedcomTexts.t("title.variation_translated"));

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(nameNode, GEDCOMMapper.TAG_SOURCE))
			attachSource(name, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(nameNode, GEDCOMMapper.TAG_NOTE))
			attachNote(name, model, noteNode, noteRawMap);

		//a type that is a description rather than a type (too many words) is kept as a note
		if(nameType != null && mappedNameType == null)
			KeptNotes.add(name, KeptNotes.TITLE_NAME_TYPE, nameType, nameNode);
		KeptNotes.keepIdentifiers(name, nameNode, GEDCOMMapper.TAG_NAME, nameNode);
		KeptNotes.keepUnmapped(name, nameNode, GEDCOMMapper.NAME_CONSUMED_TAGS, GEDCOMMapper.TAG_NAME, nameNode);

		parent.addChild(name);
	}

	/**
	 * Builds a PersonalNameStructure.
	 *
	 * @param value	The name, in GEDCOM form (e.g. {@code John /Smith/ Jr.}).
	 * @param piecesNode	The node holding the PERSONAL_NAME_PIECES, may be {@code null}.
	 * @param type	The type of the name, may be {@code null}.
	 * @return	The structure, or {@code null} if the name has no parts.
	 */
	public static FLEFRecord createPersonalName(final String value, final GEDCOMNode piecesNode, final String type){
		final Map<String, List<String>> pieces = extractNamePieces(value, piecesNode);
		if(pieces.isEmpty())
			return null;

		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME);
		if(type != null)
			name.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, type));
		for(final Map.Entry<String, List<String>> entry : pieces.entrySet())
			for(final String piece : entry.getValue())
				name.addChild(createNamePart(entry.getKey(), piece));
		return name;
	}

	/**
	 * Builds a NamePartStructure; a phonemic transcription following the value between slashes (e.g.
	 * {@code Milio /ˈmi.ljo/}) becomes its IPA variant.
	 */
	public static FLEFRecord createNamePart(final String type, final String value){
		String partValue = value;
		String phonetic = null;
		final Matcher matcher = NAME_WITH_PHONETIC.matcher(value);
		if(matcher.matches()){
			partValue = matcher.group("name");
			phonetic = matcher.group("phonetic");
		}

		final FLEFRecord part = FLEFRecord.createChildWithTag(NameReader.TAG_PART)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, type))
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, partValue));
		if(phonetic != null)
			part.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART_VARIANT)
				.addChild(FLEFRecord.createChildWithTag(TextValueVariantReader.TAG_PHONETIC)
					.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_PHONETIC_SYSTEM, "ipa"))
					.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_PHONETIC_VALUE, phonetic))));
		return part;
	}

	/**
	 * Extracts the name parts, in display order, from a name in GEDCOM form and from its PERSONAL_NAME_PIECES (which take
	 * precedence).
	 *
	 * @return	The values of the parts, by FLEF part type, in display order.
	 */
	private static Map<String, List<String>> extractNamePieces(final String value, final GEDCOMNode piecesNode){
		String given = null;
		String surname = null;
		String suffix = null;
		if(StringUtils.isNotBlank(value)){
			final int slash1 = value.indexOf('/');
			final int slash2 = (slash1 >= 0? value.indexOf('/', slash1 + 1): -1);
			if(slash2 > slash1){
				given = value.substring(0, slash1);
				surname = value.substring(slash1 + 1, slash2);
				suffix = value.substring(slash2 + 1);
			}
			else if(slash1 >= 0){
				//the closing slash is missing: the surname is what follows the opening one
				given = value.substring(0, slash1);
				surname = value.substring(slash1 + 1);
			}
			else
				given = value;
		}

		final Map<String, List<String>> pieces = new LinkedHashMap<>();
		putNamePieces(pieces, NameReader.ENUM_PART_TYPE_PREFIX, GEDCOMHelper.findFirstValue(piecesNode, "NPFX"));
		putNamePieces(pieces, NameReader.ENUM_PART_TYPE_GIVEN, ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(piecesNode, "GIVN"),
			StringUtils.trimToNull(given)));
		putNamePieces(pieces, NameReader.ENUM_PART_TYPE_NICKNAME, GEDCOMHelper.findFirstValue(piecesNode, "NICK"));
		final String surnamePrefix = GEDCOMHelper.findFirstValue(piecesNode, "SPFX");
		final String surnames = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(piecesNode, "SURN"), StringUtils.trimToNull(surname));
		if(surnames != null){
			//the surname prefix (e.g. "de", "van der") is a part of its own, just before the family name (unless the surname
			//already starts with it)
			final String[] families = StringUtils.split(surnames, ',');
			if(surnamePrefix != null && families.length > 0 && !Strings.CI.startsWith(families[0].trim(), surnamePrefix))
				putNamePieces(pieces, NameReader.ENUM_PART_TYPE_FAMILY_PREFIX, surnamePrefix);
			putNamePieces(pieces, NameReader.ENUM_PART_TYPE_FAMILY, String.join(",", families));
		}
		putNamePieces(pieces, NameReader.ENUM_PART_TYPE_SUFFIX, ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(piecesNode, "NSFX"),
			StringUtils.trimToNull(suffix)));
		return pieces;
	}

	/** Puts the comma-separated values of a name piece. */
	private static void putNamePieces(final Map<String, List<String>> pieces, final String type, final String values){
		if(values == null)
			return;

		final List<String> list = new ArrayList<>();
		for(final String piece : StringUtils.split(values, ','))
			if(StringUtils.isNotBlank(piece))
				list.add(StringUtils.normalizeSpace(piece));
		if(!list.isEmpty())
			pieces.put(type, list);
	}

	/**
	 * Attaches a phonetic, romanized, or translated (GEDCOM 7) variation of a name to the matching parts of the name.
	 * <p>
	 * A variation that cannot be split into parts is kept as a note of the name.
	 *
	 * @param kind	The kind of variant ({@code phonetic} or {@code transcription}).
	 */
	private static void attachNameVariant(final FLEFRecord name, final GEDCOMNode variantNode, final String kind,
			final String noteTitle){
		final String typeValue = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(variantNode, GEDCOMMapper.TAG_TYPE), GEDCOMHelper.findFirstValue(variantNode, GEDCOMMapper.TAG_LANGUAGE));
		final String system = ObjectUtils.firstNonNull(GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(variantNode, GEDCOMMapper.TAG_TYPE)),
			"unspecified");
		// the language of a translation tells how the name was adapted (e.g. anglicized, romanized)
		final String transcriptionType = (TextValueVariantReader.TAG_TRANSCRIPTION.equals(kind)
			? GEDCOMMapper.mapTranslationType(GEDCOMHelper.findFirstValue(variantNode, GEDCOMMapper.TAG_LANGUAGE))
			: null);

		final Map<String, List<String>> pieces = extractNamePieces(variantNode.getValue(), variantNode);
		final Map<String, Integer> occurrences = new HashMap<>();
		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(name, NameReader.TAG_PART);
		//a variation without the surname delimiters cannot be matched to the parts of a name made of more parts
		final boolean splittable = (parts.size() == 1 || StringUtils.contains(variantNode.getValue(), '/')
			|| GEDCOMHelper.findFirstChild(variantNode, "GIVN") != null || GEDCOMHelper.findFirstChild(variantNode, "SURN") != null);
		boolean attached = false;
		if(splittable)
			for(final FLEFRecord part : parts){
				final String partType = FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_TYPE);
				final int index = occurrences.merge(partType, 1, Integer::sum) - 1;
				List<String> values = pieces.get(partType);
				if(values == null && parts.size() == 1)
					values = pieces.values().stream()
						.flatMap(List::stream)
						.toList();
				if(values != null && index < values.size()){
					final FLEFRecord variant = FLEFRecord.createChildWithTag(kind)
						.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_TRANSCRIPTION_SYSTEM, system));
					if(transcriptionType != null)
						variant.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_TRANSCRIPTION_TYPE, transcriptionType));
					part.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART_VARIANT)
						.addChild(variant.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_TRANSCRIPTION_VALUE, values.get(index)))));
					attached = true;
				}
			}
		if(!attached)
			name.addChild(NoteBuilder.createNoteStruct(noteTitle + (typeValue != null? " (" + typeValue + ")": StringUtils.EMPTY),
				variantNode.getValue(), variantNode));
	}


	/*
	SOURCE_CITATION :=
	[
	n SOUR @<XREF:SOUR>@    {1:1}
	  +1 PAGE <WHERE_WITHIN_SOURCE>    {0:1}
	  +1 EVEN <EVENT_TYPE_CITED_FROM>    {0:1}
		 +2 ROLE <ROLE_IN_EVENT>    {0:1}
	  +1 DATA    {0:1}
		 +2 DATE <ENTRY_RECORDING_DATE>    {0:1}
		 +2 TEXT <TEXT_FROM_SOURCE>    {0:M}
			+3 [ CONC | CONT ] <TEXT_FROM_SOURCE>    {0:M}
	  +1 <<MULTIMEDIA_LINK>>    {0:M}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 QUAY <CERTAINTY_ASSESSMENT>    {0:1}
	|
	n SOUR <SOURCE_DESCRIPTION>    {1:1}
	  +1 [ CONC | CONT ] <SOURCE_DESCRIPTION>    {0:M}
	  +1 TEXT <TEXT_FROM_SOURCE>    {0:M}
		 +2 [ CONC | CONT ] <TEXT_FROM_SOURCE>    {0:M}
	  +1 <<MULTIMEDIA_LINK>>    {0:M}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 QUAY <CERTAINTY_ASSESSMENT>    {0:1}
	]
	 */
	/**
	 * Attaches a SourceCitation built from a SOURCE_CITATION.
	 * <p>
	 * The source records must be already in the model; a citation of an unrecorded source (a source description) creates its
	 * SourceRecord. The entry recording date goes into the date of the source (kept as is if the source is already dated),
	 * the text and the multimedia into the extracts, the notes into the remarks of the extract (or, when there is no extract, into
	 * the notes of the parent), the certainty assessment into the evidence qualifiers. The cited event is not carried over: it is
	 * the event the citation is attached to, and is used only to qualify the evidence.
	 */
	public static void attachSource(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap){
		if(node == null)
			return;

		final List<String> locator = new ArrayList<>();
		final List<FLEFRecord> extracts = new ArrayList<>();
		FLEFRecord source;
		String sourceId = GEDCOMHelper.pointer(node.getValue());
		if(sourceId != null){
			source = model.getRecordById(sourceId);
			if(source == null){
				// dangling pointer: keep the citation with a placeholder source
				source = FLEFRecord.createMainRecord(sourceId, SourceReader.TYPE)
					.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
						.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Source " + sourceId)))
					.addChild(AuditBuilder.build(node));
				model.addRecord(source);
			}
		}
		else{
			// a source description: the text is the title of an unrecorded source (GEDCOM 7 cites it as @VOID@, without text)
			final String description = ObjectUtils.firstNonNull(StringUtils.trimToNull(GEDCOMHelper.extractFullText(node)), GedcomTexts.t("text.unspecified_source"));
			source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceReader.ID_PREFIX), SourceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, description)))
				.addChild(AuditBuilder.build(node));
			sourceId = Deduplicator.getDeduplicatedRecordId(model, source);
			source = model.getRecordById(sourceId);
		}

		final String page = GEDCOMHelper.findFirstValue(node, "PAGE");
		if(page != null)
			locator.add(page);
		//the cited event (EVEN) is not carried over: it is the event the citation is attached to, and it is used only to qualify the evidence
		final GEDCOMNode evenNode = GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_GENERIC_EVENT);
		final GEDCOMNode roleNode = GEDCOMHelper.findFirstChild(evenNode, GEDCOMMapper.TAG_ROLE);
		//the role of the cited individual in the cited event has no place in a citation (for an event, it is handled with its participations)
		if(roleNode != null && StringUtils.isNotBlank(roleNode.getValue()) && !EventReader.isOfType(parent))
			discardCitedRole(roleNode, StringUtils.EMPTY);
		final GEDCOMNode dataNode = GEDCOMHelper.findFirstChild(node, "DATA");
		//the date the entry was recorded in the source is the date this source was created
		final String entryDate = GEDCOMHelper.findFirstValue(dataNode, GEDCOMMapper.TAG_DATE);
		//the locator is only the physical position in the source: a source already dated keeps its date
		if(entryDate != null){
			final FLEFRecord sourceDate = FLEFRecordHelper.findChild(source, SourceReader.TAG_DATE);
			if(sourceDate == null)
				attachDate(source, SourceReader.TAG_DATE, entryDate);
			else if(!sourceDate.equals(GEDCOMDateConverter.buildDateStructure(SourceReader.TAG_DATE, entryDate)))
				GEDCOMImportContext.discard(ImportReasons.DATE_DETAIL, GEDCOMHelper.findFirstChild(dataNode, GEDCOMMapper.TAG_DATE),
					GEDCOMMapper.TAG_SOURCE + ".DATA");
		}

		// a citation of a recorded source has its texts in DATA, a source description directly
		final List<GEDCOMNode> textNodes = new ArrayList<>(GEDCOMHelper.findChildren(node, "TEXT"));
		textNodes.addAll(GEDCOMHelper.findChildren(dataNode, "TEXT"));
		for(final GEDCOMNode textNode : textNodes){
			final String text = GEDCOMHelper.extractFullText(textNode);
			if(StringUtils.isNotBlank(text)){
				final FLEFRecord extract = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EXTRACT)
					.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_TEXT, text.trim()))
					.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_TYPE, SourceCitationReader.ENUM_EXTRACT_TYPE_VERBATIM));
				// GEDCOM 7 states the language of the text
				final String locale = GEDCOMHelper.findFirstValue(textNode, GEDCOMMapper.TAG_LANGUAGE);
				if(locale != null)
					extract.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCALE, locale));
				extracts.add(extract);
			}
		}
		for(final GEDCOMNode objeNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_MULTIMEDIA)){
			final FLEFRecord crop = buildCrop(objeNode);
			for(final String mediaId : resolveMedia(model, objeNode, noteRawMap)){
				// a media of an extract must be a media of the source
				addMediaReference(source, mediaId);
				extracts.add(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EXTRACT)
					.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_MEDIA_PART)
						.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_MEDIA, mediaId))
						.addChild(crop)));
			}
		}

		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId));
		if(!locator.isEmpty())
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, String.join(", ", locator)));
		for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_NOTE)){
			if(extracts.isEmpty())
				attachNote(parent, model, noteNode, noteRawMap, GedcomTexts.tf("text.citation_of", sourceId));
			else{
				final String noteId = GEDCOMHelper.pointer(noteNode.getValue());
				final String text = GEDCOMHelper.extractFullText(noteId != null && noteRawMap != null? noteRawMap.get(noteId): noteNode);
				if(StringUtils.isNotBlank(text))
					extracts.getFirst()
						.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_REMARK, text.trim()));
			}
		}
		for(final FLEFRecord extract : extracts)
			citation.addChild(extract);
		//GEDCOM has no way to cite a source against an assertion
		citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS));
		citation.addChild(adjustEvidence(buildEvidence(node), node, parent, sourceId));

		parent.addChild(citation);
	}

	private static void discardCitedRole(final GEDCOMNode roleNode, final String parentPath){
		GEDCOMImportContext.discard(ImportReasons.SOURCE_DETAIL, roleNode, GEDCOMImportContext.pathOf(parentPath,
			GEDCOMMapper.TAG_SOURCE + "." + GEDCOMMapper.TAG_GENERIC_EVENT));
	}

	/**
	 * Returns the role of an individual in an event, as stated by the citations of the event (the {@code SOUR.EVEN.ROLE}).
	 * <p>
	 * Only the roles in the event (witness, officiant, sponsor, ...) are taken: a kinship or an unknown role, and the roles after the
	 * first one, are reported as discarded.
	 *
	 * @param eventNode	The GEDCOM event of the individual.
	 * @return	The role, {@code null} if the citations state none.
	 */
	public static String citedEventRole(final GEDCOMNode eventNode){
		String role = null;
		for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(eventNode, GEDCOMMapper.TAG_SOURCE)){
			final GEDCOMNode roleNode = GEDCOMHelper.findFirstChild(GEDCOMHelper.findFirstChild(sourNode, GEDCOMMapper.TAG_GENERIC_EVENT),
				GEDCOMMapper.TAG_ROLE);
			if(roleNode == null || StringUtils.isBlank(roleNode.getValue()))
				continue;

			final String mapped = GEDCOMMapper.mapEventRole(roleNode.getValue());
			if(mapped == null || (role != null && !role.equals(mapped)))
				discardCitedRole(roleNode, eventNode.getTag());
			else
				role = mapped;
		}
		return role;
	}

	/**
	 * Reports, as an inconsistency of the file, the role that a citation states for the individual in its own birth, baptism, death or
	 * burial: the individual is the principal of it, so a role as a witness (or any other) cannot be the role of the participation, and
	 * it is not kept.
	 *
	 * @param eventNode	The GEDCOM event of the individual.
	 * @param role	The role that {@link #citedEventRole(GEDCOMNode)} read, as a FLEF role.
	 */
	public static void reportInconsistentCitedRole(final GEDCOMNode eventNode, final String role){
		for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(eventNode, GEDCOMMapper.TAG_SOURCE)){
			final GEDCOMNode roleNode = GEDCOMHelper.findFirstChild(GEDCOMHelper.findFirstChild(sourNode, GEDCOMMapper.TAG_GENERIC_EVENT),
				GEDCOMMapper.TAG_ROLE);
			//the roles that were not read as a role (a kinship, a second role) are already reported
			if(roleNode != null && role.equals(GEDCOMMapper.mapEventRole(roleNode.getValue())))
				GEDCOMImportContext.discard(ImportReasons.INCONSISTENT_ROLE, roleNode, GEDCOMImportContext.pathOf(eventNode.getTag(),
					GEDCOMMapper.TAG_SOURCE + "." + GEDCOMMapper.TAG_GENERIC_EVENT));
		}
	}

	/** Reports the roles stated by the citations of an event that cannot be assigned to a person (an event of a family). */
	public static void reportCitedRoles(final GEDCOMNode eventNode){
		for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(eventNode, GEDCOMMapper.TAG_SOURCE)){
			final GEDCOMNode roleNode = GEDCOMHelper.findFirstChild(GEDCOMHelper.findFirstChild(sourNode, GEDCOMMapper.TAG_GENERIC_EVENT),
				GEDCOMMapper.TAG_ROLE);
			if(roleNode != null && StringUtils.isNotBlank(roleNode.getValue()))
				discardCitedRole(roleNode, eventNode.getTag());
		}
	}

	/** Registers the types of the events a source covers (the values of its DATA.EVEN), by the id of the source. */
	public static void registerSourceEventTypes(final String sourceId, final Set<String> eventTypes){
		if(!eventTypes.isEmpty())
			SOURCE_EVENT_TYPES.put(sourceId, eventTypes);
	}

	/** Forgets the types of the events covered by the sources, call it before a conversion. */
	public static void resetSourceEventTypes(){
		SOURCE_EVENT_TYPES.clear();
	}

	/** Maps a list of GEDCOM event types (e.g. {@code BIRT, CHR}) to the FLEF types of the events and of the attributes. */
	public static Set<String> mapEventTypes(final String gedcomEventTypes){
		final Set<String> types = new HashSet<>();
		if(gedcomEventTypes != null)
			for(final String gedcomType : StringUtils.split(gedcomEventTypes, ", ;")){
				types.add(GEDCOMMapper.mapEvent(gedcomType, gedcomType));
				types.add(GEDCOMMapper.mapAttribute(gedcomType, gedcomType));
			}
		return types;
	}

	/**
	 * Adjusts the evidence qualifiers of a citation to the event it is attached to.
	 * <p>
	 * The event recorded by the cited entry (the {@code EVEN} of the citation or, if it has none, the {@code DATA.EVEN} of the
	 * source) is compared to the type of the event (or attribute, or relationship) the citation is attached to: when they are the
	 * same, the source states the event ({@code direct}); when they differ (e.g. a baptism cited for a birth, a burial for a
	 * death) the source only implies it ({@code indirect}), and its information cannot be primary. With no information on the
	 * cited event, the evidence is left as is.
	 *
	 * @param evidence	The evidence built from the QUAY, may be {@code null}.
	 * @param parent	The record the citation is attached to.
	 * @return	The evidence, may be {@code null}.
	 */
	private static FLEFRecord adjustEvidence(final FLEFRecord evidence, final GEDCOMNode citationNode, final FLEFRecord parent,
			final String sourceId){
		if(ResearchActivityReader.isOfType(parent)){
			//the sources of a 'did not occur' assertion contribute through the absence of the expected information
			final FLEFRecord negative = (evidence != null? evidence: FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE));
			final FLEFRecord type = FLEFRecordHelper.findChild(negative, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE);
			if(type != null)
				type.setValue(EvidenceQualifiersReader.EVIDENCE_TYPE_NEGATIVE);
			else
				negative.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE,
					EvidenceQualifiersReader.EVIDENCE_TYPE_NEGATIVE));
			return negative;
		}

		final boolean isEvent = EventReader.isOfType(parent);
		if(!isEvent && !IndividualAttributeReader.isOfType(parent) && !RelationshipReader.isOfType(parent))
			return evidence;

		final String type = FLEFRecordHelper.getChildValue(parent, EventReader.TAG_TYPE);
		if(type == null)
			return evidence;

		Set<String> citedTypes = mapEventTypes(GEDCOMHelper.findFirstValue(citationNode, GEDCOMMapper.TAG_GENERIC_EVENT));
		//what the source covers is known only for the events
		if(citedTypes.isEmpty() && isEvent)
			citedTypes = SOURCE_EVENT_TYPES.getOrDefault(sourceId, Set.of());
		if(citedTypes.isEmpty())
			return evidence;

		final FLEFRecord adjusted = (evidence != null? evidence: FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE));
		final FLEFRecord evidenceType = FLEFRecordHelper.findChild(adjusted, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE);
		if(citedTypes.contains(type)){
			if(evidenceType == null)
				adjusted.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE,
					EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT));
			return adjusted;
		}

		//the source is about a different event: it implies the assertion, it does not state it
		if(evidenceType != null)
			evidenceType.setValue(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT);
		else
			adjusted.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE,
				EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT));
		final FLEFRecord informationType = FLEFRecordHelper.findChild(adjusted, EvidenceQualifiersReader.TAG_INFORMATION_TYPE);
		if(informationType != null && EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY.equals(informationType.getValue()))
			informationType.setValue(EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY);
		return adjusted;
	}

	/**
	 * Maps a QUAY (CERTAINTY_ASSESSMENT) to the evidence qualifiers.
	 * <ul>
	 *   <li>0 = unreliable evidence or estimated data;</li>
	 *   <li>1 = questionable reliability of evidence;</li>
	 *   <li>2 = secondary evidence, data officially recorded sometime after event;</li>
	 *   <li>3 = direct and primary evidence used, or by dominance of the evidence.</li>
	 * </ul>
	 */
	private static FLEFRecord buildEvidence(final GEDCOMNode citationNode){
		final String quay = GEDCOMHelper.findFirstValue(citationNode, "QUAY");
		if(quay == null)
			return null;

		final FLEFRecord evidence = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE);
		switch(quay){
			case "3" -> evidence.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE,
					EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY))
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT));
			case "2" -> evidence.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE,
				EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY));
			default -> evidence.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE,
				EvidenceQualifiersReader.INFORMATION_TYPE_UNDETERMINED));
		}
		return evidence;
	}


	/*
	MULTIMEDIA_LINK :=
	[
	n OBJE @<XREF:OBJE>@    {1:1}
	|
	n OBJE    {1:1}
	  +1 FILE <MULTIMEDIA_FILE_REFN>    {1:M}
		 +2 FORM <MULTIMEDIA_FORMAT>    {1:1}
			+3 MEDI <SOURCE_MEDIA_TYPE>    {0:1}
	  +1 TITL <DESCRIPTIVE_TITLE>    {0:1}
	]
	(GEDCOM 5.5 has FORM, TITL, FILE, NOTE as siblings)
	 */
	/**
	 * Attaches a MULTIMEDIA_LINK to a record.
	 * <ul>
	 *   <li>to a SourceRecord, as one of its media;</li>
	 *   <li>to any other record, as the evidence of a SourceRecord made of the media; the preferred image of an individual
	 *   or of a group (flagged by {@code _PRIMARY} or {@code _PREF}) also becomes its {@code preferred_image}.</li>
	 * </ul>
	 */
	public static void attachMultimediaLink(FLEFRecord parent, FLEFModel model, GEDCOMNode node,
			Map<String, GEDCOMNode> noteRawMap){
		final List<String> mediaIds = resolveMedia(model, node, noteRawMap);
		if(SourceReader.isOfType(parent)){
			for(final String mediaId : mediaIds)
				addMediaReference(parent, mediaId);
			return;
		}

		//a CROP without width or height cannot be stored
		final GEDCOMNode cropNode = GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_CROP);
		if(!mediaIds.isEmpty() && cropNode != null && buildCrop(node) == null)
			GEDCOMImportContext.discard(ImportReasons.MEDIA_DETAIL, cropNode, GEDCOMMapper.TAG_MULTIMEDIA);

		final boolean preferred = ("Y".equalsIgnoreCase(GEDCOMHelper.findFirstValue(node, "_PRIMARY"))
			|| "Y".equalsIgnoreCase(GEDCOMHelper.findFirstValue(node, "_PREF")));
		final boolean canHavePreferredImage = (IndividualReader.isOfType(parent) || GroupReader.isOfType(parent));
		for(final String mediaId : mediaIds){
			if(preferred && canHavePreferredImage && FLEFRecordHelper.findChild(parent, IndividualReader.TAG_PREFERRED_IMAGE) == null)
				parent.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PREFERRED_IMAGE)
					.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_MEDIA, mediaId))
					.addChild(buildCrop(node)));

			final FLEFRecord media = model.getRecordById(mediaId);
			final String title = ObjectUtils.firstNonNull(FLEFRecordHelper.getChildValue(media, MediaReader.TAG_DESCRIPTION),
				"Media " + mediaId);
			final FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceReader.ID_PREFIX), SourceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, title)))
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_MEDIA, mediaId))
				.addChild(AuditBuilder.build(node));
			final String sourceId = Deduplicator.getDeduplicatedRecordId(model, source);

			parent.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
				.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EXTRACT)
					.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_MEDIA_PART)
						.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_MEDIA, mediaId))
						.addChild(buildCrop(node))))
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS)));
		}
	}

	/**
	 * Returns the ids of the MediaRecords a MULTIMEDIA_LINK refers to, creating those of an embedded link (one for each
	 * FILE).
	 * <p>
	 * The multimedia records must be already in the model.
	 */
	public static List<String> resolveMedia(final FLEFModel model, final GEDCOMNode node,
			final Map<String, GEDCOMNode> noteRawMap){
		final String pointer = GEDCOMHelper.pointer(node.getValue());
		final String objeId = (pointer != null? Deduplicator.resolveAlias(pointer): null);
		if(objeId != null)
			return (model.hasRecord(objeId)? List.of(objeId): List.of());

		final List<String> mediaIds = new ArrayList<>();
		final List<GEDCOMNode> fileNodes = GEDCOMHelper.findChildren(node, "FILE");
		if(fileNodes.isEmpty() && GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_BLOB) != null){
			//an embedded file (BLOB, GEDCOM 5.5)
			final FLEFRecord media = FLEFRecord.createMainRecord(IDGenerator.nextId(MediaReader.ID_PREFIX), MediaReader.TYPE);
			fillMediaRecord(media, model, node, null, noteRawMap);
			final String mediaId = Deduplicator.getDeduplicatedRecordId(model, media);
			registerEmbeddedFile(mediaId, node);
			mediaIds.add(mediaId);
		}
		for(final GEDCOMNode fileNode : fileNodes){
			final FLEFRecord media = FLEFRecord.createMainRecord(IDGenerator.nextId(MediaReader.ID_PREFIX),
				MediaReader.TYPE);
			fillMediaRecord(media, model, node, fileNode, noteRawMap);
			mediaIds.add(Deduplicator.getDeduplicatedRecordId(model, media));
		}
		return mediaIds;
	}

	/**
	 * Decodes the BLOB (GEDCOM 5.5) of a multimedia record or link, and registers it as a file that the application must write to
	 * disk and set as the uri of the media. A BLOB that cannot be decoded is reported.
	 *
	 * @param mediaId	The id of the MediaRecord.
	 */
	static void registerEmbeddedFile(final String mediaId, final GEDCOMNode objeNode){
		final GEDCOMNode blobNode = GEDCOMHelper.findFirstChild(objeNode, GEDCOMMapper.TAG_BLOB);
		if(blobNode == null)
			return;

		final byte[] data = GEDCOMBlobDecoder.decode(blobNode);
		if(data == null || data.length == 0){
			GEDCOMImportContext.discard(ImportReasons.MEDIA_DETAIL, blobNode, (objeNode.getXrefId() != null? StringUtils.EMPTY
				: GEDCOMMapper.TAG_MULTIMEDIA));
			return;
		}

		final String format = StringUtils.lowerCase(GEDCOMHelper.findFirstValue(objeNode, GEDCOMMapper.TAG_FORM));
		final StringBuilder extension = new StringBuilder();
		for(final char c : StringUtils.defaultString(format).toCharArray())
			if(Character.isLetterOrDigit(c) && c < 128)
				extension.append(c);
		final String suffix = ("jpeg".contentEquals(extension)? "jpg": (extension.isEmpty()? "bin": extension.toString()));
		GEDCOMImportContext.addPendingFile(new PendingFile(mediaId, mediaId + "." + suffix, data));
	}

	/**
	 * Fills a MediaRecord from a multimedia record or link and one of its files.
	 * <p>
	 * The kind comes from the media type (FORM.TYPE, or FORM.MEDI), else from the format (FORM), else from the extension of the
	 * file. The custom date ({@code _DATE}) is the date of the media (kept as a note when it cannot be normalized). The format,
	 * the media type, and the custom publisher ({@code _PUBL}) have no place in a MediaRecord, and become notes.
	 *
	 * @param fileNode	The FILE, may be {@code null}.
	 */
	public static void fillMediaRecord(final FLEFRecord media, final FLEFModel model, final GEDCOMNode objeNode, final GEDCOMNode fileNode,
			final Map<String, GEDCOMNode> noteRawMap){
		//the uri of a media with an embedded file (BLOB, GEDCOM 5.5) is a placeholder, until the file is written (see registerEmbeddedFile)
		final String uri = (fileNode != null? StringUtils.trimToNull(fileNode.getValue()): null);
		// 5.5.1 has FORM under FILE, 5.5 under OBJE
		final GEDCOMNode formNode = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstChild(fileNode, "FORM"), GEDCOMHelper.findFirstChild(objeNode, "FORM"));
		final String format = (formNode != null? StringUtils.trimToNull(formNode.getValue()): null);
		final String mediaType = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(formNode, GEDCOMMapper.TAG_TYPE), GEDCOMHelper.findFirstValue(formNode, "MEDI"));
		media.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_URI, ObjectUtils.firstNonNull(uri, "unknown")))
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_KIND, GEDCOMMapper.mapMediaKind(mediaType, format, uri)));

		final String title = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(fileNode, GEDCOMMapper.TAG_TITLE), GEDCOMHelper.findFirstValue(objeNode, GEDCOMMapper.TAG_TITLE));
		if(title != null)
			media.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_DESCRIPTION, title));

		//the custom date is the one the media was created (a date that cannot be normalized is kept as a note)
		attachDate(media, MediaReader.TAG_DATE, GEDCOMHelper.findFirstValue(objeNode, "_DATE"));
		media.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.format"), format, objeNode))
			.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.media_type"), mediaType, objeNode));
		final String publisher = GEDCOMHelper.findFirstValue(objeNode, "_PUBL");
		if(publisher != null && !GEDCOMHelper.isFlag(publisher))
			media.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.published_by"), publisher, objeNode));
		// GEDCOM 7 alternative versions of the file (e.g. a thumbnail, a transcription)
		for(final GEDCOMNode tranNode : GEDCOMHelper.findChildren(fileNode, GEDCOMMapper.TAG_TRANSLATION)){
			final String tranUri = StringUtils.trimToNull(tranNode.getValue());
			final String tranFormat = GEDCOMHelper.findFirstValue(tranNode, "FORM");
			if(tranUri != null)
				media.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.alternative_file"),
					tranUri + (tranFormat != null? " (" + tranFormat + ")": StringUtils.EMPTY), objeNode));
		}
		for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(objeNode, GEDCOMMapper.TAG_NOTE))
			attachNote(media, model, noteNode, noteRawMap);

		attachRestriction(media, objeNode);
		media.addChild(AuditBuilder.build(objeNode));
	}

	/** Adds a reference to a media to a SourceRecord, if not already there. */
	private static void addMediaReference(final FLEFRecord source, final String mediaId){
		final boolean present = FLEFRecordHelper.findChildren(source, SourceReader.TAG_MEDIA).stream()
			.anyMatch(reference -> mediaId.equals(reference.getValue()));
		if(!present)
			source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_MEDIA, mediaId));
	}

	/*
	(GEDCOM 7)
	n OBJE @<XREF:OBJE>@
	  +1 CROP    {0:1}
	    +2 TOP <Integer>    {0:1}
	    +2 LEFT <Integer>    {0:1}
	    +2 HEIGHT <Integer>    {0:1}
	    +2 WIDTH <Integer>    {0:1}
	*/
	/**
	 * Builds a CropRect from the CROP of GEDCOM 7, or from the custom {@code _CUTD} ("x y width height"), or {@code null}.
	 * <p>
	 * A CROP without HEIGHT or WIDTH extends to the edge of the image, whose size is not known here: it is dropped.
	 */
	private static FLEFRecord buildCrop(final GEDCOMNode objeNode){
		final GEDCOMNode cropNode = GEDCOMHelper.findFirstChild(objeNode, "CROP");
		if(cropNode != null)
			return createCrop(ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(cropNode, "LEFT"), "0"),
				ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(cropNode, "TOP"), "0"),
				GEDCOMHelper.findFirstValue(cropNode, "WIDTH"), GEDCOMHelper.findFirstValue(cropNode, "HEIGHT"));

		final String cut = GEDCOMHelper.findFirstValue(objeNode, "_CUTD");
		if(cut == null)
			return null;

		final String[] parts = StringUtils.split(cut);
		return (parts.length == 4? createCrop(parts[0], parts[1], parts[2], parts[3]): null);
	}

	private static FLEFRecord createCrop(final String x, final String y, final String width, final String height){
		try{
			final int w = Integer.parseInt(width);
			final int h = Integer.parseInt(height);
			if(w <= 0 || h <= 0)
				return null;

			return FLEFRecord.createChildWithTag(CropReader.TAG_CROP)
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_X, String.valueOf(Integer.parseInt(x))))
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_Y, String.valueOf(Integer.parseInt(y))))
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_WIDTH, String.valueOf(w)))
				.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_HEIGHT, String.valueOf(h)));
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}


	/*
	EVENT_DETAIL :=
	  n TYPE <EVENT_OR_FACT_CLASSIFICATION>    {0:1}
	  n DATE <DATE_VALUE>    {0:1}
	  n <<PLACE_STRUCTURE>>    {0:1}
	  n <<ADDRESS_STRUCTURE>>    {0:1}
	  n AGNC <RESPONSIBLE_AGENCY>    {0:1}
	  n RELI <RELIGIOUS_AFFILIATION>    {0:1}
	  n CAUS <CAUSE_OF_EVENT>    {0:1}
	  n RESN <RESTRICTION_NOTICE>    {0:1}
	  n <<NOTE_STRUCTURE>>    {0:M}
	  n <<SOURCE_CITATION>>    {0:M}
	  n <<MULTIMEDIA_LINK>>    {0:M}
	*/
	/**
	 * Creates an EventRecord from an INDIVIDUAL_EVENT_STRUCTURE or a FAMILY_EVENT_STRUCTURE, and adds it to the model.
	 * <p>
	 * The participants (and their age, which GEDCOM records in the event) are linked by the caller.
	 *
	 * @return	The event.
	 */
	public static FLEFRecord createEvent(FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap,
			PlaceCache placeCache){
		final String tag = node.getTag();
		final String typeValue = GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_TYPE);
		final String type = GEDCOMMapper.mapEventType(tag, typeValue);
		final FLEFRecord event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventReader.ID_PREFIX), EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));

		// Description: the value of the event (e.g. of EVEN), or else a TYPE that does not give the type of the event
		final boolean typeConsumed = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(tag) && GEDCOMMapper.toTypeToken(typeValue) != null);
		String description = (!GEDCOMHelper.isFlag(node.getValue())? StringUtils.trimToNull(GEDCOMHelper.extractFullText(node)): null);
		if(typeValue != null && !typeConsumed){
			if(description == null)
				description = typeValue;
			else
				event.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.type"), typeValue, node));
		}
		if(description != null)
			event.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_DESCRIPTION, description));

		attachDate(event, EventReader.TAG_DATE, GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_DATE));
		final FLEFRecord place = attachPlaceCitation(event, model, GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_PLACE),
			GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_ADDRESS), noteRawMap, placeCache);

		final String cause = GEDCOMHelper.findFirstValue(node, "CAUS");
		if(cause != null)
			event.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_CAUSE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_REASON, cause)));

		attachEventDetail(event, place, model, node, noteRawMap);
		return event;
	}

	/**
	 * Creates a ResearchTaskRecord from a custom to-do tag ({@code _TODO}), and adds it to the model.
	 * <p>
	 * The description is the text of the tag, or else its {@code DESC} or {@code NOTE}; the task is open. A task has no field for
	 * the person or the family it is about, so a research question targeting it is created and the task is linked to that question.
	 *
	 * @param targetTag	The tag of the target of the question ({@link AgentReader#TAG_INDIVIDUAL} or {@link AgentReader#TAG_GROUP}).
	 * @param targetId	The ID of the individual or the group the to-do is about.
	 * @return	The task, or {@code null} if the tag holds no text.
	 */
	public static FLEFRecord createResearchTask(final FLEFModel model, final GEDCOMNode node, final Map<String, GEDCOMNode> noteRawMap,
			final String targetTag, final String targetId){
		String description = (!GEDCOMHelper.isFlag(node.getValue())? StringUtils.trimToNull(GEDCOMHelper.extractFullText(node)): null);
		if(description == null)
			description = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_DESCRIPTION)));
		if(description == null)
			description = StringUtils.trimToNull(GEDCOMHelper.extractFullText(resolveNote(GEDCOMHelper.findFirstChild(node,
				GEDCOMMapper.TAG_NOTE), noteRawMap)));
		if(description == null)
			return null;

		final FLEFRecord question = FLEFRecord.createMainRecord(IDGenerator.nextId(ResearchQuestionReader.ID_PREFIX),
				ResearchQuestionReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, description))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_QUESTION, description))
			.addChild(FLEFRecord.createChildWithTag(ResearchQuestionReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(targetTag, targetId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_STATUS, ResearchQuestionReader.ENUM_STATUS_OPEN))
			.addChild(AuditBuilder.build(node));
		model.addRecord(question);

		final FLEFRecord task = FLEFRecord.createMainRecord(IDGenerator.nextId(ResearchTaskReader.ID_PREFIX), ResearchTaskReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, description))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, question.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, ResearchTaskReader.ENUM_STATUS_OPEN))
			.addChild(AuditBuilder.build(node));
		model.addRecord(task);
		return task;
	}

	/**
	 * Creates an IndividualAttributeRecord from an INDIVIDUAL_ATTRIBUTE_STRUCTURE (or from an event that is an attribute in
	 * FLEF), and adds it to the model.
	 *
	 * @param type	The FLEF type of the attribute, or {@code null} to derive it from the GEDCOM tag.
	 */
	public static FLEFRecord createIndividualAttribute(FLEFModel model, String individualId, GEDCOMNode node, String type,
			Map<String, GEDCOMNode> noteRawMap, PlaceCache placeCache){
		final String tag = node.getTag();
		final String typeValue = GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_TYPE);
		boolean typeConsumed = (type != null);
		if(type == null){
			type = GEDCOMMapper.mapAttribute(tag, tag);
			if(GEDCOMMapper.TAG_GENERIC_FACT.equals(tag) && GEDCOMMapper.toTypeToken(typeValue) != null){
				type = GEDCOMMapper.toTypeToken(typeValue);
				typeConsumed = true;
			}
		}

		final FLEFRecord attribute = FLEFRecord.createMainRecord(IDGenerator.nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individualId))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, type));

		// Value: the value of the attribute, or else its TYPE (that some programs use to hold it)
		String value = (!GEDCOMHelper.isFlag(node.getValue())? StringUtils.trimToNull(GEDCOMHelper.extractFullText(node)): null);
		if(typeValue != null && !typeConsumed){
			if(value == null)
				value = typeValue;
			else
				attribute.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.type"), typeValue, node));
		}
		if(value != null)
			attribute.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value));

		attachValidity(attribute, GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_DATE));
		final FLEFRecord place = attachPlaceCitation(attribute, model, GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_PLACE),
			GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_ADDRESS), noteRawMap, placeCache);

		// Cause and age have no place in an attribute
		attribute.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.cause"), GEDCOMHelper.findFirstValue(node, "CAUS"), node));
		attribute.addChild(NoteBuilder.createNoteStruct(NoteBuilder.AGE_NOTE_TITLE, GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_AGE), node));

		attachEventDetail(attribute, place, model, node, noteRawMap);
		return attribute;
	}

	/**
	 * Keeps, as notes of an event or of an attribute, what has no field in FLEF: the identifiers, the places beyond the first one,
	 * the contact data of the address (on the place built from the address), the date of the status of an LDS ordinance, and the
	 * tags that are not read.
	 *
	 * @param place	The place of the event, may be {@code null}.
	 */
	public static void keepEventLosses(final FLEFRecord record, final FLEFRecord place, final GEDCOMNode node){
		final String path = node.getTag();
		KeptNotes.keepIdentifiers(record, node, path, node);

		final List<GEDCOMNode> places = GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_PLACE);
		for(int i = 1; i < places.size(); i ++)
			if(GEDCOMImportContext.isSignificant(places.get(i)))
				KeptNotes.keepOtherPlace(record, places.get(i), node);

		KeptNotes.keepContactData(record, place, node);

		final GEDCOMNode statusDate = GEDCOMHelper.findFirstChild(GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_STATUS),
			GEDCOMMapper.TAG_DATE);
		if(statusDate != null && StringUtils.isNotBlank(statusDate.getValue()))
			KeptNotes.add(record, KeptNotes.TITLE_STATUS_DATE, KeptNotes.TITLE_STATUS_DATE + ": " + statusDate.getValue().trim(), node);

		KeptNotes.keepUnmapped(record, node, GEDCOMMapper.EVENT_CONSUMED_TAGS, path, node);
	}

	/** Attaches the details shared by events and attributes, the audit, and adds the record to the model. */
	private static void attachEventDetail(FLEFRecord record, FLEFRecord place, FLEFModel model, GEDCOMNode node,
			Map<String, GEDCOMNode> noteRawMap){
		attachNote(record, model, GEDCOMHelper.findFirstChild(node, "AGNC"), noteRawMap, GedcomTexts.t("title.agency"));
		attachNote(record, model, GEDCOMHelper.findFirstChild(node, "RELI"), noteRawMap, GedcomTexts.t("title.religion"));
		// LDS ordinances
		attachNote(record, model, GEDCOMHelper.findFirstChild(node, "TEMP"), noteRawMap, GedcomTexts.t("title.temple"));
		attachNote(record, model, GEDCOMHelper.findFirstChild(node, "STAT"), noteRawMap, GedcomTexts.t("title.status"));
		// the family a child was sealed to (LDS ordinance): the group has the same id as the FAM record, converted or not yet
		if(GEDCOMMapper.TAG_SEALING_CHILD.equalsIgnoreCase(node.getTag())){
			final String familyId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_FAMILY_CHILD));
			if(familyId != null)
				record.addChild(NoteBuilder.createLinkNote(GedcomTexts.t("title.sealed_to_family"), GedcomTexts.tf("text.sealed_to_family", familyId, GEDCOMMapper.FLEF_LINK_PREFIX), node));
		}

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_SOURCE))
			attachSource(record, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_NOTE))
			attachNote(record, model, noteNode, noteRawMap);
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_MULTIMEDIA))
			attachMultimediaLink(record, model, multimediaLinkNode, noteRawMap);

		keepEventLosses(record, place, node);
		attachRestriction(record, node);
		record.addChild(AuditBuilder.build(node));

		model.addRecord(record);
	}

	/**
	 * Attaches an EventParticipationRecord linking an individual or group to an EventRecord.
	 *
	 * @param age	The age of the participant at the event, may be {@code null}.
	 * @param node	The GEDCOM node used as the audit source, may be {@code null}.
	 */
	public static String attachEventParticipation(String eventId, String entityType, String entityId, String role, String age,
			GEDCOMNode node, FLEFModel model){
		if(StringUtils.isEmpty(eventId) || StringUtils.isEmpty(entityId))
			return null;

		final FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationReader.ID_PREFIX),
				EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(entityType, entityId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId));
		if(role != null)
			participation.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
		participation.addChild(NoteBuilder.createNoteStruct(NoteBuilder.AGE_NOTE_TITLE, age, node))
			.addChild(AuditBuilder.build(node));

		return Deduplicator.getDeduplicatedRecordId(model, participation);
	}

	/*
	(GEDCOM 7, under an event)
	n ASSO @<XREF:INDI>@
	  +1 PHRASE <Text>    {0:1}
	  +1 ROLE <Enum>    {1:1}
	    +2 PHRASE <Text>    {0:1}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 <<SOURCE_CITATION>>    {0:M}
	*/
	/**
	 * Attaches an EventParticipationRecord for each individual associated with an event (GEDCOM 7), with its role.
	 * <p>
	 * A role that is not a function in the event (e.g. a kinship, as {@code FATH}) is kept as a note, as roles never express
	 * kinship.
	 *
	 * @param eventNode	The GEDCOM event.
	 * @param recordsById	The GEDCOM records, by id.
	 */
	public static void attachEventAssociates(String eventId, GEDCOMNode eventNode, FLEFModel model,
			Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> recordsById){
		for(GEDCOMNode assoNode : GEDCOMHelper.findChildren(eventNode, GEDCOMMapper.TAG_ASSOCIATION)){
			final String associateId = GEDCOMHelper.pointer(assoNode.getValue());
			final GEDCOMNode associate = recordsById.get(associateId);
			if(associate == null || !GEDCOMMapper.TAG_INDIVIDUAL.equals(associate.getTag())){
				GEDCOMImportContext.discard(ImportReasons.OTHER, assoNode, eventNode.getTag());
				continue;
			}

			final String gedcomRole = GEDCOMHelper.findFirstValue(assoNode, GEDCOMMapper.TAG_ROLE);
			final String role = GEDCOMMapper.mapEventRole(gedcomRole);
			final FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationReader.ID_PREFIX),
					EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, associateId)))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId));
			if(role != null)
				participation.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
			for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(assoNode, GEDCOMMapper.TAG_SOURCE))
				attachSource(participation, model, sourNode, noteRawMap);
			if(role == null)
				participation.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.role"), GEDCOMMapper.mapAssociationRoleLabel(gedcomRole), assoNode));
			for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(assoNode, GEDCOMMapper.TAG_NOTE))
				attachNote(participation, model, noteNode, noteRawMap);
			participation.addChild(AuditBuilder.build(eventNode));

			Deduplicator.getDeduplicatedRecordId(model, participation);
		}
	}

	/*
	(GEDCOM 7)
	n NO <EVENT_TYPE>
	  +1 DATE <DatePeriod>    {0:1}
	    +2 PHRASE <Text>    {0:1}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 <<SOURCE_CITATION>>    {0:M}
	*/
	/**
	 * Creates the ResearchActivityRecord stating that an event did not happen (in the period given, if any), and adds it to
	 * the model: a completed review with a negative result, on the individual or group, with the sources stating it.
	 *
	 * @param targetTag	The tag of the target, {@link AgentReader#TAG_INDIVIDUAL} or {@link AgentReader#TAG_GROUP}.
	 * @param targetId	The id of the individual or group.
	 * @return	The activity, or {@code null} if the event type is missing.
	 */
	public static FLEFRecord createNonEventActivity(final FLEFModel model, final String targetTag, final String targetId,
			final GEDCOMNode node, final Map<String, GEDCOMNode> noteRawMap){
		final String eventTag = StringUtils.trimToNull(node.getValue());
		if(eventTag == null)
			return null;

		final String eventType = GEDCOMMapper.mapEvent(eventTag, eventTag)
			.replace('_', ' ');
		final StringBuilder observation = new StringBuilder();
		final String period = GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_DATE);
		if(period != null)
			observation.append("Period: ").append(period);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_NOTE)){
			final String noteText = StringUtils.trimToNull(GEDCOMHelper.extractFullText(resolveNote(noteNode, noteRawMap)));
			if(noteText != null){
				if(!observation.isEmpty())
					observation.append(StringUtils.LF);
				observation.append(noteText);
			}
		}

		final FLEFRecord activity = FLEFRecord.createMainRecord(IDGenerator.nextId(ResearchActivityReader.ID_PREFIX),
				ResearchActivityReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTIVITY_TYPE,
				ResearchActivityReader.ENUM_ACTIVITY_TYPE_REVIEW))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_STATUS, ResearchActivityReader.ENUM_STATUS_COMPLETED))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_ACTION,
				GedcomTexts.tf("text.no_event_recorded", eventType)))
			.addChild(FLEFRecord.createChildWithTag(ResearchActivityReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(targetTag, targetId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_RESULT, ResearchActivityReader.ENUM_RESULT_NEGATIVE));
		final FLEFRecord observationField = FLEFRecord.createChildWithTagAndValue(ResearchActivityReader.TAG_OBSERVATION,
			observation.toString());
		if(!observation.isEmpty())
			activity.addChild(observationField);
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_SOURCE))
			attachSource(activity, model, sourNode, noteRawMap);
		// an activity cannot have notes: those of the citations (see attachSource) are appended to its observation
		final List<FLEFRecord> citationNotes = FLEFRecordHelper.findChildren(activity, NoteReader.TAG_NOTE);
		if(!citationNotes.isEmpty()){
			activity.getChildren()
				.removeAll(citationNotes);
			for(final FLEFRecord citationNote : citationNotes){
				if(!observation.isEmpty())
					observation.append(StringUtils.LF);
				observation.append(FLEFRecordHelper.getChildValue(citationNote, NoteReader.TAG_TEXT));
			}
			observationField.setValue(observation.toString());
			// the observation goes before the sources
			if(!activity.getChildren().contains(observationField))
				activity.getChildren()
					.add(activity.getChildren().indexOf(FLEFRecordHelper.findChild(activity,
						ResearchActivityReader.TAG_RESULT)) + 1, observationField);
		}
		activity.addChild(AuditBuilder.build(node));
		model.addRecord(activity);
		return activity;
	}


	/*
	n ADOP [Y|<NULL>]
	  +1 FAMC @<XREF:FAM>@    {0:1}
	    +2 ADOP <ADOPTED_BY_WHICH_PARENT>    {0:1}
	*/
	/**
	 * Creates the adoptive relationships stated by an adoption event (ADOP.FAMC, with ADOP telling which parents adopted).
	 * <p>
	 * A parent by birth of the individual is not an adoptive one (e.g. the mother whose new husband adopted the child).
	 *
	 * @param indiNode	The GEDCOM individual.
	 * @param recordsById	The GEDCOM records, by id.
	 */
	public static void attachAdoptiveParents(FLEFRecord individual, FLEFModel model, GEDCOMNode indiNode,
			GEDCOMNode adoptionNode, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> recordsById){
		final Set<String> biologicalParents = biologicalParents(indiNode, recordsById);
		for(GEDCOMNode famcNode : GEDCOMHelper.findChildren(adoptionNode, GEDCOMMapper.TAG_FAMILY_CHILD)){
			final GEDCOMNode familyNode = recordsById.get(GEDCOMHelper.pointer(famcNode.getValue()));
			if(familyNode == null)
				continue;

			final String adoptedBy = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(famcNode, GEDCOMMapper.TAG_ADOPTION), "BOTH")
				.toUpperCase(Locale.ROOT);
			final String husbandId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(familyNode, GEDCOMMapper.TAG_HUSBAND));
			final String wifeId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(familyNode, GEDCOMMapper.TAG_WIFE));
			if(husbandId != null && !biologicalParents.contains(husbandId)
					&& (GEDCOMMapper.TAG_HUSBAND.equals(adoptedBy) || "BOTH".equals(adoptedBy)))
				attachAdoptiveRelationship(individual, model, adoptionNode, famcNode, husbandId, noteRawMap);
			if(wifeId != null && !biologicalParents.contains(wifeId)
					&& (GEDCOMMapper.TAG_WIFE.equals(adoptedBy) || "BOTH".equals(adoptedBy)))
				attachAdoptiveRelationship(individual, model, adoptionNode, famcNode, wifeId, noteRawMap);
		}
	}

	/**
	 * Returns the parents by birth of an individual: the spouses of the families it is a child of with a birth (or no)
	 * pedigree, except the families of an adoption event.
	 *
	 * @param indiNode	The GEDCOM individual, may be {@code null}.
	 * @param recordsById	The GEDCOM records, by id.
	 * @return	The ids of the parents by birth.
	 */
	public static Set<String> biologicalParents(GEDCOMNode indiNode, Map<String, GEDCOMNode> recordsById){
		final Set<String> adoptionFamilyIds = new HashSet<>();
		for(GEDCOMNode adoptionNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_ADOPTION))
			for(GEDCOMNode famcNode : GEDCOMHelper.findChildren(adoptionNode, GEDCOMMapper.TAG_FAMILY_CHILD))
				adoptionFamilyIds.add(GEDCOMHelper.pointer(famcNode.getValue()));

		final Set<String> parents = new HashSet<>();
		for(GEDCOMNode famcNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_FAMILY_CHILD)){
			final String familyId = GEDCOMHelper.pointer(famcNode.getValue());
			final GEDCOMNode familyNode = recordsById.get(familyId);
			if(familyNode == null || adoptionFamilyIds.contains(familyId)
					|| !RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(GEDCOMMapper.mapPedigree(GEDCOMHelper.findFirstValue(famcNode, "PEDI"))))
				continue;

			for(String spouseTag : List.of(GEDCOMMapper.TAG_HUSBAND, GEDCOMMapper.TAG_WIFE)){
				final String parentId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(familyNode, spouseTag));
				if(parentId != null)
					parents.add(parentId);
			}
		}
		return parents;
	}

	private static void attachAdoptiveRelationship(FLEFRecord individual, FLEFModel model, GEDCOMNode adoptionNode,
			GEDCOMNode famcNode, String parentId, Map<String, GEDCOMNode> noteRawMap){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, individual.getId())))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD));
		attachDate(relationship, RelationshipReader.TAG_VALID_FROM, GEDCOMHelper.findFirstValue(adoptionNode, GEDCOMMapper.TAG_DATE));
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(famcNode, GEDCOMMapper.TAG_NOTE))
			attachNote(relationship, model, noteNode, noteRawMap);
		relationship.addChild(AuditBuilder.build(adoptionNode));

		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}


	/**
	 * Attaches a {@code DateStructure} built from a GEDCOM date; a date that cannot be normalized is kept as a note.
	 *
	 * @param date	The GEDCOM date, may be {@code null}.
	 */
	public static void attachDate(FLEFRecord parent, String tag, String date){
		if(StringUtils.isBlank(date))
			return;

		final List<FLEFRecord> dateStructures = GEDCOMDateConverter.buildDateStructures(tag, date);
		if(dateStructures.isEmpty()){
			parent.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.date"), date, null));
			return;
		}

		//a structure has a single date: with alternatives, the first is the date and each other one is kept as a note
		parent.addChild(dateStructures.getFirst());
		for(final String alternative : GEDCOMDateConverter.buildAlternativeTexts(date))
			parent.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.date_alternative"), alternative, null));
	}

	/**
	 * Attaches the {@code valid_from} and {@code valid_to} of a fact observed at a GEDCOM date; a date that cannot be
	 * normalized is kept as a note.
	 *
	 * @param date	The GEDCOM date, may be {@code null}.
	 */
	public static void attachValidity(FLEFRecord parent, String date){
		if(StringUtils.isBlank(date))
			return;

		final FLEFRecord[] validity = GEDCOMDateConverter.buildValidity(date);
		if(validity != null){
			parent.addChild(validity[0]);
			parent.addChild(validity[1]);
			//the validity keeps the first alternative: each other one is kept as a note
			for(final String alternative : GEDCOMDateConverter.buildAlternativeTexts(date))
				parent.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.date_alternative"), alternative, null));
		}
		else
			parent.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.date"), date, null));
	}


	/**
	 * Parses a GEDCOM PLAC node and attaches a PlaceCitation.
	 * It also creates/updates the underlying PlaceRecord with subfields.
	 *
	 * @return	The place record (the most specific one), or {@code null} if there is none.
	 */
	public static FLEFRecord attachPlaceCitation(FLEFRecord parent, FLEFModel model,
			GEDCOMNode placNode, GEDCOMNode addrNode, Map<String, GEDCOMNode> noteRawMap, PlaceCache placeCache){
		if(placNode == null && addrNode == null)
			return null;

		final String placeName = (placNode != null? StringUtils.trimToNull(placNode.getValue()): null);

		// Extract address components if present
		final StringBuilder streetAddr = new StringBuilder();
		String addressPlaceName = null;
		if(addrNode != null){
			boolean hasSubTags = false;
			for(final String subTag : List.of("ADR1", "ADR2", "ADR3")){
				final GEDCOMNode sub = GEDCOMHelper.findFirstChild(addrNode, subTag);
				if(sub != null && sub.getValue() != null){
					hasSubTags = true;
					if(!streetAddr.isEmpty())
						streetAddr.append(StringUtils.LF);
					streetAddr.append(sub.getValue());
				}
			}
			// Fallback to main address value if no specific street sub-tags were found
			if(!hasSubTags && addrNode.getValue() != null){
				streetAddr.append(GEDCOMHelper.extractFullText(addrNode));
			}

			// The jurisdictions of the address, used when there is no place
			final List<String> jurisdictions = new ArrayList<>();
			for(final String subTag : List.of("CITY", "STAE", "CTRY")){
				final String jurisdiction = GEDCOMHelper.findFirstValue(addrNode, subTag);
				if(jurisdiction != null)
					jurisdictions.add(jurisdiction);
			}
			if(!jurisdictions.isEmpty())
				addressPlaceName = String.join(", ", jurisdictions);
		}
		final String basePlaceName = ObjectUtils.firstNonNull(placeName, addressPlaceName);

		final String finalPlaceName;
		final GEDCOMNode targetPlacNode;
		//the levels of the jurisdictions (PLAC.FORM): the default one when null, none when empty
		String form = null;
		if(StringUtils.isEmpty(basePlaceName)){
			// Only a street address exists, use it as place name
			finalPlaceName = StringUtils.trimToNull(streetAddr.toString().replace(StringUtils.LF, ", "));
			targetPlacNode = new GEDCOMNode(1, GEDCOMMapper.TAG_PLACE, finalPlaceName);
			form = StringUtils.EMPTY;
		}
		else if(streetAddr.isEmpty()){
			// Only PLAC exists
			finalPlaceName = basePlaceName;
			targetPlacNode = (placNode != null? placNode: new GEDCOMNode(1, GEDCOMMapper.TAG_PLACE, finalPlaceName));
			if(placNode == null)
				form = StringUtils.EMPTY;
		}
		else{
			// Both exist: combine street address with place name, avoiding duplication
			final String streetStr = streetAddr.toString()
				.replace(StringUtils.LF, ", ");
			if(!basePlaceName.toLowerCase(Locale.ROOT).contains(streetStr.toLowerCase(Locale.ROOT))){
				finalPlaceName = streetStr + ", " + basePlaceName;
			}
			else{
				//the place already holds the street address
				finalPlaceName = basePlaceName;
			}
			targetPlacNode = createSyntheticPlacNode(placNode, finalPlaceName);
			form = shiftPlaceForm(placNode, placeCache, StringUtils.countMatches(finalPlaceName, ',') - StringUtils.countMatches(basePlaceName, ','));
		}

		if(StringUtils.isEmpty(finalPlaceName))
			return null;

		// Use PlaceCache to handle hierarchy, splitting, deduplication, and links (and FONE, ROMN, MAP)
		final FLEFRecord placeRecord = (placeCache != null? placeCache.getOrCreatePlace(targetPlacNode, form): null);
		if(placeRecord == null)
			return null;

		// Attach NOTE (notes under PLAC) to the leaf place if needed
		for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(placNode, GEDCOMMapper.TAG_NOTE)){
			attachNote(placeRecord, model, noteNode, noteRawMap);
		}

		parent.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceCitationReader.TAG_PLACE, placeRecord.getId()))
		);
		return placeRecord;
	}

	/**
	 * Returns the levels of the jurisdictions of a place that has a street address before them: the levels of its PLAC.FORM (or
	 * the default ones), with an empty level for each part of the street address.
	 *
	 * @param streetParts	The number of the parts of the street address.
	 * @return	The levels, empty if the place is not a PLAC or the street address is not before it.
	 */
	private static String shiftPlaceForm(final GEDCOMNode placNode, final PlaceCache placeCache, final int streetParts){
		if(placNode == null || placeCache == null || streetParts <= 0)
			return StringUtils.EMPTY;

		final String form = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(placNode, GEDCOMMapper.TAG_FORM), placeCache.getDefaultForm());
		return (form != null? StringUtils.repeat(',', streetParts) + form: StringUtils.EMPTY);
	}

	private static GEDCOMNode createSyntheticPlacNode(final GEDCOMNode originalPlac, final String newAddress){
		final GEDCOMNode synthetic = new GEDCOMNode((originalPlac != null? originalPlac.getLevel(): 1), GEDCOMMapper.TAG_PLACE, newAddress);
		if(originalPlac != null)
			for(final GEDCOMNode child : originalPlac.getChildren())
				synthetic.addChild(child);
		return synthetic;
	}


	public static void attachRestriction(FLEFRecord parent, GEDCOMNode node){
		final String level = GEDCOMMapper.mapPrivacyLevel(GEDCOMHelper.findFirstValue(node, "RESN"));
		if(level != null)
			parent.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PRIVACY)
				.addChild(FLEFRecord.createChildWithTagAndValue(PrivacyReader.TAG_LEVEL, level)));
	}


	/*
	ADDRESS_STRUCTURE :=
	  n ADDR <ADDRESS_LINE>    {1:1}
	    +1 CONT <ADDRESS_LINE>    {0:3}
	    +1 ADR1, ADR2, ADR3, CITY, STAE, POST, CTRY    {0:1}
	  n PHON <PHONE_NUMBER>    {0:3}
	  n EMAIL <ADDRESS_EMAIL>    {0:3}
	  n FAX <ADDRESS_FAX>    {0:3}
	  n WWW <ADDRESS_WEB_PAGE>    {0:3}
	*/
	/**
	 * Attaches a ContactStructure for the address, and one for each phone, email, fax, and web page of an ADDRESS_STRUCTURE.
	 * <p>
	 * PHON, EMAIL, FAX, and WWW are siblings of ADDR, but some programs write them under it.
	 *
	 * @param parentNode	The node holding the ADDRESS_STRUCTURE.
	 * @param contactName	The name of the person or organization the contacts belong to, may be {@code null}.
	 */
	public static void attachContacts(FLEFRecord parent, GEDCOMNode parentNode, String contactName){
		final GEDCOMNode addrNode = GEDCOMHelper.findFirstChild(parentNode, GEDCOMMapper.TAG_ADDRESS);
		if(addrNode != null){
			final String formattedAddress = StringUtils.trimToNull(GEDCOMHelper.extractFullText(addrNode));
			final StringBuilder fullAddr = new StringBuilder(StringUtils.defaultString(formattedAddress));
			for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
				final String sub = GEDCOMHelper.findFirstValue(addrNode, subTag);
				// the ADDR value already holds the formatted address, its parts only make it machine-readable
				if(sub != null && (formattedAddress == null || !Strings.CI.contains(formattedAddress, sub))){
					if(!fullAddr.isEmpty())
						fullAddr.append(StringUtils.LF);
					fullAddr.append(sub);
				}
			}
			addContact(parent, fullAddr.toString(), ContactReader.ENUM_TYPE_POSTAL, contactName, parentNode);
		}

		final List<GEDCOMNode> holders = new ArrayList<>(2);
		holders.add(parentNode);
		if(addrNode != null)
			holders.add(addrNode);
		for(final GEDCOMNode holder : holders)
			for(final GEDCOMNode child : holder.getChildren()){
				final String type = switch(child.getTag()){
					case "PHON" -> ContactReader.ENUM_TYPE_PHONE;
					case "EMAIL", "_EMAIL" -> ContactReader.ENUM_TYPE_EMAIL;
					case "FAX" -> ContactReader.ENUM_TYPE_FAX;
					case "WWW", "_WWW" -> ContactReader.ENUM_TYPE_WEBSITE;
					default -> null;
				};
				if(type != null)
					addContact(parent, child.getValue(), type, contactName, parentNode);
			}
	}

	private static void addContact(FLEFRecord parent, String value, String type, String contactName, GEDCOMNode auditNode){
		if(StringUtils.isBlank(value))
			return;

		final FLEFRecord contact = FLEFRecord.createChildWithTag(RepositoryReader.TAG_CONTACT)
			.addChild(FLEFRecord.createChildWithTagAndValue(ContactReader.TAG_VALUE, value.trim()))
			.addChild(FLEFRecord.createChildWithTagAndValue(ContactReader.TAG_TYPE, type));
		if(StringUtils.isNotBlank(contactName))
			contact.addChild(FLEFRecord.createChildWithTag(ContactReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, contactName.trim())));
		contact.addChild(AuditBuilder.build(auditNode));
		parent.addChild(contact);
	}

}
