package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DateStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.DocumentStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RepositoryCitation;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.TextValue;

import java.util.List;
import java.util.Optional;


public record SourceRecord(
	String id,
	List<TextValue> title,
	Optional<String> author,
	Optional<DateStructure> date,
	Optional<PlaceStructure> place,
	Optional<String> publisher,
	List<RepositoryCitation> repository,
	Optional<String> mediaType,
	List<DocumentStructure> document,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	Optional<RestrictionStructure> restriction,
	List<Xref<ConclusionRecord>> conclusion,
	ModificationStructure modification
) implements Record{}
