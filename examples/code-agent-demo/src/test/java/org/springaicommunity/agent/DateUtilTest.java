package org.springaicommunity.agent;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("DateUtil Tests")
class DateUtilTest {

	@Nested
	@DisplayName("parseDate")
	class ParseDate {

		@Test
		@DisplayName("Should parse a valid ISO date")
		void shouldParseValidIsoDate() {
			assertThat(DateUtil.parseDate("2024-01-15")).isEqualTo(LocalDate.of(2024, 1, 15));
		}

		@Test
		@DisplayName("Should ignore surrounding whitespace")
		void shouldIgnoreSurroundingWhitespace() {
			assertThat(DateUtil.parseDate("  2024-01-15 ")).isEqualTo(LocalDate.of(2024, 1, 15));
		}

		@Test
		@DisplayName("Should reject null")
		void shouldRejectNull() {
			assertThatIllegalArgumentException().isThrownBy(() -> DateUtil.parseDate(null))
				.withMessageContaining("null or blank");
		}

		@Test
		@DisplayName("Should reject blank")
		void shouldRejectBlank() {
			assertThatIllegalArgumentException().isThrownBy(() -> DateUtil.parseDate("   "))
				.withMessageContaining("null or blank");
		}

		@Test
		@DisplayName("Should reject a malformed date")
		void shouldRejectMalformedDate() {
			assertThatThrownBy(() -> DateUtil.parseDate("15/01/2024"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid ISO-8601 date");
		}

		@Test
		@DisplayName("Should reject an out-of-range date")
		void shouldRejectOutOfRangeDate() {
			assertThatThrownBy(() -> DateUtil.parseDate("2024-13-01"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid ISO-8601 date");
		}

	}

	@Nested
	@DisplayName("formatDate")
	class FormatDate {

		@Test
		@DisplayName("Should format to ISO-8601")
		void shouldFormatToIso() {
			assertThat(DateUtil.formatDate(LocalDate.of(2024, 1, 5))).isEqualTo("2024-01-05");
		}

		@Test
		@DisplayName("Should reject null")
		void shouldRejectNull() {
			assertThatIllegalArgumentException().isThrownBy(() -> DateUtil.formatDate(null))
				.withMessageContaining("must not be null");
		}

		@Test
		@DisplayName("Should round-trip with parseDate")
		void shouldRoundTripWithParseDate() {
			LocalDate date = LocalDate.of(2024, 2, 29);
			assertThat(DateUtil.parseDate(DateUtil.formatDate(date))).isEqualTo(date);
		}

	}

	@Nested
	@DisplayName("daysBetween")
	class DaysBetween {

		@Test
		@DisplayName("Should count forward days")
		void shouldCountForwardDays() {
			assertThat(DateUtil.daysBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))).isEqualTo(30);
		}

		@Test
		@DisplayName("Should return zero for the same date")
		void shouldReturnZeroForSameDate() {
			assertThat(DateUtil.daysBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))).isZero();
		}

		@Test
		@DisplayName("Should return a negative value when end is before start")
		void shouldReturnNegativeWhenEndBeforeStart() {
			assertThat(DateUtil.daysBetween(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 1, 1))).isEqualTo(-30);
		}

		@Test
		@DisplayName("Should reject null arguments")
		void shouldRejectNullArguments() {
			assertThatIllegalArgumentException()
				.isThrownBy(() -> DateUtil.daysBetween(null, LocalDate.of(2024, 1, 1)));
			assertThatIllegalArgumentException()
				.isThrownBy(() -> DateUtil.daysBetween(LocalDate.of(2024, 1, 1), null));
		}

	}

	@Nested
	@DisplayName("isWeekend")
	class IsWeekend {

		@Test
		@DisplayName("Should be true for Saturday and Sunday")
		void shouldBeTrueForSaturdayAndSunday() {
			assertThat(DateUtil.isWeekend(LocalDate.of(2024, 1, 6))).isTrue(); // Saturday
			assertThat(DateUtil.isWeekend(LocalDate.of(2024, 1, 7))).isTrue(); // Sunday
		}

		@Test
		@DisplayName("Should be false for weekdays")
		void shouldBeFalseForWeekdays() {
			assertThat(DateUtil.isWeekend(LocalDate.of(2024, 1, 8))).isFalse(); // Monday
			assertThat(DateUtil.isWeekend(LocalDate.of(2024, 1, 5))).isFalse(); // Friday
		}

		@Test
		@DisplayName("Should reject null")
		void shouldRejectNull() {
			assertThatIllegalArgumentException().isThrownBy(() -> DateUtil.isWeekend(null));
		}

	}

	@Nested
	@DisplayName("addBusinessDays")
	class AddBusinessDays {

		@Test
		@DisplayName("Should add within the same week")
		void shouldAddWithinSameWeek() {
			assertThat(DateUtil.addBusinessDays(LocalDate.of(2024, 1, 1), 1)).isEqualTo(LocalDate.of(2024, 1, 2));
		}

		@Test
		@DisplayName("Should skip the weekend from Friday")
		void shouldSkipWeekendFromFriday() {
			assertThat(DateUtil.addBusinessDays(LocalDate.of(2024, 1, 5), 1)).isEqualTo(LocalDate.of(2024, 1, 8));
		}

		@Test
		@DisplayName("Should skip the weekend when starting on Saturday")
		void shouldSkipWeekendWhenStartingOnSaturday() {
			assertThat(DateUtil.addBusinessDays(LocalDate.of(2024, 1, 6), 1)).isEqualTo(LocalDate.of(2024, 1, 8));
		}

		@Test
		@DisplayName("Should span multiple weeks")
		void shouldSpanMultipleWeeks() {
			assertThat(DateUtil.addBusinessDays(LocalDate.of(2024, 1, 1), 5)).isEqualTo(LocalDate.of(2024, 1, 8));
		}

		@Test
		@DisplayName("Should return the same date when adding zero days")
		void shouldReturnSameDateWhenAddingZero() {
			LocalDate saturday = LocalDate.of(2024, 1, 6);
			assertThat(DateUtil.addBusinessDays(saturday, 0)).isEqualTo(saturday);
		}

		@Test
		@DisplayName("Should reject negative days")
		void shouldRejectNegativeDays() {
			assertThatThrownBy(() -> DateUtil.addBusinessDays(LocalDate.of(2024, 1, 1), -1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not be negative");
		}

		@Test
		@DisplayName("Should reject null date")
		void shouldRejectNullDate() {
			assertThatIllegalArgumentException().isThrownBy(() -> DateUtil.addBusinessDays(null, 1));
		}

	}

}
