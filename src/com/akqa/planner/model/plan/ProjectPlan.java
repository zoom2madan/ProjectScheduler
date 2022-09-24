package com.akqa.planner.model.plan;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.akqa.planner.dto.TaskDTO;

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

    public List<List<String>> asTaskCalendar(){
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

    public List<List<String>> asTaskList(){
        // Declare the output attribute
        List<List<String>> taskList = new ArrayList<>();

        // Initialize the lookup table that will be used to summarize the data
        Map<TaskDTO, LocalDate[]> taskAllocationSummary = new HashMap<>();
        TaskDTO taskDTO = null;

        // Initialize the constituents of output data
        // 1.
        List<String> headerRow = Arrays.asList(new String[]{
                "Epic", "Story", "Task", "Task Effort", "Allocated To", "Start Date", "End Date"
        });
        taskList.add(headerRow);

        // 2.
        List<String> summarizedDataRow = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM");

        // Declare the iteration variables
        TreeMap<LocalDate,ProjectPlanItem> memberRow = null;
        ProjectPlanItem projectPlanItem = null;
        LocalDate startDate = null;
        LocalDate endDate = null;

        // Initialize the source
        Map<TeamMember, TreeMap<LocalDate,ProjectPlanItem>> planItemMap = this.planItemMap;

        // Iterate over source data and prepare interim / output data
        for(TeamMember member : planItemMap.keySet()) {
            memberRow = planItemMap.get(member);
            for(LocalDate planDate : memberRow.keySet()) {
                projectPlanItem = memberRow.get(planDate);
                for (Allocation allocation : projectPlanItem.getAllocations()) {
                    taskDTO = new TaskDTO();
                    taskDTO.setEpic(allocation.getTask().getEpicName());
                    taskDTO.setStory(allocation.getTask().getStoryName());
                    taskDTO.setTask(allocation.getTask().getName());
                    taskDTO.setTaskEffort(allocation.getTask().getEffort());
                    taskDTO.setAllocatedTo(member.getName());

                    // Check if the taskDTO has already been seen
                    if(taskAllocationSummary.get(taskDTO) != null){
                        // The task is already in the map
                        startDate = taskAllocationSummary.get(taskDTO)[0];
                        endDate = taskAllocationSummary.get(taskDTO)[1];
                        if(planDate.compareTo(startDate) < 0) {
                            startDate = planDate;
                        }
                        if(planDate.compareTo(endDate) > 0) {
                            endDate = planDate;
                        }
                    } else {
                        // This task is being added for the first time
                        startDate = planDate;
                        endDate = planDate;
                    }
                    // In both IF or ELSE case
                    taskAllocationSummary.put(taskDTO, new LocalDate[]{startDate, endDate});
                }
            }
        }
        // By this point, all the records in the source data have been traversed
        // and summarized in the map

        // Convert the map into the output list
        for(TaskDTO task : taskAllocationSummary.keySet()) {
            summarizedDataRow = new ArrayList<>();
            summarizedDataRow.add(task.getEpic());
            summarizedDataRow.add(task.getStory());
            summarizedDataRow.add(task.getTask());
            summarizedDataRow.add(task.getTaskEffort().toString());
            summarizedDataRow.add(task.getAllocatedTo());
            summarizedDataRow.add(taskAllocationSummary.get(task)[0].format(formatter));
            summarizedDataRow.add(taskAllocationSummary.get(task)[1].format(formatter));
            taskList.add(summarizedDataRow);
        }

        // The tasklist is now fully populated
        return taskList;
    }
}
