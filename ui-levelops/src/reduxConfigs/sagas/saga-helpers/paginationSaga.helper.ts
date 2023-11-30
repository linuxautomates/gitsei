import { SPRINT } from "dashboard/constants/applications/names";
import { forEach, get } from "lodash";
import { sprintJiraIssueRecordType } from "../saga-types/paginationSaga.types";
import { toTitleCase } from "utils/stringUtils";

export const deriveSprintJiraIssueKeysHelper = (
  jiraIssueRecords: sprintJiraIssueRecordType[],
  integrationIdsRecords: any[],
  sprintRecord: any[],
  isAzureReport = false
) => {
  const issueStoryPointMapping = get(
    sprintRecord,
    [isAzureReport ? "story_points_by_workitem" : "story_points_by_issue"],
    {}
  );
  const committedKeys: string[] = get(sprintRecord, ["committed_keys"], []);
  const creepKeys: string[] = get(sprintRecord, ["creep_keys"], []);
  const deliveredKeys: string[] = get(sprintRecord, ["delivered_keys"], []);
  return (jiraIssueRecords || []).map((issue: sprintJiraIssueRecordType) => {
    const id = (isAzureReport ? issue?.workitem_id : issue?.key) || "";
    let story_points = "-";
    const storyPointChangeObj = get(issueStoryPointMapping, id, {});
    if (Object.keys(storyPointChangeObj).length) {
      if (storyPointChangeObj.before !== storyPointChangeObj.after) {
        story_points = `${storyPointChangeObj.before} -> ${storyPointChangeObj.after}`;
      } else if (storyPointChangeObj.before !== 0) {
        story_points = storyPointChangeObj.before;
      }
    }
    const ticketCategory = committedKeys.includes(id) ? "Committed" : creepKeys.includes(id) ? "Creep" : "";
    const integs = integrationIdsRecords.filter(integ => issue.integration_id === integ.id);
    const integration_url = integs.length === 0 ? "" : integs[0].url;
    return {
      ...(issue || {}),
      story_points,
      integration_url,
      ticket_category: ticketCategory || toTitleCase(issue?.ticket_category || ""),
      resolved_in_sprint: deliveredKeys.includes(id) ? "Yes" : "No"
    };
  });
};
