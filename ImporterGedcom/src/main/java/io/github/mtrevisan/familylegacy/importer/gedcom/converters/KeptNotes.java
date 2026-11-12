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
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMImportContext;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.UnconsumedTags;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Keeps, as a NoteStructure of the closest record having notes, the data of a GEDCOM file that has no field of its own in FLEF
 * (identifiers, contact data of an address, unmapped tags, ...), so that it is not lost and is not reported as a discard.
 * <p>
 * Every method accepting a {@code null} target falls back to the report of the discard, where it exists.
 */
final class KeptNotes{

	/** The title of the note of an unmapped tag is this prefix followed by the tag. */
	static final String TITLE_UNMAPPED_PREFIX = GedcomTexts.t("title.unmapped_prefix");
	static final String TITLE_CONTACT = GedcomTexts.t("title.contact");
	static final String TITLE_OTHER_PLACE = GedcomTexts.t("title.other_place");
	static final String TITLE_OTHER_COORDINATES = GedcomTexts.t("title.other_coordinates");
	static final String TITLE_TRANSLATION = GedcomTexts.t("title.translation");
	static final String TITLE_NAME_TYPE = GedcomTexts.t("title.name_type");
	static final String TITLE_SEX = GedcomTexts.t("title.sex");
	static final String SEX_X_TEXT = GedcomTexts.t("text.sex_x");
	static final String TITLE_STATUS_DATE = GedcomTexts.t("title.status_date");

	private static final String LABEL_PHONE = GedcomTexts.t("label.phone");
	private static final String LABEL_EMAIL = GedcomTexts.t("label.email");
	private static final String LABEL_FAX = GedcomTexts.t("label.fax");
	private static final String LABEL_WEB = GedcomTexts.t("label.web");
	private static final String LABEL_POSTAL_CODE = GedcomTexts.t("label.postal_code");
	private static final String SEPARATOR = ": ";
	private static final String INDENT = "  ";

	/** The fields that go before the notes (the citations of the sources) or are the notes themselves. */
	private static final Set<String> NOTE_AND_SOURCE = Set.of(NoteReader.TAG_NOTE, NoteReader.TAG_SOURCE);
	/** The fields that follow the notes. */
	private static final Set<String> AFTER_NOTES = Set.of(IndividualReader.TAG_PREFERRED_IMAGE, EventReader.TAG_CONFIDENCE,
		EventReader.TAG_PRIVACY, AuditReader.TAG_AUDIT);
	/** The labels of the contact tags. */
	private static final Map<String, String> CONTACT_LABELS = Map.of(
		GEDCOMMapper.TAG_POSTAL_CODE, LABEL_POSTAL_CODE,
		GEDCOMMapper.TAG_PHONE, LABEL_PHONE,
		GEDCOMMapper.TAG_EMAIL, LABEL_EMAIL,
		GEDCOMMapper.TAG_CUSTOM_EMAIL, LABEL_EMAIL,
		GEDCOMMapper.TAG_FAX, LABEL_FAX,
		GEDCOMMapper.TAG_WEB, LABEL_WEB,
		GEDCOMMapper.TAG_CUSTOM_WEB, LABEL_WEB);


	private KeptNotes(){}


	/**
	 * Adds a note to a record, after its other notes (or its source citations), unless the record already has the same note.
	 *
	 * @param parent	The record, may be {@code null}.
	 * @param auditNode	The GEDCOM node the audit of the note comes from, may be {@code null}.
	 * @return	Whether the note is on the record (also if it was already there).
	 */
	static boolean add(final FLEFRecord parent, final String title, final String text, final GEDCOMNode auditNode){
		if(parent == null || StringUtils.isBlank(text))
			return false;

		final String trimmedTitle = StringUtils.trimToNull(title);
		final String trimmedText = text.trim();
		for(final FLEFRecord child : parent.getChildren())
			if(NoteReader.TAG_NOTE.equals(child.getTag())
					&& Objects.equals(trimmedTitle, FLEFRecordHelper.getChildValue(child, NoteReader.TAG_TITLE))
					&& trimmedText.equals(FLEFRecordHelper.getChildValue(child, NoteReader.TAG_TEXT)))
				return true;

		final FLEFRecord note = NoteBuilder.createNoteStruct(title, text, auditNode);
		if(note == null)
			return false;

		final List<FLEFRecord> children = parent.getChildren();
		int index = -1;
		for(int i = 0; i < children.size(); i ++)
			if(NOTE_AND_SOURCE.contains(children.get(i).getTag()))
				index = i + 1;
		if(index < 0){
			index = children.size();
			for(int i = 0; i < children.size(); i ++)
				if(AFTER_NOTES.contains(children.get(i).getTag())){
					index = i;
					break;
				}
		}
		children.add(index, note);
		return true;
	}

