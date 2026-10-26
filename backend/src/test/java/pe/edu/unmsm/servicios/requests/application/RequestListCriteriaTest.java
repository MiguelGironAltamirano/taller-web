package pe.edu.unmsm.servicios.requests.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

class RequestListCriteriaTest {

	private static RequestListCriteria.Params params(List<String> status, String areaId, String categoryId,
			String priorityId, String technicianId, String unassigned, String overdue, String q, String from,
			String to, String sort, String page, String size) {
		return new RequestListCriteria.Params(status, areaId, categoryId, priorityId, technicianId, unassigned,
				overdue, q, from, to, sort, page, size);
	}

	private static RequestListCriteria.Params empty() {
		return params(null, null, null, null, null, null, null, null, null, null, null, null, null);
	}

	private static java.util.Map<String, String> errorsOf(RequestListCriteria.Params params) {
		try {
			RequestListCriteria.parse(params);
		}
		catch (InvalidRequestException ex) {
			return ex.errors();
		}
		throw new AssertionError("Expected the parameters to be rejected");
	}

	@Test
	void withoutParametersItListsEverythingNewestFirstOnTheFirstPage() {
		RequestListCriteria criteria = RequestListCriteria.parse(empty());

		assertThat(criteria).isEqualTo(RequestListCriteria.firstPage());
		assertThat(criteria.statuses()).isEmpty();
		assertThat(criteria.hasText()).isFalse();
		assertThat(criteria.overdue()).isNull();
		assertThat(criteria.sort()).isEqualTo(RequestSort.NEWEST);
		assertThat(criteria.page()).isZero();
		assertThat(criteria.size()).isEqualTo(RequestListCriteria.DEFAULT_PAGE_SIZE);
	}

	@Test
	void statusAcceptsRepeatedAndCommaSeparatedValues() {
		RequestListCriteria criteria = RequestListCriteria
				.parse(params(List.of("REGISTRADA,ASIGNADA", "RESUELTA", " "), null, null, null, null, null, null, null,
						null, null, null, null, null));

		assertThat(criteria.statuses()).isEqualTo(
				Set.of(RequestStatus.REGISTRADA, RequestStatus.ASIGNADA, RequestStatus.RESUELTA));
	}

	@Test
	void everyFilterIsParsed() {
		RequestListCriteria criteria = RequestListCriteria.parse(params(null, "1", "2", "3", "4", "false", "true",
				"  aula 101 ", "2026-10-01T05:00:00Z", "2026-11-01T05:00:00Z", "priority", "2", "50"));

		assertThat(criteria.areaId()).isEqualTo(1L);
		assertThat(criteria.categoryId()).isEqualTo(2L);
		assertThat(criteria.priorityId()).isEqualTo(3L);
		assertThat(criteria.technicianId()).isEqualTo(4L);
		assertThat(criteria.unassigned()).isFalse();
		assertThat(criteria.overdue()).isTrue();
		assertThat(criteria.text()).isEqualTo("aula 101");
		assertThat(criteria.from()).isEqualTo(Instant.parse("2026-10-01T05:00:00Z"));
		assertThat(criteria.to()).isEqualTo(Instant.parse("2026-11-01T05:00:00Z"));
		assertThat(criteria.sort()).isEqualTo(RequestSort.PRIORITY);
		assertThat(criteria.page()).isEqualTo(2);
		assertThat(criteria.size()).isEqualTo(50);
	}

	@Test
	void unknownStatusSortAndMalformedValuesAreReportedPerField() {
		var errors = errorsOf(params(List.of("ABIERTA"), "x", "0", "-1", "1.5", "si", "yes", null, "2026-10-01",
				"ayer", "random", "uno", "big"));

		assertThat(errors).containsOnlyKeys("status", "areaId", "categoryId", "priorityId", "technicianId",
				"unassigned", "overdue", "from", "to", "sort", "page", "size");
	}

	@Test
	void pageAndSizeMustBeWithinBounds() {
		assertThat(errorsOf(params(null, null, null, null, null, null, null, null, null, null, null, "-1", null)))
				.containsOnlyKeys("page");
		assertThat(errorsOf(params(null, null, null, null, null, null, null, null, null, null, null, null, "0")))
				.containsOnlyKeys("size");
		assertThat(errorsOf(params(null, null, null, null, null, null, null, null, null, null, null, null,
				String.valueOf(RequestListCriteria.MAX_PAGE_SIZE + 1)))).containsOnlyKeys("size");
		assertThat(RequestListCriteria.parse(params(null, null, null, null, null, null, null, null, null, null, null,
				null, String.valueOf(RequestListCriteria.MAX_PAGE_SIZE))).size())
				.isEqualTo(RequestListCriteria.MAX_PAGE_SIZE);
	}

	@Test
	void theDateRangeMustBeIncreasing() {
		assertThat(errorsOf(params(null, null, null, null, null, null, null, null, "2026-10-02T00:00:00Z",
				"2026-10-01T00:00:00Z", null, null, null))).containsOnlyKeys("to");
		assertThat(errorsOf(params(null, null, null, null, null, null, null, null, "2026-10-01T00:00:00Z",
				"2026-10-01T00:00:00Z", null, null, null))).containsOnlyKeys("to");
	}

	@Test
	void unassignedCannotBeCombinedWithATechnician() {
		assertThat(errorsOf(params(null, null, null, null, "3", "true", null, null, null, null, null, null, null)))
				.containsOnlyKeys("unassigned");
	}

	@Test
	void theTextSearchIsBounded() {
		String longText = "a".repeat(RequestListCriteria.MAX_TEXT_LENGTH + 1);

		assertThat(errorsOf(params(null, null, null, null, null, null, null, longText, null, null, null, null, null)))
				.containsOnlyKeys("q");
	}

	@Test
	void blankParametersAreIgnored() {
		RequestListCriteria criteria = RequestListCriteria.parse(params(List.of(""), " ", "", null, "", "", " ",
				"   ", "", "", "", "", ""));

		assertThat(criteria).isEqualTo(RequestListCriteria.firstPage());
	}

	@Test
	void theSortParameterIsAnAllowList() {
		for (RequestSort sort : RequestSort.values()) {
			assertThat(RequestListCriteria.parse(params(null, null, null, null, null, null, null, null, null, null,
					sort.param(), null, null)).sort()).isEqualTo(sort);
		}
		assertThatThrownBy(() -> RequestListCriteria.parse(params(null, null, null, null, null, null, null, null, null,
				null, "created_at desc; drop table users", null, null))).isInstanceOf(InvalidRequestException.class);
	}

}
