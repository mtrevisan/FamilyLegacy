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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Converts the age of a participant at an event (or of an individual at an attribute) into a calculated birth event.
 * <p>
 * The births are created after all the events are known ({@link #convert()}), because an individual that already has a dated
 * birth event gets none: the age stays a note of the participation (or of the attribute), as it does for ages that cannot be
 * calculated from ({@code CHILD}, {@code INFANT}, {@code STILLBORN}) or when the event has no usable date. The note states that
 * the age was converted.
 * <p>
 * The birth event cites the sources of the event the age comes from, as indirect evidence; when there are none, a note links to
 * that event.
 */
public class AgeBirthConverter{

	/** An age to convert. */
	private record PendingAge(String individualId, FLEFRecord source, String participationId, String age, GEDCOMNode node){}


	private final FLEFModel model;
	private final Set<String> individualsWithDatedBirth = new HashSet<>();
	private final List<PendingAge> pendingAges = new ArrayList<>();


	public AgeBirthConverter(final FLEFModel model){
		this.model = model;
	}

	/**
	 * Keeps track of an event or an attribute of an individual.
	 *
	 * @param individualId	The individual.
	 * @param source	The event or the attribute, in the model.
	 * @param participationId	The id of the participation of the individual in the event, {@code null} for an attribute.
	 * @param age	The age of the individual, may be {@code null}.
	 * @param node	The GEDCOM event or attribute.
	 */
	public void track(final String individualId, final FLEFRecord source, final String participationId, final String age,
			final GEDCOMNode node){
		if(EventReader.isOfType(source) && EventReader.ENUM_TYPE_BIRTH.equals(FLEFRecordHelper.getChildValue(source, EventReader.TAG_TYPE))
				&& FLEFRecordHelper.findChild(source, EventReader.TAG_DATE) != null)
			individualsWithDatedBirth.add(individualId);

		if(StringUtils.isNotBlank(age))
			pendingAges.add(new PendingAge(individualId, source, participationId, age, node));
	}

	/** Creates the births, call it once all the events and attributes are converted. */
	public void convert(){
		for(final PendingAge pending : pendingAges){
			if(individualsWithDatedBirth.contains(pending.individualId()))
				continue;

			final FLEFRecord date = GEDCOMDateConverter.buildBirthDateFromAge(EventReader.TAG_DATE,
				GEDCOMHelper.findFirstValue(pending.node(), GEDCOMMapper.TAG_DATE), pending.age());
			if(date == null)
				continue;

			final FLEFRecord event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventReader.ID_PREFIX), EventReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
				.addChild(date);
			boolean cited = false;
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(pending.source(), EventReader.TAG_SOURCE)){
				event.addChild(copyAsIndirectEvidence(citation));
				cited = true;
			}
			if(!cited)
				event.addChild(GEDCOMRecordBuilder.createLinkNote("Age", "Calculated from the age stated by [" + pending.source().getId()
					+ "](" + GEDCOMMapper.FLEF_LINK_PREFIX + pending.source().getId() + ")", pending.node()));
			event.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_CONFIDENCE, ConfidenceReader.ENUM_LOW))
				.addChild(AuditBuilder.build(pending.node()));
			model.addRecord(event);

			GEDCOMRecordBuilder.attachEventParticipation(event.getId(), AgentReader.TAG_INDIVIDUAL, pending.individualId(),
				EventParticipationReader.ENUM_ROLE_PRINCIPAL, null, pending.node(), model);
			individualsWithDatedBirth.add(pending.individualId());

			markConverted(pending, event.getId());
		}
		pendingAges.clear();
	}

	/** Copies a citation, making its evidence indirect. */
	private static FLEFRecord copyAsIndirectEvidence(final FLEFRecord citation){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		citation.deepCopyTo(copy);

		FLEFRecord evidence = FLEFRecordHelper.findChild(copy, SourceCitationReader.TAG_EVIDENCE);
		if(evidence == null){
			evidence = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EVIDENCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_INFORMATION_TYPE,
					EvidenceQualifiersReader.INFORMATION_TYPE_UNDETERMINED));
			copy.addChild(evidence);
		}
		final FLEFRecord evidenceType = FLEFRecordHelper.findChild(evidence, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE);
		if(evidenceType != null)
			evidenceType.setValue(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT);
		else
			evidence.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE,
				EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT));
		return copy;
	}

	/** States, in the note of the age, that the age was converted. */
	private void markConverted(final PendingAge pending, final String birthId){
		final FLEFRecord holder = (pending.participationId() != null? model.getRecordById(pending.participationId()): pending.source());
		if(holder == null)
			return;

		for(final FLEFRecord note : FLEFRecordHelper.findChildren(holder, NoteReader.TAG_NOTE))
			if(AGE_NOTE_TITLE.equals(FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE))){
				final FLEFRecord text = FLEFRecordHelper.findChild(note, NoteReader.TAG_TEXT);
				if(text != null)
					text.setValue(text.getValue() + " (converted to the birth " + birthId + ")");
				return;
			}
	}

	/** The title of the note of an age. */
	static final String AGE_NOTE_TITLE = "Age";

}
