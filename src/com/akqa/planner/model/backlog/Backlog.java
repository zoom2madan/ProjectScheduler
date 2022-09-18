package com.akqa.planner.model.backlog;

import java.util.List;

import com.akqa.planner.model.WorkStream;

public class Backlog {
    private List<BacklogItem> items;

    public List<BacklogItem> getItems() {
        return items;
    }

    public void setItems(List<BacklogItem> items) {
        this.items = items;
    }
}
