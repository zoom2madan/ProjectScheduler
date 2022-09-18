# Project Scheduler
This application takes a Backlog of Tasks, and a Staffing Plan as input, 
and creates a Project Plan.

## Planning Algorithm
``````
plan {
    ...
    planStory {
        ...
        createTaskListForStory {
            ...
            orderTaskListByDependencyGraph
        }
        getDependenciesForTask
        planTask {
            ...
            identifyTeamForWorkStream
            allocateTaskToWorkStreamTeam {
                ...
                getTaskStartEndDateForMember {
                    ...
                    getPlannedEndDateOfDependencies
                    computeTaskEndDateForMemberAsPerPlan
                }
                allocateTaskToTeamMember {
                    ..
                }
            }
        }    
    }
}
```

allocateTaskToTeamMember and computeTaskEndDateForMemberAsPerPlan have 
very similar logic and can be combined together in a single function.