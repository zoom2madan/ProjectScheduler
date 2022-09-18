package com.akqa.planner.model.plan;

import java.util.List;

public class ProjectPlanItem {
   private Float availableCapacity;
   private List<Allocation> allocations;

    public Float getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Float availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public List<Allocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<Allocation> allocations) {
        this.allocations = allocations;
    }

    public String asCell(){
        /*
         * In a Non-Allocated state, it would display availableCapacity
         * In an Allocated state, it would display collated labels from allocations
         * Eg. MTB-201:BE:2h | MTB-202:BE:4h | Free:2h
         * OR
         * If the  availableCapacity is zero, then Unavailable
         */

        // Declare the output variable
        String label = "";
        String taskLabel = null;

        if(this.availableCapacity == 0.0f) {
            label = "Unavailable";
        } else {
            for(Allocation allocation : this.allocations) {
                taskLabel = "" + allocation.getTask().getName() + ":" + allocation.getAllocatedCapacity();
                label = label + taskLabel + " | ";
            }
            float totalAllocation = (float)this.allocations
                    .stream()
                    .mapToDouble( a -> a.getAllocatedCapacity())
                    .sum();
            if(totalAllocation < 1.0f ) {
                label = label + "Free:" + (1.0f - totalAllocation);
            }
        }

        return label;
    }
}
