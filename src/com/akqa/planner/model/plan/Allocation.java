package com.akqa.planner.model.plan;

public class Allocation {
    private Task task;
    private Float allocatedCapacity;

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Float getAllocatedCapacity() {
        return allocatedCapacity;
    }

    public void setAllocatedCapacity(Float allocatedCapacity) {
        this.allocatedCapacity = allocatedCapacity;
    }

    public String toLabel(){
        /*
         * Display a combination of task name and allocatedCapacity in hours
         * E.g. MTB-201:BE:2h
         */
        // TODO
        return null;
    }

}
