package io.github.mtrevisan.familylegacy.v2.io.grammar;

import io.github.mtrevisan.familylegacy.v2.io.grammar.typedefinitions.FieldDefinition;


public record FileDefinition(String name, FieldDefinition headerField, FieldDefinition recordsField){

}
