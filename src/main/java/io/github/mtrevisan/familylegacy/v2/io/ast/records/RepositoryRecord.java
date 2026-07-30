package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ContactStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.NameStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.PlaceStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.RestrictionStructure;

import java.util.List;
import java.util.Optional;


public record RepositoryRecord(
	String id,
	List<NameStructure> name,
	Optional<Xref<IndividualRecord>> custodian,
	Optional<PlaceStructure> place,
	List<ContactStructure> contact,
	List<Xref<NoteRecord>> note,
	Optional<RestrictionStructure> restriction,
	ModificationStructure modification
) implements Record{}
