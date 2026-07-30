package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record HistoricEventRecord(
	String id,
	Optional<String> title,
	Optional<DateStructure> date,
	Optional<PlaceStructure> place,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	ModificationStructure modification
) implements Record{}
