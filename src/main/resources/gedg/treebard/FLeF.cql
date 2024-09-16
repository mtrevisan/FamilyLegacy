-- Assertion - Citation - Source - Repository

CREATE (a:Assertion {id: $id, role: $role, certainty: $certainty, credibility: $credibility});
MATCH (a:Assertion {id: $assertionID}), (c:Citation {id: $citationID})
CREATE (a)-[:ASSERTED_IN]->(c);
MATCH (a:Assertion {id: $assertionID}), (p:Place {id: $placeID})
CREATE (a)-[:ASSERTION_FOR]->(p);
MATCH (a:Assertion {id: $assertionID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (a)-[:ASSERTION_FOR]->(cn);
MATCH (a:Assertion {id: $assertionID}), (hd:HistoricDate {id: $historicDateID})
CREATE (a)-[:ASSERTION_FOR]->(hd);
MATCH (a:Assertion {id: $assertionID}), (cld:Calendar {id: $calendarID})
CREATE (a)-[:ASSERTION_FOR]->(cld);
MATCH (a:Assertion {id: $assertionID}), (prs:Person {id: $personID})
CREATE (a)-[:ASSERTION_FOR]->(prs);
MATCH (a:Assertion {id: $assertionID}), (g:Group {id: $groupID})
CREATE (a)-[:ASSERTION_FOR]->(g);
MATCH (a:Assertion {id: $assertionID}), (m:Media {id: $mediaID})
CREATE (a)-[:ASSERTION_FOR]->(m);
MATCH (a:Assertion {id: $assertionID}), (pn:PersonName {id: $personNameID})
CREATE (a)-[:ASSERTION_FOR]->(pn);

CREATE (c:Citation {id: $id, location: $location, extract: $extract, extractLocale: $extractLocale, extractType: $extractType});
CREATE CONSTRAINT ON (c:Citation) ASSERT exists(c.extract);
MATCH (c:Citation {id: $citationID}), (s:Source {id: $sourceID})
CREATE (c)-[:CITED_IN]->(s);

CREATE (s:Source {id: $id, identifier: $identifier, type: $type, author: $author, location: $location});
CREATE CONSTRAINT ON (s:Source) ASSERT exists(s.identifier);
CREATE CONSTRAINT ON (s:Source) ASSERT s.identifier IS UNIQUE;
MATCH (s:Source {id: $sourceID}), (r:Repository {id: $repositoryID})
CREATE (s)-[:SOURCED_IN]->(r);
MATCH (s:Source {id: $sourceID}), (p:Place {id: $placeID})
CREATE (s)-[:CREATED_IN]->(r);
MATCH (s:Source {id: $sourceID}), (hd:HistoricDate {id: $historicDateID})
CREATE (s)-[:CREATED_WHEN]->(hd);

CREATE (r:Repository {id: $id, identifier: $identifier, type: $type});
CREATE CONSTRAINT ON (r:Repository) ASSERT exists(r.identifier);
CREATE CONSTRAINT ON (r:Repository) ASSERT r.identifier IS UNIQUE;
MATCH (prs:Person {id: $personID}), (r:Repository {id: $repositoryID})
CREATE (prs)-[:OWNER_OF]->(r);
MATCH (r:Repository {id: $repositoryID}), (p:Place {id: $placeID})
CREATE (r)-[:LOCATED_IN]->(p);


-- Date

CREATE (hd:HistoricDate {id: $id, date: $date, dateOriginal: $dateOriginal, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (hd:HistoricDate) ASSERT exists(hd.date);
MATCH (hd:HistoricDate {id: $historicDateID}), (cld:Calendar {id: $calendarOriginalID})
CREATE (hd)-[:REPRESENTED_WITHIN]->(cld);

CREATE (cld:Calendar {id: $id, type: $type});
CREATE CONSTRAINT ON (cld:Calendar) ASSERT exists(cld.type);
CREATE CONSTRAINT ON (cld:Calendar) ASSERT cld.type IS UNIQUE;


-- Place

CREATE (p:Place {id: $id, identifier: $identifier, name: $name, locale: $locale, type: $type, coordinate: $coordinate, coordinateSystem: $coordinateSystem, coordinateCredibility: $coordinateCredibility, photoCrop: $photoCrop});
CREATE CONSTRAINT ON (p:Place) ASSERT exists(p.identifier);
CREATE CONSTRAINT ON (p:Place) ASSERT exists(p.name);
CREATE CONSTRAINT ON (p:Place) ASSERT p.identifier IS UNIQUE;
MATCH (p:Place {id: $placeID}), (m:Media {id: $photoID})
CREATE (p)-[:DEPICTED_WITH]->(m);


--- Localized text - Note

CREATE (lt:LocalizedText {id: $id, text: $text, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
CREATE CONSTRAINT ON (lt:LocalizedText) ASSERT exists(lt.text);

MATCH (lt:LocalizedText {id: $localizedTextID}), (c:Citation {id: $citationID})
CREATE (lt)-[:NAME_FOR {referenceType: "extract"}]->(c);
CREATE CONSTRAINT ON (lt)-[rel:NAME_FOR]->(c) ASSERT exists(rel.referenceType);
MATCH (lt:LocalizedText {id: $localizedTextID}), (p:Place {id: $placeID})
CREATE (lt)-[:NAME_FOR {referenceType: "name"}]->(p);
CREATE CONSTRAINT ON (lt)-[rel:NAME_FOR]->(p) ASSERT exists(rel.referenceType);

CREATE (n:Note {id: $id, note: $note, locale: $locale});
CREATE CONSTRAINT ON (n:Note) ASSERT exists(n.note);
MATCH (n:Note {id: $noteID}), (a:Assertion {id: $assertionID})
CREATE (n)-[:NOTE_FOR]->(a);
MATCH (n:Note {id: $noteID}), (c:Citation {id: $citationID})
CREATE (n)-[:NOTE_FOR]->(c);
MATCH (n:Note {id: $noteID}), (s:Source {id: $sourceID})
CREATE (n)-[:NOTE_FOR]->(s);
MATCH (n:Note {id: $noteID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (n)-[:NOTE_FOR]->(cn);
MATCH (n:Note {id: $noteID}), (hd:HistoricDate {id: $historicDateID})
CREATE (n)-[:NOTE_FOR]->(hd);
MATCH (n:Note {id: $noteID}), (cld:Calendar {id: $calendarID})
CREATE (n)-[:NOTE_FOR]->(cld);
MATCH (n:Note {id: $noteID}), (e:Event {id: $eventID})
CREATE (n)-[:NOTE_FOR]->(e);
MATCH (n:Note {id: $noteID}), (r:Repository {id: $repositoryID})
CREATE (n)-[:NOTE_FOR]->(r);
MATCH (n:Note {id: $noteID}), (p:Place {id: $placeID})
CREATE (n)-[:NOTE_FOR]->(p);
MATCH (n:Note {id: $noteID}), (pn:PersonName {id: $personNameID})
CREATE (n)-[:NOTE_FOR]->(pn);
MATCH (n:Note {id: $noteID}), (p:Person {id: $personID})
CREATE (n)-[:NOTE_FOR]->(p);
MATCH (n:Note {id: $noteID}), (g:Group {id: $groupID})
CREATE (n)-[:NOTE_FOR]->(g);
MATCH (n:Note {id: $noteID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (n)-[:NOTE_FOR]->(rs);
MATCH (n:Note {id: $noteID}), (m:Media {id: $mediaID})
CREATE (n)-[:NOTE_FOR]->(m);


-- Media

CREATE (m:Media {id: $id, identifier: $identifier, title: $title, payload: $payload, type: $type, photoProjection: $photoProjection});
CREATE CONSTRAINT ON (m:Media) ASSERT exists(m.identifier);
CREATE CONSTRAINT ON (m:Media) ASSERT m.identifier IS UNIQUE;
MATCH (m:Media {id: $mediaID}), (hd:HistoricDate {id: $historicDateID})
CREATE (m)-[:RECORDED_ON]->(hd);

MATCH (m:Media {id: $mediaID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (m)-[:MEDIA_FOR]->(cn);
MATCH (m:Media {id: $mediaID}), (e:Event {id: $eventID})
CREATE (m)-[:MEDIA_FOR]->(e);
MATCH (m:Media {id: $mediaID}), (r:Repository {id: $repositoryID})
CREATE (m)-[:MEDIA_FOR]->(r);
MATCH (m:Media {id: $mediaID}), (s:Source {id: $sourceID})
CREATE (m)-[:MEDIA_FOR]->(s);
MATCH (m:Media {id: $mediaID}), (c:Citation {id: $citationID})
CREATE (m)-[:MEDIA_FOR]->(c);
MATCH (m:Media {id: $mediaID}), (a:Assertion {id: $assertionID})
CREATE (m)-[:MEDIA_FOR]->(a);
MATCH (m:Media {id: $mediaID}), (p:Place {id: $placeID})
CREATE (m)-[:MEDIA_FOR]->(p);
MATCH (m:Media {id: $mediaID}), (n:Note {id: $noteID})
CREATE (m)-[:MEDIA_FOR]->(n);
MATCH (m:Media {id: $mediaID}), (p:Person {id: $personID})
CREATE (m)-[:MEDIA_FOR]->(p);
MATCH (m:Media {id: $mediaID}), (pn:PersonName {id: $personNameID})
CREATE (m)-[:MEDIA_FOR]->(pn);
MATCH (m:Media {id: $mediaID}), (g:Group {id: $groupID})
CREATE (m)-[:MEDIA_FOR]->(g);
MATCH (m:Media {id: $mediaID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (m)-[:MEDIA_FOR]->(rs);


-- Person

CREATE (prs:Person {id: $id, photoCrop: $photoCrop});
MATCH (m:Media {id: $photoID}), (prs:Person {id: $personID})
CREATE (m)-[:PHOTO_FOR]->(prs);

CREATE (pn:PersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type});
MATCH (pn:PersonName {id: $personNameID}), (prs:Person {id: $personID})
CREATE (pn)-[:NAME_FOR]->(prs);

CREATE (lpn:LocalizedPersonName {id: $id, personalName: $personalName, familyName: $familyName, locale: $locale, type: $type, transcription: $transcription, transcriptionType: $transcriptionType});
MATCH (lpn:LocalizedPersonName {id: $personNameID}), (pn:PersonName {id: $personNameID})
CREATE (lpn)-[:TRANSCRIPTION_FOR]->(pn);


-- Group

CREATE (g:Group {id: $id, type: $type, photoCrop: $photoCrop});
MATCH (m:Media {id: $photoID}), (g:Group {id: $groupID})
CREATE (m)-[:PHOTO_FOR]->(g);

MATCH (g:Group {id: $groupID}), (prs:Person {id: $personID})
CREATE (g)-[:GROUP_FOR {role: $role, certainty: $certainty, credibility: $credibility}]->(prs);
MATCH (g1:Group {id: $groupID}), (g2:Group {id: $groupID})
CREATE (g1)-[:GROUP_FOR {role: $role, certainty: $certainty, credibility: $credibility}]->(g2);
MATCH (g:Group {id: $groupID}), (p:Place {id: $placeID})
CREATE (g)-[:GROUP_FOR {role: $role, certainty: $certainty, credibility: $credibility}]->(p);


-- Event

CREATE (e:Event {id: $id, description: $description});
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
CREATE CONSTRAINT ON (et:EventType) ASSERT exists(et.type);
MATCH (et:EventType {id: $eventTypeID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (et)-[:OF]->(pn);

CREATE (est:EventSuperType {id: $id, superType: $superType});
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT exists(est.superType);
CREATE CONSTRAINT ON (est:EventSuperType) ASSERT est.superType IS UNIQUE;


-- Cultural norm

CREATE (cn:CulturalNorm {id: $id, identifier: $identifier, description: $description, certainty: $certainty, credibility: $credibility});
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT exists(cn.identifier);
CREATE CONSTRAINT ON (cn:CulturalNorm) ASSERT cn.identifier IS UNIQUE;
MATCH (cn:CulturalNorm {id: $culturalNormID}), (p:Place {id: $placeID})
CREATE (cn)-[:APPLY_IN]->(p);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (hds:HistoricDate {id: $historicDateID})
CREATE (cn)-[:START_ON]->(hds);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (hde:HistoricDate {id: $historicDateID})
CREATE (cn)-[:END_ON]->(hde);

MATCH (cn:CulturalNorm {id: $culturalNormID}), (a:Assertion {id: $assertionID})
CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(a);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (n:Note {id: $noteID})
CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(n);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (pn:PersonName {id: $personNameID})
CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(pn);
MATCH (cn:CulturalNorm {id: $culturalNormID}), (g:Group {id: $groupID})
CREATE (cn)-[:END_ON {certainty: $certainty, credibility: $credibility}]->(g);


-- Other application-related things

CREATE (rst:Restriction {id: $id, restriction: $restriction});
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
MATCH (mdf:Modification {id: $modificationID}), (lpn:Assertion {id: $localizedPersonNameID})
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
MATCH (rs:ResearchStatus {id: $researchStatusID}), (lpn:Assertion {id: $localizedPersonNameID})
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
CREATE CONSTRAINT ON (ctc:Contact) ASSERT exists(ctc.callerID);

MATCH (ctc:Contact {id: $contactID}), (a:Assertion {id: $assertionID})
CREATE (ctc)-[:FOR]->(a);
MATCH (ctc:Contact {id: $contactID}), (c:Citation {id: $citationID})
CREATE (ctc)-[:FOR]->(c);
MATCH (ctc:Contact {id: $contactID}), (s:Source {id: $sourceID})
CREATE (ctc)-[:FOR]->(s);
MATCH (ctc:Contact {id: $contactID}), (r:Repository {id: $repositoryID})
CREATE (ctc)-[:FOR]->(r);
MATCH (ctc:Contact {id: $contactID}), (hd:HistoricDate {id: $historicDateID})
CREATE (ctc)-[:FOR]->(hd);
MATCH (ctc:Contact {id: $contactID}), (cld:Calendar {id: $calendarID})
CREATE (ctc)-[:FOR]->(cld);
MATCH (ctc:Contact {id: $contactID}), (p:Place {id: $placeID})
CREATE (ctc)-[:FOR]->(p);
MATCH (ctc:Contact {id: $contactID}), (lt:LocalizedText {id: $localizedTextID})
CREATE (ctc)-[:FOR]->(lt);
MATCH (ctc:Contact {id: $contactID}), (n:Note {id: $noteID})
CREATE (ctc)-[:FOR]->(n);
MATCH (ctc:Contact {id: $contactID}), (m:Media {id: $mediaID})
CREATE (ctc)-[:FOR]->(m);
MATCH (ctc:Contact {id: $contactID}), (prs:Person {id: $personID})
CREATE (ctc)-[:FOR]->(prs);
MATCH (ctc:Contact {id: $contactID}), (pn:PersonName {id: $personNameID})
CREATE (ctc)-[:FOR]->(pn);
MATCH (ctc:Contact {id: $contactID}), (lpn:Assertion {id: $localizedPersonNameID})
CREATE (ctc)-[:FOR]->(lpn);
MATCH (ctc:Contact {id: $contactID}), (g:Group {id: $groupID})
CREATE (ctc)-[:FOR]->(g);
MATCH (ctc:Contact {id: $contactID}), (e:Event {id: $eventID})
CREATE (ctc)-[:FOR]->(e);
MATCH (ctc:Contact {id: $contactID}), (et:EventType {id: $eventTypeID})
CREATE (ctc)-[:FOR]->(et);
MATCH (ctc:Contact {id: $contactID}), (est:EventSuperType {id: $eventSuperTypeID})
CREATE (ctc)-[:FOR]->(est);
MATCH (ctc:Contact {id: $contactID}), (cn:CulturalNorm {id: $culturalNormID})
CREATE (ctc)-[:FOR]->(cn);
MATCH (ctc:Contact {id: $contactID}), (rst:Restriction {id: $restrictionID})
CREATE (ctc)-[:FOR]->(rst);
MATCH (ctc:Contact {id: $contactID}), (mdf:Modification {id: $modificationID})
CREATE (ctc)-[:FOR]->(mdf);
MATCH (ctc:Contact {id: $contactID}), (rs:ResearchStatus {id: $researchStatusID})
CREATE (ctc)-[:FOR]->(rs);
MATCH (ctc:Contact {id: $contactID}), (prj:Project {id: $projectID})
CREATE (ctc)-[:FOR]->(prj);

CREATE (prj:Project {id: $id, protocolName: $protocolName, protocolVersion: $protocolVersion, copyright: $copyright, note: $note, locale: $locale, creationDate: $creationDate, updateDate: $updateDate});
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.protocolName);
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.protocolVersion);
CREATE CONSTRAINT ON (prj:Project) ASSERT exists(prj.creationDate);
