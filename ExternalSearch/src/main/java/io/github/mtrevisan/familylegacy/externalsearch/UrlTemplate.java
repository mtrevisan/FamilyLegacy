/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mtrevisan.familylegacy.externalsearch;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;


/**
 * The address of a search on a site, with the values of the person between braces:
 * {@code https://example.org/search?first={name_given}&last={surname}&born={birth_year}}.
 * <p>
 * A placeholder is one of {@link Placeholder}; {@code {credential:<name>}} is a credential of the user for the site (an API key, a
 * user name), kept apart from the template (see {@link CredentialStore}). A brace that is part of the address is written twice
 * ({@code {{} and {@code }}}), and is percent-encoded in the address. Every value is percent-encoded (UTF-8, a space is {@code %20}); a value not known is empty. The address
 * must be {@code http} or {@code https}.
 */
public final class UrlTemplate{

	/** What comes before the name of a credential, between the braces. */
	public static final String CREDENTIAL_PREFIX = "credential:";

	/** Written in place of a credential where the address is shown or kept (never the credential itself). */
	public static final String HIDDEN_CREDENTIAL = "***";

	private static final String HTTP = "http://";
	private static final String HTTPS = "https://";
	private static final char OPEN = '{';
	private static final char CLOSE = '}';
	/** A brace is not a valid character of an address: written twice in a template, it is percent-encoded. */
	private static final String ENCODED_OPEN = "%7B";
	private static final String ENCODED_CLOSE = "%7D";


	/** Why a template is not valid. */
	public enum Problem{
		/** There is no template. */
		EMPTY,
		/** The address is not {@code http} or {@code https}. */
		NOT_WEB,
		/** A brace is opened and not closed. */
		UNCLOSED_BRACE,
		/** The name between the braces is not a known placeholder. */
		UNKNOWN_PLACEHOLDER,
		/** A credential without a name, or with characters a name cannot have. */
		BAD_CREDENTIAL_NAME,
		/** Once filled, the text is not a valid address. */
		INVALID_ADDRESS;

		/** @return	The (localized) explanation. */
		public String label(){
			return I18N.t("external_search.template.problem." + name().toLowerCase(Locale.ROOT));
		}
	}

	/** A template that is not valid. */
	public static final class TemplateException extends Exception{

		private final Problem problem;
		private final String detail;


		TemplateException(final Problem problem, final String detail){
			super(problem + (detail != null? ": " + detail: StringUtils.EMPTY));

			this.problem = problem;
			this.detail = detail;
		}

		public Problem problem(){
			return problem;
		}

		/** @return	What is wrong (the unknown name, the bad text), or {@code null}. */
		public String detail(){
			return detail;
		}

		/** @return	The (localized) explanation, with the detail. */
		public String describe(){
			return (detail != null? problem.label() + ": " + detail: problem.label());
		}

	}


	private sealed interface Segment permits Literal, Value, Credential{}

	private record Literal(String text) implements Segment{}

	private record Value(Placeholder placeholder) implements Segment{}

	private record Credential(String name) implements Segment{}


	private final String text;
	private final List<Segment> segments;


	private UrlTemplate(final String text, final List<Segment> segments){
		this.text = text;
		this.segments = segments;
	}

