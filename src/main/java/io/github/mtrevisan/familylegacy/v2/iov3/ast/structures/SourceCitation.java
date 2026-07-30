package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.SourceRecord;

import java.util.Optional;


public record SourceCitation(
	Xref<SourceRecord> source,
	Optional<String> location,
	Optional<CropCoord> crop,
	Optional<String> note,
	Optional<EvidenceQualifiers> evidence
){}
