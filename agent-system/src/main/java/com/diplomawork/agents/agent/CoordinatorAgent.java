package com.diplomawork.agents.agent;

import com.diplomawork.agents.behaviours.CoordinatorSchedulingBehaviour;
import com.diplomawork.agents.db.TaskRepository;
import com.diplomawork.agents.model.Task;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * JADE coordinator agent.
 *
 * It periodically assigns the highest-priority pending task to the least-loaded
 * configured executor agent and sends a JADE message with the task id.
 */
public class CoordinatorAgent extends Agent {
    private static final int DEFAULT_TASK_LIMIT = 10;
    private static final int DEFAULT_STALE_TASK_MINUTES = 30;
    private static final long SCHEDULING_INTERVAL_MILLIS = 2000L;
    private static final String DEFAULT_EXECUTOR_AGENT_NAME = "executor";
    private static final String STALE_TASK_MINUTES_ENV = "STALE_TASK_MINUTES";

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " started.");
        System.out.println(getLocalName() + " scheduling interval ms: " + SCHEDULING_INTERVAL_MILLIS);
        recoverStaleTasks();
        assignOnePendingTask();
        addBehaviour(new CoordinatorSchedulingBehaviour(this, SCHEDULING_INTERVAL_MILLIS));
    }

    public void assignOnePendingTask() {
        TaskRepository repository = new TaskRepository();
        try {
            List<String> executorNames = configuredExecutorNames();
            System.out.println(getLocalName() + " configured executors: " + executorNames);

            List<Task> tasks = repository.findPendingTasks(DEFAULT_TASK_LIMIT);
            System.out.println(getLocalName() + " found pending tasks: " + tasks.size());

            if (tasks.isEmpty()) {
                System.out.println(getLocalName() + " has no pending task to assign.");
                return;
            }

            String selectedExecutor = selectLeastLoadedExecutor(repository, executorNames);
            Task selectedTask = tasks.get(0);
            Optional<Task> assignedTask = repository.assignPendingTask(
                selectedTask.getId(),
                selectedExecutor
            );

            if (assignedTask.isEmpty()) {
                System.out.println(getLocalName() + " could not assign task " + selectedTask.getId()
                    + ". It may have been assigned by another agent.");
                return;
            }

            System.out.println(getLocalName() + " assigned task:");
            System.out.println(assignedTask.get());
            sendAssignmentMessage(assignedTask.get(), selectedExecutor);
        } catch (SQLException error) {
            System.err.println(getLocalName() + " failed during scheduling: " + error.getMessage());
            error.printStackTrace(System.err);
        }
    }

    private void recoverStaleTasks() {
        int staleMinutes = configuredStaleTaskMinutes();
        if (staleMinutes == 0) {
            System.out.println(getLocalName() + " stale task recovery is disabled.");
            return;
        }

        try {
            int resolvedTasks = new TaskRepository().resolveStaleTasks(staleMinutes);
            System.out.println(getLocalName() + " resolved stale tasks: " + resolvedTasks
                + " using threshold minutes: " + staleMinutes);
        } catch (SQLException error) {
            System.err.println(getLocalName() + " failed during stale task recovery: " + error.getMessage());
            error.printStackTrace(System.err);
        }
    }

    private int configuredStaleTaskMinutes() {
        String configuredValue = System.getenv(STALE_TASK_MINUTES_ENV);
        if (configuredValue == null || configuredValue.isBlank()) {
            return DEFAULT_STALE_TASK_MINUTES;
        }

        try {
            int value = Integer.parseInt(configuredValue);
            if (value < 0) {
                throw new NumberFormatException("value must not be negative");
            }
            return value;
        } catch (NumberFormatException error) {
            System.err.println(getLocalName() + " invalid " + STALE_TASK_MINUTES_ENV
                + " value '" + configuredValue + "'; using " + DEFAULT_STALE_TASK_MINUTES + ".");
            return DEFAULT_STALE_TASK_MINUTES;
        }
    }

    private List<String> configuredExecutorNames() {
        Object[] arguments = getArguments();
        if (arguments == null || arguments.length == 0) {
            return List.of(DEFAULT_EXECUTOR_AGENT_NAME);
        }

        List<String> executorNames = new ArrayList<>();
        Arrays.stream(arguments)
            .map(String::valueOf)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .forEach(executorNames::add);

        if (executorNames.isEmpty()) {
            return List.of(DEFAULT_EXECUTOR_AGENT_NAME);
        }
        return executorNames;
    }

    private String selectLeastLoadedExecutor(
        TaskRepository repository,
        List<String> executorNames
    ) throws SQLException {
        String selectedExecutor = executorNames.get(0);
        int selectedLoad = repository.countActiveTasksForAgent(selectedExecutor);

        for (String executorName : executorNames) {
            int load = repository.countActiveTasksForAgent(executorName);
            System.out.println(getLocalName() + " observed executor load: "
                + executorName + "=" + load);
            if (load < selectedLoad) {
                selectedExecutor = executorName;
                selectedLoad = load;
            }
        }

        System.out.println(getLocalName() + " selected executor: " + selectedExecutor);
        return selectedExecutor;
    }

    private void sendAssignmentMessage(Task task, String executorName) {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID(executorName, AID.ISLOCALNAME));
        message.setConversationId("task-assignment");
        message.setContent(Long.toString(task.getId()));
        send(message);
        System.out.println(getLocalName() + " sent task " + task.getId()
            + " to " + executorName + ".");
    }
}
