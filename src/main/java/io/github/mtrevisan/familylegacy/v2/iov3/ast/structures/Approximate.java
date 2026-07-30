package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.CulturalNormRecord;

import java.util.Optional;


public record Approximate(
	Optional<String> basis,
	Optional<Xref<CulturalNormRecord>> culturalNorm,
	Optional<String> margin){}
