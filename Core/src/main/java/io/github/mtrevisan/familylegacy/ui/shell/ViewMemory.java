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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.ProjectMeta;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;


/**
 * The person and the view that were shown when a file was last used, kept next to the file ({@code view.properties} in its
 * {@code .meta} folder), so that the file opens where the user left it.
 */
final class ViewMemory{

	private static final String FILE_NAME = "view.properties";
	private static final String KEY_ROOT = "root";
	private static final String KEY_PROJECTION = "projection";


	/** Where a file opens: the person, and the view (may be {@code null}: the one shown now). */
	record Start(String rootId, ProjectionType projection){}


	private ViewMemory(){}


	/**
	 * @param documentPath	The path of the FLEF file, {@code null} if it has none.
	 * @return	What was remembered, {@code null} if there is nothing, or the person is not in the model any more (or is not an individual).
	 */
	static Start read(final String documentPath, final FLEFModel model){
		if(documentPath == null || model == null)
			return null;

		final Properties props = ProjectMeta.read(ProjectMeta.file(documentPath, FILE_NAME));
		final String rootId = props.getProperty(KEY_ROOT);
		if(StringUtils.isBlank(rootId) || !model.hasRecord(rootId) || !IndividualHandler.isOfType(model.getRecordById(rootId)))
			return null;

		ProjectionType projection = null;
		final String name = props.getProperty(KEY_PROJECTION);
		if(name != null)
			try{
				projection = ProjectionType.valueOf(name);
			}
			catch(final IllegalArgumentException ignored){
				//a view this version does not know: the one shown now stays
			}
		//the chronomap geocodes the places of the file: it is not opened by itself
		if(projection == ProjectionType.CHRONOMAP)
			projection = null;
		return new Start(rootId, projection);
	}

	/**
	 * Remembers the person and the view. Nothing is written for a file that has nothing remembered yet while the person is the one
	 * the file opens on anyway: no folder is made for a file that is only looked at.
	 */
	static void save(final String documentPath, final String rootId, final ProjectionType projection, final FLEFModel model){
		if(documentPath == null || rootId == null || model == null)
			return;

		final Path file = ProjectMeta.file(documentPath, FILE_NAME);
		if(!Files.exists(file) && rootId.equals(ModelUtils.findLowestIndividualId(model)))
			return;

		final Properties props = new Properties();
		props.setProperty(KEY_ROOT, rootId);
		if(projection != null)
			props.setProperty(KEY_PROJECTION, projection.name());
		ProjectMeta.write(file, props, "view");
	}

}
