package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.records.CulturalNormRecord;

import java.util.Optional;


public record Approximate(
	Optional<String> basis,
	Optional<Xref<CulturalNormRecord>> culturalNorm,
	Optional<String> margin){}
