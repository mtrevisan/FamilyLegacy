CREATE (a:Assertion {id: $id, role: $role, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (a:Assertion) ASSERT a.id IS UNIQUE;
MATCH (a:Assertion {id: $assertionID}), (c:Citation {id: $citationID})
CREATE (a)-[:INFERRED_FROM {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(c);
MATCH (a:Assertion {id: $assertionID}), (p:Place {id: $placeID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(p);
MATCH (a:Assertion {id: $assertionID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(cn);
MATCH (a:Assertion {id: $assertionID}), (hd:HistoricDate {id: $historicDateID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(hd);
MATCH (a:Assertion {id: $assertionID}), (cld:Calendar {id: $calendarID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(cld);
MATCH (a:Assertion {id: $assertionID}), (prs:Person {id: $personID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(prs);
MATCH (a:Assertion {id: $assertionID}), (pn:PersonName {id: $personNameID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(pn);
MATCH (a:Assertion {id: $assertionID}), (g:Group {id: $groupID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(g);
MATCH (a:Assertion {id: $assertionID}), (m:Media {id: $mediaID})
CREATE (a)<-[:SUPPORTED_BY {onDelete: "RELATIONSHIP_ONLY"}]-(m);

CREATE (c:Citation {id: $id, location: $location, extract: $extract, extractLocale: $extractLocale, extractType: $extractType});
CREATE CONSTRAINT ON (c:Citation) ASSERT c.id IS UNIQUE;
CREATE CONSTRAINT ON (c:Citation) ASSERT exists(c.extract);
MATCH (c:Citation {id: $citationID}), (s:Source {id: $sourceID})
CREATE (c)<-[:QUOTES {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]-(s);

CREATE (s:Source {id: $id, identifier: $identifier, type: $type, author: $author, location: $location});
CREATE CONSTRAINT ON (s:Source) ASSERT s.id IS UNIQUE;
CREATE CONSTRAINT ON (s:Source) ASSERT exists(s.identifier);
CREATE CONSTRAINT ON (s:Source) ASSERT s.identifier IS UNIQUE;
MATCH (s:Source {id: $sourceID}), (r:Repository {id: $repositoryID})
CREATE (s)-[:STORED_IN {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(r);
MATCH (s:Source {id: $sourceID}), (p:Place {id: $placeID})
CREATE (s)-[:CREATED_IN {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (s:Source {id: $sourceID}), (hd:HistoricDate {id: $historicDateID})
CREATE (s)-[:CREATED_ON {onDelete: "RELATIONSHIP_ONLY"}]->(hd);

CREATE (r:Repository {id: $id, identifier: $identifier, type: $type});
CREATE CONSTRAINT ON (r:Repository) ASSERT r.id IS UNIQUE;
CREATE CONSTRAINT ON (r:Repository) ASSERT exists(r.identifier);
CREATE CONSTRAINT ON (r:Repository) ASSERT r.identifier IS UNIQUE;
MATCH (prs:Person {id: $personID}), (r:Repository {id: $repositoryID})
CREATE (r)<-[:OWNS {onDelete: "RELATIONSHIP_ONLY"}]-(prs);
MATCH (r:Repository {id: $repositoryID}), (p:Place {id: $placeID})
CREATE (r)-[:LOCATED_IN {onDelete: "RELATIONSHIP_ONLY"}]->(p);


CREATE (hd:HistoricDate {id: $id, date: $date, dateOriginal: $dateOriginal, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (hd:HistoricDate) ASSERT hd.id IS UNIQUE;
CREATE CONSTRAINT ON (hd:HistoricDate) ASSERT exists(hd.date);
MATCH (hd:HistoricDate {id: $historicDateID}), (cld:Calendar {id: $calendarOriginalID})
CREATE (hd)-[:EXPRESSED_IN {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(cld);

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
CREATE (p)-[:DEPICTED_BY {onDelete: "RELATIONSHIP_ONLY"}]->(m);


CREATE (lt:LocalizedText {id: $id, text: $text, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
CREATE CONSTRAINT ON (lt:LocalizedText) ASSERT lt.id IS UNIQUE;
CREATE CONSTRAINT ON (lt:LocalizedText) ASSERT exists(lt.text);

MATCH (lt:LocalizedText {id: $localizedTextID}), (c:Citation {id: $citationID})
CREATE (lt)-[:TRANSCRIPTION_FOR {type: "extract", onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(c);
CREATE CONSTRAINT ON (lt)-[rel:TRANSCRIPTION_FOR]->(c) ASSERT exists(rel.type);
MATCH (lt:LocalizedText {id: $localizedTextID}), (p:Place {id: $placeID})
CREATE (lt)-[:TRANSCRIPTION_FOR {type: "name", onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(p);
CREATE CONSTRAINT ON (lt)-[rel:TRANSCRIPTION_FOR]->(p) ASSERT exists(rel.type);

CREATE (n:Note {id: $id, note: $note, locale: $locale});
CREATE CONSTRAINT ON (n:Note) ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Note) ASSERT exists(n.note);
MATCH (n:Note {id: $noteID}), (a:Assertion {id: $assertionID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(a);
MATCH (n:Note {id: $noteID}), (c:Citation {id: $citationID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(c);
MATCH (n:Note {id: $noteID}), (s:Source {id: $sourceID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(s);
MATCH (n:Note {id: $noteID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cn);
MATCH (n:Note {id: $noteID}), (hd:HistoricDate {id: $historicDateID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(hd);
MATCH (n:Note {id: $noteID}), (cld:Calendar {id: $calendarID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cld);
MATCH (n:Note {id: $noteID}), (e:Event {id: $eventID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(e);
MATCH (n:Note {id: $noteID}), (r:Repository {id: $repositoryID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(r);
MATCH (n:Note {id: $noteID}), (p:Place {id: $placeID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (n:Note {id: $noteID}), (p:Person {id: $personID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (n:Note {id: $noteID}), (pn:PersonName {id: $personNameID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(pn);
MATCH (n:Note {id: $noteID}), (g:Group {id: $groupID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(g);
MATCH (n:Note {id: $noteID}), (m:Media {id: $mediaID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(m);
MATCH (n:Note {id: $noteID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (n)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(rs);


CREATE (m:Media {id: $id, identifier: $identifier, title: $title, payload: $payload, type: $type, photoProjection: $photoProjection});
CREATE CONSTRAINT ON (m:Media) ASSERT m.id IS UNIQUE;
CREATE CONSTRAINT ON (m:Media) ASSERT exists(m.identifier);
CREATE CONSTRAINT ON (m:Media) ASSERT m.identifier IS UNIQUE;
MATCH (m:Media {id: $mediaID}), (hd:HistoricDate {id: $historicDateID})
CREATE (m)-[:CREATED_ON {onDelete: "RELATIONSHIP_ONLY"}]->(hd);

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
MATCH (prs:Person {id: $personID}), (m:Media {id: $photoID})
CREATE (prs)-[:DEPICTED_BY {onDelete: "RELATIONSHIP_ONLY"}]->(m);

CREATE (pn:PersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type});
CREATE CONSTRAINT ON (pn:PersonName) ASSERT pn.id IS UNIQUE;
MATCH (pn:PersonName {id: $personNameID}), (prs:Person {id: $personID})
CREATE (pn)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(prs);

CREATE (lpn:LocalizedPersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
CREATE CONSTRAINT ON (lpn:LocalizedPersonName) ASSERT lpn.id IS UNIQUE;
MATCH (lpn:LocalizedPersonName {id: $personNameID}), (pn:PersonName {id: $personNameID})
CREATE (lpn)-[:TRANSCRIPTION_FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(pn);


CREATE (g:Group {id: $id, type: $type, photoCrop: $photoCrop});
CREATE CONSTRAINT ON (g:Group) ASSERT g.id IS UNIQUE;
MATCH (g:Group {id: $groupID}), (m:Media {id: $photoID})
CREATE (g)-[:DEPICTED_BY {onDelete: "RELATIONSHIP_ONLY"}]->(m);

MATCH (g:Group {id: $groupID}), (prs:Person {id: $personID})
CREATE (g)-[:OF {role: $role, certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]->(prs);
MATCH (g1:Group {id: $groupID}), (g2:Group {id: $groupID})
CREATE (g1)-[:OF {role: $role, certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]->(g2);
MATCH (g:Group {id: $groupID}), (p:Place {id: $placeID})
CREATE (g)-[:OF {role: $role, certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]->(p);


CREATE (e:Event {id: $id, description: $description});
CREATE CONSTRAINT ON (e:Event) ASSERT e.id IS UNIQUE;
MATCH (e:Event {id: $eventID}), (et:EventType {id: $eventTypeID})
CREATE (e)-[:OF_TYPE {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(et);
MATCH (e:Event {id: $eventID}), (p:Place {id: $placeID})
CREATE (e)-[:HAPPENED_IN {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (e:Event {id: $eventID}), (hd:HistoricDate {id: $historicDateID})
CREATE (e)-[:HAPPENED_ON {onDelete: "RELATIONSHIP_ONLY"}]->(hd);
MATCH (e:Event {id: $eventID}), (prs:Person {id: $personID})
CREATE (e)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(prs);
MATCH (e:Event {id: $eventID}), (pn:PersonName {id: $personNameID})
CREATE (e)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(pn);
MATCH (e:Event {id: $eventID}), (g:Group {id: $groupID})
CREATE (e)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(g);
MATCH (e:Event {id: $eventID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (e)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cn);
MATCH (e:Event {id: $eventID}), (cld:Calendar {id: $calendarID})
CREATE (e)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(cld);
MATCH (e:Event {id: $eventID}), (m:Media {id: $mediaID})
CREATE (e)-[:FOR {onDelete: "RELATIONSHIP_ONLY"}]->(m);

CREATE (et:EventType {id: $id, type: $type, category: $category});
CREATE CONSTRAINT ON (et:EventType) ASSERT et.id IS UNIQUE;
CREATE CONSTRAINT ON (et:EventType) ASSERT exists(et.type);
MATCH (et:EventType {id: $eventTypeID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (et)-[:OF {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(est);

CREATE (est:EventSuperType {id: $id, superType: $superType});
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT est.id IS UNIQUE;
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT exists(est.superType);
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT est.superType IS UNIQUE;


CREATE (cn:CulturalNorm {id: $id, identifier: $identifier, description: $description, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT cn.id IS UNIQUE;
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT exists(cn.identifier);
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT cn.identifier IS UNIQUE;
MATCH (cn:CulturalNorm {id: $culturalNormID}), (p:Place {id: $placeID})
CREATE (cn)-[:APPLIES_IN {onDelete: "RELATIONSHIP_ONLY"}]->(p);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (hd:HistoricDate {id: $historicDateID})
CREATE (cn)-[:STARTED_ON {onDelete: "RELATIONSHIP_ONLY"}]->(hd);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (hd:HistoricDate {id: $historicDateID})
CREATE (cn)-[:ENDED_ON {onDelete: "RELATIONSHIP_ONLY"}]->(hd);

MATCH (cn:CulturalNorm {id: $culturalNormID}), (a:Assertion {id: $assertionID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(a);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (n:Note {id: $noteID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(n);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (pn:PersonName {id: $personNameID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(pn);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (g:Group {id: $groupID})
CREATE (cn)<-[:SUPPORTED_BY {certainty: $certainty, credibility: $credibility, onDelete: "RELATIONSHIP_ONLY"}]-(g);


CREATE (rst:Restriction&Application {id: $id, restriction: $restriction});
CREATE CONSTRAINT ON (rst:Restriction) ASSERT rst.id IS UNIQUE;
CREATE CONSTRAINT ON (rst:Restriction) ASSERT exists(rst.restriction);
MATCH (rst:Restriction {id: $restrictionID}), (a:Assertion {id: $assertionID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(a);
MATCH (rst:Restriction {id: $restrictionID}), (c:Citation {id: $citationID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(c);
MATCH (rst:Restriction {id: $restrictionID}), (s:Source {id: $sourceID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(s);
MATCH (rst:Restriction {id: $restrictionID}), (r:Repository {id: $repositoryID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(r);
MATCH (rst:Restriction {id: $restrictionID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(cn);
MATCH (rst:Restriction {id: $restrictionID}), (hd:HistoricDate {id: $historicDateID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(hd);
MATCH (rst:Restriction {id: $restrictionID}), (e:Event {id: $eventID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(e);
MATCH (rst:Restriction {id: $restrictionID}), (p:Place {id: $placeID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(p);
MATCH (rst:Restriction {id: $restrictionID}), (n:Note {id: $noteID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(n);
MATCH (rst:Restriction {id: $restrictionID}), (pn:PersonName {id: $personNameID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(pn);
MATCH (rst:Restriction {id: $restrictionID}), (prs:Person {id: $personID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(prs);
MATCH (rst:Restriction {id: $restrictionID}), (g:Group {id: $groupID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(g);
MATCH (rst:Restriction {id: $restrictionID}), (m:Media {id: $mediaID})
CREATE (rst)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(m);

CREATE (mdf:Modification&Application {id: $id, creationDate: $creationDate, updateDate: $updateDate});
CREATE CONSTRAINT ON (mdf:Modification) ASSERT mdf.id IS UNIQUE;
CREATE CONSTRAINT ON (mdf:Modification) ASSERT exists(mdf.creationDate);
MATCH (mdf:Modification {id: $modificationID}), (n:Note {id: $noteID})
CREATE (mdf)<-[:CHANGELOG_FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]-(n);
MATCH (mdf:Modification {id: $modificationID}), (a:Assertion {id: $assertionID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(a);
MATCH (mdf:Modification {id: $modificationID}), (c:Citation {id: $citationID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(c);
MATCH (mdf:Modification {id: $modificationID}), (s:Source {id: $sourceID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(s);
MATCH (mdf:Modification {id: $modificationID}), (r:Repository {id: $repositoryID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(r);
MATCH (mdf:Modification {id: $modificationID}), (hd:HistoricDate {id: $historicDateID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(hd);
MATCH (mdf:Modification {id: $modificationID}), (cld:Calendar {id: $calendarID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(cld);
MATCH (mdf:Modification {id: $modificationID}), (p:Place {id: $placeID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(p);
MATCH (mdf:Modification {id: $modificationID}), (lt:LocalizedText {id: $localizedTextID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(lt);
MATCH (mdf:Modification {id: $modificationID}), (n:Note {id: $noteID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(n);
MATCH (mdf:Modification {id: $modificationID}), (m:Media {id: $mediaID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(m);
MATCH (mdf:Modification {id: $modificationID}), (prs:Person {id: $personID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(prs);
MATCH (mdf:Modification {id: $modificationID}), (pn:PersonName {id: $personNameID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(pn);
MATCH (mdf:Modification {id: $modificationID}), (lpn:LocalizedPersonName {id: $localizedPersonNameID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(lpn);
MATCH (mdf:Modification {id: $modificationID}), (g:Group {id: $groupID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(g);
MATCH (mdf:Modification {id: $modificationID}), (e:Event {id: $eventID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(e);
MATCH (mdf:Modification {id: $modificationID}), (et:EventType {id: $eventTypeID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(et);
MATCH (mdf:Modification {id: $modificationID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(est);
MATCH (mdf:Modification {id: $modificationID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(cn);
MATCH (mdf:Modification {id: $modificationID}), (rst:Restriction {id: $restrictionID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(rst);
MATCH (mdf:Modification {id: $modificationID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(rs);
MATCH (mdf:Modification {id: $modificationID}), (ctc:Contact {id: $contactID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(ctc);
MATCH (mdf:Modification {id: $modificationID}), (prj:Project {id: $projectID})
CREATE (mdf)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(prj);

CREATE (rs:ResearchStatus&Application {id: $id, identifier: $identifier, description: $description, status: $status, priority: $priority, creationDate: $creationDate});
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT rs.id IS UNIQUE;
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT exists(rs.identifier);
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT exists(rs.creationDate);
CREATE CONSTRAINT ON (rs:ResearchStatus) ASSERT rs.identifier IS UNIQUE;
MATCH (rs:ResearchStatus {id: $researchStatusID}), (a:Assertion {id: $assertionID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(a);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (c:Citation {id: $citationID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(c);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (s:Source {id: $sourceID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(s);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (r:Repository {id: $repositoryID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(r);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (hd:HistoricDate {id: $historicDateID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(hd);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (cld:Calendar {id: $calendarID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(cld);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (p:Place {id: $placeID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(p);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (lt:LocalizedText {id: $localizedTextID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(lt);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (n:Note {id: $noteID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(n);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (m:Media {id: $mediaID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(m);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (prs:Person {id: $personID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(prs);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (pn:PersonName {id: $personNameID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(pn);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (lpn:LocalizedPersonName {id: $localizedPersonNameID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(lpn);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (g:Group {id: $groupID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(g);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (e:Event {id: $eventID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(e);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (et:EventType {id: $eventTypeID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(et);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(est);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(cn);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (rst:Restriction {id: $restrictionID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(rst);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (mdf:Modification {id: $modificationID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(mdf);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (ctc:Contact {id: $contactID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(ctc);
MATCH (rs:ResearchStatus {id: $researchStatusID}), (prj:Project {id: $projectID})
CREATE (rs)-[:FOR {onDeleteStart: "RELATIONSHIP_ONLY", onDeleteEnd: "CASCADE"}]->(prj);

CREATE (ctc:Contact&Application {id: $id, callerID: $callerID, note: $note});
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

CREATE (prj:Project {id: $id, protocolName: $protocolName, protocolVersion: $protocolVersion, copyright: $copyright, note: $note, locale: $locale, includeMediaPayload: $includeMediaPayload, creationDate: $creationDate, updateDate: $updateDate});
CREATE CONSTRAINT ON (prj:Project) ASSERT prj.id IS UNIQUE;
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.protocolName);
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.protocolVersion);
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.creationDate);
MATCH (prj:Project {id: $projectID}), (prs:Person {id: $personID})
CREATE (prj)<-[:HOME_PERSON_OF {onDelete: "RELATIONSHIP_ONLY"}]-(prs);
