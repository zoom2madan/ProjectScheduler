package com.akqa.planner.helper;

import java.util.*;

import com.akqa.planner.model.backlog.Backlog;
import com.akqa.planner.model.backlog.BacklogItem;
import com.akqa.planner.model.WorkStream;
import com.akqa.planner.util.FileUtils;

public class BacklogTestDataManager {


    public static Backlog prepareBacklog(String backlogFilePath){
        // Initialize the return type
        Backlog backlog = new Backlog();

        // Define source data
        List<List<String>> backlogData = FileUtils.readCSV(backlogFilePath);

        // Define target attributes
        List<BacklogItem> backlogItems = new ArrayList<>();
        BacklogItem backlogItem ;
        Map<WorkStream, Float> effortMap;

        // convert source data into target attributes
        for(List<String> backlogDataRow : backlogData) {
            backlogItem = new BacklogItem();

            // Set Epic
            backlogItem.setEpic(backlogDataRow.get(0));

            // Set Story
            backlogItem.setStory(backlogDataRow.get(1));

            // Set Effort Map
            effortMap = new HashMap();
            effortMap.put(WorkStream.BE, Float.parseFloat(backlogDataRow.get(2)));
            effortMap.put(WorkStream.FE, Float.parseFloat(backlogDataRow.get(3)));
            effortMap.put(WorkStream.QA, Float.parseFloat(backlogDataRow.get(4)));
            backlogItem.setEffortMap(effortMap);

            // Add backlog item to the list
            backlogItems.add(backlogItem);
        }
        // set the populated backlog item list on backlog
        backlog.setItems(backlogItems);

        return backlog;
    }


}
