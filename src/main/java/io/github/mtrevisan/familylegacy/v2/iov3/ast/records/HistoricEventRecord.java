package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

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
