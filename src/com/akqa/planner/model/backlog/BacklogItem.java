package com.akqa.planner.model.backlog;

import java.util.Map;

import com.akqa.planner.model.WorkStream;

public class BacklogItem {
    private String epic;
    private String story;
    private Map<WorkStream, Float> effortMap;

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

    public Map<WorkStream, Float> getEffortMap() {
        return effortMap;
    }

    public void setEffortMap(Map<WorkStream, Float> effortMap) {
        this.effortMap = effortMap;
    }
}
