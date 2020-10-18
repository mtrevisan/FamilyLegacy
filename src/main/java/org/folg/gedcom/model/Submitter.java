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
 * 
 * add: value, Www, Email
 */
public class Submitter extends ExtensionContainer {
   private String id = null;
   private String value = null;
   private Address addr = null;
   private String phon = null;
   private String fax = null;
   private String name = null;
   private Change chan = null;
   private String rin = null;
   private String lang = null;
   private String _www = null;
   private String wwwTag = null;
   private String _email = null;
   private String emailTag = null;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public Address getAddress() {
      return addr;
   }

   public void setAddress(Address addr) {
      this.addr = addr;
   }

   public String getPhone() {
      return phon;
   }

   public void setPhone(String phon) {
      this.phon = phon;
   }

   public String getFax() {
      return fax;
   }

   public void setFax(String fax) {
      this.fax = fax;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Change getChange() {
      return chan;
   }

   public void setChange(Change chan) {
      this.chan = chan;
   }

   public String getRin() {
      return rin;
   }

   public void setRin(String rin) {
      this.rin = rin;
   }

   public String getLanguage() {
      return lang;
   }

   public void setLanguage(String lang) {
      this.lang = lang;
   }

   public String getWww() {
      return _www;
   }

   public void setWww(String www) {
      this._www = www;
   }

   public String getWwwTag() {
      return wwwTag;
   }

   public void setWwwTag(String wwwTag) {
      this.wwwTag = wwwTag;
   }

   public String getEmail() {
      return _email;
   }

   public void setEmail(String email) {
      this._email = email;
   }

   public String getEmailTag() {
      return emailTag;
   }
   
   public void setEmailTag(String emailTag) {
      this.emailTag = emailTag;
   }

   public void accept(Visitor visitor) {
      if (visitor.visit(this)) {
         if (addr != null) {
            addr.accept(visitor);
         }
         if (chan != null) {
            chan.accept(visitor);
         }
         super.visitContainedObjects(visitor);
         visitor.endVisit(this);
      }
   }
}
