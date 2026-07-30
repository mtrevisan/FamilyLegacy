package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import java.util.List;
import java.util.Optional;


public record ModificationStructure(
	Change creation,
	List<Change> update
){
	public record Change(String date, Optional<String> comment){}
}
