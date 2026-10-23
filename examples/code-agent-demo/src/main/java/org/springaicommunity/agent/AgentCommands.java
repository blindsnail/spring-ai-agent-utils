package org.springaicommunity.agent;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springaicommunity.agent.advisors.TurnInterruptedException;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.core.command.annotation.Arguments;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

/**
 * Spring Shell commands that expose the agent.
 *
 * <p>
 * {@code ask <message>} runs one turn: the message is sent to the model and the assistant
 * reply is printed by the shell. {@code new-conversation} drops the chat memory by
 * switching to a fresh conversation id.
 */
@Component
public class AgentCommands {

	private final Agent agent;

	private final Duration turnTimeout;

	private final AtomicLong conversationCounter = new AtomicLong(1);

	private String conversationId = "session-1";

	public AgentCommands(Agent agent, @Value("${agent.turn.timeout:10m}") Duration turnTimeout) {
		this.agent = agent;
		this.turnTimeout = turnTimeout;
	}

	/**
	 * Sends a free-form message to the agent. All remaining tokens are joined back into a
	 * single prompt, so quotes are optional: {@code ask what is the weather in Paris}.
	 */
	@Command(name = "ask", alias = "chat", description = "Send a message to the agent.")
	public String ask(@Arguments String[] message) {
		if (message == null || message.length == 0) {
			return "Usage: ask <message>";
		}
		return reply(String.join(" ", message));
	}

	/**
	 * Starts a new conversation, discarding the previous chat memory.
	 */
	@Command(name = "new-conversation", description = "Forget the current conversation and start a new one.")
	public String newConversation() {
		this.conversationId = "session-" + this.conversationCounter.incrementAndGet();
		return "Started a new conversation (" + this.conversationId + ").";
	}

	private String reply(String userMessage) {
		this.agent.turnDeadline().set(System.currentTimeMillis() + this.turnTimeout.toMillis());
		try {
			return this.agent.chatClient()
				.prompt(userMessage)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, this.conversationId))
				.call()
				.content();
		}
		catch (TurnInterruptedException ex) {
			return "[turn stopped after exceeding " + this.turnTimeout + "]";
		}
	}

}
