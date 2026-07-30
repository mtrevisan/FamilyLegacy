package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.ContactStructure;

import java.util.List;
import java.util.Optional;


public record Header(
	Protocol protocol,
	Source source,
	String date,
	Optional<String> copyright,
	Submitter submitter,
	Optional<String> scope
) {
	public record Protocol(String name, String version) {}
	public record Source(String systemId, Optional<String> name, Optional<String> version, Optional<String> corporate) {}
	public record Submitter(String name, List<ContactStructure> contact, List<String> note) {}
}
