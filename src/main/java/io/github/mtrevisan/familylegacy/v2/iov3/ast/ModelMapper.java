package io.github.mtrevisan.familylegacy.v2.iov3.ast;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.ConclusionRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.CulturalNormRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.EventParticipationRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.EventRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.GroupAttributeRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.GroupRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.Header;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.HistoricEventRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.IdentityHypothesisRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.IndividualAttributeRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.IndividualRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.NoteRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.PlaceRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.PlaceRelationshipRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.RelationshipRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.RepositoryRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.ResearchLogRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.ResearchStatusRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.SourceRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.Approximate;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ContactStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.CropCoord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DateValue;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DocumentStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EventStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.NameStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.PersonalNameStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.PreferredImage;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.QualifiedDate;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RepositoryCitation;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SingleDate;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.TextValue;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.TextValueVariant;
import io.github.mtrevisan.familylegacy.v2.iov3.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class ModelMapper{

	private ModelMapper(){}


	public static FamilyLegacyFile map(final FLEFRecord root){
		FLEFRecord headerNode = root.findChild("header");
		if(headerNode == null)
			// If the header is a top-level sister record to the records
			headerNode = root.findChildren("HEADER").stream().findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Header mancante nel file FLEF"));

		final Header header = mapHeader(headerNode);
		final List<Record> records = new ArrayList<>();

		// If root directly contains records in its children
		final List<FLEFRecord> rawRecords = root.getChildren().stream()
			.filter(r -> !"header".equalsIgnoreCase(r.getTag()) && !"HEADER".equalsIgnoreCase(r.getTag()))
			.toList();

		for(final FLEFRecord rec : rawRecords)
			mapRecord(rec).ifPresent(records::add);

		return new FamilyLegacyFile(header, records);
	}

	// ==========================================
	// HEADER MAPPER
	// ==========================================

	private static Header mapHeader(final FLEFRecord node){
		final FLEFRecord protoNode = req(node, "protocol");
		final Header.Protocol protocol = new Header.Protocol(
			reqVal(protoNode, "name"),
			reqVal(protoNode, "version")
		);

		final FLEFRecord srcNode = req(node, "source");
		final Header.Source source = new Header.Source(
			reqVal(srcNode, "system_id"),
			optVal(srcNode, "name"),
			optVal(srcNode, "version"),
			optVal(srcNode, "corporate")
		);

		final String date = reqVal(node, "date");
		final Optional<String> copyright = optVal(node, "copyright");

		final FLEFRecord subNode = req(node, "submitter");
		final Header.Submitter submitter = new Header.Submitter(
			reqVal(subNode, "name"),
			subNode.findChildren("contact").stream().map(ModelMapper::mapContact).toList(),
			subNode.findChildren("note").stream().map(FLEFRecord::getValue).toList()
		);

		final Optional<String> scope = optVal(node, "scope");

		return new Header(protocol, source, date, copyright, submitter, scope);
	}

	// ==========================================
	// TOP-LEVEL RECORD DISPATCHER
	// ==========================================

	private static Optional<Record> mapRecord(final FLEFRecord node){
		final String tag = node.getTag().toLowerCase();
		final String id = node.getId() != null ? node.getId() : node.getChildValue("id");

		return switch(tag){
			case "individual" -> Optional.of(mapIndividual(id, node));
			case "event" -> Optional.of(mapEvent(id, node));
			case "individual_attribute" -> Optional.of(mapIndividualAttribute(id, node));
			case "event_participation" -> Optional.of(mapEventParticipation(id, node));
			case "group" -> Optional.of(mapGroup(id, node));
			case "group_attribute" -> Optional.of(mapGroupAttribute(id, node));
			case "relationship" -> Optional.of(mapRelationship(id, node));
			case "place" -> Optional.of(mapPlace(id, node));
			case "place_relationship" -> Optional.of(mapPlaceRelationship(id, node));
			case "note" -> Optional.of(mapNote(id, node));
			case "repository" -> Optional.of(mapRepository(id, node));
			case "cultural_norm" -> Optional.of(mapCulturalNorm(id, node));
			case "source" -> Optional.of(mapSource(id, node));
			case "historic_event" -> Optional.of(mapHistoricEvent(id, node));
			case "research_status" -> Optional.of(mapResearchStatus(id, node));
			case "research_log" -> Optional.of(mapResearchLog(id, node));
			case "conclusion" -> Optional.of(mapConclusion(id, node));
			case "identity_hypothesis" -> Optional.of(mapIdentityHypothesis(id, node));
			default -> Optional.empty();
		};
	}

	// ==========================================
	// RECORD MAPPERS
	// ==========================================

	private static IndividualRecord mapIndividual(final String id, final FLEFRecord node){
		return new IndividualRecord(
			id,
			node.findChildren("name").stream().map(ModelMapper::mapPersonalName).toList(),
			optVal(node, "sex"),
			mapXrefList(node, "cultural_norm"),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "preferred_image").map(ModelMapper::mapPreferredImage),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static EventRecord mapEvent(final String id, final FLEFRecord node){
		return new EventRecord(
			id,
			reqVal(node, "type"),
			mapEventStructure(req(node, "detail"))
		);
	}

	private static IndividualAttributeRecord mapIndividualAttribute(final String id, final FLEFRecord node){
		return new IndividualAttributeRecord(
			id,
			Xref.of(reqVal(node, "individual")),
			reqVal(node, "type"),
			optVal(node, "value"),
			optChild(node, "date").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_from").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_to").map(ModelMapper::mapDateStructure),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static EventParticipationRecord mapEventParticipation(final String id, final FLEFRecord node){
		return new EventParticipationRecord(
			id,
			Xref.of(reqVal(node, "event")),
			XrefOrVoid.of(reqVal(node, "entity")),
			optVal(node, "role"),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapModification(req(node, "modification"))
		);
	}

	private static GroupRecord mapGroup(final String id, final FLEFRecord node){
		return new GroupRecord(
			id,
			node.findChildren("name").stream().map(ModelMapper::mapNameStructure).toList(),
			optVal(node, "type"),
			mapXrefList(node, "cultural_norm"),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "preferred_image").map(ModelMapper::mapPreferredImage),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static GroupAttributeRecord mapGroupAttribute(final String id, final FLEFRecord node){
		return new GroupAttributeRecord(
			id,
			Xref.of(reqVal(node, "group")),
			reqVal(node, "type"),
			optVal(node, "value"),
			optChild(node, "date").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_from").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_to").map(ModelMapper::mapDateStructure),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static RelationshipRecord mapRelationship(final String id, final FLEFRecord node){
		return new RelationshipRecord(
			id,
			XrefOrVoid.of(reqVal(node, "subject")),
			XrefOrVoid.of(reqVal(node, "object")),
			reqVal(node, "type"),
			optVal(node, "role"),
			optVal(node, "status"),
			optChild(node, "date").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_from").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_to").map(ModelMapper::mapDateStructure),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static PlaceRecord mapPlace(final String id, final FLEFRecord node){
		final Optional<PlaceRecord.MapStructure> mapStruct = optChild(node, "map")
			.map(m -> new PlaceRecord.MapStructure(
				reqVal(m, "coordinate"),
				optChild(m, "evidence").map(ModelMapper::mapEvidenceQualifiers)
			));

		return new PlaceRecord(
			id,
			node.findChildren("name").stream().map(ModelMapper::mapNameStructure).toList(),
			optVal(node, "type"),
			mapStruct,
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static PlaceRelationshipRecord mapPlaceRelationship(final String id, final FLEFRecord node){
		return new PlaceRelationshipRecord(
			id,
			Xref.of(reqVal(node, "subject")),
			Xref.of(reqVal(node, "object")),
			reqVal(node, "type"),
			optChild(node, "valid_from").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_to").map(ModelMapper::mapDateStructure),
			optVal(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			mapModification(req(node, "modification"))
		);
	}

	private static NoteRecord mapNote(final String id, final FLEFRecord node){
		final List<NoteRecord.Translation> translations = node.findChildren("translation").stream()
			.map(t -> new NoteRecord.Translation(reqVal(t, "value"), optVal(t, "locale")))
			.toList();

		return new NoteRecord(
			id,
			optVal(node, "title"),
			reqVal(node, "value"),
			optVal(node, "mime"),
			optVal(node, "locale"),
			translations,
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapModification(req(node, "modification"))
		);
	}

	private static RepositoryRecord mapRepository(final String id, final FLEFRecord node){
		return new RepositoryRecord(
			id,
			node.findChildren("name").stream().map(ModelMapper::mapNameStructure).toList(),
			optVal(node, "custodian").map(Xref::of),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			node.findChildren("contact").stream().map(ModelMapper::mapContact).toList(),
			mapXrefList(node, "note"),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapModification(req(node, "modification"))
		);
	}

	private static CulturalNormRecord mapCulturalNorm(final String id, final FLEFRecord node){
		return new CulturalNormRecord(
			id,
			optVal(node, "title"),
			optVal(node, "rule_type"),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			optChild(node, "valid_from").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_to").map(ModelMapper::mapDateStructure),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			mapModification(req(node, "modification"))
		);
	}

	private static SourceRecord mapSource(final String id, final FLEFRecord node){
		return new SourceRecord(
			id,
			node.findChildren("title").stream().map(ModelMapper::mapTextValue).toList(),
			optVal(node, "author"),
			optChild(node, "date").map(ModelMapper::mapDateStructure),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			optVal(node, "publisher"),
			node.findChildren("repository").stream().map(ModelMapper::mapRepositoryCitation).toList(),
			optVal(node, "media_type"),
			node.findChildren("document").stream().map(ModelMapper::mapDocumentStructure).toList(),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static HistoricEventRecord mapHistoricEvent(final String id, final FLEFRecord node){
		return new HistoricEventRecord(
			id,
			optVal(node, "title"),
			optChild(node, "date").map(ModelMapper::mapDateStructure),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			mapModification(req(node, "modification"))
		);
	}

	private static ResearchStatusRecord mapResearchStatus(final String id, final FLEFRecord node){
		final List<ResearchStatusRecord.Association> assoc = node.findChildren("association").stream()
			.map(a -> new ResearchStatusRecord.Association(XrefOrVoid.of(reqVal(a, "target")), optVal(a, "name")))
			.toList();

		return new ResearchStatusRecord(
			id,
			optVal(node, "status"),
			reqVal(node, "question"),
			optVal(node, "priority"),
			assoc,
			mapXrefList(node, "blocked_by"),
			optVal(node, "plan"),
			optVal(node, "resolution"),
			mapModification(req(node, "modification"))
		);
	}

	private static ResearchLogRecord mapResearchLog(final String id, final FLEFRecord node){
		return new ResearchLogRecord(
			id,
			reqVal(node, "action"),
			optVal(node, "target").map(XrefOrVoid::of),
			mapXrefList(node, "source"),
			optVal(node, "search_scope"),
			optVal(node, "search_outcome"),
			optVal(node, "finding"),
			optVal(node, "next_step"),
			mapXrefList(node, "follow_up"),
			optChild(node, "research").map(r -> Xref.of(r.getValue())),
			reqVal(node, "date"),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapModification(req(node, "modification"))
		);
	}

	private static ConclusionRecord mapConclusion(final String id, final FLEFRecord node){
		return new ConclusionRecord(
			id,
			reqVal(node, "context"),
			mapXrefList(node, "resolves"),
			optVal(node, "preferred").map(Xref::of),
			reqVal(node, "proof_status"),
			optVal(node, "narrative"),
			optVal(node, "research").map(Xref::of),
			optVal(node, "date"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList()
		);
	}

	private static IdentityHypothesisRecord mapIdentityHypothesis(final String id, final FLEFRecord node){
		return new IdentityHypothesisRecord(
			id,
			Xref.of(reqVal(node, "subject")),
			Xref.of(reqVal(node, "candidate")),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			optVal(node, "comment"),
			mapModification(req(node, "modification"))
		);
	}

	// ==========================================
	// STRUCTURE MAPPERS
	// ==========================================

	private static PersonalNameStructure mapPersonalName(final FLEFRecord node){
		final List<PersonalNameStructure.Part> parts = node.findChildren("part").stream()
			.map(p -> new PersonalNameStructure.Part(
				reqVal(p, "type"),
				reqVal(p, "value"),
				p.findChildren("variant").stream().map(ModelMapper::mapTextValueVariant).toList()
			)).toList();

		return new PersonalNameStructure(
			optVal(node, "type"),
			parts,
			mapXrefList(node, "cultural_norm"),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList()
		);
	}

	private static EventStructure mapEventStructure(final FLEFRecord node){
		return new EventStructure(
			optVal(node, "description"),
			optChild(node, "date").map(ModelMapper::mapDateStructure),
			optChild(node, "place").map(ModelMapper::mapPlaceStructure),
			optVal(node, "agency"),
			optChild(node, "cause").map(c -> new EventStructure.Cause(reqVal(c, "value"), optChild(c, "evidence").map(ModelMapper::mapEvidenceQualifiers))),
			mapXrefList(node, "cultural_norm"),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers),
			optChild(node, "restriction").map(ModelMapper::mapRestriction),
			mapXrefList(node, "conclusion"),
			mapModification(req(node, "modification"))
		);
	}

	private static NameStructure mapNameStructure(final FLEFRecord node){
		return new NameStructure(
			mapTextValue(req(node, "value")),
			optVal(node, "type")
		);
	}

	private static TextValue mapTextValue(final FLEFRecord node){
		return new TextValue(
			reqVal(node, "value"),
			node.findChildren("variant").stream().map(ModelMapper::mapTextValueVariant).toList(),
			optVal(node, "locale"),
			optChild(node, "valid_from").map(ModelMapper::mapDateStructure),
			optChild(node, "valid_to").map(ModelMapper::mapDateStructure),
			mapXrefList(node, "note"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList()
		);
	}

	private static TextValueVariant mapTextValueVariant(final FLEFRecord node){
		final FLEFRecord firstChild = (node.getChildren().isEmpty()? node: node.getChildren().getFirst());
		final String tag = firstChild.getTag().toLowerCase();

		if("phonetic".equals(tag))
			return new TextValueVariant.Phonetic(reqVal(firstChild, "system"), reqVal(firstChild, "value"));
		return new TextValueVariant.Transcription(
			reqVal(firstChild, "system"),
			optVal(firstChild, "type"),
			reqVal(firstChild, "value")
		);
	}

	private static DateStructure mapDateStructure(final FLEFRecord node){
		final FLEFRecord valNode = req(node, "value");
		final FLEFRecord firstChild = valNode.getChildren().getFirst();
		final String type = firstChild.getTag().toLowerCase();

		final DateValue dateVal = switch(type){
			case "point" -> new DateValue.Point(mapQualifiedDate(firstChild));
			case "bounded" -> new DateValue.Bounded(
				optChild(firstChild, "not_before").map(ModelMapper::mapQualifiedDate),
				optChild(firstChild, "not_after").map(ModelMapper::mapQualifiedDate)
			);
			case "spanning" -> new DateValue.Spanning(
				optChild(firstChild, "from").map(ModelMapper::mapQualifiedDate),
				optChild(firstChild, "to").map(ModelMapper::mapQualifiedDate)
			);
			default -> throw new IllegalArgumentException("Tipo DateValue sconosciuto: " + type);
		};

		return new DateStructure(
			dateVal,
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers)
		);
	}

	private static QualifiedDate mapQualifiedDate(final FLEFRecord node){
		final FLEFRecord singleNode = req(node, "single_date");
		final FLEFRecord dateChild = singleNode.getChildren().get(0);
		final String tag = dateChild.getTag().toLowerCase();

		final SingleDate singleDate = switch(tag){
			case "full_date" -> new SingleDate.FullDate(reqVal(dateChild, "value"), reqVal(dateChild, "calendar"));
			case "decade" ->
				new SingleDate.Decade(Integer.parseInt(reqVal(dateChild, "start_year")), reqVal(dateChild, "calendar"));
			case "century" -> new SingleDate.Century(
				Integer.parseInt(reqVal(dateChild, "ordinal")),
				optVal(dateChild, "part"),
				reqVal(dateChild, "calendar")
			);
			default -> throw new IllegalArgumentException("Tipo SingleDate non valido: " + tag);
		};

		final Optional<Approximate> approx = optChild(node, "approximate").map(a -> new Approximate(
			optVal(a, "basis"),
			optVal(a, "cultural_norm").map(Xref::of),
			optVal(a, "margin")
		));

		return new QualifiedDate(singleDate, approx);
	}

	private static PlaceStructure mapPlaceStructure(final FLEFRecord node){
		return new PlaceStructure(
			Xref.of(reqVal(node, "place")),
			optVal(node, "original_text"),
			node.findChildren("citation").stream().map(ModelMapper::mapSourceCitation).toList(),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers)
		);
	}

	private static SourceCitation mapSourceCitation(final FLEFRecord node){
		return new SourceCitation(
			Xref.of(reqVal(node, "source")),
			optVal(node, "location"),
			optChild(node, "crop").map(ModelMapper::mapCropCoord),
			optVal(node, "note"),
			optChild(node, "evidence").map(ModelMapper::mapEvidenceQualifiers)
		);
	}

	private static PreferredImage mapPreferredImage(final FLEFRecord node){
		return new PreferredImage(
			reqVal(node, "uri"),
			optChild(node, "crop").map(ModelMapper::mapCropCoord)
		);
	}

	private static CropCoord mapCropCoord(final FLEFRecord node){
		final FLEFRecord tl = req(node, "top_left");
		final FLEFRecord br = req(node, "bottom_right");
		return new CropCoord(
			new CropCoord.Point(Integer.parseInt(reqVal(tl, "x")), Integer.parseInt(reqVal(tl, "y"))),
			new CropCoord.Point(Integer.parseInt(reqVal(br, "x")), Integer.parseInt(reqVal(br, "y")))
		);
	}

	private static ContactStructure mapContact(final FLEFRecord node){
		final Optional<ContactStructure.CallerId> callerId = optChild(node, "caller_id").map(c -> new ContactStructure.CallerId(
			reqVal(c, "value"),
			c.findChildren("variant").stream().map(ModelMapper::mapTextValueVariant).toList()
		));

		return new ContactStructure(
			reqVal(node, "address"),
			optVal(node, "type"),
			callerId,
			optVal(node, "note"),
			optChild(node, "restriction").map(ModelMapper::mapRestriction)
		);
	}

	private static RepositoryCitation mapRepositoryCitation(final FLEFRecord node){
		return new RepositoryCitation(
			Xref.of(reqVal(node, "repository")),
			optVal(node, "location"),
			optVal(node, "note")
		);
	}

	private static DocumentStructure mapDocumentStructure(final FLEFRecord node){
		final List<DocumentStructure.Extract> extracts = node.findChildren("extract").stream()
			.map(e -> new DocumentStructure.Extract(reqVal(e, "text"), reqVal(e, "type"), optVal(e, "locale")))
			.toList();

		return new DocumentStructure(
			reqVal(node, "file"),
			optVal(node, "spherical").map(Boolean::parseBoolean),
			optVal(node, "mapping"),
			optVal(node, "description"),
			extracts,
			mapXrefList(node, "note"),
			optChild(node, "restriction").map(ModelMapper::mapRestriction)
		);
	}

	private static RestrictionStructure mapRestriction(final FLEFRecord node){
		return new RestrictionStructure(
			reqVal(node, "level"),
			optVal(node, "reason"),
			optVal(node, "expires")
		);
	}

	private static ModificationStructure mapModification(final FLEFRecord node){
		final FLEFRecord cr = req(node, "creation");
		final ModificationStructure.Change creation = new ModificationStructure.Change(reqVal(cr, "date"), optVal(cr, "comment"));

		final List<ModificationStructure.Change> updates = node.findChildren("update").stream()
			.map(u -> new ModificationStructure.Change(reqVal(u, "date"), optVal(u, "comment")))
			.toList();

		return new ModificationStructure(creation, updates);
	}

	private static EvidenceQualifiers mapEvidenceQualifiers(final FLEFRecord node){
		return new EvidenceQualifiers(
			optVal(node, "certainty"),
			optVal(node, "source_type"),
			optVal(node, "information_type"),
			optVal(node, "evidence_type")
		);
	}

	// ==========================================
	// UTILITY METHOD HELPER
	// ==========================================

	private static FLEFRecord req(final FLEFRecord parent, final String tag){
		final FLEFRecord child = parent.findChild(tag);
		if(child == null)
			throw new IllegalArgumentException("Missing required property '" + tag + "' in " + parent.getTag());

		return child;
	}

	private static String reqVal(final FLEFRecord parent, final String tag){
		final String val = parent.getChildValue(tag);
		if(val == null){
			// If the value is directly on the node itself
			if(parent.getTag().equalsIgnoreCase(tag) && parent.getValue() != null)
				return parent.getValue();

			throw new IllegalArgumentException("Value for '" + tag + "' missing in " + parent.getTag());
		}
		return val;
	}

	private static Optional<String> optVal(final FLEFRecord parent, final String tag){
		return Optional.ofNullable(parent.getChildValue(tag));
	}

	private static Optional<FLEFRecord> optChild(final FLEFRecord parent, final String tag){
		return Optional.ofNullable(parent.findChild(tag));
	}

	private static <T> List<Xref<T>> mapXrefList(final FLEFRecord parent, final String tag){
		return parent.findChildren(tag).stream()
			.map(r -> Xref.<T>of(r.getValue() != null ? r.getValue() : r.getReferenceId()))
			.toList();
	}

}
