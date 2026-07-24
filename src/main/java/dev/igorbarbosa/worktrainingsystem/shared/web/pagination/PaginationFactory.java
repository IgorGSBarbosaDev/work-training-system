package dev.igorbarbosa.worktrainingsystem.shared.web.pagination;

import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PaginationFactory {

	public PageRequest create(int page, int size, String sortExpression, Set<String> allowedProperties) {
		String[] sortParts = sortExpression.split(",", -1);
		if (sortParts.length != 2) {
			throw new InvalidPaginationException("A ordenação deve seguir o formato campo,direção.");
		}

		String property = sortParts[0].trim();
		if (!allowedProperties.contains(property)) {
			throw new InvalidPaginationException("O campo de ordenação informado não é permitido.");
		}

		try {
			Sort.Direction direction = Sort.Direction.fromString(sortParts[1].trim().toUpperCase(Locale.ROOT));
			Sort sort = Sort.by(direction, property).and(Sort.by(direction, "id"));
			return PageRequest.of(page, size, sort);
		} catch (IllegalArgumentException exception) {
			throw new InvalidPaginationException("A direção deve ser asc ou desc.");
		}
	}
}
