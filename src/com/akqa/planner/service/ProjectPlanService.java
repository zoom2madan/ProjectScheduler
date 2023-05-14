package com.akqa.planner.service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import com.akqa.planner.model.WorkStream;
import com.akqa.planner.model.backlog.Backlog;
import com.akqa.planner.model.backlog.BacklogItem;
import com.akqa.planner.model.plan.*;

public class ProjectPlanService {

    public ProjectPlan plan(Backlog backlog,
                            ProjectPlan staffingPlan,
                            Map<WorkStream, List<WorkStream>> workStreamDependencyGraph)
    {
        // Declare and initialize the return attribute
        ProjectPlan projectPlan = staffingPlan;

        for(BacklogItem backlogItem: backlog.getItems()){
            planStory(backlogItem, projectPlan, workStreamDependencyGraph);
        }
        return projectPlan;
    }

    private void planStory(BacklogItem backlogItem, ProjectPlan projectPlan,
                           Map<WorkStream, List<WorkStream>> workStreamDependencyGraph)
    {
        List<Task> taskList = createTaskListForStory(backlogItem, workStreamDependencyGraph);
        List<Task> dependencies = null;

        for(Task task : taskList) {
            try {
                dependencies = getDependenciesForTask(task, taskList, workStreamDependencyGraph);
                planTask(task, projectPlan, dependencies);
            } catch(UnableToPlanTaskException ex) {
                // Log the error
                System.err.println("Could not plan task: " + ex.getMessage());
                // Continue planning subsequent tasks
            }
        }
    }

    private List<Task> createTaskListForStory(BacklogItem backlogItem,
                                              Map<WorkStream, List<WorkStream>> workStreamDependencyGraph)
    {
        // Declare and initialize the return attribute
        List<Task> orderedTaskList = null;
        List<Task> taskList = new ArrayList<>();
        Task task;
        for (WorkStream workStream : backlogItem.getEffortMap().keySet()) {
            // create the target attr
            task = new Task();
            task.setWorkStream(workStream);
            task.setEffort(backlogItem.getEffortMap().get(workStream));
            task.setEpicName(backlogItem.getEpic());
            task.setStoryName(backlogItem.getStory());
            task.setName(backlogItem.getEpic() + " : " + backlogItem.getStory() + " : " + workStream.name());

            // add to the list
            if(task.getEffort() > 0.0f) {
                taskList.add(task);
            }
        }

        // We have to identify the depth of workstream in the graph, and then use that depth to sort the tasklist.
        // Delegating to a separate method.
        orderedTaskList = orderTaskListByDependencyGraph(taskList, workStreamDependencyGraph);
        return orderedTaskList;
    }

    private List<Task> orderTaskListByDependencyGraph(List<Task> taskList,
                                                      Map<WorkStream, List<WorkStream>> workStreamDependencyGraph)
    {
        // Declare the output
        List<Task> orderedTaskList = new ArrayList<>(taskList);

        // Initialize map which tracks depth of each node
        Map<WorkStream, Integer> workStreamDepthInGraph = new HashMap<>();

        // Prepare a collection of source data for iteration
        // We are transforming the collection because we want to add objects to the coll during iteration
        // and avoid Concurrent Modification Exception
        Collection<WorkStream> queue = new ConcurrentLinkedQueue();
        queue.addAll(workStreamDependencyGraph.keySet());

        // Object representing individual item of the iteration
        WorkStream workStream = null;

        // Sub object of the current object
        List<WorkStream> dependencies = null;

        Iterator<WorkStream> queueIterator = queue.iterator();
        while(queueIterator.hasNext()){
            workStream = queueIterator.next();
            dependencies = workStreamDependencyGraph.get(workStream);
            if(dependencies.size() == 0){
                // This is the starting node in the graph
                workStreamDepthInGraph.put(workStream, 1);
            } else {
                // Check if the depth of all dependencies is available in workStreamDepthInGraph
                // If yes, compute the depth of current workstream, otherwise park in queue
                boolean wsInDepthGraph = true;
                for(WorkStream dependencyWS : dependencies) {
                    if(workStreamDepthInGraph.get(dependencyWS) == null) {
                        wsInDepthGraph = false;
                    }
                }
                if(wsInDepthGraph) {
                    // Identify the maximum value in the map
                    Optional<Map.Entry<WorkStream, Integer>> maxEntry = workStreamDepthInGraph.entrySet()
                            .stream()
                            .max((Map.Entry<WorkStream, Integer> e1, Map.Entry<WorkStream, Integer> e2) -> e1.getValue()
                                    .compareTo(e2.getValue())
                            );
                    // Set that value + 1 as the depth of the current node in the map
                    workStreamDepthInGraph.put(workStream, maxEntry.get().getValue() + 1);
                } else {
                    // The depth of the current node cannot be assessed, as we dont yet know the depth
                    // of all its dependencies
                    // Park this workstream node in the queue for a while
                    queue.add(workStream);
                }
            }
        }

        // Finally by this point, all the workstreams should have a depth in the map workStreamDepthInGraph
        // Use this information to order the task list

        Collections.sort(orderedTaskList, new Comparator<Task>() {
            public int compare(Task left, Task right) {
                return Integer.compare(
                        workStreamDepthInGraph.get(left.getWorkStream()),
                        workStreamDepthInGraph.get(right.getWorkStream())
                );
            }
        });
        return orderedTaskList;
    }

