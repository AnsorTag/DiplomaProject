package com.diplomawork.agents.agent;

import com.diplomawork.agents.behaviours.TaskAssignmentReceiverBehaviour;
import com.diplomawork.agents.db.TaskRepository;
import com.diplomawork.agents.execution.TaskExecutionException;
import com.diplomawork.agents.execution.TaskExecutor;
import com.diplomawork.agents.model.Task;
import com.diplomawork.agents.model.TaskStatus;

import jade.core.Agent;

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
        addBehaviour(new TaskAssignmentReceiverBehaviour(this));
    }

    public void executeTask(long taskId) {
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
