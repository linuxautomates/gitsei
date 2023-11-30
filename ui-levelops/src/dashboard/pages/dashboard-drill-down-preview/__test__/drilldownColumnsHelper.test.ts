import { defaultColumns } from "../drilldownColumnsHelper";
import { JIRA_SPRINT_REPORTS } from "../../../constants/applications/names";
import { sprintReportDataKeyTypes } from "../../../graph-filters/components/sprintFilters.constant";

const filters = {
  filter: {
    metric: "average_churn_rate",
    agg_type: "average",
    completed_at: {
      $gt: "1672531200",
      $lt: "1700179199"
    },
    include_issue_keys: true,
    integration_ids: ["4218", "4414"],
    include_total_count: true
  },
  ou_ids: ["33554"],
  ou_user_filter_designation: {
    sprint: ["sprint_report"]
  }
};

const expectedColumnsNames = [
  sprintReportDataKeyTypes.SPRINT_CHURN_RATE,
  sprintReportDataKeyTypes.STORY_POINTS_PLANNED,
  sprintReportDataKeyTypes.STORY_POINTS_REMOVED,
  sprintReportDataKeyTypes.STORY_POINTS_ADDED,
  sprintReportDataKeyTypes.STORY_POINTS_CHANGED
];

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useParams: jest.fn()
}));

jest.mock("configurations/pages/Organization/Filters/jenkins/jenkins-job-filter.config", () => {
  const originalModule = jest.requireActual(
    "configurations/pages/Organization/Filters/jenkins/jenkins-job-filter.config"
  );
  return {
    __esModule: true,
    ...originalModule,
    OUJenkinsJobsCommonFiltersConfig: []
  };
});

jest.mock("configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config", () => {
  const originalModule = jest.requireActual(
    "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config"
  );
  return {
    __esModule: true,
    ...originalModule,
    OUHarnessngJobsCommonFiltersConfig: []
  };
});

describe("getSprintMetricSingleStatColumns tests", () => {
  it("test churn rates columns are visible when average_churn_rate metric is selected", () => {
    const columns = defaultColumns({ widget: { type: JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT }, filters });
    const isAllColumnsVisible = expectedColumnsNames.every(col => (columns || []).find(tCol => tCol.key === col));
    expect(isAllColumnsVisible).toBeTruthy();
  });

  it("test churn rates columns are hidden when metric is different than average_churn_rate", () => {
    const columns = defaultColumns({ widget: { type: JIRA_SPRINT_REPORTS.SPRINT_CREEP }, filters });
    const isAllColumnsHidden = expectedColumnsNames.every(col => !(columns || []).find(tCol => tCol.key === col));
    expect(isAllColumnsHidden).toBeTruthy();
  });
});
