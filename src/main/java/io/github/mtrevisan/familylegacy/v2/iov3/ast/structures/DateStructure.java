package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.List;
import java.util.Optional;


public record DateStructure(
	DateValue value,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence
){}
