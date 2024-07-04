package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import io.github.mtrevisan.familylegacy.services.JavaHelper;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;


public class ZeroException extends Exception{

	@Serial
	private static final long serialVersionUID = -1960874856936000015L;


	public static ZeroException create(final String message, final Object... parameters){
		return new ZeroException(JavaHelper.textFormat(message, parameters));
	}


	private ZeroException(final String message){
		super(message);
	}


	@Serial
	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@Serial
	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
