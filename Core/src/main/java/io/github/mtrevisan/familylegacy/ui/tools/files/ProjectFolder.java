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
package io.github.mtrevisan.familylegacy.ui.tools.files;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Strings;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * A project is best kept in a folder of its own: the FLEF file, the folder of the data the application keeps for it (see
 * {@code TagStore#metaDirectory}), the backups and the media then travel together. This tells where such a file goes, and whether the
 * folder chosen by the user is already shared with other family trees.
 */
final class ProjectFolder{

	private static final String EXTENSION = "flef";


	private ProjectFolder(){}


	/**
	 * @param target	The FLEF file chosen by the user.
	 * @return	The file inside a folder of its own, named after the file ({@code dir/name.flef} becomes {@code dir/name/name.flef}); the file
	 * 	itself if it already is in a folder with that name.
	 */
	static File inOwnFolder(final File target){
		final File parent = target.getParentFile();
		final String name = FilenameUtils.getBaseName(target.getName());
		if(parent == null || Strings.CI.equals(parent.getName(), name))
			return target;

		return new File(new File(parent, name), target.getName());
	}

	/**
	 * @return	Whether the folder of the file holds the FLEF file of another project (the file itself does not count).
	 */
	static boolean sharesFolderWithOtherProjects(final File target){
		final File parent = target.getParentFile();
		if(parent == null || !parent.isDirectory())
			return false;

		try(final DirectoryStream<Path> files = Files.newDirectoryStream(parent.toPath())){
			for(final Path path : files){
				final String name = path.getFileName()
					.toString();
				if(Strings.CI.endsWith(name, "." + EXTENSION) && !Strings.CI.equals(name, target.getName()) && Files.isRegularFile(path))
					return true;
			}
		}
		catch(final IOException ignored){
			//cannot tell: no advice
		}
		return false;
	}

}
