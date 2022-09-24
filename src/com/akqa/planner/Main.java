package com.akqa.planner;

import java.util.*;

import com.akqa.planner.model.backlog.Backlog;
import com.akqa.planner.model.plan.ProjectPlan;
import com.akqa.planner.service.ProjectPlanService;
import com.akqa.planner.model.WorkStream;
import com.akqa.planner.helper.BacklogTestDataManager;
import com.akqa.planner.helper.StaffingTestDataManager;
import com.akqa.planner.util.FileUtils;

public class Main {

    private static final String BACKLOG_FILE_PATH = "testdata/backlog.csv";
    private static final String STAFFING_FILE_PATH = "testdata/staffing.csv";
    private static final String PROJECT_PLAN_TASK_CALENDAR_FILE_PATH = "testdata/project-plan-calendar.csv";
    private static final String PROJECT_PLAN_TASK_VIEW_FILE_PATH = "testdata/project-plan-tasklist.csv";

    private static Map<WorkStream, List<WorkStream>> workStreamDependencyGraph;

    static {
        // This approach to creating graphs is called Adjacency List
        Map<WorkStream, List<WorkStream>> graph = new HashMap<>();
        WorkStream[] beDependencies = new WorkStream[]{};
        WorkStream[] feDependencies = new WorkStream[]{WorkStream.BE};
        WorkStream[] qaDependencies = new WorkStream[]{WorkStream.BE, WorkStream.FE};
        graph.put(WorkStream.BE, Arrays.asList(beDependencies));
        graph.put(WorkStream.FE, Arrays.asList(feDependencies));
        graph.put(WorkStream.QA, Arrays.asList(qaDependencies));
        workStreamDependencyGraph = Collections.unmodifiableMap(graph);
    }

    public static void main(String[] args) {
        // Create backlog object model
        Backlog backlog = BacklogTestDataManager.prepareBacklog(BACKLOG_FILE_PATH);

        // Create staffing plan object model
        ProjectPlan staffingPlan = StaffingTestDataManager.prepareStaffingPlan(STAFFING_FILE_PATH);

        // Initialize the project plan service
        ProjectPlanService projectPlanService = new ProjectPlanService();

        // Prepare the staffing plan
        projectPlanService.plan(backlog, staffingPlan, workStreamDependencyGraph);

        FileUtils.writeCSV(staffingPlan.asTaskCalendar(), PROJECT_PLAN_TASK_CALENDAR_FILE_PATH);

        FileUtils.writeCSV(staffingPlan.asTaskList(), PROJECT_PLAN_TASK_VIEW_FILE_PATH);
    }
}
