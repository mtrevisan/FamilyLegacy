package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;


public class FilterString{

	private static final String FILER_DELIMITER = " | ";

	private final StringJoiner filter = new StringJoiner(FILER_DELIMITER);
	private final Set<String> data = new HashSet<>(0);


	public static FilterString create(){
		return new FilterString();
	}


	private FilterString(){}


	public FilterString add(final Integer value){
		if(value != null)
			data.add(Integer.toString(value));

		return this;
	}

	public FilterString add(final String text){
		if(StringUtils.isNotBlank(text))
			data.add(text);

		return this;
	}

	public FilterString add(final StringJoiner sj){
		if(sj != null)
			add(sj.toString());

		return this;
	}

	@Override
	public String toString(){
		for(final String datum : data)
			filter.add(datum);
		return filter.toString();
	}

}