	/**
	 * @param text	The template.
	 * @return	The template, checked.
	 * @throws TemplateException	If the template is not valid.
	 */
	public static UrlTemplate parse(final String text) throws TemplateException{
		if(StringUtils.isBlank(text))
			throw new TemplateException(Problem.EMPTY, null);

		final String trimmed = text.strip();
		if(!Strings.CI.startsWith(trimmed, HTTP) && !Strings.CI.startsWith(trimmed, HTTPS))
			throw new TemplateException(Problem.NOT_WEB, StringUtils.abbreviate(trimmed, 40));

		final List<Segment> segments = new ArrayList<>();
		final StringBuilder literal = new StringBuilder();
		int index = 0;
		final int length = trimmed.length();
		while(index < length){
			final char c = trimmed.charAt(index);
			if(c == OPEN && index + 1 < length && trimmed.charAt(index + 1) == OPEN){
				literal.append(ENCODED_OPEN);
				index += 2;
			}
			else if(c == CLOSE && index + 1 < length && trimmed.charAt(index + 1) == CLOSE){
				literal.append(ENCODED_CLOSE);
				index += 2;
			}
			else if(c == OPEN){
				final int end = trimmed.indexOf(CLOSE, index + 1);
				final int nextOpen = trimmed.indexOf(OPEN, index + 1);
				if(end < 0 || (nextOpen >= 0 && nextOpen < end))
					throw new TemplateException(Problem.UNCLOSED_BRACE, trimmed.substring(index, Math.min(length, index + 20)));

				if(!literal.isEmpty()){
					segments.add(new Literal(literal.toString()));
					literal.setLength(0);
				}
				segments.add(token(trimmed.substring(index + 1, end)));
				index = end + 1;
			}
			else{
				literal.append(c);
				index ++;
			}
		}
		if(!literal.isEmpty())
			segments.add(new Literal(literal.toString()));

		final UrlTemplate template = new UrlTemplate(trimmed, List.copyOf(segments));
		//filled with values that need encoding, the result must still be an address
		final String sample = template.fill(placeholder -> "a b", name -> "x");
		try{
			final URI uri = new URI(sample);
			if(uri.getHost() == null)
				throw new TemplateException(Problem.INVALID_ADDRESS, StringUtils.abbreviate(trimmed, 60));
		}
		catch(final URISyntaxException e){
			throw new TemplateException(Problem.INVALID_ADDRESS, e.getReason());
		}
		return template;
	}

	private static Segment token(final String content) throws TemplateException{
		final String name = content.strip();
		if(Strings.CI.startsWith(name, CREDENTIAL_PREFIX)){
			final String credential = name.substring(CREDENTIAL_PREFIX.length()).strip();
			if(!SearchSite.isValidName(credential))
				throw new TemplateException(Problem.BAD_CREDENTIAL_NAME, OPEN + content + CLOSE);

			return new Credential(credential);
		}

		return Placeholder.of(name)
			.<Segment>map(Value::new)
			.orElseThrow(() -> new TemplateException(Problem.UNKNOWN_PLACEHOLDER, OPEN + content + CLOSE));
	}


	/** @return	The template as written. */
	public String text(){
		return text;
	}

	/** @return	The placeholders the template contains, in their order. */
	public Set<Placeholder> placeholders(){
		final Set<Placeholder> placeholders = EnumSet.noneOf(Placeholder.class);
		for(final Segment segment : segments)
			if(segment instanceof final Value value)
				placeholders.add(value.placeholder());
		return placeholders;
	}

	/** @return	The names of the credentials the template contains, in their order. */
	public Set<String> credentials(){
		final Set<String> names = new LinkedHashSet<>();
		for(final Segment segment : segments)
			if(segment instanceof final Credential credential)
				names.add(credential.name());
		return Collections.unmodifiableSet(names);
	}

	/**
	 * @param subject	The values of the person.
	 * @param credentials	The value of each credential by name ({@code null} if the user has not given it).
	 * @return	The address, with every value encoded.
	 */
	public String expand(final SearchSubject subject, final Function<String, String> credentials){
		return fill(subject::value, credentials);
	}

	/**
	 * @param subject	The values of the person.
	 * @return	The address to show or to keep: the credentials are replaced by {@value #HIDDEN_CREDENTIAL}.
	 */
	public String expandForDisplay(final SearchSubject subject){
		return fill(subject::value, name -> HIDDEN_CREDENTIAL);
	}

	private String fill(final Function<Placeholder, String> values, final Function<String, String> credentials){
		final StringBuilder sb = new StringBuilder(text.length() + 32);
		for(final Segment segment : segments)
			switch(segment){
				case final Literal literal -> sb.append(literal.text());
				case final Value value -> sb.append(encode(values.apply(value.placeholder())));
				//an asterisk is not encoded: a hidden credential stays readable
				case final Credential credential -> sb.append(encode(credentials.apply(credential.name())));
			}
		return sb.toString();
	}

	/**
	 * @param value	A value.
	 * @return	The value percent-encoded as UTF-8, a space as {@code %20} (good in a path and in a query); empty for {@code null}.
	 */
	static String encode(final String value){
		if(value == null)
			return StringUtils.EMPTY;

		return StringUtils.replace(URLEncoder.encode(value, StandardCharsets.UTF_8), "+", "%20");
	}


	@Override
	public String toString(){
		return text;
	}

}
