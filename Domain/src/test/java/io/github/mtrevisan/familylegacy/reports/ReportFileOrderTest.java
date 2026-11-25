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
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * The records that a list of an editor shows (the attributes of an individual, its relationships, the attributes of a group) are in the
 * report in the order they have in the file, so the order the user gives them in the list is the one of the report.
 */
class ReportFileOrderTest{

	private static ReportConfig config(){
		return ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL)
			.withLivingPrivacy(LivingPrivacy.SHOW)
			.with(ReportOption.GROUP_ATTRIBUTES, true);
	}

	/** @return	Everything the report writes, in order (headings, paragraphs, lists, the rows of the tables). */
	private static List<String> texts(final ReportDocument document){
		final List<String> texts = new ArrayList<>();
		for(final ReportSection section : document.sections())
			switch(section){
				case ReportSection.Heading heading -> texts.add(heading.text());
				case ReportSection.Paragraph paragraph -> texts.add(paragraph.text());
				case ReportSection.BulletList list -> texts.addAll(list.items());
				case ReportSection.Table table -> table.rows().forEach(row -> texts.add(String.join(" | ", row)));
				default -> {
				}
			}
		return texts;
	}

	/** @return	The values in the order they are first written in the report (a text may have several of them: the position counts). */
	private static List<String> orderOf(final List<String> texts, final List<String> values){
		final String all = String.join(String.valueOf((char)10), texts);
		final List<String> order = new ArrayList<>(values);
		order.sort(Comparator.comparingInt(value -> {
			final int position = all.indexOf(value);
			return (position >= 0? position: Integer.MAX_VALUE);
		}));
		return order;
	}

	private static FLEFRecord attribute(final String id, final String value){
		return FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_OCCUPATION))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value));
	}

	private static FLEFRecord association(final String id, final String otherId){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, "I1")))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, otherId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_ASSOCIATE));
	}

	private static FLEFRecord groupAttribute(final String id, final String value){
		return FLEFRecord.createMainRecord(id, GroupAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_GROUP, "G1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_TYPE, "motto"))
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_VALUE, value));
	}

	private static ReportDocument report(final ReportType type, final FLEFModel model, final String rootId){
		return ReportBuilder.build(type, model, model.getRecordById(rootId), config(), ReportProgressListener.NOOP);
	}


	@Test
	void theAttributesOfAnIndividualFollowTheOrderOfTheFile(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(attribute("IA1", "baker"));
		model.addRecord(attribute("IA2", "farmer"));
		model.addRecord(attribute("IA3", "mayor"));
		final List<String> values = List.of("baker", "farmer", "mayor");

		Assertions.assertEquals(List.of("baker", "farmer", "mayor"), orderOf(texts(report(ReportType.INDIVIDUAL, model, "I1")), values));

		//the user arranges the list of the attributes: the file has the records in the new order, and the report too
		model.reorderRecords(List.of(model.getRecordById("IA3"), model.getRecordById("IA1"), model.getRecordById("IA2")));

		Assertions.assertEquals(List.of("mayor", "baker", "farmer"), orderOf(texts(report(ReportType.INDIVIDUAL, model, "I1")), values));
	}

	@Test
	void theRelationshipsOfAnIndividualFollowTheOrderOfTheFile(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I3", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I4", IndividualReader.TYPE));
		model.addRecord(association("R1", "I2"));
		model.addRecord(association("R2", "I3"));
		model.addRecord(association("R3", "I4"));
		//(the others are named by their identifier)
		final List<String> others = List.of("I2", "I3", "I4");

		Assertions.assertEquals(List.of("I2", "I3", "I4"), orderOf(headings(report(ReportType.INDIVIDUAL, model, "I1")), others));

		model.reorderRecords(List.of(model.getRecordById("R3"), model.getRecordById("R1"), model.getRecordById("R2")));

		Assertions.assertEquals(List.of("I4", "I2", "I3"), orderOf(headings(report(ReportType.INDIVIDUAL, model, "I1")), others));
	}

	@Test
	void theAttributesOfAGroupFollowTheOrderOfTheFile(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("G1", GroupReader.TYPE));
		model.addRecord(groupAttribute("GA1", "first words"));
		model.addRecord(groupAttribute("GA2", "second words"));
		model.addRecord(groupAttribute("GA3", "third words"));
		final List<String> values = List.of("first words", "second words", "third words");

		Assertions.assertEquals(values, orderOf(texts(report(ReportType.GROUP, model, "G1")), values));

		model.reorderRecords(List.of(model.getRecordById("GA2"), model.getRecordById("GA3"), model.getRecordById("GA1")));

		Assertions.assertEquals(List.of("second words", "third words", "first words"), orderOf(texts(report(ReportType.GROUP, model, "G1")), values));
	}

	@Test
	void theChildrenOfAPersonAreToldInTheOrderOfTheirRelationshipsInTheFile(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I3", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I4", IndividualReader.TYPE));
		model.addRecord(childOf("R1", "I2", "I1"));
		model.addRecord(childOf("R2", "I3", "I1"));
		model.addRecord(childOf("R3", "I4", "I1"));
		final List<String> children = List.of("I2", "I3", "I4");

		//(not by birth, not by name: the children are in the order the user gave them, that is the one of the file)
		Assertions.assertEquals(List.of("I2", "I3", "I4"), orderOf(narrative(model, "I1"), children));

		model.reorderRecords(List.of(model.getRecordById("R3"), model.getRecordById("R1"), model.getRecordById("R2")));

		Assertions.assertEquals(List.of("I4", "I2", "I3"), orderOf(narrative(model, "I1"), children));
	}

	@Test
	void theParticipantsOfAnEventAreInTheOrderOfTheirParticipationsInTheFile(){
		final FLEFModel model = new FLEFModel();
		for(final String id : List.of("I1", "I2", "I3"))
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH)));
		model.addRecord(participation("EP1", "I1", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		model.addRecord(participation("EP2", "I2", EventParticipationReader.ENUM_ROLE_WITNESS));
		model.addRecord(participation("EP3", "I3", EventParticipationReader.ENUM_ROLE_INFORMANT));
		final List<String> people = List.of("I1", "I2", "I3");

		Assertions.assertEquals(List.of("I1", "I2", "I3"), orderOf(participantRows(model), people));

		model.reorderRecords(List.of(model.getRecordById("EP3"), model.getRecordById("EP1"), model.getRecordById("EP2")));

		Assertions.assertEquals(List.of("I3", "I1", "I2"), orderOf(participantRows(model), people));
	}

	private static List<String> narrative(final FLEFModel model, final String rootId){
		final List<String> texts = new ArrayList<>();
		for(final ReportSection section : report(ReportType.INDIVIDUAL, model, rootId).sections())
			if(section instanceof final ReportSection.Paragraph paragraph)
				texts.add(paragraph.text());
		return texts;
	}

	/** @return	The rows of the table of the participants of the event of the report. */
	private static List<String> participantRows(final FLEFModel model){
		final List<String> rows = new ArrayList<>();
		for(final ReportSection section : report(ReportType.INDIVIDUAL, model, "E1").sections())
			if(section instanceof final ReportSection.Table table)
				table.rows().forEach(row -> rows.add(String.join(" | ", row)));
		return rows;
	}

	private static FLEFRecord childOf(final String id, final String childId, final String parentId){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
	}

	private static FLEFRecord participation(final String id, final String individualId, final String role){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
	}

	/** @return	The headings of the relationships (the ones under the heading of the relationships). */
	private static List<String> headings(final ReportDocument document){
		final List<String> headings = new ArrayList<>();
		for(final ReportSection section : document.sections())
			if(section instanceof final ReportSection.Heading heading && heading.level() == 3)
				headings.add(heading.text());
		return headings;
	}

}
