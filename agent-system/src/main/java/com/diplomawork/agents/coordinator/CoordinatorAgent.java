package com.diplomawork.agents.coordinator;

import com.diplomawork.agents.db.TaskRepository;
import com.diplomawork.agents.model.Task;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * First JADE coordinator agent.
 *
 * On startup, it performs one scheduling step by assigning the highest-priority
 * pending task to the executor agent and sending a JADE message with the task id.
 */
public class CoordinatorAgent extends Agent {
    private static final int DEFAULT_TASK_LIMIT = 10;
    private static final String EXECUTOR_AGENT_NAME = "executor";

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        assignOnePendingTask();
    }

    private void assignOnePendingTask() {
        TaskRepository repository = new TaskRepository();
        try {
            List<Task> tasks = repository.findPendingTasks(DEFAULT_TASK_LIMIT);
            System.out.println(getLocalName() + " found pending tasks: " + tasks.size());

            if (tasks.isEmpty()) {
                System.out.println(getLocalName() + " has no pending task to assign.");
                return;
            }

            Task selectedTask = tasks.get(0);
            Optional<Task> assignedTask = repository.assignPendingTask(
                selectedTask.getId(),
                EXECUTOR_AGENT_NAME
            );

            if (assignedTask.isEmpty()) {
                System.out.println(getLocalName() + " could not assign task " + selectedTask.getId()
                    + ". It may have been assigned by another agent.");
                return;
            }

            System.out.println(getLocalName() + " assigned task:");
            System.out.println(assignedTask.get());
            sendAssignmentMessage(assignedTask.get());
        } catch (SQLException error) {
            System.err.println(getLocalName() + " failed during scheduling: " + error.getMessage());
            error.printStackTrace(System.err);
        }
    }

    private void sendAssignmentMessage(Task task) {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID(EXECUTOR_AGENT_NAME, AID.ISLOCALNAME));
        message.setConversationId("task-assignment");
        message.setContent(Long.toString(task.getId()));
        send(message);
        System.out.println(getLocalName() + " sent task " + task.getId()
            + " to " + EXECUTOR_AGENT_NAME + ".");
    }
}
