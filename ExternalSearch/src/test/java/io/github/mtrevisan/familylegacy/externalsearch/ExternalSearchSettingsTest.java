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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Optional;
import java.util.Set;


class ExternalSearchSettingsTest{

	private static SearchSite userSite(final String id){
		return new SearchSite(id, "My archive", "https://archive.example.org/find?who={surname}&key={credential:api_key}",
			SiteKind.ARCHIVES, SiteCoverage.parse("IT", "1600", null), List.of("username"), "Local archive", true, false);
	}


	@Test
	void theBuiltInSitesAreThereAndEnabled(){
		final ExternalSearchSettings settings = ExternalSearchSettings.inMemory();

		Assertions.assertEquals(BuiltInSites.all().size(), settings.sites().size());
		Assertions.assertEquals(settings.sites(), settings.enabledSites());
	}

	@Test
	void theSitesOfTheUserAndTheChoicesAreKeptInTheFolder(@TempDir final Path folder) throws IOException{
		final ExternalSearchSettings settings = ExternalSearchSettings.at(folder);
		final String id = settings.newSiteId("My archive");
		Assertions.assertEquals("user-my-archive", id);
		settings.save(userSite(id));
		settings.setEnabled("ancestry", false);
		final SearchSite changed = BuiltInSites.find("wikitree").orElseThrow().withName("WikiTree (mine)");
		settings.save(changed);

		final ExternalSearchSettings reloaded = ExternalSearchSettings.at(folder);
		Assertions.assertEquals(Optional.of(userSite(id)), reloaded.site(id));
		Assertions.assertFalse(reloaded.isEnabled("ancestry"));
		Assertions.assertTrue(reloaded.isModified("wikitree"));
		Assertions.assertEquals("WikiTree (mine)", reloaded.site("wikitree").orElseThrow().name());
		Assertions.assertEquals(BuiltInSites.all().size() + 1, reloaded.sites().size());
		Assertions.assertTrue(reloaded.enabledSites().stream().noneMatch(site -> site.id().equals("ancestry")));
		//a new site never takes the identifier of another one
		Assertions.assertEquals("user-my-archive-2", reloaded.newSiteId("My archive"));
	}

	@Test
	void aBuiltInSiteIsRestoredAndASiteOfTheUserIsRemovedWithItsCredentials(@TempDir final Path folder) throws IOException{
		final ExternalSearchSettings settings = ExternalSearchSettings.at(folder);
		settings.save(userSite("user-a"));
		settings.credentials().put("user-a", "api_key", "secret");
		settings.save(BuiltInSites.find("wikitree").orElseThrow().withName("Changed"));
		settings.credentials().put("wikitree", "app_id", "mine");

		settings.remove("wikitree");
		settings.remove("user-a");

		final ExternalSearchSettings reloaded = ExternalSearchSettings.at(folder);
		Assertions.assertFalse(reloaded.isModified("wikitree"));
		Assertions.assertEquals("WikiTree", reloaded.site("wikitree").orElseThrow().name());
		Assertions.assertEquals(Optional.of("mine"), reloaded.credentials().get("wikitree", "app_id"));
		Assertions.assertTrue(reloaded.site("user-a").isEmpty());
		Assertions.assertEquals(Optional.empty(), reloaded.credentials().get("user-a", "api_key"));
	}

	@Test
	void savingABuiltInSiteAsItComesKeepsNothing(@TempDir final Path folder) throws IOException{
		final ExternalSearchSettings settings = ExternalSearchSettings.at(folder);
		settings.save(BuiltInSites.find("antenati").orElseThrow());

		Assertions.assertFalse(settings.isModified("antenati"));
	}

	@Test
	void theCredentialsAreKeptApartAndOnlyForTheOwner(@TempDir final Path folder) throws IOException{
		final ExternalSearchSettings settings = ExternalSearchSettings.at(folder);
		settings.save(userSite("user-a"));
		settings.credentials().put("user-a", "api_key", " k3y ");
		settings.credentials().put("user-a", "username", "me");
		settings.credentials().put("user-a", "username", "  ");

		final Path file = folder.resolve(ExternalSearchSettings.CREDENTIALS_FILE);
		Assertions.assertTrue(Files.isRegularFile(file));
		Assertions.assertFalse(Files.readString(folder.resolve(ExternalSearchSettings.SITES_FILE)).contains("k3y"));
		final ExternalSearchSettings reloaded = ExternalSearchSettings.at(folder);
		Assertions.assertEquals(Optional.of("k3y"), reloaded.credentials().get("user-a", "api_key"));
		Assertions.assertEquals(List.of("api_key"), reloaded.credentials().names("user-a"));
		Assertions.assertEquals(List.of(), reloaded.credentials().missing(reloaded.site("user-a").orElseThrow()));

		final PosixFileAttributeView posix = Files.getFileAttributeView(file, PosixFileAttributeView.class);
		if(posix != null)
			Assertions.assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
				posix.readAttributes().permissions());
		else{
			final AclFileAttributeView acl = Files.getFileAttributeView(file, AclFileAttributeView.class);
			if(acl != null)
				for(final AclEntry entry : acl.getAcl())
					Assertions.assertEquals(acl.getOwner(), entry.principal(), "only the owner can read the credentials");
		}
	}

	@Test
	void theSearchesFollowTheEnabledSites(){
		final ExternalSearchSettings settings = ExternalSearchSettings.inMemory();
		final SearchSubject subject = SearchSubject.EMPTY.with(Placeholder.SURNAME, "Rossi");

		Assertions.assertEquals(settings.enabledSites().size(), settings.links(subject).size());
	}

}