	/**
	 * Keeps each identifier (REFN, UID, EXID, ...) among the children of a node as a note of the record: the title is the tag
	 * (followed by the TYPE between brackets, but for EXID, whose TYPE is the authority and follows the value in the text), the
	 * text starts with {@code <TAG>: <value>}.
	 *
	 * @param target	The record, {@code null} to report the identifiers.
	 * @param path	The tag path of the node inside the record, empty for the record itself.
	 */
	static void keepIdentifiers(final FLEFRecord target, final GEDCOMNode node, final String path, final GEDCOMNode auditNode){
		if(target == null){
			GEDCOMImportContext.reportIdentifiers(node, path);
			return;
		}

		for(final GEDCOMNode child : node.getChildren())
			if(GEDCOMMapper.IDENTIFIER_TAGS.contains(child.getTag()) && GEDCOMImportContext.isSignificant(child)
					&& !keepIdentifier(target, child, auditNode))
				GEDCOMImportContext.discard(ImportReasons.IDENTIFIER, child, path);
	}

	private static boolean keepIdentifier(final FLEFRecord target, final GEDCOMNode identifier, final GEDCOMNode auditNode){
		final String tag = identifier.getTag();
		final String value = StringUtils.trimToNull(GEDCOMHelper.extractFullText(identifier));
		final String type = GEDCOMHelper.findFirstValue(identifier, GEDCOMMapper.TAG_TYPE);
		final boolean typeFollowsValue = GEDCOMMapper.TAG_EXTERNAL_ID.equals(tag);

		final StringBuilder text = new StringBuilder(tag).append(':');
		if(value != null)
			text.append(StringUtils.SPACE).append(value);
		if(type != null && typeFollowsValue)
			text.append(" (").append(type).append(')');
		final String details = flatten(identifier, GEDCOMMapper.TAG_TYPE);
		if(!details.isEmpty())
			text.append(StringUtils.LF).append(details);
		final String title = (type != null && !typeFollowsValue? tag + " (" + type + ")": tag);
		return add(target, title, text.toString(), auditNode);
	}

	/**
	 * Keeps each child of a node that is not read by the converter (see {@link UnconsumedTags}) as a note titled {@code GEDCOM <TAG>}
	 * whose text is the value of the line followed by its subtree.
	 *
	 * @param target	The record, {@code null} to report the lines.
	 * @param path	The tag path of the node inside the record, empty for the record itself.
	 */
	static void keepUnmapped(final FLEFRecord target, final GEDCOMNode node, final Set<String> consumedTags, final String path,
			final GEDCOMNode auditNode){
		for(final GEDCOMNode child : node.getChildren())
			keepUnmappedChild(target, child, consumedTags, path, auditNode);
	}

	static void keepUnmappedChild(final FLEFRecord target, final GEDCOMNode child, final Set<String> consumedTags,
			final String parentPath, final GEDCOMNode auditNode){
		if(target == null){
			UnconsumedTags.reportChild(child, consumedTags, parentPath);
			return;
		}
		if(!UnconsumedTags.isUnmapped(child, consumedTags))
			return;

		final StringBuilder text = new StringBuilder(StringUtils.defaultString(GEDCOMHelper.extractFullText(child)));
		final String details = flatten(child, null);
		if(!details.isEmpty()){
			if(!text.isEmpty())
				text.append(StringUtils.LF);
			text.append(details);
		}
		if(!add(target, TITLE_UNMAPPED_PREFIX + child.getTag(), text.toString(), auditNode))
			GEDCOMImportContext.discard(ImportReasons.UNMAPPED_TAG, child, parentPath);
	}