    private void planTask(Task task, ProjectPlan projectPlan, List<Task> dependencies)
            throws UnableToPlanTaskException
    {
        List<TeamMember> team = identifyTeamForWorkStream(task.getWorkStream(), projectPlan);

        allocateTaskToWorkStreamTeam(team, task, projectPlan, dependencies);
    }

    private List<TeamMember> identifyTeamForWorkStream(WorkStream workStream, ProjectPlan projectPlan)
    {
        // Declare and initialize the return attr
        List<TeamMember> team = projectPlan.getPlanItemMap()
                .keySet()
                .stream()
                .filter(tm -> tm.getWorkStream().equals(workStream))
                .collect(Collectors.toList());

        return team;
    }

    private void allocateTaskToWorkStreamTeam(List<TeamMember> team, Task task, ProjectPlan projectPlan,
                                              List<Task> dependencies)
            throws UnableToPlanTaskException
    {
        /*
         * Rules of allocation
         * 1. Start as soon as a team member is available
         * 2. Don't start until the dependency tasks are complete
         * 3. Select a team member who can finish the task (i.e. their remaining capacity > task effort)
         * 4. Select a team member who can finish the task at the earliest
         *
         */

        TeamMember selectedTeamMember = null;
        LocalDate selectedMemberEndDate = null;

        LocalDate[] startEndDates = null;
        LocalDate earliestStartDate = null;
        LocalDate memberTaskEndDate = null;

        for(TeamMember member : team) {
            startEndDates = getTaskStartEndDateForMember(task, member, projectPlan, dependencies);

            earliestStartDate = startEndDates[0];
            memberTaskEndDate = startEndDates[1];

            if(memberTaskEndDate != null) {
                // This means that the current member can finish the task, and by the given date
                if(selectedTeamMember == null && selectedMemberEndDate == null) {
                    // This is the first member who can do it
                    selectedTeamMember = member;
                    selectedMemberEndDate = memberTaskEndDate;
                } else if(memberTaskEndDate.compareTo(selectedMemberEndDate) < 0){
                    // We have found a local minima
                    // The selected member can finish this task by selectedMemberEndDate
                    selectedTeamMember = member;
                    selectedMemberEndDate = memberTaskEndDate;
                } else {
                    // Either the current iteration date is same as previous minima
                    // or it is greater than previous minima
                    // In both cases, we can ignore the current iteration values
                }
            }
        }

        if(selectedTeamMember != null) {
            allocateTaskToTeamMember(task, selectedTeamMember, projectPlan, earliestStartDate);
        } else {
            // We did not find a team member who could finish this task.
            // Raise UnableToPlanTaskException
            throw new UnableToPlanTaskException(task.getEpicName() + ":" + task.getStoryName() + ":" + task.getWorkStream());
        }

    }

