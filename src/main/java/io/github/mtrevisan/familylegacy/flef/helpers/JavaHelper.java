package io.github.mtrevisan.familylegacy.flef.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;


public final class JavaHelper{

	private JavaHelper(){}


	public static <K, V> Map<K, V> deepClone(final Map<K, V> original){
		if(original == null)
			return null;

		try{
			final byte[] byteArray = serializeToByteArray(original);
			return deserializeFromByteArray(byteArray);
		}
		catch(final IOException | ClassNotFoundException e){
			e.printStackTrace();

			return null;
		}
	}

	private static <K, V> byte[] serializeToByteArray(final Map<K, V> original) throws IOException{
		try(
				final ByteArrayOutputStream bos = new ByteArrayOutputStream();
				final ObjectOutputStream out = new ObjectOutputStream(bos)){
			out.writeObject(original);
			return bos.toByteArray();
		}
	}

	@SuppressWarnings("unchecked")
	private static <K, V> Map<K, V> deserializeFromByteArray(final byte[] byteArray) throws IOException, ClassNotFoundException{
		try(
				final ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
				final ObjectInputStream in = new ObjectInputStream(bis)){
			return (Map<K, V>)in.readObject();
		}
	}

}
