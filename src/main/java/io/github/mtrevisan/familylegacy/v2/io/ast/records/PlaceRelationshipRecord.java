package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record PlaceRelationshipRecord(
	String id,
	Xref<PlaceRecord> subject,
	Xref<PlaceRecord> object,
	String type,
	Optional<DateStructure> validFrom,
	Optional<DateStructure> validTo,
	Optional<String> note,
	List<SourceCitation> citation,
	ModificationStructure modification
) implements Record{}
