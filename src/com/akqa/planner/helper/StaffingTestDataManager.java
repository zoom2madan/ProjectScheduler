package com.akqa.planner.helper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.akqa.planner.model.WorkStream;
import com.akqa.planner.model.plan.ProjectPlan;
import com.akqa.planner.model.plan.ProjectPlanItem;
import com.akqa.planner.model.plan.TeamMember;
import com.akqa.planner.util.FileUtils;

public class StaffingTestDataManager {

    public static ProjectPlan prepareStaffingPlan(String staffingFilePath){
        // Initialize return value
        ProjectPlan staffingPlan = new ProjectPlan();

        // Source Data
        List<List<String>> staffingData = FileUtils.readCSV(staffingFilePath);

        // Declare target attributes
        Map<TeamMember, TreeMap<LocalDate, ProjectPlanItem>> planItemMap = new HashMap<>();

        // Extract the first row of staffing data to create header information
        List<String> headerRow = staffingData.get(0);
        List<LocalDate> planDateList = preparePlanDateList(headerRow);

        // Iterate over rest of the data and populate the project plan for staffing information
        for(List<String> staffingDataRow : staffingData.subList(1,staffingData.size())) {
            processStaffingDataRow(planDateList, staffingDataRow, planItemMap);
        }

        // The planItemMap has been populated at this stage. It can be set on the return attribute
        staffingPlan.setPlanItemMap(planItemMap);

        return staffingPlan;
    }

    public static List<LocalDate>  preparePlanDateList(List<String> headerRow){
        return headerRow.subList(2, headerRow.size())
                .stream()
                .map( LocalDate::parse )
                .collect( Collectors.toList() );
    }

    public static void processStaffingDataRow(List<LocalDate> planDateList,
                                              List<String> staffingDataRow,
                                              Map<TeamMember, TreeMap<LocalDate,ProjectPlanItem>> planItemMap)
    {
        // We have to add an entry in planItemMap based on staffingDataRow and planDateList

        // 1. Create the key
        TeamMember teamMember = new TeamMember();
        teamMember.setName(staffingDataRow.get(0));
        teamMember.setWorkStream(WorkStream.valueOf(staffingDataRow.get(1)));

        // 2. Create the value
        TreeMap<LocalDate,ProjectPlanItem> planItemNestedMap = new TreeMap<>();

        // Declare the target attributes
        ProjectPlanItem projectPlanItem;

        // Keep a counter for data corresponding to a date in the staffingDataRow list
        // It starts with 2 because first 2 values are team member name and their workstream
        int capacityForGivenDateIndex = 2;

        // Iterate over planDateList. These become the keys
        for(LocalDate planDate : planDateList) {
            // 2.1 Key is the plan date

            // 2.2 Create the value for planItemNestedMap
            projectPlanItem = new ProjectPlanItem();
            projectPlanItem.setAvailableCapacity(Float.parseFloat(staffingDataRow.get(capacityForGivenDateIndex)));
            projectPlanItem.setAllocations(new ArrayList<>());
            capacityForGivenDateIndex++;

            // 2.3 Set the key and value on the target attr
            planItemNestedMap.put(planDate, projectPlanItem);
        }

        // 3. Set the key and value in the return attribute
        planItemMap.put(teamMember, planItemNestedMap);

        // There is no return. the values are passed by reference.
    }
}
