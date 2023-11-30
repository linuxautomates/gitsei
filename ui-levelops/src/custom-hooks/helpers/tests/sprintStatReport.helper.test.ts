import { isString } from "lodash";
import { sprintStatReportTransformer } from "../sprintStatReporthelper";
import { JIRA_SPRINT_REPORTS } from "../../../dashboard/constants/applications/names";
import apiData from "../__mocks__/sprintStatReport.helper.mocks.json";

const filters = {
  metric: "average_churn_rate",
  agg_type: "average",
  completed_at: {
    $gt: "1672531200",
    $lt: "1699660799"
  }
};

// Need to mock this function, as facing issues because of circular dependency.
jest.mock("../sprintStatReporthelper", () => {
  const originalModule = jest.requireActual("../sprintStatReporthelper");

  //Mock the default export and named export 'sprintMetricStatColumnSorterComparater'
  return {
    __esModule: true,
    ...originalModule,
    sprintMetricStatColumnSorterComparater: jest.fn()
  };
});

describe("sprintStatReportTransformer Tests", () => {
  test("test::average_churn_rate empty properties for sprintStatReportTransformer", () => {
    const param1: any = { reportType: "" };
    const transformer = sprintStatReportTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
    expect(transformer).toStrictEqual({});
  });

  test("test::average_churn_rate result should match with the expected value for sprintStatReportTransformer", () => {
    const param1: any = { reportType: JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT, filters, apiData };
    const expectedChurnRate = Number((apiData[0].average_churn_rate * 100).toFixed(2));
    const transformer = sprintStatReportTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
    expect(transformer).toMatchObject({ stat: expectedChurnRate, unit: "%" });
  });

  test("test::average_churn_rate result expect default value when key is not present for sprintStatReportTransformer", () => {
    const updatedApiData = apiData.map(({ average_churn_rate, ...rest }) => rest);
    const param1: any = {
      reportType: JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
      filters,
      apiData: updatedApiData
    };
    const transformer = sprintStatReportTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
    expect(transformer).toMatchObject({ stat: 0, unit: "%" });
  });
});
