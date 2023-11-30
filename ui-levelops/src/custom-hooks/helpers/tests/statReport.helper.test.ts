import { isArray, isEqual, isString } from "lodash";
import { statReportTransformer } from "../statReport.helper";
import { statDataHelperMock1, statDataHelperMock2, statDataHelperMock3 } from "./helper.mock";

describe("Stat Report Helper Tests", () => {
  test("test parameters for statReportTransformer", () => {
    const param1: any = { reportType: "" };
    const transformer = statReportTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
  });
  test("test parameter for statReportTransformer case2: should have apiData and apiData should be an array", () => {
    const param1: any = { apiData: [] };
    const transformer = statReportTransformer(param1);

    expect(param1.hasOwnProperty("apiData") && isArray(param1.apiData)).toBeTruthy();
  });
  test("test parameter for statReportTransformer case4: should have filters and filters should be an object", () => {
    const param1: any = { filters: {} };
    const transformer = statReportTransformer(param1);

    expect(
      param1.hasOwnProperty("filters") && typeof param1.filters === "object" && !isArray(param1.filters)
    ).toBeTruthy();
  });
  test("test mock parameters and repsonse for statReportTransformer case1 ", () => {
    const transformer = statReportTransformer(statDataHelperMock1.parameters);

    expect(isEqual(statDataHelperMock1.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for statReportTransformer case2 ", () => {
    const transformer = statReportTransformer(statDataHelperMock2.parameters);

    expect(isEqual(statDataHelperMock2.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for statReportTransformer case3 ", () => {
    const transformer = statReportTransformer(statDataHelperMock3.parameters);

    expect(isEqual(statDataHelperMock3.response, transformer)).toBeTruthy();
  });
});
