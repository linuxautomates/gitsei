import { baseColumnConfig } from "utils/base-table-config";
import { addTextToValue, convertSecToDay, timeColumn } from "./common-table-columns";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const JiraReleaseTableConfig = [
    baseColumnConfig("Release Version", "name"),
    baseColumnConfig("Project", "project"),
    timeColumn("Release Date", "released_end_time", { sorter: false }),
    convertSecToDay("Average Lead Time", "average_lead_time", "days"),
    addTextToValue("Number of Issues", "issue_count", "tickets"),
];


export const JiraReleaseTableKeyLableMapping: basicMappingType<string> = {
    name: "Release Version",
    project: "Project",
    released_end_time: "Release Date",
    average_lead_time: "Average Lead Time",
    issue_count: "Number of Issues",
};

export const JiraReleaseTableKeyValue = [
    { key: "name", title: "Release Version" },
    { key: "project", title: "Project" },
    { key: "released_end_time", title: "Release Date" },
    { key: "average_lead_time", title: "Average Lead Time" },
    { key: "issue_count", title: "Number of Issues" },
]