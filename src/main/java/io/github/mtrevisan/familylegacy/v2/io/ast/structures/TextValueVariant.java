package io.github.mtrevisan.familylegacy.v2.io.ast.structures;

import java.util.Optional;


public sealed interface TextValueVariant permits TextValueVariant.Phonetic, TextValueVariant.Transcription{
	record Phonetic(String system, String value) implements TextValueVariant{}

	record Transcription(String system, Optional<String> type, String value) implements TextValueVariant{}
}
