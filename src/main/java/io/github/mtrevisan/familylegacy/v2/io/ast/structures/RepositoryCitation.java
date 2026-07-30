package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import io.github.mtrevisan.familylegacy.v2.io.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.io.ast.records.RepositoryRecord;

import java.util.Optional;


public record RepositoryCitation(
	Xref<RepositoryRecord> repository,
	Optional<String> location,
	Optional<String> note
){}
