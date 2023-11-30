import { isString, isArray, isEqual } from "lodash";
import { bullseyeTrendTransformer, trendReportTransformer } from "..";
import {
  trendReportMockData,
  trendReportMockData2,
  trendReportMockData3,
  bullseyeTrendReportMockData
} from "./helper.mock";

describe("Trend Report Helper Tests", () => {
  test("test parameters for trendReportTransformer", () => {
    const param1: any = { reportType: "" };
    const transformer = trendReportTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
  });
  test("test parameter for trendReportTransformer case2: should have apiData and apiData should be an array", () => {
    const param1: any = { apiData: [] };
    const transformer = trendReportTransformer(param1);

    expect(param1.hasOwnProperty("apiData") && isArray(param1.apiData)).toBeTruthy();
  });
  test("test mock parameters and repsonse for trendReportTransformer case1 ", () => {
    const transformer = trendReportTransformer(trendReportMockData.parameters);

    expect(isEqual(trendReportMockData.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for trendReportTransformer case2 ", () => {
    const transformer = trendReportTransformer(trendReportMockData2.parameters);

    expect(isEqual(trendReportMockData2.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for trendReportTransformer case3 ", () => {
    const transformer = trendReportTransformer(trendReportMockData3.parameters);

    expect(isEqual(trendReportMockData3.response, transformer)).toBeTruthy();
  });
});

describe("Bullseye Trend Report Helper Tests", () => {
  test("test parameters for bullseyeTrendTransformer", () => {
    const param1: any = { reportType: "" };
    const transformer = bullseyeTrendTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
  });
  test("test parameter for bullseyeTrendTransformer case2: should have apiData and apiData should be an array", () => {
    const param1: any = { apiData: [] };
    const transformer = bullseyeTrendTransformer(param1);

    expect(param1.hasOwnProperty("apiData") && isArray(param1.apiData)).toBeTruthy();
  });
  test("test parameter for bullseyeTrendTransformer case4: should have filters and filters should be an object", () => {
    const param1: any = { filters: {} };
    const transformer = bullseyeTrendTransformer(param1);

    expect(
      param1.hasOwnProperty("filters") && typeof param1.filters === "object" && !isArray(param1.filters)
    ).toBeTruthy();
  });
  test("test mock parameters and repsonse for bullseyeTrendTransformer ", () => {
    const transformer = bullseyeTrendTransformer(bullseyeTrendReportMockData.parameters);

    expect(isEqual(bullseyeTrendReportMockData.response, transformer)).toBeTruthy();
  });
});
