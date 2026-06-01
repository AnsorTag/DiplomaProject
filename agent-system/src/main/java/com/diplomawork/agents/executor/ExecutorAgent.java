package com.diplomawork.agents.executor;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Minimal executor agent that receives task assignment messages.
 *
 * This version only prints received task ids. It does not update task status or
 * execute work yet.
 */
public class ExecutorAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        addBehaviour(new TaskAssignmentReceiver());
    }

    private class TaskAssignmentReceiver extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage message = receive();
            if (message == null) {
                block();
                return;
            }

            if (!"task-assignment".equals(message.getConversationId())) {
                System.out.println(getLocalName() + " ignored message with conversation id: "
                    + message.getConversationId());
                return;
            }

            System.out.println(getLocalName() + " received task assignment: " + message.getContent());
            System.out.println(getLocalName() + " assignment sender: " + message.getSender().getLocalName());
        }
    }
}
