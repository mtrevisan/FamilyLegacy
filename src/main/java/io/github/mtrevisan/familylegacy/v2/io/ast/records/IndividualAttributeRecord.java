package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

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
