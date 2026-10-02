package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.tools.reports.CulturalNormRootSection;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportFormatters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


public final class SourceAndDocumentIndex{

	private final Map<String, List<FLEFRecord>> sourceToCitationsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> sourceToDocumentsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> sourceToRepositoriesMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> docToSourcesMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> docToCitationsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> repoToSourcesMap = new HashMap<>();

	public SourceAndDocumentIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> sources = model.getRecordsByType(SourceHandler.TYPE);
		for(final FLEFRecord source : sources){
			if(!filter.test(source))
				continue;
			final String srcId = source.getId();

			for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(source, "document")){
				if(docRef.getValue() != null){
					sourceToDocumentsMap.computeIfAbsent(srcId, k -> new ArrayList<>()).add(docRef);
					docToSourcesMap.computeIfAbsent(docRef.getValue(), k -> new ArrayList<>()).add(source);
				}
			}

			for(final FLEFRecord repoRef : FLEFRecordHelper.findChildren(source, "repository")){
				final String repoId = FLEFRecordHelper.getChildValue(repoRef, "repository");
				if(repoId != null){
					sourceToRepositoriesMap.computeIfAbsent(srcId, k -> new ArrayList<>()).add(repoRef);
					repoToSourcesMap.computeIfAbsent(repoId, k -> new ArrayList<>()).add(source);
				}
			}
		}

		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records){
			for(final FLEFRecord cit : CulturalNormRootSection.collectDescendantsWithTag(record, "source")){
				final String sid = ReportFormatters.extractSourceId(cit);
				if(sid != null && filter.test(model.getRecordById(sid))){
					sourceToCitationsMap.computeIfAbsent(sid, k -> new ArrayList<>()).add(cit);
				}
				for(final FLEFRecord dp : FLEFRecordHelper.findChildren(cit, "extract.document_part.document")){
					final String did = dp.getValue();
					if(did != null){
						docToCitationsMap.computeIfAbsent(did, k -> new ArrayList<>()).add(cit);
					}
				}
			}
		}
	}

	public List<FLEFRecord> citationsOfSource(final FLEFRecord src){
		return (src != null && src.getId() != null)? sourceToCitationsMap.getOrDefault(src.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> documentsOfSource(final FLEFRecord src){
		return (src != null && src.getId() != null)? sourceToDocumentsMap.getOrDefault(src.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> repositoriesOfSource(final FLEFRecord src){
		return (src != null && src.getId() != null)? sourceToRepositoriesMap.getOrDefault(src.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> sourcesOfDocument(final FLEFRecord doc){
		return (doc != null && doc.getId() != null)? docToSourcesMap.getOrDefault(doc.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> citationsOfDocument(final FLEFRecord doc){
		return (doc != null && doc.getId() != null)? docToCitationsMap.getOrDefault(doc.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> sourcesOfRepository(final FLEFRecord repo){
		return (repo != null && repo.getId() != null)? repoToSourcesMap.getOrDefault(repo.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> repositoryCitationsOf(final FLEFRecord repo){
		return Collections.emptyList();
	}
}
