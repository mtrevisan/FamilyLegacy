package io.github.mtrevisan.familylegacy.v2.io.ast.records;

import io.github.mtrevisan.familylegacy.v2.io.ast.structures.EventStructure;


public record EventRecord(String id, String type, EventStructure detail) implements Record{}
