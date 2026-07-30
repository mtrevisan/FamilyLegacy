package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.EvidenceQualifiers;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.NameStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record PlaceRecord(
	String id,
	List<NameStructure> name,
	Optional<String> type,
	Optional<MapStructure> map,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence,
	Optional<RestrictionStructure> restriction,
	List<Xref<ConclusionRecord>> conclusion,
	ModificationStructure modification
) implements Record{
	public record MapStructure(String coordinate, Optional<EvidenceQualifiers> evidence){}
}
