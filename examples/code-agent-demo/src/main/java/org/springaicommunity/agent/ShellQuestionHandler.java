package org.springaicommunity.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.reader.LineReader;
import org.springaicommunity.agent.tools.AskUserQuestionTool.Question;
import org.springaicommunity.agent.tools.AskUserQuestionTool.Question.Option;
import org.springaicommunity.agent.tools.AskUserQuestionTool.QuestionHandler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@link QuestionHandler} for {@code AskUserQuestionTool} that reads answers through the
 * Spring Shell terminal (JLine {@link LineReader}) instead of opening a second
 * {@link java.util.Scanner} on {@code System.in}, which would fight with the shell for
 * the terminal.
 *
 * <p>
 * The {@link LineReader} is resolved lazily through an {@link ObjectProvider}: the tool
 * (and therefore this handler) is built while the command registry is being created,
 * which is before the {@code LineReader} bean exists. By the time the agent actually asks
 * a question the shell is running and the provider resolves the singleton.
 */
@Component
class ShellQuestionHandler implements QuestionHandler {

	private final ObjectProvider<LineReader> lineReader;

	ShellQuestionHandler(ObjectProvider<LineReader> lineReader) {
		this.lineReader = lineReader;
	}

	@Override
	public Map<String, String> handle(List<Question> questions) {
		LineReader reader = this.lineReader.getObject();
		Map<String, String> answers = new HashMap<>();
		for (Question question : questions) {
			answers.put(question.question(), ask(reader, question));
		}
		return answers;
	}

	private static String ask(LineReader reader, Question question) {
		StringBuilder prompt = new StringBuilder();
		prompt.append(question.header()).append(": ").append(question.question());

		List<Option> options = question.options();
		if (options != null) {
			for (int i = 0; i < options.size(); i++) {
				Option option = options.get(i);
				prompt.append("\n  ")
					.append(i + 1)
					.append(". ")
					.append(option.label())
					.append(" - ")
					.append(option.description());
			}
		}

		prompt.append(Boolean.TRUE.equals(question.multiSelect())
				? "\n(Enter numbers separated by commas, or type custom text) "
				: "\n(Enter a number, or type custom text) ");

		return parse(reader.readLine(prompt.toString()).trim(), options);
	}

	/**
	 * Resolves comma-separated option numbers to their labels, mirroring the built-in
	 * {@code CommandLineQuestionHandler}. Anything that is not a valid option number is
	 * passed through unchanged as custom text.
	 */
	private static String parse(String response, List<Option> options) {
		if (options == null || options.isEmpty()) {
			return response;
		}
		try {
			List<String> labels = new ArrayList<>();
			for (String token : response.split(",")) {
				int index = Integer.parseInt(token.trim()) - 1;
				if (index >= 0 && index < options.size()) {
					labels.add(options.get(index).label());
				}
			}
			return labels.isEmpty() ? response : String.join(",", labels);
		}
		catch (NumberFormatException ex) {
			return response;
		}
	}

}
