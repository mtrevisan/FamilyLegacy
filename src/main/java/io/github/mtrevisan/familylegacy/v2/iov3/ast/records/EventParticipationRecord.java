package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.XrefOrVoid;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record EventParticipationRecord(
	String id,
	Xref<EventRecord> event,
	XrefOrVoid<String> entity,
	Optional<String> role,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence,
	Optional<RestrictionStructure> restriction,
	ModificationStructure modification
) implements Record{}
