package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.XrefOrVoid;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record RelationshipRecord(
	String id,
	XrefOrVoid<String> subject,
	XrefOrVoid<String> object,
	String type,
	Optional<String> role,
	Optional<String> status,
	Optional<DateStructure> date,
	Optional<DateStructure> validFrom,
	Optional<DateStructure> validTo,
	Optional<EvidenceQualifiers> evidence,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	Optional<RestrictionStructure> restriction,
	List<Xref<ConclusionRecord>> conclusion,
	ModificationStructure modification
) implements Record{}
