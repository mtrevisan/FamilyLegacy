package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.XrefOrVoid;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.ModificationStructure;
import io.github.mtrevisan.familylegacy.v2.io.ast.structures.RestrictionStructure;

import java.util.List;
import java.util.Optional;


public record ResearchLogRecord(
	String id,
	String action,
	Optional<XrefOrVoid<String>> target,
	List<Xref<SourceRecord>> source,
	Optional<String> searchScope,
	Optional<String> searchOutcome,
	Optional<String> finding,
	Optional<String> nextStep,
	List<Xref<ResearchLogRecord>> followUp,
	Optional<Xref<ResearchStatusRecord>> research,
	String date,
	Optional<RestrictionStructure> restriction,
	ModificationStructure modification
) implements Record{}