    private void allocateTaskToTeamMember(Task task, TeamMember member,
                                          ProjectPlan projectPlan, LocalDate earliestStartDateOfTask)
    {
        boolean taskNotCompleted = true;

        Map<LocalDate,ProjectPlanItem> planRow = projectPlan.getPlanItemMap().get(member);
        Iterator<LocalDate> planDateIterator = planRow.keySet().iterator();
        LocalDate planDate = null;

        // A
        Float availableCapacityForDay = 0.0f;
        // B
        Float capacityAllocatedToOtherTasksForDay = 0.0f;
        // C = A-B
        Float netAvailableCapacityForDay = availableCapacityForDay - capacityAllocatedToOtherTasksForDay;

        // X
        Float totalCapacityNeededForThisTask = task.getEffort();
        // Y
        Float totalCapacityPlannedForThisTask = 0.0f;
        // Z = X - Y
        Float pendingCapacityToBePlannedForThisTask = totalCapacityNeededForThisTask - totalCapacityPlannedForThisTask;

        Allocation allocation = null;

        while(taskNotCompleted & planDateIterator.hasNext()){
            planDate = planDateIterator.next();

            if(planDate.compareTo(earliestStartDateOfTask) >= 0) {
                // After the earliestStartDateOfTask,
                // Keep burning capacity and allocating tasks, until the task will get completed

                availableCapacityForDay = planRow.get(planDate).getAvailableCapacity();
                capacityAllocatedToOtherTasksForDay = (float)planRow.get(planDate).getAllocations()
                        .stream().mapToDouble( a -> a.getAllocatedCapacity()).sum();

                netAvailableCapacityForDay = availableCapacityForDay - capacityAllocatedToOtherTasksForDay;


                if(netAvailableCapacityForDay > 0.0f) {
                    // Create a new task allocation
                    allocation = new Allocation();
                    allocation.setTask(task);

                    // Identify the capacity to be burned
                    if (pendingCapacityToBePlannedForThisTask > netAvailableCapacityForDay) {
                        // set allocation for netAvailableCapacityForDay
                        allocation.setAllocatedCapacity(netAvailableCapacityForDay);
                    } else {
                        // set allocation for requiredCapacityToBeBurnedForThisTask
                        allocation.setAllocatedCapacity(pendingCapacityToBePlannedForThisTask);
                    }

                    // Add allocation to plan
                    projectPlan.addAllocation(member, planDate, allocation);

                    // Track the burned capacity
                    totalCapacityPlannedForThisTask += allocation.getAllocatedCapacity();

                    // Recompute the pending capacity
                    pendingCapacityToBePlannedForThisTask = totalCapacityNeededForThisTask - totalCapacityPlannedForThisTask;

                    // If the pending capacity for this task is zero, then the task is complete
                    if(pendingCapacityToBePlannedForThisTask == 0.0f) {
                        // The task can be fully delivered by this date
                        taskNotCompleted = false;
                    }
                }
            } else {
                // Skip this date. Do nothing
            }
        }

    }

    private LocalDate[] getTaskStartEndDateForMember(Task task, TeamMember member, ProjectPlan projectPlan,
                                                     List<Task> dependencies)
    {
        /*
         * We have to compute the following things
         * 1. When is the earliest date when the dependencies of this task are getting completed?
         * 2. After that date, when the earliest that this team member can pick up this task
         * 3. Can the team member finish this task if they pick up this task
         * 4. When would they finish this task?
         */

        LocalDate lastDateOfDependencies = getPlannedEndDateOfDependencies(dependencies, projectPlan);
        LocalDate earliestStartDateOfTask = lastDateOfDependencies.plusDays(1);
        LocalDate taskEndDateForMember = computeTaskEndDateForMemberAsPerPlan(task, member, projectPlan, earliestStartDateOfTask);

        return new LocalDate[]{earliestStartDateOfTask,taskEndDateForMember};
    }

    private List<Task> getDependenciesForTask(Task task, List<Task> taskList,
                                              Map<WorkStream, List<WorkStream>> workStreamDependencyGraph)
    {
        // Declare the return variable
        List<Task> dependenciesForTask = new ArrayList<>();

        List<WorkStream> dependencyWorkstreams = workStreamDependencyGraph.get(task.getWorkStream());
        for (WorkStream workStream : dependencyWorkstreams) {
            dependenciesForTask.addAll(taskList
                    .stream()
                    .filter( t -> t.getWorkStream().equals(workStream))
                    .collect(Collectors.toList())
            );
        }

        return dependenciesForTask;
    }

