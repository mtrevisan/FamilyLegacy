package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


final class TransformationHelper{

	private TransformationHelper(){}

	public static Object getStructure(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		final List<Object> elements = context.read(selector.toString());
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());
		return (!elements.isEmpty()? elements.get(0): null);
	}

	public static List<Object> getStructures(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		return context.read(selector.toString());
	}

	public static void moveValueOfKey(final String key, final String value, final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		final List<Object> elements = context.read(selector.toString());
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());

		if(!elements.isEmpty())
			((Map<String, Object>)elements.get(0)).put(key, value);
	}

	public static void deleteKey(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		context.delete(selector.toString());
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> extractPlace(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		final List<Object> elements = context.read(selector.toString());
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());

		if(!elements.isEmpty()){
			final Map<String, Object> address = (Map<String, Object>)elements.get(0);
			final StringJoiner street = new StringJoiner(" - ");
			String value = (String)address.get("value");
			selector.append(".children[?(@.tag in ['ADDR','CONT','ADR1','ADR2','ADR3'])]");
			for(final Object streetComponent : (List<Object>)context.read(selector.toString())){
				value = (String)((Map<String, Object>)streetComponent).get("value");
				if(value != null && !value.isEmpty())
					street.add(value);
			}
			if(!street.toString().isEmpty())
				value = street.toString();

			final GedcomNode place = new GedcomNode(0, "PLACE");
			//TODO calculate ID
			place.setXRef("@P1@");
			if(value != null && !value.isEmpty()){
				final GedcomNode component = new GedcomNode(1, "STREET");
				component.setValue(value);
				place.addChild(component);
			}
			/*n STREET <ADDRESS_STREET>    {0:1}					n ADDR + +1 CONT + +1 ADR1 + +1 ADR2 + +1 ADR3
			n CITY <ADDRESS_CITY>    {0:1}							+1 CITY <ADDRESS_CITY>    {0:1}
			n STATE <ADDRESS_STATE>    {0:1}							+1 STAE <ADDRESS_STATE>    {0:1}
			n POSTAL_CODE <ADDRESS_POSTAL_CODE>    {0:1}			+1 POST <ADDRESS_POSTAL_CODE>    {0:1}
			n COUNTRY <ADDRESS_COUNTRY>    {0:1}					+1 CTRY <ADDRESS_COUNTRY>    {0:1}
			n PHONE <PHONE_NUMBER>    {0:M}						n PHON <PHONE_NUMBER>    {0:3}
			n FAX <ADDRESS_FAX>    {0:M}							n FAX <ADDRESS_FAX>    {0:3}
			n EMAIL <ADDRESS_EMAIL>    {0:M}						n EMAIL <ADDRESS_EMAIL>    {0:3}
			n WEB <ADDRESS_WEB_PAGE>    {0:M}					n WWW <ADDRESS_WEB_PAGE>    {0:3}*/

//			final StringBuilder sb = new StringBuilder((String)address.get("value"));
//			selector.append(".children[?(@.tag in ['CONC','CONT'])]");
//			final List<Object> noteChildren = context.read(selector.toString());
//			for(final Object noteChild : noteChildren){
//				if(((CharSequence)((Map<String, Object>)noteChild).get("tag")).charAt(3) == 'T')
//					sb.append("\\n");
//				sb.append(((Map<String, Object>)noteChild).get("value"));
//			}
//			address.put("value", sb.toString());
			context.delete(selector.toString());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static void mergeNote(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		final List<Object> elements = context.read(selector.toString());
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());

		if(!elements.isEmpty()){
			final Map<String, Object> note = (Map<String, Object>)elements.get(0);

			final StringBuilder sb = new StringBuilder((String)note.get("value"));
			selector.append(".children[?(@.tag in ['CONC','CONT'])]");
			final List<Object> noteChildren = context.read(selector.toString());
			for(final Object noteChild : noteChildren){
				if(((CharSequence)((Map<String, Object>)noteChild).get("tag")).charAt(3) == 'T')
					sb.append("\\n");
				sb.append(((Map<String, Object>)noteChild).get("value"));
			}
			note.put("value", sb.toString());
			context.delete(selector.toString());
		}
	}

	private static StringBuilder composeSelector(final String[] keys){
		final StringBuilder selector = new StringBuilder("$");
		for(final String key : keys)
			selector.append(".children[?(@.tag=='").append(key).append("')]");
		return selector;
	}

}
