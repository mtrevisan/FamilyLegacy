package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;

import java.util.Objects;


/**
 * Abstract base class for record type handlers.
 * Provides default implementations for {@link #equals(Object)}, {@link #hashCode()},
 * and {@link #toString()} based on the handler's type.
 *
 * @param <T> the dialog type associated with this handler
 */
public abstract class AbstractRecordTypeHandler<T extends BaseRecordDialog> implements RecordTypeHandler<T>{

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final RecordTypeHandler<?> other = (RecordTypeHandler<?>)obj;
		return Objects.equals(getType(), other.getType());
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(getType());
	}

	@Override
	public String toString(){
		return getType();
	}

}
