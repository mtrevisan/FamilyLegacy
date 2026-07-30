package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record ConclusionRecord(
	String id,
	String context,
	List<Xref<String>> resolves,
	Optional<Xref<String>> preferred,
	String proofStatus,
	Optional<String> narrative,
	Optional<Xref<ResearchStatusRecord>> research,
	Optional<String> date,
	List<SourceCitation> citation
) implements Record{}
