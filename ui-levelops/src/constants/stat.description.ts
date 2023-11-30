export enum StatsMap {
  BOUNCES = "bounces",
  HOPS = "hops",
  RESOLUTION_TIME = "resolution_time",
  RESPONSE_TIME = "response_time",
  ESCALATION_TIME = "escalation_time",
  FILES_MODIFIED = "files_modified",
  LINES_MODIFIED = "lines_modified",
  SCM_RSEOLUTION_TIME = "scm_resolution_time",
  JIRA_RELEASE_TOTAL_TIME = "velocity_stage_total_time"
}

export const StatsDescription: { [x in StatsMap]: string } = {
  [StatsMap.RESOLUTION_TIME]:
    "Time taken to resolve a ticket after it was created.\n If a ticket is resolved multiple times then the time \n taken to resolve the ticket a last and final time is used.",
  [StatsMap.RESPONSE_TIME]:
    "Time taken to add the first comment \n (by someone other than the ticket \n creator/reporter) after the ticket was created.",
  [StatsMap.ESCALATION_TIME]: "Time taken to escalate a Support issue to Jira \n after the Support issue was created.",
  [StatsMap.BOUNCES]: "Number of times a ticket was reassigned \n to the same engineer.",
  [StatsMap.HOPS]: "Number of times a ticket was reassigned.",
  [StatsMap.LINES_MODIFIED]: "Total number of lines added, removed and changed",
  [StatsMap.FILES_MODIFIED]: "Total number of files added, removed and changed",
  [StatsMap.SCM_RSEOLUTION_TIME]: "Time taken to close the issue after it was created",
  [StatsMap.JIRA_RELEASE_TOTAL_TIME]: `The total time is determined by measuring the duration between the ticket creation and its release. 
  You can use the 'Lead time by Time spent in stages' report to further analyze the time spent in each stage of the workflow`
};
