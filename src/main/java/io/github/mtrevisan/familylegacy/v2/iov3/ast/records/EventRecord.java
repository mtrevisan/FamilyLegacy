package io.github.mtrevisan.familylegacy.v2.iov3.ast.records;

import io.github.mtrevisan.familylegacy.v2.iov3.ast.structures.EventStructure;


public record EventRecord(String id, String type, EventStructure detail) implements Record{}
