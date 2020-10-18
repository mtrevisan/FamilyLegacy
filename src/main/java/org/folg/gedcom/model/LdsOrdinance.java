/*
 * Copyright 2011 Foundation for On-Line Genealogy, Inc.
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
 */

package org.folg.gedcom.model;

/**
 * User: Dallan
 * Date: 12/27/11
 */
public class LdsOrdinance extends EventFact {
   private String stat = null;
   private String temp = null;

   public String getStatus() {
      return stat;
   }

   public void setStatus(String stat) {
      this.stat = stat;
   }

   public String getTemple() {
      return temp;
   }

   public void setTemple(String temp) {
      this.temp = temp;
   }

   public void accept(Visitor visitor) {
      if (visitor.visit(this)) {
         super.visitContainedObjects(visitor);
         visitor.endVisit(this);
      }
   }
}
