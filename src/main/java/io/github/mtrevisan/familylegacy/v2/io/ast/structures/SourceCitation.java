package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.records.SourceRecord;

import java.util.Optional;


public record SourceCitation(
	Xref<SourceRecord> source,
	Optional<String> location,
	Optional<CropCoord> crop,
	Optional<String> note,
	Optional<EvidenceQualifiers> evidence
){}
