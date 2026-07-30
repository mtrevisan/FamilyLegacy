package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.NoteRecord;

import java.util.List;
import java.util.Optional;


public record TextValue(
	String value,
	List<TextValueVariant> variant,
	Optional<String> locale,
	Optional<DateStructure> validFrom,
	Optional<DateStructure> validTo,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation
){}
