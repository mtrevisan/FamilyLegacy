package io.github.mtrevisan.familylegacy.flef;

import java.util.Objects;


record GenericKey(Object[] key){

	@Override
	public int hashCode(){
		int hash = 0;
		for(int i = 0, keyLength = key.length; i < keyLength; i ++)
			hash = 31 * hash + Objects.hashCode(key[i]);
		return hash;
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final GenericKey other = (GenericKey)obj;
		return Objects.deepEquals(key, other.key);
	}

}
