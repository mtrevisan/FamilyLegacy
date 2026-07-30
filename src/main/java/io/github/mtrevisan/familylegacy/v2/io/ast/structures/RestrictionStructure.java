package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import java.util.Optional;


public record RestrictionStructure(
	String level,
	Optional<String> reason,
	Optional<String> expires
){}
