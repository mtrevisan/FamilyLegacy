package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record IndividualAttributeRecord(
	String id,
	Xref<IndividualRecord> individual,
	String type,
	Optional<String> value,
	Optional<DateStructure> date,
	Optional<DateStructure> validFrom,
	Optional<DateStructure> validTo,
	Optional<PlaceStructure> place,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence,
	Optional<RestrictionStructure> restriction,
	List<Xref<ConclusionRecord>> conclusion,
	ModificationStructure modification
) implements Record{}
