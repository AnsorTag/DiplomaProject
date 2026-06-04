package com.diplomawork.agents.executor;

import com.diplomawork.agents.db.TaskRepository;
import com.diplomawork.agents.execution.TaskExecutionException;
import com.diplomawork.agents.execution.TaskExecutor;
import com.diplomawork.agents.model.Task;
import com.diplomawork.agents.model.TaskStatus;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Minimal executor agent that receives task assignment messages.
 *
 * It marks assigned tasks as running, executes supported task types, and records
 * completion, retry, or terminal failure.
 */
public class ExecutorAgent extends Agent {
    private final TaskExecutor taskExecutor = new TaskExecutor();

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

                String result = taskExecutor.execute(runningTask.get());

                Optional<Task> completedTask = repository.markTaskCompleted(
                    taskId,
                    getLocalName(),
                    result
                );

                if (completedTask.isEmpty()) {
                    System.out.println(getLocalName() + " could not complete task " + taskId
                        + ". It may no longer be running.");
                    return;
                }

                System.out.println(getLocalName() + " completed task:");
                System.out.println(completedTask.get());
            } catch (TaskExecutionException error) {
                System.err.println(getLocalName() + " could not execute task " + taskId
                    + ": " + error.getMessage());
                handleFailure(repository, taskId, error.getMessage());
            } catch (SQLException error) {
                System.err.println(getLocalName() + " failed while executing task " + taskId
                    + ": " + error.getMessage());
                handleFailure(repository, taskId, error.getMessage());
            }
        }

        private void handleFailure(TaskRepository repository, long taskId, String errorMessage) {
            try {
                Optional<Task> updatedTask = repository.handleTaskFailure(taskId, getLocalName(), errorMessage);
                if (updatedTask.isPresent()) {
                    if (updatedTask.get().getStatus() == TaskStatus.PENDING) {
                        System.out.println(getLocalName() + " scheduled task retry:");
                    } else {
                        System.out.println(getLocalName() + " marked task failed:");
                    }
                    System.out.println(updatedTask.get());
                }
            } catch (SQLException databaseError) {
                System.err.println(getLocalName() + " could not mark task " + taskId
                    + " failed: " + databaseError.getMessage());
            }
        }
    }
}
