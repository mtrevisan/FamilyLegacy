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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * A site where a person can be searched.
 *
 * @param id	The identifier (letters, digits, {@code _}, {@code -}, {@code .}): a built-in site keeps its own, a site of the user gets one
 * 	when it is created.
 * @param name	The name shown.
 * @param urlTemplate	The address of a search, with the placeholders (see {@link UrlTemplate}); it may be invalid (a site saved by
 * 	hand in the file), {@link #template()} says why.
 * @param kind	What the site holds.
 * @param coverage	Where and when the site has records.
 * @param credentials	The names of the credentials the user may give for the site (an API key, a user name): the ones the template
 * 	uses are added by themselves.
 * @param description	A short description, may be empty.
 * @param loginRequired	Whether the site shows its results only to a user logged in on it (in the browser).
 * @param builtIn	Whether it comes with the application (it can be changed and restored, not deleted).
 */
public record SearchSite(String id, String name, String urlTemplate, SiteKind kind, SiteCoverage coverage, List<String> credentials,
		String description, boolean loginRequired, boolean builtIn){

	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.-]+");


	public SearchSite{
		if(!isValidName(id))
			throw new IllegalArgumentException("Not a valid identifier of a site: " + id);
		if(StringUtils.isBlank(name))
			throw new IllegalArgumentException("A site needs a name");

		name = name.strip();
		urlTemplate = StringUtils.strip(urlTemplate);
		if(urlTemplate == null)
			urlTemplate = StringUtils.EMPTY;
		if(kind == null)
			kind = SiteKind.OTHER;
		if(coverage == null)
			coverage = SiteCoverage.WORLDWIDE;
		final Set<String> names = new LinkedHashSet<>();
		if(credentials != null)
			for(final String credential : credentials){
				final String stripped = StringUtils.strip(credential);
				if(!isValidName(stripped))
					throw new IllegalArgumentException("Not a valid name of a credential: " + credential);

				names.add(stripped);
			}
		credentials = List.copyOf(names);
		description = StringUtils.defaultString(StringUtils.strip(description));
	}

	/**
	 * @param name	A text.
	 * @return	Whether it can be the identifier of a site or the name of a credential.
	 */
	public static boolean isValidName(final String name){
		return (name != null && NAME.matcher(name).matches());
	}


	/**
	 * @return	The template, checked.
	 * @throws UrlTemplate.TemplateException	If the template is not valid.
	 */
	public UrlTemplate template() throws UrlTemplate.TemplateException{
		return UrlTemplate.parse(urlTemplate);
	}

	/** @return	The credentials of the site: the declared ones, then the ones the template uses (if it is valid). */
	public List<String> allCredentials(){
		final Set<String> names = new LinkedHashSet<>(credentials);
		try{
			names.addAll(template().credentials());
		}
		catch(final UrlTemplate.TemplateException ignored){
			//an invalid template uses no credential
		}
		return new ArrayList<>(names);
	}

	/**
	 * @param credential	The name of a credential.
	 * @return	The (localized) label of a credential known to the application, the name itself otherwise.
	 */
	public static String credentialLabel(final String credential){
		return I18N.t("external_search.credential." + credential, StringUtils.replaceChars(credential, '_', ' '));
	}

	public SearchSite withId(final String newId, final boolean newBuiltIn){
		return new SearchSite(newId, name, urlTemplate, kind, coverage, credentials, description, loginRequired, newBuiltIn);
	}

	public SearchSite withName(final String newName){
		return new SearchSite(id, newName, urlTemplate, kind, coverage, credentials, description, loginRequired, builtIn);
	}

}
