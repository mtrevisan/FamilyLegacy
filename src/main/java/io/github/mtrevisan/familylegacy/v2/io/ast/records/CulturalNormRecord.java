package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record CulturalNormRecord(
	String id,
	Optional<String> title,
	Optional<String> ruleType,
	Optional<PlaceStructure> place,
	Optional<DateStructure> validFrom,
	Optional<DateStructure> validTo,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence,
	ModificationStructure modification
) implements Record{}
