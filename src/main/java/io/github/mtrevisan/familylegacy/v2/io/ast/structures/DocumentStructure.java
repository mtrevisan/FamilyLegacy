package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.records.NoteRecord;

import java.util.List;
import java.util.Optional;


public record DocumentStructure(
	String file,
	Optional<Boolean> spherical,
	Optional<String> mapping,
	Optional<String> description,
	List<Extract> extract,
	List<Xref<NoteRecord>> note,
	Optional<RestrictionStructure> restriction
){
	public record Extract(String text, String type, Optional<String> locale){}
}
