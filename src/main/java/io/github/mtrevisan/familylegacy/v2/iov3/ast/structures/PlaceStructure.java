package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.PlaceRecord;

import java.util.List;
import java.util.Optional;


public record PlaceStructure(
	Xref<PlaceRecord> place,
	Optional<String> originalText,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence
){}
