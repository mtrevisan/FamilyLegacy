package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.XrefOrVoid;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;

import java.util.List;
import java.util.Optional;


public record ResearchStatusRecord(
	String id,
	Optional<String> status,
	String question,
	Optional<String> priority,
	List<Association> association,
	List<Xref<ResearchStatusRecord>> blockedBy,
	Optional<String> plan,
	Optional<String> resolution,
	ModificationStructure modification
) implements Record{
	public record Association(XrefOrVoid<String> target, Optional<String> name){}
}
