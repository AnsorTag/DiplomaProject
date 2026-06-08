package com.diplomawork.agents.behaviours;

import com.diplomawork.agents.agent.CoordinatorAgent;

import jade.core.behaviours.TickerBehaviour;

/**
 * Periodic behaviour that asks the coordinator to schedule one pending task.
 */
public class CoordinatorSchedulingBehaviour extends TickerBehaviour {
    private final CoordinatorAgent coordinatorAgent;

    public CoordinatorSchedulingBehaviour(CoordinatorAgent coordinatorAgent, long period) {
        super(coordinatorAgent, period);
        this.coordinatorAgent = coordinatorAgent;
    }

    @Override
    protected void onTick() {
        coordinatorAgent.assignOnePendingTask();
    }
}
