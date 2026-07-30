package io.github.mtrevisan.familylegacy.v2.io.ast;

import io.github.mtrevisan.familylegacy.v2.io.ast.records.Header;

import java.util.List;


public record FamilyLegacyFile(
	Header header,
	List<Record> records
){
}
