package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.Optional;


public record NameStructure(
	TextValue value,
	Optional<String> type
){}
