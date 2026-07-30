package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.Optional;


public record EvidenceQualifiers(
	Optional<String> certainty,
	Optional<String> sourceType,
	Optional<String> informationType,
	Optional<String> evidenceType
){}
