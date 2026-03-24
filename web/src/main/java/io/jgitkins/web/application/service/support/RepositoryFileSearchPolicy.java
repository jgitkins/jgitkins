package io.jgitkins.web.application.service.support;

import io.jgitkins.web.application.dto.RepositoryFileIndexEntry;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RepositoryFileSearchPolicy {

	public List<RepositoryFileIndexEntry> search(List<RepositoryFileIndexEntry> index, String query, int limit) {
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
		int safeLimit = Math.max(1, Math.min(limit, 100));

		return (index == null ? List.<RepositoryFileIndexEntry>of() : index).stream()
				.filter(entry -> entry != null && entry.path() != null)
				.filter(entry -> "tree".equalsIgnoreCase(entry.type()) || "blob".equalsIgnoreCase(entry.type()) || entry.type() == null)
				.filter(entry -> {
					if (!StringUtils.hasText(normalizedQuery)) {
						return true;
					}
					String path = entry.path() == null ? "" : entry.path().toLowerCase();
					String name = entry.name() == null ? "" : entry.name().toLowerCase();
					return path.contains(normalizedQuery) || name.contains(normalizedQuery);
				})
				.sorted(Comparator.comparing(RepositoryFileIndexEntry::path, Comparator.nullsLast(String::compareToIgnoreCase)))
				.limit(safeLimit)
				.toList();
	}
}
