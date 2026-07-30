package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.ConclusionRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.CulturalNormRecord;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.NoteRecord;

import java.util.List;
import java.util.Optional;


public record EventStructure(
	Optional<String> description,
	Optional<DateStructure> date,
	Optional<PlaceStructure> place,
	Optional<String> agency,
	Optional<Cause> cause,
	List<Xref<CulturalNormRecord>> culturalNorm,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	Optional<EvidenceQualifiers> evidence,
	Optional<RestrictionStructure> restriction,
	List<Xref<ConclusionRecord>> conclusion,
	ModificationStructure modification
){
	public record Cause(String value, Optional<EvidenceQualifiers> evidence){}
}
