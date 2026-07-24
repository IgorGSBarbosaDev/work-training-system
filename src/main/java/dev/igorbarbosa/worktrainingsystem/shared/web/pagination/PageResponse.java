package dev.igorbarbosa.worktrainingsystem.shared.web.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last,
		List<SortItem> sort) {

	public static <T> PageResponse<T> from(Page<T> page) {
		List<SortItem> sort = page.getSort().stream()
				.map(order -> new SortItem(order.getProperty(), order.getDirection().name()))
				.toList();
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast(),
				sort);
	}

	public record SortItem(String property, String direction) {
	}
}
