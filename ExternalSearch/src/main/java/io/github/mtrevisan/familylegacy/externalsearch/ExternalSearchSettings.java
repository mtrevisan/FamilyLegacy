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

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;


/**
 * The sites of the user: the built-in ones (possibly changed by the user), the ones the user added, which of them are enabled, and the
 * credentials of the user for them.
 * <p>
 * Kept in the folder of the preferences of the user: {@value #SITES_FILE} for the sites, {@value #CREDENTIALS_FILE} for the
 * credentials (see {@link CredentialStore}). Nothing is kept in a FLEF file: the sites are a choice of the user, not genealogical data.
 */
public final class ExternalSearchSettings{

	public static final String SITES_FILE = "external-sites.properties";
	public static final String CREDENTIALS_FILE = "external-credentials.properties";

	private static final String HEADER = "Family Legacy -- external search sites";
	private static final String KEY_SITES = "sites";
	private static final String KEY_DISABLED = "disabled";
	private static final String KEY_SITE = "site.";
	private static final String FIELD_NAME = ".name";
	private static final String FIELD_URL = ".url";
	private static final String FIELD_KIND = ".kind";
	private static final String FIELD_COUNTRIES = ".countries";
	private static final String FIELD_FROM = ".from";
	private static final String FIELD_TO = ".to";
	private static final String FIELD_CREDENTIALS = ".credentials";
	private static final String FIELD_DESCRIPTION = ".description";
	private static final String FIELD_LOGIN = ".login";
	private static final String LIST_SEPARATOR = ",";
	private static final String USER_ID_PREFIX = "user-";

	private static ExternalSearchSettings shared;


	/** {@code null} when the settings are kept only in memory. */
	private final Path sitesFile;
	private final CredentialStore credentials;
	/** The sites saved by the user, built-in ones changed by the user included, in their order. */
	private final Map<String, SearchSite> saved = new LinkedHashMap<>();
	private final Set<String> disabled = new LinkedHashSet<>();


	private ExternalSearchSettings(final Path sitesFile, final CredentialStore credentials){
		this.sitesFile = sitesFile;
		this.credentials = credentials;
		load();
	}

	/** @return	The settings of the user, in the folder of the preferences (read once). */
	public static synchronized ExternalSearchSettings shared(){
		if(shared == null)
			shared = at(AppPreferences.filePath().toAbsolutePath().getParent());
		return shared;
	}

	/**
	 * @param directory	The folder the two files are in.
	 * @return	The settings kept in the folder.
	 */
	public static ExternalSearchSettings at(final Path directory){
		return new ExternalSearchSettings(directory.resolve(SITES_FILE), CredentialStore.at(directory.resolve(CREDENTIALS_FILE)));
	}

	/** @return	Settings that are kept only as long as the application runs (tests). */
	public static ExternalSearchSettings inMemory(){
		return new ExternalSearchSettings(null, CredentialStore.inMemory());
	}


	/** @return	The credentials of the user. */
	public CredentialStore credentials(){
		return credentials;
	}

	/** @return	Every site, enabled or not: the built-in ones (as changed by the user), then the ones of the user, in their order. */
	public synchronized List<SearchSite> sites(){
		final List<SearchSite> sites = new ArrayList<>();
		for(final SearchSite builtIn : BuiltInSites.all())
			sites.add(saved.getOrDefault(builtIn.id(), builtIn));
		for(final SearchSite site : saved.values())
			if(!site.builtIn())
				sites.add(site);
		return Collections.unmodifiableList(sites);
	}

	/** @return	The sites that are enabled. */
	public synchronized List<SearchSite> enabledSites(){
		return sites().stream()
			.filter(site -> !disabled.contains(site.id()))
			.toList();
	}

	/**
	 * @param id	The identifier of a site.
	 * @return	The site, empty if there is none with that identifier.
	 */
	public synchronized Optional<SearchSite> site(final String id){
		return sites().stream()
			.filter(site -> site.id().equals(id))
			.findFirst();
	}

	/**
	 * @param id	The identifier of a site.
	 * @return	Whether it is offered in the search.
	 */
	public synchronized boolean isEnabled(final String id){
		return !disabled.contains(id);
	}

	/**
	 * @param id	The identifier of a site.
	 * @param enabled	Whether it is offered in the search.
	 * @throws IOException	If the settings cannot be written (the change is kept in memory).
	 */
	public synchronized void setEnabled(final String id, final boolean enabled) throws IOException{
		final boolean changed = (enabled? disabled.remove(id): disabled.add(id));
		if(changed)
			save();
	}

	/**
	 * @param id	The identifier of a built-in site.
	 * @return	Whether the user changed it.
	 */
	public synchronized boolean isModified(final String id){
		final SearchSite site = saved.get(id);
		return (site != null && site.builtIn());
	}

	/**
	 * Adds a site, or replaces the one with the same identifier (a built-in site changed by the user is kept apart and can be restored).
	 *
	 * @param site	The site.
	 * @throws IOException	If the settings cannot be written (the change is kept in memory).
	 */
	public synchronized void save(final SearchSite site) throws IOException{
		final boolean builtIn = BuiltInSites.find(site.id()).isPresent();
		final SearchSite toSave = (site.builtIn() == builtIn? site: site.withId(site.id(), builtIn));
		final SearchSite original = BuiltInSites.find(site.id()).orElse(null);
		if(toSave.equals(original))
			saved.remove(site.id());
		else
			saved.put(toSave.id(), toSave);
		save();
	}

