package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.Xref;
import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.RepositoryRecord;

import java.util.Optional;


public record RepositoryCitation(
	Xref<RepositoryRecord> repository,
	Optional<String> location,
	Optional<String> note
){}
