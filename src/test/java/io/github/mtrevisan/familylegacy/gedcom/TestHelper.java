package io.github.mtrevisan.familylegacy.gedcom;

import java.lang.reflect.Field;


public final class TestHelper{

	private TestHelper(){}

	public static void inject(final Object obj, final String variableName, final Object value) throws NoSuchFieldException,
			IllegalAccessException{
		final Field headerField = obj.getClass().getDeclaredField(variableName);
		headerField.setAccessible(true);
		headerField.set(obj, value);
	}

}
