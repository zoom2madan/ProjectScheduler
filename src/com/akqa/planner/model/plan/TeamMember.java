package com.akqa.planner.model.plan;

import com.akqa.planner.model.WorkStream;

public class TeamMember {
    private String name;
    private WorkStream workStream;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkStream getWorkStream() {
        return workStream;
    }

    public void setWorkStream(WorkStream workStream) {
        this.workStream = workStream;
    }
}
