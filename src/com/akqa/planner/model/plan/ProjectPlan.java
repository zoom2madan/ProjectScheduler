package com.akqa.planner.model.plan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ProjectPlan {
    /*
     * A 2D matrix with both row labels and column labels can be modeled as map of map of cell data.
     */
    private Map<TeamMember, TreeMap<LocalDate,ProjectPlanItem>> planItemMap;

    public Map<TeamMember, TreeMap<LocalDate, ProjectPlanItem>> getPlanItemMap() {
        return planItemMap;
    }

    public void setPlanItemMap(Map<TeamMember, TreeMap<LocalDate, ProjectPlanItem>> planItemMap) {
        this.planItemMap = planItemMap;
    }

    public void addAllocation(TeamMember member, LocalDate planDate, Allocation allocation){
        // The entire project plan object graph is assumed to have been already created
        // at the time of initialization of project plan with the staffing plan
        this.getPlanItemMap().get(member).get(planDate).getAllocations().add(allocation);
    }

    public List<List<String>> asTable(){
        // Declare the return value
        List<List<String>> table = new ArrayList<>();

        // Declare the iteration variables
        List<String> headerRow = null;
        List<String> dataRow = null;

        // Initialize the source
        Map<TeamMember, TreeMap<LocalDate,ProjectPlanItem>> planItemMap = this.planItemMap;

        // Prepare Header Row
        headerRow = new ArrayList<>();
        headerRow.add("Name");
        headerRow.add("WorkStream");
        TeamMember firstTeamMember = planItemMap.keySet().stream().findFirst().get();
        headerRow.addAll(
                planItemMap.get(firstTeamMember).keySet()
                        .stream()
                        .map( dt -> dt.toString())
                        .collect(Collectors.toList())
        );

        // Add the header row to the master table
        table.add(headerRow);

        // Prepare data rows
        for(TeamMember teamMember : planItemMap.keySet()) {
            dataRow = new ArrayList<>();
            dataRow.add(teamMember.getName());
            dataRow.add(teamMember.getWorkStream().toString());
            dataRow.addAll(
                    planItemMap.get(teamMember).values()
                            .stream()
                            .map( ppItem -> ppItem.asCell())
                            .collect(Collectors.toList())
            );
            // Add the data row to the master table
            table.add(dataRow);
        }
        return table;
    }
}
