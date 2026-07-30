package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.PersonalNameStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.PreferredImage;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.RestrictionStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.SourceCitation;

import java.util.List;
import java.util.Optional;


public record IndividualRecord(
	String id,
	List<PersonalNameStructure> name,
	Optional<String> sex,
	List<Xref<CulturalNormRecord>> culturalNorm,
	List<Xref<NoteRecord>> note,
	List<SourceCitation> citation,
	Optional<PreferredImage> preferredImage,
	Optional<RestrictionStructure> restriction,
	List<Xref<ConclusionRecord>> conclusion,
	ModificationStructure modification
) implements Record{}
