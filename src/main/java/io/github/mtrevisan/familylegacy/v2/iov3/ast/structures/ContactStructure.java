package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.List;
import java.util.Optional;


public record ContactStructure(
	String address,
	Optional<String> type,
	Optional<CallerId> callerId,
	Optional<String> note,
	Optional<RestrictionStructure> restriction
){
	public record CallerId(String value, List<TextValueVariant> variant){}
}
