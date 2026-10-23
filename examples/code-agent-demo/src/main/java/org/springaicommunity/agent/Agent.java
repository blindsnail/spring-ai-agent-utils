package org.springaicommunity.agent;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.ai.chat.client.ChatClient;

/**
 * The configured agent: the {@link ChatClient} to send prompts to, plus the mutable turn
 * deadline polled by the {@code InterruptAdvisor}. Shell commands set the deadline to
 * {@code now + turnTimeout} before starting a turn.
 */
record Agent(ChatClient chatClient, AtomicLong turnDeadline) {
}
