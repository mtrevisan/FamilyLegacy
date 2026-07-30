package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.records.CulturalNormRecord;
import io.github.mtrevisan.familylegacy.v2.io.ast.records.NoteRecord;

import java.util.List;
import java.util.Optional;


public record PersonalNameStructure(
	Optional<String> type,
	List<Part> part,
	List<Xref<CulturalNormRecord>> culturalNorm,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation
){
	public record Part(String type, String value, List<TextValueVariant> variant){}
}
