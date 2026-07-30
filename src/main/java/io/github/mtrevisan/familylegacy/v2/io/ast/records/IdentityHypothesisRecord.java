package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record IdentityHypothesisRecord(
	String id,
	Xref<String> subject,
	Xref<String> candidate,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence,
	Optional<String> comment,
	ModificationStructure modification
) implements Record{}
