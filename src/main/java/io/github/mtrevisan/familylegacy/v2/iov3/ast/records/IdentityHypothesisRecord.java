package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

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
