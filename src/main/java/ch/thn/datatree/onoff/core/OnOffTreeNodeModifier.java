/**
 *    Copyright 2014 Thomas Naeff (github.com/thnaeff)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ch.thn.datatree.onoff.core;

import ch.thn.datatree.onoff.OnOffTreeUtil;

/**
 * An empty interface which can be implemented in a custom class to modify the
 * behavior of the conversion of an On-Off-Tree to a simple tree when using
 * {@link OnOffTreeUtil#convertToSimpleTree(AbstractGenericTreeNode, boolean, boolean, OnOffTreeNodeModifier)}.
 * This modifier object will then be passed on through the On-Off-Tree methods
 * ignoreNode, forceNodeVisible etc. so that the method behavior can be modified
 * according to the given modifier.
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public interface OnOffTreeNodeModifier {

}