    private LocalDate getPlannedEndDateOfDependencies(List<Task> dependencies, ProjectPlan projectPlan){
        // Declare the return variable

        // Initialize it to the first team member's first date in the plan - 1 day
        TeamMember firstTeamMember = projectPlan.getPlanItemMap().keySet().stream().findFirst().get();
        LocalDate maxDependencyTaskEndDate = projectPlan.getPlanItemMap().get(firstTeamMember).firstKey().minusDays(1);

        // Validate input - dependencies
        if(dependencies == null || dependencies.size() == 0) {
            return maxDependencyTaskEndDate;
        }

        LocalDate planDate = null;
        ProjectPlanItem projectPlanItem = null;
        for (Map<LocalDate, ProjectPlanItem> planItemMapRow : projectPlan.getPlanItemMap().values()) {

            for(Map.Entry<LocalDate, ProjectPlanItem> planItemCell : planItemMapRow.entrySet()) {
                planDate = planItemCell.getKey();
                projectPlanItem = planItemCell.getValue();

                if(projectPlanItem != null && projectPlanItem.getAllocations() != null) {
                    for (Allocation allocation : projectPlanItem.getAllocations()) {
                        if (dependencies.contains(allocation.getTask())) {
                            if (maxDependencyTaskEndDate.compareTo(planDate) < 0) {
                                maxDependencyTaskEndDate = planDate;
                            }
                        }
                    }
                }
            }
        }

        return maxDependencyTaskEndDate;
    }

    private LocalDate computeTaskEndDateForMemberAsPerPlan(Task task, TeamMember member,
                                                           ProjectPlan projectPlan, LocalDate earliestStartDateOfTask)
    {
        // Declare return variable
        LocalDate taskEndDate = null;
        boolean taskNotCompleted = true;

        Map<LocalDate,ProjectPlanItem> planRow = projectPlan.getPlanItemMap().get(member);
        Iterator<LocalDate> planDateIterator = planRow.keySet().iterator();
        LocalDate planDate = null;

        Float availableCapacityForDay = 0.0f;
        Float capacityAllocatedToOtherTasksForDay = 0.0f;
        Float netAvailableCapacityForDay = availableCapacityForDay - capacityAllocatedToOtherTasksForDay;

        // X
        Float totalCapacityNeededForThisTask = task.getEffort();
        // Y
        Float totalCapacityPlannedForThisTask = 0.0f;
        // Z = X - Y
        Float pendingCapacityToBePlannedForThisTask = totalCapacityNeededForThisTask - totalCapacityPlannedForThisTask;

        while(taskNotCompleted & planDateIterator.hasNext()){
            planDate = planDateIterator.next();
            if(planDate.compareTo(earliestStartDateOfTask) >= 0) {
                // Start counting how many dates will be need to burn the required vs available capacity
                availableCapacityForDay = planRow.get(planDate).getAvailableCapacity();

                capacityAllocatedToOtherTasksForDay = (float) planRow.get(planDate).getAllocations()
                        .stream()
                        .mapToDouble( a -> a.getAllocatedCapacity())
                        .sum();

                netAvailableCapacityForDay = availableCapacityForDay - capacityAllocatedToOtherTasksForDay;

                if(netAvailableCapacityForDay > 0.0f) {
                    // Burn the required capacity

                    // Identify the capacity to be burned
                    if (pendingCapacityToBePlannedForThisTask > netAvailableCapacityForDay) {
                        // set allocation for netAvailableCapacityForDay
                        totalCapacityPlannedForThisTask += netAvailableCapacityForDay;
                    } else {
                        // set allocation for requiredCapacityToBeBurnedForThisTask
                        totalCapacityPlannedForThisTask += pendingCapacityToBePlannedForThisTask;
                    }

                    // Recompute the pending capacity
                    pendingCapacityToBePlannedForThisTask = totalCapacityNeededForThisTask - totalCapacityPlannedForThisTask;

                    // If the pending capacity for this task is zero, then the task is complete
                    if(pendingCapacityToBePlannedForThisTask == 0.0f) {
                        // The task can be fully delivered by this date
                        taskEndDate = planDate;
                        taskNotCompleted = false;
                    }

                }
            } else {
                //Skip the date. Do nothing
            }
        }

        return taskEndDate;
    }

}