	/**
	 * Removes a site of the user (and its credentials), or restores a built-in one as it comes with the application (its credentials
	 * are kept).
	 *
	 * @param id	The identifier of the site.
	 * @throws IOException	If the settings cannot be written (the change is kept in memory).
	 */
	public synchronized void remove(final String id) throws IOException{
		final SearchSite removed = saved.remove(id);
		if(removed == null)
			return;

		if(!removed.builtIn()){
			disabled.remove(id);
			credentials.removeAll(id);
		}
		save();
	}

	/**
	 * @param name	The name of a new site.
	 * @return	An identifier no other site has, made from the name.
	 */
	public synchronized String newSiteId(final String name){
		final StringBuilder base = new StringBuilder(USER_ID_PREFIX);
		for(final char c : StringUtils.stripAccents(StringUtils.defaultString(name)).toLowerCase(Locale.ROOT).toCharArray())
			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
				base.append(c);
			else if(base.charAt(base.length() - 1) != '-')
				base.append('-');
		String id = StringUtils.removeEnd(base.toString(), "-");
		if(id.length() <= USER_ID_PREFIX.length())
			id = USER_ID_PREFIX + "site";
		String candidate = id;
		int counter = 2;
		while(site(candidate).isPresent())
			candidate = id + "-" + counter ++;
		return candidate;
	}

	/**
	 * @param subject	The values of the person.
	 * @return	The searches on the enabled sites, the most relevant first.
	 */
	public List<SearchLink> links(final SearchSubject subject){
		return SearchLink.forSites(enabledSites(), subject, credentials);
	}


	/* ======================================================================
	 *                          Storage
	 * ====================================================================== */

	private void load(){
		if(sitesFile == null || !Files.isRegularFile(sitesFile))
			return;

		final Properties properties = new Properties();
		try(final InputStream in = Files.newInputStream(sitesFile)){
			properties.load(in);
		}
		catch(final IOException ignored){
			//an unreadable file is an empty one: the built-in sites are still there
			return;
		}

		disabled.addAll(list(properties.getProperty(KEY_DISABLED)));
		for(final String id : list(properties.getProperty(KEY_SITES))){
			final SearchSite site = read(properties, id);
			if(site != null)
				saved.put(id, site);
		}
	}

	/** @return	The site written in the properties, {@code null} if what is written is not a site (it is skipped, not lost forever). */
	private static SearchSite read(final Properties properties, final String id){
		final String prefix = KEY_SITE + id;
		try{
			final boolean builtIn = BuiltInSites.find(id).isPresent();
			SiteCoverage coverage;
			try{
				coverage = SiteCoverage.parse(properties.getProperty(prefix + FIELD_COUNTRIES), properties.getProperty(prefix + FIELD_FROM),
					properties.getProperty(prefix + FIELD_TO));
			}
			catch(final IllegalArgumentException ignored){
				coverage = SiteCoverage.WORLDWIDE;
			}
			return new SearchSite(id, properties.getProperty(prefix + FIELD_NAME), properties.getProperty(prefix + FIELD_URL),
				SiteKind.of(properties.getProperty(prefix + FIELD_KIND)), coverage, list(properties.getProperty(prefix + FIELD_CREDENTIALS)),
				properties.getProperty(prefix + FIELD_DESCRIPTION), Boolean.parseBoolean(properties.getProperty(prefix + FIELD_LOGIN)), builtIn);
		}
		catch(final IllegalArgumentException ignored){
			return null;
		}
	}

	private static List<String> list(final String text){
		final List<String> items = new ArrayList<>();
		if(text != null)
			for(final String item : StringUtils.split(text, LIST_SEPARATOR)){
				final String stripped = item.strip();
				if(!stripped.isEmpty())
					items.add(stripped);
			}
		return items;
	}

	private void save() throws IOException{
		if(sitesFile == null)
			return;

		final Properties properties = new Properties();
		properties.setProperty(KEY_SITES, String.join(LIST_SEPARATOR, saved.keySet()));
		if(!disabled.isEmpty())
			properties.setProperty(KEY_DISABLED, String.join(LIST_SEPARATOR, disabled));
		for(final SearchSite site : saved.values()){
			final String prefix = KEY_SITE + site.id();
			properties.setProperty(prefix + FIELD_NAME, site.name());
			properties.setProperty(prefix + FIELD_URL, site.urlTemplate());
			properties.setProperty(prefix + FIELD_KIND, site.kind().name());
			setIfNotEmpty(properties, prefix + FIELD_COUNTRIES, String.join(LIST_SEPARATOR, site.coverage().countries()));
			if(site.coverage().fromYear() != null)
				properties.setProperty(prefix + FIELD_FROM, site.coverage().fromYear().toString());
			if(site.coverage().toYear() != null)
				properties.setProperty(prefix + FIELD_TO, site.coverage().toYear().toString());
			setIfNotEmpty(properties, prefix + FIELD_CREDENTIALS, String.join(LIST_SEPARATOR, site.credentials()));
			setIfNotEmpty(properties, prefix + FIELD_DESCRIPTION, site.description());
			if(site.loginRequired())
				properties.setProperty(prefix + FIELD_LOGIN, Boolean.TRUE.toString());
		}

		Files.createDirectories(sitesFile.toAbsolutePath().getParent());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		properties.store(out, HEADER);
		AtomicFileWriter.write(sitesFile, out.toString(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
	}

	private static void setIfNotEmpty(final Properties properties, final String key, final String value){
		if(StringUtils.isNotEmpty(value))
			properties.setProperty(key, value);
	}

}
