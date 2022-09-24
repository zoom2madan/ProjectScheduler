package com.akqa.planner.dto;

import java.util.Objects;

public class TaskDTO {

    private String epic;
    private String story;
    private String task;
    private Float taskEffort;
    private String allocatedTo;

    public String getEpic() {
        return epic;
    }

    public void setEpic(String epic) {
        this.epic = epic;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Float getTaskEffort() {
        return taskEffort;
    }

    public void setTaskEffort(Float taskEffort) {
        this.taskEffort = taskEffort;
    }

    public String getAllocatedTo() {
        return allocatedTo;
    }

    public void setAllocatedTo(String allocatedTo) {
        this.allocatedTo = allocatedTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDTO taskDTO = (TaskDTO) o;
        return epic.equals(taskDTO.epic) &&
                story.equals(taskDTO.story) &&
                task.equals(taskDTO.task) &&
                taskEffort.equals(taskDTO.taskEffort) &&
                allocatedTo.equals(taskDTO.allocatedTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(epic, story, task, taskEffort, allocatedTo);
    }
}
