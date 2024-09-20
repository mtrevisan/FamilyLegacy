CREATE (a:Assertion {id: $id, role: $role, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (a:Assertion) ASSERT a.id IS UNIQUE;
MATCH (a:Assertion {id: $assertionID}), (c:Citation {id: $citationID})
CREATE (a)-[:INFERRED_FROM]->(c);
MATCH (a:Assertion {id: $assertionID}), (p:Place {id: $placeID})
CREATE (a)<-[:SUPPORTED_BY]-(p);
MATCH (a:Assertion {id: $assertionID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (a)<-[:SUPPORTED_BY]-(cn);
MATCH (a:Assertion {id: $assertionID}), (hd:HistoricDate {id: $historicDateID})
CREATE (a)<-[:SUPPORTED_BY]-(hd);
MATCH (a:Assertion {id: $assertionID}), (cld:Calendar {id: $calendarID})
CREATE (a)<-[:SUPPORTED_BY]-(cld);
MATCH (a:Assertion {id: $assertionID}), (prs:Person {id: $personID})
CREATE (a)<-[:SUPPORTED_BY]-(prs);
MATCH (a:Assertion {id: $assertionID}), (g:Group {id: $groupID})
CREATE (a)<-[:SUPPORTED_BY]-(g);
MATCH (a:Assertion {id: $assertionID}), (m:Media {id: $mediaID})
CREATE (a)<-[:SUPPORTED_BY]-(m);
MATCH (a:Assertion {id: $assertionID}), (pn:PersonName {id: $personNameID})
CREATE (a)<-[:SUPPORTED_BY]-(pn);

CREATE (c:Citation {id: $id, location: $location, extract: $extract, extractLocale: $extractLocale, extractType: $extractType});
CREATE CONSTRAINT ON (c:Citation) ASSERT c.id IS UNIQUE;
CREATE CONSTRAINT ON (c:Citation) ASSERT exists(c.extract);
MATCH (c:Citation {id: $citationID}), (s:Source {id: $sourceID})
CREATE (c)<-[:QUOTES]-(s);

CREATE (s:Source {id: $id, identifier: $identifier, type: $type, author: $author, location: $location});
CREATE CONSTRAINT ON (s:Source) ASSERT s.id IS UNIQUE;
CREATE CONSTRAINT ON (s:Source) ASSERT exists(s.identifier);
CREATE CONSTRAINT ON (s:Source) ASSERT s.identifier IS UNIQUE;
MATCH (s:Source {id: $sourceID}), (r:Repository {id: $repositoryID})
CREATE (s)-[:STORED_IN]->(r);
MATCH (s:Source {id: $sourceID}), (p:Place {id: $placeID})
CREATE (s)-[:CREATED_IN]->(r);
MATCH (s:Source {id: $sourceID}), (hd:HistoricDate {id: $historicDateID})
CREATE (s)-[:CREATED_ON]->(hd);

CREATE (r:Repository {id: $id, identifier: $identifier, type: $type});
CREATE CONSTRAINT ON (r:Repository) ASSERT r.id IS UNIQUE;
CREATE CONSTRAINT ON (r:Repository) ASSERT exists(r.identifier);
CREATE CONSTRAINT ON (r:Repository) ASSERT r.identifier IS UNIQUE;
MATCH (prs:Person {id: $personID}), (r:Repository {id: $repositoryID})
CREATE (r)<-[:OWNS]-(prs);
MATCH (r:Repository {id: $repositoryID}), (p:Place {id: $placeID})
CREATE (r)-[:LOCATED_IN]->(p);


CREATE (hd:HistoricDate {id: $id, date: $date, dateOriginal: $dateOriginal, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (hd:HistoricDate) ASSERT hd.id IS UNIQUE;
CREATE CONSTRAINT ON (hd:HistoricDate) ASSERT exists(hd.date);
MATCH (hd:HistoricDate {id: $historicDateID}), (cld:Calendar {id: $calendarOriginalID})
CREATE (hd)-[:EXPRESSED_IN]->(cld);

CREATE (cld:Calendar {id: $id, type: $type});
CREATE CONSTRAINT ON (cld:Calendar) ASSERT cld.id IS UNIQUE;
CREATE CONSTRAINT ON (cld:Calendar) ASSERT exists(cld.type);
CREATE CONSTRAINT ON (cld:Calendar) ASSERT cld.type IS UNIQUE;


CREATE (p:Place {id: $id, identifier: $identifier, name: $name, locale: $locale, type: $type, coordinate: $coordinate, coordinateSystem: $coordinateSystem, coordinateCredibility: $coordinateCredibility, photoCrop: $photoCrop});
CREATE CONSTRAINT ON (p:Place) ASSERT p.id IS UNIQUE;
CREATE CONSTRAINT ON (p:Place) ASSERT exists(p.identifier);
CREATE CONSTRAINT ON (p:Place) ASSERT exists(p.name);
CREATE CONSTRAINT ON (p:Place) ASSERT p.identifier IS UNIQUE;
MATCH (p:Place {id: $placeID}), (m:Media {id: $photoID})
CREATE (p)-[:DEPICTED_BY]->(m);


CREATE (lt:LocalizedText {id: $id, text: $text, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
CREATE CONSTRAINT ON (lt:LocalizedText) ASSERT lt.id IS UNIQUE;
CREATE CONSTRAINT ON (lt:LocalizedText) ASSERT exists(lt.text);

MATCH (lt:LocalizedText {id: $localizedTextID}), (c:Citation {id: $citationID})
CREATE (lt)-[:FOR {referenceType: "extract", onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(c);
CREATE CONSTRAINT ON (lt)-[rel:FOR]->(c) ASSERT exists(rel.referenceType);
MATCH (lt:LocalizedText {id: $localizedTextID}), (p:Place {id: $placeID})
CREATE (lt)-[:FOR {referenceType: "name", onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(p);
CREATE CONSTRAINT ON (lt)-[rel:FOR]->(p) ASSERT exists(rel.referenceType);

CREATE (n:Note {id: $id, note: $note, locale: $locale});
CREATE CONSTRAINT ON (n:Note) ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Note) ASSERT exists(n.note);
MATCH (n:Note {id: $noteID}), (a:Assertion {id: $assertionID})
CREATE (n)-[:FOR]->(a);
MATCH (n:Note {id: $noteID}), (c:Citation {id: $citationID})
CREATE (n)-[:FOR]->(c);
MATCH (n:Note {id: $noteID}), (s:Source {id: $sourceID})
CREATE (n)-[:FOR]->(s);
MATCH (n:Note {id: $noteID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (n)-[:FOR]->(cn);
MATCH (n:Note {id: $noteID}), (hd:HistoricDate {id: $historicDateID})
CREATE (n)-[:FOR]->(hd);
MATCH (n:Note {id: $noteID}), (cld:Calendar {id: $calendarID})
CREATE (n)-[:FOR]->(cld);
MATCH (n:Note {id: $noteID}), (e:Event {id: $eventID})
CREATE (n)-[:FOR]->(e);
MATCH (n:Note {id: $noteID}), (r:Repository {id: $repositoryID})
CREATE (n)-[:FOR]->(r);
MATCH (n:Note {id: $noteID}), (p:Place {id: $placeID})
CREATE (n)-[:FOR]->(p);
MATCH (n:Note {id: $noteID}), (pn:PersonName {id: $personNameID})
CREATE (n)-[:FOR]->(pn);
MATCH (n:Note {id: $noteID}), (p:Person {id: $personID})
CREATE (n)-[:FOR]->(p);
MATCH (n:Note {id: $noteID}), (g:Group {id: $groupID})
CREATE (n)-[:FOR]->(g);
MATCH (n:Note {id: $noteID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (n)-[:FOR]->(rs);
MATCH (n:Note {id: $noteID}), (m:Media {id: $mediaID})
CREATE (n)-[:FOR]->(m);


CREATE (m:Media {id: $id, identifier: $identifier, title: $title, payload: $payload, type: $type, photoProjection: $photoProjection});
CREATE CONSTRAINT ON (m:Media) ASSERT m.id IS UNIQUE;
CREATE CONSTRAINT ON (m:Media) ASSERT exists(m.identifier);
CREATE CONSTRAINT ON (m:Media) ASSERT m.identifier IS UNIQUE;
MATCH (m:Media {id: $mediaID}), (hd:HistoricDate {id: $historicDateID})
CREATE (m)-[:CREATED_ON]->(hd);

MATCH (m:Media {id: $mediaID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cn);
MATCH (m:Media {id: $mediaID}), (e:Event {id: $eventID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(e);
MATCH (m:Media {id: $mediaID}), (r:Repository {id: $repositoryID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(r);
MATCH (m:Media {id: $mediaID}), (s:Source {id: $sourceID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(s);
MATCH (m:Media {id: $mediaID}), (c:Citation {id: $citationID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(c);
MATCH (m:Media {id: $mediaID}), (a:Assertion {id: $assertionID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(a);
MATCH (m:Media {id: $mediaID}), (p:Place {id: $placeID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (m:Media {id: $mediaID}), (n:Note {id: $noteID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(n);
MATCH (m:Media {id: $mediaID}), (p:Person {id: $personID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (m:Media {id: $mediaID}), (pn:PersonName {id: $personNameID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(pn);
MATCH (m:Media {id: $mediaID}), (g:Group {id: $groupID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(g);
MATCH (m:Media {id: $mediaID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (m)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(rs);


CREATE (prs:Person {id: $id, photoCrop: $photoCrop});
CREATE CONSTRAINT ON (prs:Person) ASSERT prs.id IS UNIQUE;
MATCH (m:Media {id: $photoID}), (prs:Person {id: $personID})
CREATE (m)-[:DEPICTED_BY]->(prs);

CREATE (pn:PersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type});
CREATE CONSTRAINT ON (pn:PersonName) ASSERT pn.id IS UNIQUE;
MATCH (pn:PersonName {id: $personNameID}), (prs:Person {id: $personID})
CREATE (pn)-[:FOR]->(prs);

CREATE (lpn:LocalizedPersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
CREATE CONSTRAINT ON (lpn:LocalizedPersonName) ASSERT lpn.id IS UNIQUE;
MATCH (lpn:LocalizedPersonName {id: $personNameID}), (pn:PersonName {id: $personNameID})
CREATE (lpn)-[:TRANSCRIPTION_FOR]->(pn);


CREATE (g:Group {id: $id, type: $type, photoCrop: $photoCrop});
CREATE CONSTRAINT ON (g:Group) ASSERT g.id IS UNIQUE;
MATCH (m:Media {id: $photoID}), (g:Group {id: $groupID})
CREATE (m)-[:DEPICTED_BY]->(g);

MATCH (g:Group {id: $groupID}), (prs:Person {id: $personID})
CREATE (g)-[:OF {role: $role, certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]->(prs);
MATCH (g1:Group {id: $groupID}), (g2:Group {id: $groupID})
CREATE (g1)-[:OF {role: $role, certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]->(g2);
MATCH (g:Group {id: $groupID}), (p:Place {id: $placeID})
CREATE (g)-[:OF {role: $role, certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]->(p);


CREATE (e:Event {id: $id, description: $description});
CREATE CONSTRAINT ON (e:Event) ASSERT e.id IS UNIQUE;
MATCH (e:Event {id: $eventID}), (et:EventType {id: $eventTypeID})
CREATE (e)-[:OF_TYPE]->(et);
MATCH (e:Event {id: $eventID}), (p:Place {id: $placeID})
CREATE (e)-[:HAPPENED_IN]->(p);
MATCH (e:Event {id: $eventID}), (hd:HistoricDate {id: $historicDateID})
CREATE (e)-[:HAPPENED_ON]->(hd);
MATCH (e:Event {id: $eventID}), (prs:Person {id: $personID})
CREATE (e)-[:FOR]->(prs);
MATCH (e:Event {id: $eventID}), (g:Group {id: $groupID})
CREATE (e)-[:FOR]->(g);
MATCH (e:Event {id: $eventID}), (p:Place {id: $placeID})
CREATE (e)-[:FOR]->(p);
MATCH (e:Event {id: $eventID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (e)-[:FOR]->(cn);
MATCH (e:Event {id: $eventID}), (cld:Calendar {id: $calendarID})
CREATE (e)-[:FOR]->(cld);
MATCH (e:Event {id: $eventID}), (m:Media {id: $mediaID})
CREATE (e)-[:FOR]->(m);
MATCH (e:Event {id: $eventID}), (pn:PersonName {id: $personNameID})
CREATE (e)-[:FOR]->(pn);

CREATE (et:EventType {id: $id, type: $type, category: $category});
CREATE CONSTRAINT ON (et:EventType) ASSERT et.id IS UNIQUE;
CREATE CONSTRAINT ON (et:EventType) ASSERT exists(et.type);
MATCH (et:EventType {id: $eventTypeID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (et)-[:OF]->(pn);

CREATE (est:EventSuperType {id: $id, superType: $superType});
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT est.id IS UNIQUE;
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT exists(est.superType);
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT est.superType IS UNIQUE;


CREATE (cn:CulturalNorm {id: $id, identifier: $identifier, description: $description, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT cn.id IS UNIQUE;
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT exists(cn.identifier);
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT cn.identifier IS UNIQUE;
MATCH (cn:CulturalNorm {id: $culturalNormID}), (p:Place {id: $placeID})
CREATE (cn)-[:APPLIES_IN]->(p);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (hd:HistoricDate {id: $historicDateID})
CREATE (cn)-[:STARTED_ON]->(hd);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (hd:HistoricDate {id: $historicDateID})
CREATE (cn)-[:ENDED_ON]->(hd);

MATCH (cn:CulturalNorm {id: $culturalNormID}), (a:Assertion {id: $assertionID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(a);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (n:Note {id: $noteID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(n);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (pn:PersonName {id: $personNameID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(pn);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (g:Group {id: $groupID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(g);


CREATE (rst:Restriction {id: $id, restriction: $restriction});
CREATE CONSTRAINT ON (rst:Restriction) ASSERT rst.id IS UNIQUE;
CREATE CONSTRAINT ON (rst:Restriction) ASSERT exists(rst.restriction);
MATCH (rst:Restriction {id: $restrictionID}), (a:Assertion {id: $assertionID})
CREATE (rst)-[:FOR]->(a);
MATCH (rst:Restriction {id: $restrictionID}), (c:Citation {id: $citationID})
CREATE (rst)-[:FOR]->(c);
MATCH (rst:Restriction {id: $restrictionID}), (s:Source {id: $sourceID})
CREATE (rst)-[:FOR]->(s);
MATCH (rst:Restriction {id: $restrictionID}), (r:Repository {id: $repositoryID})
CREATE (rst)-[:FOR]->(r);
MATCH (rst:Restriction {id: $restrictionID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (rst)-[:FOR]->(cn);
MATCH (rst:Restriction {id: $restrictionID}), (hd:HistoricDate {id: $historicDateID})
CREATE (rst)-[:FOR]->(hd);
MATCH (rst:Restriction {id: $restrictionID}), (e:Event {id: $eventID})
CREATE (rst)-[:FOR]->(e);
MATCH (rst:Restriction {id: $restrictionID}), (p:Place {id: $placeID})
CREATE (rst)-[:FOR]->(p);
MATCH (rst:Restriction {id: $restrictionID}), (n:Note {id: $noteID})
CREATE (rst)-[:FOR]->(n);
MATCH (rst:Restriction {id: $restrictionID}), (pn:PersonName {id: $personNameID})
CREATE (rst)-[:FOR]->(pn);
MATCH (rst:Restriction {id: $restrictionID}), (prs:Person {id: $personID})
CREATE (rst)-[:FOR]->(prs);
MATCH (rst:Restriction {id: $restrictionID}), (g:Group {id: $groupID})
CREATE (rst)-[:FOR]->(g);
MATCH (rst:Restriction {id: $restrictionID}), (m:Media {id: $mediaID})
CREATE (rst)-[:FOR]->(m);

CREATE (mdf:Modification {id: $id, creationDate: $creationDate, updateDate: $updateDate});
CREATE CONSTRAINT ON (mdf:Modification) ASSERT mdf.id IS UNIQUE;
CREATE CONSTRAINT ON (mdf:Modification) ASSERT exists(mdf.creationDate);
MATCH (mdf:Modification {id: $modificationID}), (a:Assertion {id: $assertionID})
CREATE (mdf)-[:FOR]->(a);
MATCH (mdf:Modification {id: $modificationID}), (c:Citation {id: $citationID})
CREATE (mdf)-[:FOR]->(c);
MATCH (mdf:Modification {id: $modificationID}), (s:Source {id: $sourceID})
CREATE (mdf)-[:FOR]->(s);
MATCH (mdf:Modification {id: $modificationID}), (r:Repository {id: $repositoryID})
CREATE (mdf)-[:FOR]->(r);
MATCH (mdf:Modification {id: $modificationID}), (hd:HistoricDate {id: $historicDateID})
CREATE (mdf)-[:FOR]->(hd);
MATCH (mdf:Modification {id: $modificationID}), (cld:Calendar {id: $calendarID})
CREATE (mdf)-[:FOR]->(cld);
MATCH (mdf:Modification {id: $modificationID}), (p:Place {id: $placeID})
CREATE (mdf)-[:FOR]->(p);
MATCH (mdf:Modification {id: $modificationID}), (lt:LocalizedText {id: $localizedTextID})
CREATE (mdf)-[:FOR]->(lt);
MATCH (mdf:Modification {id: $modificationID}), (n:Note {id: $noteID})
CREATE (mdf)-[:FOR]->(n);
MATCH (mdf:Modification {id: $modificationID}), (m:Media {id: $mediaID})
CREATE (mdf)-[:FOR]->(m);
MATCH (mdf:Modification {id: $modificationID}), (prs:Person {id: $personID})
CREATE (mdf)-[:FOR]->(prs);
MATCH (mdf:Modification {id: $modificationID}), (pn:PersonName {id: $personNameID})
CREATE (mdf)-[:FOR]->(pn);
MATCH (mdf:Modification {id: $modificationID}), (lpn:LocalizedPersonName {id: $localizedPersonNameID})
CREATE (mdf)-[:FOR]->(lpn);
MATCH (mdf:Modification {id: $modificationID}), (g:Group {id: $groupID})
CREATE (mdf)-[:FOR]->(g);
MATCH (mdf:Modification {id: $modificationID}), (e:Event {id: $eventID})
CREATE (mdf)-[:FOR]->(e);
MATCH (mdf:Modification {id: $modificationID}), (et:EventType {id: $eventTypeID})
CREATE (mdf)-[:FOR]->(et);
MATCH (mdf:Modification {id: $modificationID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (mdf)-[:FOR]->(est);
MATCH (mdf:Modification {id: $modificationID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (mdf)-[:FOR]->(cn);
MATCH (mdf:Modification {id: $modificationID}), (rst:Restriction {id: $restrictionID})
CREATE (mdf)-[:FOR]->(rst);
MATCH (mdf:Modification {id: $modificationID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (mdf)-[:FOR]->(rs);
MATCH (mdf:Modification {id: $modificationID}), (ctc:Contact {id: $contactID})
CREATE (mdf)-[:FOR]->(ctc);
MATCH (mdf:Modification {id: $modificationID}), (prj:Project {id: $projectID})
CREATE (mdf)-[:FOR]->(prj);

CREATE (rs:ResearchStatus {id: $id, identifier: $identifier, description: $description, status: $status, priority: $priority, creationDate: $creationDate});
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT rs.id IS UNIQUE;
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT exists(rs.identifier);
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT exists(rs.creationDate);
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT rs.identifier IS UNIQUE;
MATCH (rs:ResearchStatus {id: $researchStatusID}), (a:Assertion {id: $assertionID})
CREATE (rs)-[:FOR]->(a);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (c:Citation {id: $citationID})
CREATE (rs)-[:FOR]->(c);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (s:Source {id: $sourceID})
CREATE (rs)-[:FOR]->(s);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (r:Repository {id: $repositoryID})
CREATE (rs)-[:FOR]->(r);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (hd:HistoricDate {id: $historicDateID})
CREATE (rs)-[:FOR]->(hd);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (cld:Calendar {id: $calendarID})
CREATE (rs)-[:FOR]->(cld);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (p:Place {id: $placeID})
CREATE (rs)-[:FOR]->(p);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (lt:LocalizedText {id: $localizedTextID})
CREATE (rs)-[:FOR]->(lt);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (n:Note {id: $noteID})
CREATE (rs)-[:FOR]->(n);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (m:Media {id: $mediaID})
CREATE (rs)-[:FOR]->(m);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (prs:Person {id: $personID})
CREATE (rs)-[:FOR]->(prs);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (pn:PersonName {id: $personNameID})
CREATE (rs)-[:FOR]->(pn);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (lpn:LocalizedPersonName {id: $localizedPersonNameID})
CREATE (rs)-[:FOR]->(lpn);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (g:Group {id: $groupID})
CREATE (rs)-[:FOR]->(g);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (e:Event {id: $eventID})
CREATE (rs)-[:FOR]->(e);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (et:EventType {id: $eventTypeID})
CREATE (rs)-[:FOR]->(et);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (rs)-[:FOR]->(est);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (rs)-[:FOR]->(cn);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (rst:Restriction {id: $restrictionID})
CREATE (rs)-[:FOR]->(rst);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (mdf:Modification {id: $modificationID})
CREATE (rs)-[:FOR]->(mdf);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (ctc:Contact {id: $contactID})
CREATE (rs)-[:FOR]->(ctc);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (prj:Project {id: $projectID})
CREATE (rs)-[:FOR]->(prj);

CREATE (ctc:Contact {id: $id, callerID: $callerID, note: $note});
CREATE CONSTRAINT ON (ctc:Contact) ASSERT ctc.id IS UNIQUE;
CREATE CONSTRAINT ON (ctc:Contact) ASSERT exists(ctc.callerID);

MATCH (ctc:Contact {id: $contactID}), (a:Assertion {id: $assertionID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(a);
MATCH (ctc:Contact {id: $contactID}), (c:Citation {id: $citationID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(c);
MATCH (ctc:Contact {id: $contactID}), (s:Source {id: $sourceID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(s);
MATCH (ctc:Contact {id: $contactID}), (r:Repository {id: $repositoryID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(r);
MATCH (ctc:Contact {id: $contactID}), (hd:HistoricDate {id: $historicDateID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(hd);
MATCH (ctc:Contact {id: $contactID}), (cld:Calendar {id: $calendarID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cld);
MATCH (ctc:Contact {id: $contactID}), (p:Place {id: $placeID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (ctc:Contact {id: $contactID}), (lt:LocalizedText {id: $localizedTextID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(lt);
MATCH (ctc:Contact {id: $contactID}), (n:Note {id: $noteID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(n);
MATCH (ctc:Contact {id: $contactID}), (m:Media {id: $mediaID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(m);
MATCH (ctc:Contact {id: $contactID}), (prs:Person {id: $personID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(prs);
MATCH (ctc:Contact {id: $contactID}), (pn:PersonName {id: $personNameID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(pn);
MATCH (ctc:Contact {id: $contactID}), (lpn:LocalizedPersonName {id: $localizedPersonNameID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(lpn);
MATCH (ctc:Contact {id: $contactID}), (g:Group {id: $groupID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(g);
MATCH (ctc:Contact {id: $contactID}), (e:Event {id: $eventID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(e);
MATCH (ctc:Contact {id: $contactID}), (et:EventType {id: $eventTypeID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(et);
MATCH (ctc:Contact {id: $contactID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(est);
MATCH (ctc:Contact {id: $contactID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cn);
MATCH (ctc:Contact {id: $contactID}), (rst:Restriction {id: $restrictionID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(rst);
MATCH (ctc:Contact {id: $contactID}), (mdf:Modification {id: $modificationID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(mdf);
MATCH (ctc:Contact {id: $contactID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(rs);
MATCH (ctc:Contact {id: $contactID}), (prj:Project {id: $projectID})
CREATE (ctc)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(prj);

CREATE (prj:Project {id: $id, protocolName: $protocolName, protocolVersion: $protocolVersion, copyright: $copyright, note: $note, locale: $locale, creationDate: $creationDate, updateDate: $updateDate});
CREATE CONSTRAINT ON (prj:Project) ASSERT prj.id IS UNIQUE;
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.protocolName);
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.protocolVersion);
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.creationDate);
