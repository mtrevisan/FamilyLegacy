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

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;


/**
 * The credentials the user gives for the sites (API keys, application keys, user names, passwords), by site and by name.
 * <p>
 * They are kept in a file of their own in the folder of the preferences of the user, never in a FLEF file, never in the preferences
 * (which a user may send with a report of a problem), never in the log. The file can be read only by the account of the user (and
 * by the administrators of the computer): the permissions are set so when it is written. It is <em>not</em> encrypted: whoever can
 * read the files of the account can read it.
 */
public final class CredentialStore{

	private static final char SEPARATOR = '/';
	private static final String HEADER = "Family Legacy -- credentials for the external search sites (keep private)";


	/** {@code null} when the credentials are kept only in memory (tests, or a home that cannot be written). */
	private final Path file;
	private final Properties properties = new Properties();


	private CredentialStore(final Path file){
		this.file = file;
		load();
	}

	/**
	 * @param file	The file of the credentials (it may not exist yet).
	 * @return	The store, with what the file holds.
	 */
	public static CredentialStore at(final Path file){
		return new CredentialStore(file);
	}

	/** @return	A store that keeps the credentials only as long as the application runs. */
	public static CredentialStore inMemory(){
		return new CredentialStore(null);
	}


	/**
	 * @param siteId	The identifier of the site.
	 * @param name	The name of the credential.
	 * @return	The value, empty if the user has not given it.
	 */
	public synchronized Optional<String> get(final String siteId, final String name){
		return Optional.ofNullable(properties.getProperty(key(siteId, name)));
	}

	/**
	 * Changes a credential, and writes the file.
	 *
	 * @param siteId	The identifier of the site.
	 * @param name	The name of the credential.
	 * @param value	The value; blank or {@code null} removes the credential.
	 * @throws IOException	If the file cannot be written (the change is kept in memory).
	 */
	public synchronized void put(final String siteId, final String name, final String value) throws IOException{
		final String key = key(siteId, name);
		if(StringUtils.isBlank(value))
			properties.remove(key);
		else
			properties.setProperty(key, value.strip());
		save();
	}

	/**
	 * Removes all the credentials of a site, and writes the file.
	 *
	 * @param siteId	The identifier of the site.
	 * @throws IOException	If the file cannot be written (the change is kept in memory).
	 */
	public synchronized void removeAll(final String siteId) throws IOException{
		final String prefix = siteId + SEPARATOR;
		if(properties.stringPropertyNames().stream().noneMatch(key -> key.startsWith(prefix)))
			return;

		properties.stringPropertyNames().stream()
			.filter(key -> key.startsWith(prefix))
			.forEach(properties::remove);
		save();
	}

	/**
	 * @param siteId	The identifier of the site.
	 * @return	The names of the credentials given for the site, in alphabetical order.
	 */
	public synchronized List<String> names(final String siteId){
		final String prefix = siteId + SEPARATOR;
		final List<String> names = new ArrayList<>();
		for(final String key : properties.stringPropertyNames())
			if(key.startsWith(prefix))
				names.add(key.substring(prefix.length()));
		names.sort(String::compareTo);
		return names;
	}

	/**
	 * @param site	A site.
	 * @return	The credentials its template uses that the user has not given.
	 */
	public synchronized List<String> missing(final SearchSite site){
		final List<String> missing = new ArrayList<>();
		try{
			for(final String name : site.template().credentials())
				if(!properties.containsKey(key(site.id(), name)))
					missing.add(name);
		}
		catch(final UrlTemplate.TemplateException ignored){
			//an invalid template needs nothing
		}
		return missing;
	}

	/** @return	The file of the credentials, {@code null} if they are kept only in memory. */
	public Path file(){
		return file;
	}

	private static String key(final String siteId, final String name){
		if(!SearchSite.isValidName(siteId) || !SearchSite.isValidName(name))
			throw new IllegalArgumentException("Not a valid site or credential name");

		return siteId + SEPARATOR + name;
	}


	private void load(){
		if(file == null || !Files.isRegularFile(file))
			return;

		try(final InputStream in = Files.newInputStream(file)){
			properties.load(in);
		}
		catch(final IOException ignored){
			//an unreadable file is an empty one: the user gives the credentials again
		}
	}

	private void save() throws IOException{
		if(file == null)
			return;

		final Path directory = file.toAbsolutePath().getParent();
		Files.createDirectories(directory);
		final ByteArrayOutputStream content = new ByteArrayOutputStream();
		properties.store(content, HEADER);

		//the new content goes to a file of the same folder, made private before anything is written in it, then takes the place of the old one
		final Path temporary = Files.createTempFile(directory, "credentials", ".tmp");
		try{
			restrictToOwner(temporary);
			try(final OutputStream out = Files.newOutputStream(temporary)){
				content.writeTo(out);
			}
			try{
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch(final AtomicMoveNotSupportedException e){
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally{
			Files.deleteIfExists(temporary);
		}
	}

	/**
	 * Lets only the owner of a file read and write it: the permissions of the owner on POSIX systems, a list of access with only the
	 * owner on Windows. Done as well as the file system allows.
	 *
	 * @param path	The file.
	 * @throws IOException	If the permissions cannot be changed.
	 */
	static void restrictToOwner(final Path path) throws IOException{
		final PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
		if(posix != null){
			posix.setPermissions(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
			return;
		}

		final AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
		if(acl != null){
			final UserPrincipal owner = acl.getOwner();
			final AclEntry entry = AclEntry.newBuilder()
				.setType(AclEntryType.ALLOW)
				.setPrincipal(owner)
				.setPermissions(EnumSet.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA,
					AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.READ_NAMED_ATTRS,
					AclEntryPermission.WRITE_NAMED_ATTRS, AclEntryPermission.READ_ACL, AclEntryPermission.WRITE_ACL,
					AclEntryPermission.DELETE, AclEntryPermission.SYNCHRONIZE))
				.build();
			acl.setAcl(List.of(entry));
		}
	}

}
