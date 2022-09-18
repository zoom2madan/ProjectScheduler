package com.akqa.planner.model.plan;

import com.akqa.planner.model.WorkStream;

public class Task {
    private String name;
    private String storyName;
    private String epicName;
    private WorkStream workStream;
    private Float effort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStoryName() {
        return storyName;
    }

    public void setStoryName(String storyName) {
        this.storyName = storyName;
    }

    public String getEpicName() {
        return epicName;
    }

    public void setEpicName(String epicName) {
        this.epicName = epicName;
    }

    public WorkStream getWorkStream() {
        return workStream;
    }

    public void setWorkStream(WorkStream workStream) {
        this.workStream = workStream;
    }

    public Float getEffort() {
        return effort;
    }

    public void setEffort(Float effort) {
        this.effort = effort;
    }
}
