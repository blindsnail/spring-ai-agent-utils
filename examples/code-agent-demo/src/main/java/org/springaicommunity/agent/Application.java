package org.springaicommunity.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.jline.utils.AttributedString;
import org.springaicommunity.agent.advisors.InterruptAdvisor;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.BraveWebSearchTool;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.SmartWebFetchTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.ToolCallListeners;
import org.springaicommunity.agent.utils.AgentEnvironment;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.util.StringUtils;

/**
 * Builds the agent (ChatClient + tools) and exposes it through an interactive Spring
 * Shell (see {@link AgentCommands}).
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * 暴露给 Agent 的全部工具：MCP 提供的工具、Skills 工具以及内置的 agentic 工具。
	 * 仅当设置了 BRAVE_API_KEY 时才注册 Brave 网络搜索。
	 */
	@Bean
	AgentTools agentTools(ToolCallbackProvider mcpToolCallbackProvider, ChatClient.Builder chatClientBuilder,
			@Value("${agent.skills.paths}") List<Resource> skillPaths,
			@Value("${BRAVE_API_KEY:#{null}}") String braveApiKey, ShellQuestionHandler questionHandler) {

		// @formatter:off
		List<ToolCallback> tools = new ArrayList<>();

		// MCP 工具（例如 Airbnb 服务器）
		Collections.addAll(tools, mcpToolCallbackProvider.getToolCallbacks());

		// Skills 工具
		tools.add(SkillsTool.builder().addSkillsResources(skillPaths).build());

		Collections.addAll(tools, ToolCallbacks.from(
				// 待办管理工具
				TodoWriteTool.builder().build(),

				// 向用户提问工具（在 Spring Shell 中提示并读取答案）
				AskUserQuestionTool.builder()
						.questionHandler(questionHandler)
						.answersValidation(false)
						.build(),

				// 通用 agentic 工具
				ShellTools.builder().build(), // Skill 执行脚本时需要
				FileSystemTools.builder().build(), // Skill 读写额外资源时需要
				SmartWebFetchTool.builder(chatClientBuilder.clone().build()).build(),
				GrepTool.builder().build()));

		// Brave 网络搜索是可选的：仅当设置了 BRAVE_API_KEY 时才注册
		if (StringUtils.hasText(braveApiKey)) {
			Collections.addAll(tools,
					ToolCallbacks.from(BraveWebSearchTool.builder(braveApiKey).resultCount(15).build()));
		}
		// @formatter:on

		return new AgentTools(List.copyOf(tools));
	}

	/**
	 * 构建与模型对话的 {@link ChatClient}，并把取消信号（{@link AtomicLong} 截止时间）
	 * 交给 {@link AgentCommands} 在每个 turn 开始时设置。
	 */
	@Bean
	Agent agent(ChatClient.Builder chatClientBuilder,
			@Value("classpath:/prompt/MAIN_AGENT_SYSTEM_PROMPT_V2.md") Resource systemPrompt,
			@Value("${agent.model.knowledge.cutoff:Unknown}") String agentModelKnowledgeCutoff,
			@Value("${agent.model:Unknown}") String agentModel, AgentTools agentTools) {

		// 在控制台追踪每一次工具调用（ToolCallListener）
		List<ToolCallback> observedTools = ToolCallListeners.wrapAll(agentTools.callbacks(),
				new ConsoleToolCallListener());

		// 取消超过截止时间的 turn（InterruptAdvisor）。该信号会在工具调用循环的每次模型请求前轮询。
		AtomicLong turnDeadline = new AtomicLong(Long.MAX_VALUE);

		// @formatter:off
		ChatClient chatClient = chatClientBuilder
				.defaultSystem(p -> p.text(systemPrompt) // 系统提示词
						.param(AgentEnvironment.ENVIRONMENT_INFO_KEY, AgentEnvironment.info())
						.param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
						.param(AgentEnvironment.AGENT_MODEL_KEY, agentModel)
						.param(AgentEnvironment.AGENT_MODEL_KNOWLEDGE_CUTOFF_KEY, agentModelKnowledgeCutoff))

				// 全部工具，已用控制台监听器包装
				.defaultTools(observedTools)

				// 顾问（Advisor）
				.defaultAdvisors(
						InterruptAdvisor.builder()
							.interruptSignal(() -> System.currentTimeMillis() > turnDeadline.get())
							.build(),
						MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build())
							.order(Ordered.HIGHEST_PRECEDENCE + 1000)
							.build()
						// 日志 advisor
//						MyLoggingAdvisor.builder()
//							.showAvailableTools(true)
//							.showSystemMessage(true)
//							.build()
				)
				.build();
		// @formatter:on

		return new Agent(chatClient, turnDeadline);
	}

	/**
	 * Old-REPL style prompt, replacing Spring Shell's default {@code shell:>}.
	 */
	@Bean
	PromptProvider promptProvider() {
		return () -> new AttributedString("> USER: ");
	}

	/**
	 * Restores the free-form REPL of the original demo: input that does not name a
	 * registered command is routed to {@code ask}. Spring Shell keeps handling the
	 * terminal (history, completion, Ctrl-C), and built-ins such as {@code help},
	 * {@code exit} and {@code new-conversation} still work.
	 */
	@Bean
	CommandParser commandParser(CommandRegistry commandRegistry) {
		return new PromptPassthroughCommandParser("ask", commandRegistry);
	}

	/**
	 * Agent 工具的包装类型，使其能作为单个 Bean 注入。直接使用
	 * {@code List<ToolCallback>} Bean 会与 Spring 对单个 {@link ToolCallback} Bean
	 * 的集合注入语义产生歧义。
	 */
	record AgentTools(List<ToolCallback> callbacks) {
	}

}
