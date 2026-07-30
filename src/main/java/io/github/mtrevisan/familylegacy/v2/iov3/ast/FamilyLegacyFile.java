package io.github.mtrevisan.familylegacy.v2.iov3.ast;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.records.Header;

import java.util.List;


public record FamilyLegacyFile(
	Header header,
	List<Record> records
){
}
