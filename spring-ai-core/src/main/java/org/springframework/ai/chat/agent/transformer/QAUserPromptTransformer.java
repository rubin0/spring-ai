package org.springframework.ai.chat.agent.transformer;

import org.springframework.ai.chat.agent.AgentContext;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.node.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QAUserPromptTransformer implements PromptTransformer {

	private static final String DEFAULT_USER_PROMPT_TEXT = """
			   "Context information is below.\\n"
			   "---------------------\\n"
			   "{context}\\n"
			   "---------------------\\n"
			   "Given the context information and not prior knowledge, "
			   "answer the question. If the answer is not in the context, inform "
			   "the user that you can't answer the question.\\n"
			   "Question: {question}\\n"
			   "Answer: "
			""";

	@Override
	public AgentContext transform(AgentContext agentContext) {
		String context = doCreateContext(agentContext.getDataList());
		Map<String, Object> contextMap = doCreateContextMap(agentContext.getPrompt(), context);
		Prompt prompt = doCreatePrompt(agentContext.getPrompt(), contextMap);
		agentContext.setPrompt(prompt);
		// Add old prompt to 'provenance' or history...
		return agentContext;
	}

	protected String doCreateContext(List<Node<?>> data) {
		return data.stream()
			.filter(node -> node != null)
			.map(node -> (Node<String>) node)
			.map(Node::getContent)
			.collect(Collectors.joining("\n"));
	}

	private Map<String, Object> doCreateContextMap(Prompt prompt, String context) {
		String originalUserMessage = prompt.getInstructions()
			.stream()
			.filter(m -> m.getMessageType() == MessageType.USER)
			.map(m -> m.getContent())
			.collect(Collectors.joining(System.lineSeparator()));

		return Map.of("context", context, "question", originalUserMessage);
	}

	protected Prompt doCreatePrompt(Prompt originalPrompt, Map<String, Object> contextMap) {
		PromptTemplate promptTemplate = new PromptTemplate(DEFAULT_USER_PROMPT_TEXT);
		Message systemMessageToAppend = promptTemplate.createMessage(contextMap);
		List<Message> messageList = originalPrompt.getInstructions()
			.stream()
			.filter(m -> m.getMessageType() != MessageType.USER)
			.collect(Collectors.toList());
		messageList.add(systemMessageToAppend);
		return new Prompt(messageList, (ChatOptions) originalPrompt.getOptions());
	}

}