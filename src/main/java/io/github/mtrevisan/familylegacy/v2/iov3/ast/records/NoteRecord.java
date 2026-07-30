package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record NoteRecord(
	String id,
	Optional<String> title,
	String value,
	Optional<String> mime,
	Optional<String> locale,
	List<Translation> translation,
	List<SourceCitation> citation,
	Optional<RestrictionStructure> restriction,
	ModificationStructure modification
) implements Record{
	public record Translation(String value, Optional<String> locale){}
}
