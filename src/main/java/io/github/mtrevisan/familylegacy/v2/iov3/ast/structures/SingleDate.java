package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.Optional;


public sealed interface SingleDate permits SingleDate.FullDate, SingleDate.Decade, SingleDate.Century{
	record FullDate(String value, String calendar) implements SingleDate{}

	record Decade(int startYear, String calendar) implements SingleDate{}

	record Century(int ordinal, Optional<String> part, String calendar) implements SingleDate{}
}
