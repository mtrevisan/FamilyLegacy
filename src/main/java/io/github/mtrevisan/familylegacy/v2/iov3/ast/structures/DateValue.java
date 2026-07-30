package io.github.mtrevisan.familylegacy.v2.iov3.ast.structures;

import java.util.Optional;


public sealed interface DateValue permits DateValue.Point, DateValue.Bounded, DateValue.Spanning{
	record Point(QualifiedDate date) implements DateValue{}

	record Bounded(Optional<QualifiedDate> notBefore, Optional<QualifiedDate> notAfter) implements DateValue{}

	record Spanning(Optional<QualifiedDate> from, Optional<QualifiedDate> to) implements DateValue{}
}