	/**
	 * Flattens the subtree of a node into indented lines {@code TAG value}, skipping the continuation lines (they are part of
	 * the value of their parent).
	 *
	 * @param skippedTag	A tag that is not flattened among the children of the node, may be {@code null}.
	 */
	static String flatten(final GEDCOMNode node, final String skippedTag){
		final List<String> lines = new ArrayList<>();
		flatten(node, skippedTag, StringUtils.EMPTY, lines);
		return String.join(StringUtils.LF, lines);
	}

	private static void flatten(final GEDCOMNode node, final String skippedTag, final String indent, final List<String> lines){
		for(final GEDCOMNode child : node.getChildren()){
			final String tag = child.getTag();
			if(GEDCOMMapper.TAG_CONCATENATION.equals(tag) || GEDCOMMapper.TAG_CONTINUATION.equals(tag) || tag.equals(skippedTag))
				continue;

			final String value = StringUtils.normalizeSpace(GEDCOMHelper.extractFullText(child));
			lines.add(indent + tag + (StringUtils.isNotEmpty(value)? StringUtils.SPACE + value: StringUtils.EMPTY));
			flatten(child, null, indent + INDENT, lines);
		}
	}

	/**
	 * Keeps the contact data of the address of an event or attribute (postal code, phone, e-mail, fax, web page: the ones under
	 * the ADDR and the ones under the event itself) as a note {@code Contact}, on the place built from the address, or else on the
	 * event.
	 *
	 * @param record	The event or attribute.
	 * @param place	The place of the event, may be {@code null}.
	 */
	static void keepContactData(final FLEFRecord record, final FLEFRecord place, final GEDCOMNode node){
		final GEDCOMNode addressNode = GEDCOMHelper.findFirstChild(node, GEDCOMMapper.TAG_ADDRESS);
		final List<String[]> contacts = new ArrayList<>();
		if(addressNode != null)
			collectContactLines(addressNode, contacts);
		collectContactLines(node, contacts);
		if(contacts.isEmpty())
			return;

		//the place is the one built from the address, not the one of a PLAC
		final FLEFRecord target = (addressNode != null && place != null? place: record);
		final StringBuilder text = new StringBuilder();
		for(final String[] contact : contacts){
			if(!text.isEmpty())
				text.append(StringUtils.LF);
			text.append(contact[0]).append(SEPARATOR).append(contact[1]);
		}
		add(target, TITLE_CONTACT, text.toString(), node);
	}

	private static void collectContactLines(final GEDCOMNode holder, final List<String[]> contacts){
		for(final GEDCOMNode child : holder.getChildren()){
			final String label = CONTACT_LABELS.get(child.getTag());
			final String value = StringUtils.trimToNull(child.getValue());
			if(label != null && value != null)
				contacts.add(new String[]{label, value});
		}
	}

	/** Keeps the coordinates of a PLAC that differ from the ones of the (shared) place as a note of the place, once for each value. */
	static void keepOtherCoordinates(final FLEFRecord place, final String latitude, final String longitude){
		add(place, TITLE_OTHER_COORDINATES, TITLE_OTHER_COORDINATES + SEPARATOR + latitude + ", " + longitude, null);
	}

	/**
	 * Keeps the translation of a place, which lists the jurisdictions as the PLAC does, as a note of the place (only the name of the
	 * most specific place is stored as a translated name).
	 */
	static void keepTranslation(final FLEFRecord place, final GEDCOMNode translationNode){
		final String language = GEDCOMHelper.findFirstValue(translationNode, GEDCOMMapper.TAG_LANGUAGE);
		add(place, TITLE_TRANSLATION, TITLE_TRANSLATION + (language != null? " (" + language + ")": StringUtils.EMPTY) + SEPARATOR
			+ StringUtils.trimToEmpty(translationNode.getValue()), null);
	}

	/** Keeps a place beyond the first one of an event as a note of the event. */
	static void keepOtherPlace(final FLEFRecord record, final GEDCOMNode placeNode, final GEDCOMNode auditNode){
		final StringBuilder text = new StringBuilder(TITLE_OTHER_PLACE).append(SEPARATOR)
			.append(StringUtils.defaultString(StringUtils.trimToNull(placeNode.getValue())));
		final String details = flatten(placeNode, null);
		if(!details.isEmpty())
			text.append(StringUtils.LF).append(details);
		add(record, TITLE_OTHER_PLACE, text.toString(), auditNode);
	}

}
