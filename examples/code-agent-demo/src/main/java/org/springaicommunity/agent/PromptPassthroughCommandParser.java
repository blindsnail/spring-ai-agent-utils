package org.springaicommunity.agent;

import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.ParsedInput;

/**
 * Gives the interactive shell the feel of the original demo REPL: a line that does not
 * start with a registered command is treated as a prompt for the agent.
 *
 * <p>
 * Lines whose first token is a known command name or alias are handed to the standard
 * {@link DefaultCommandParser}, so built-ins ({@code help}, {@code exit}, {@code clear},
 * ...) and the demo commands ({@code new-conversation}, ...) keep working. Every other
 * line is parsed as {@code <passthroughCommand> <line>}, passing the original line through
 * as a single argument so quotes, dashes and option-like tokens (e.g. {@code --help}) reach
 * the model verbatim instead of being interpreted as shell syntax.
 *
 * <p>
 * This is registered as a {@link CommandParser} bean, which makes Spring Shell's
 * auto-configured {@code DefaultCommandParser} back off.
 */
class PromptPassthroughCommandParser implements CommandParser {

	private final String passthroughCommand;

	private final CommandRegistry commandRegistry;

	private final CommandParser delegate;

	PromptPassthroughCommandParser(String passthroughCommand, CommandRegistry commandRegistry) {
		this.passthroughCommand = passthroughCommand;
		this.commandRegistry = commandRegistry;
		this.delegate = new DefaultCommandParser(commandRegistry);
	}

	@Override
	public ParsedInput parse(String line) {
		if (isRegisteredCommand(firstToken(line))) {
			return this.delegate.parse(line);
		}
		return ParsedInput.builder()
			.commandName(this.passthroughCommand)
			.addArgument(CommandArgument.with().index(0).value(line).type(String.class).build())
			.build();
	}

	private boolean isRegisteredCommand(String name) {
		return this.commandRegistry.getCommandByName(name) != null
				|| this.commandRegistry.getCommandByAlias(name) != null;
	}

	/**
	 * The first whitespace-delimited token of the line, i.e. the potential command name.
	 * Returns an empty string for blank input, which never matches a command.
	 */
	private static String firstToken(String line) {
		String trimmed = line.stripLeading();
		int end = 0;
		while (end < trimmed.length() && !Character.isWhitespace(trimmed.charAt(end))) {
			end++;
		}
		return trimmed.substring(0, end);
	}

}
