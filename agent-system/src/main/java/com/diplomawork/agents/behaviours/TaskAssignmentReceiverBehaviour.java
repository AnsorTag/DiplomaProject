package com.diplomawork.agents.behaviours;

import com.diplomawork.agents.agent.ExecutorAgent;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Receives task assignment messages and delegates execution to the executor agent.
 */
public class TaskAssignmentReceiverBehaviour extends CyclicBehaviour {
    private static final String TASK_ASSIGNMENT_CONVERSATION_ID = "task-assignment";

    private final ExecutorAgent executorAgent;

    public TaskAssignmentReceiverBehaviour(ExecutorAgent executorAgent) {
        super(executorAgent);
        this.executorAgent = executorAgent;
    }

    @Override
    public void action() {
        ACLMessage message = executorAgent.receive();
        if (message == null) {
            block();
            return;
        }

        if (!TASK_ASSIGNMENT_CONVERSATION_ID.equals(message.getConversationId())) {
            System.out.println(executorAgent.getLocalName() + " ignored message with conversation id: "
                + message.getConversationId());
            return;
        }

        long taskId = Long.parseLong(message.getContent());
        System.out.println(executorAgent.getLocalName() + " received task assignment: " + taskId);
        System.out.println(executorAgent.getLocalName() + " assignment sender: "
            + message.getSender().getLocalName());
        executorAgent.executeTask(taskId);
    }
}
