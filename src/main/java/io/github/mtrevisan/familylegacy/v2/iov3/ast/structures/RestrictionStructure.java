package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.Optional;


public record RestrictionStructure(
	String level,
	Optional<String> reason,
	Optional<String> expires
){}
