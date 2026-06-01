package com.diplomawork.agents.executor;

import com.diplomawork.agents.db.TaskRepository;
import com.diplomawork.agents.model.Task;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Minimal executor agent that receives task assignment messages.
 *
 * This version marks assigned tasks as running, simulates execution, and marks
 * them completed with a placeholder result.
 */
public class ExecutorAgent extends Agent {
    private static final long SIMULATED_WORK_MILLIS = 1000L;

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

            long taskId = Long.parseLong(message.getContent());
            System.out.println(getLocalName() + " received task assignment: " + taskId);
            System.out.println(getLocalName() + " assignment sender: " + message.getSender().getLocalName());
            executeTask(taskId);
        }

        private void executeTask(long taskId) {
            TaskRepository repository = new TaskRepository();
            try {
                Optional<Task> runningTask = repository.markTaskRunning(taskId, getLocalName());
                if (runningTask.isEmpty()) {
                    System.out.println(getLocalName() + " could not start task " + taskId
                        + ". It may no longer be assigned to this agent.");
                    return;
                }

                System.out.println(getLocalName() + " started task:");
                System.out.println(runningTask.get());

                Thread.sleep(SIMULATED_WORK_MILLIS);

                Optional<Task> completedTask = repository.markTaskCompleted(
                    taskId,
                    getLocalName(),
                    "Executed by " + getLocalName()
                );

                if (completedTask.isEmpty()) {
                    System.out.println(getLocalName() + " could not complete task " + taskId
                        + ". It may no longer be running.");
                    return;
                }

                System.out.println(getLocalName() + " completed task:");
                System.out.println(completedTask.get());
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                markFailed(repository, taskId, "Execution interrupted");
            } catch (SQLException error) {
                System.err.println(getLocalName() + " failed while executing task " + taskId
                    + ": " + error.getMessage());
                markFailed(repository, taskId, error.getMessage());
            }
        }

        private void markFailed(TaskRepository repository, long taskId, String errorMessage) {
            try {
                Optional<Task> failedTask = repository.markTaskFailed(taskId, getLocalName(), errorMessage);
                if (failedTask.isPresent()) {
                    System.out.println(getLocalName() + " marked task failed:");
                    System.out.println(failedTask.get());
                }
            } catch (SQLException databaseError) {
                System.err.println(getLocalName() + " could not mark task " + taskId
                    + " failed: " + databaseError.getMessage());
            }
        }
    }
}
