package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;


public sealed interface Record permits
	IndividualRecord, GroupRecord, EventRecord, IndividualAttributeRecord,
	EventParticipationRecord, GroupAttributeRecord, RelationshipRecord, PlaceRecord,
	PlaceRelationshipRecord, NoteRecord, RepositoryRecord, CulturalNormRecord,
	SourceRecord, ConclusionRecord, HistoricEventRecord, IdentityHypothesisRecord,
	ResearchStatusRecord, ResearchLogRecord{
}
