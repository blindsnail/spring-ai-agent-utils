package org.springaicommunity.agent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Small collection of {@link LocalDate} helpers used by the demo agent.
 *
 * <p>
 * All methods are null-safe: passing {@code null} where a date is required throws
 * {@link IllegalArgumentException} rather than a {@link NullPointerException}.
 */
public final class DateUtil {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private DateUtil() {
	}

	/**
	 * Parses an ISO-8601 date (e.g. {@code 2024-01-15}).
	 * @param date the date text to parse, surrounding whitespace is ignored
	 * @return the parsed date, never {@code null}
	 * @throws IllegalArgumentException if the text is null, blank, or not a valid ISO date
	 */
	public static LocalDate parseDate(String date) {
		if (date == null || date.isBlank()) {
			throw new IllegalArgumentException("Date must not be null or blank");
		}
		try {
			return LocalDate.parse(date.trim(), ISO_DATE);
		}
		catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("Invalid ISO-8601 date: '" + date + "'", ex);
		}
	}

	/**
	 * Formats a date as an ISO-8601 string (e.g. {@code 2024-01-15}).
	 * @param date the date to format
	 * @return the ISO-8601 representation, never {@code null}
	 * @throws IllegalArgumentException if the date is {@code null}
	 */
	public static String formatDate(LocalDate date) {
		requireNonNull(date);
		return date.format(ISO_DATE);
	}

	/**
	 * Returns the number of days between two dates. The result is negative when {@code end}
	 * is before {@code start}.
	 * @param start the start date (inclusive)
	 * @param end the end date (exclusive)
	 * @return the signed number of days between the two dates
	 * @throws IllegalArgumentException if either date is {@code null}
	 */
	public static long daysBetween(LocalDate start, LocalDate end) {
		requireNonNull(start);
		requireNonNull(end);
		return ChronoUnit.DAYS.between(start, end);
	}

	/**
	 * Reports whether the given date falls on a Saturday or Sunday.
	 * @param date the date to inspect
	 * @return {@code true} for Saturday and Sunday, {@code false} otherwise
	 * @throws IllegalArgumentException if the date is {@code null}
	 */
	public static boolean isWeekend(LocalDate date) {
		requireNonNull(date);
		DayOfWeek day = date.getDayOfWeek();
		return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
	}

	/**
	 * Adds a number of business days (Monday through Friday) to a date, skipping weekends.
	 * @param date the starting date
	 * @param days the number of business days to add, must not be negative
	 * @return the resulting date
	 * @throws IllegalArgumentException if the date is {@code null} or {@code days} is negative
	 */
	public static LocalDate addBusinessDays(LocalDate date, int days) {
		requireNonNull(date);
		if (days < 0) {
			throw new IllegalArgumentException("days must not be negative, was " + days);
		}
		LocalDate result = date;
		for (int added = 0; added < days;) {
			result = result.plusDays(1);
			if (!isWeekend(result)) {
				added++;
			}
		}
		return result;
	}

	private static void requireNonNull(LocalDate date) {
		if (date == null) {
			throw new IllegalArgumentException("Date must not be null");
		}
	}

}
