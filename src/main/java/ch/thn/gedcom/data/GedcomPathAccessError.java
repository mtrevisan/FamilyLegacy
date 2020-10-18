/**
 * Copyright 2013 Thomas Naeff (github.com/thnaeff)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.thn.gedcom.data;

/**
 * An error which occurred while accessing gedcom objects. The error message contains
 * an error description and the access path is the path which caused the error
 * (retrieve the path with {@link #getAccessPath()}).
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class GedcomPathAccessError extends GedcomError{
	private static final long serialVersionUID = 2159417452645645856L;

	private String[] accessPath = null;

	private int pathIndex = 0;

	/**
	 *
	 *
	 * @param accessPath The path which caused the error
	 * @param pathIndex The index of the path where the error occurred
	 * @param message The error message
	 */
	public GedcomPathAccessError(String[] accessPath, int pathIndex, String message){
		super(message);
		this.pathIndex = pathIndex;
		this.accessPath = accessPath;
	}

	/**
	 *
	 *
	 * @return
	 */
	public int getPathIndex(){
		return pathIndex;
	}

	/**
	 * The path which caused the error
	 *
	 * @return
	 */
	public String[] getAccessPath(){
		return accessPath;
	}

}
