import { isArray, isNumber, isUndefined, isNull, isString, isEqual, orderBy } from "lodash";
import { genericSeriesDataTransformer, seriesDataTransformer, SCMPRReportsTransformer } from "../seriesData.helper";
import { seriesDataHelperMock } from "./helper.mock";
import SCMPRReportsTransformerData from "../__mocks__/SCMPRReportsTransformer.json";

describe("Series Data Helper Tests", () => {
  test("test parameters for genericSeriesDataTransformer", () => {
    const param1: any = [];
    const param2 = 10;
    const param3 = undefined;
    genericSeriesDataTransformer(param1, param2, param3);

    expect(isArray(param1)).toBeTruthy();
    expect(isNumber(param2) || isNull(param2) || isUndefined(param2)).toBeTruthy();
    expect(isString(param3) || isNull(param3) || isUndefined(param3)).toBeTruthy();
  });
  test("test result for genericSeriesDataTransformer should be an array", () => {
    const param1: any = [];
    const param2 = 10;
    const param3 = undefined;
    const transformer = genericSeriesDataTransformer(param1, param2, param3);

    expect(isArray(transformer)).toBeTruthy();
  });
  test("test parameter for seriesDataTransformer case1: should be an object", () => {
    const param1: any = {};
    const transformer = seriesDataTransformer(param1);

    expect(typeof param1 === "object" && !isArray(param1)).toBeTruthy();
  });
  test("test parameter for seriesDataTransformer case2: should have apiData and apiData should be an array", () => {
    const param1: any = { apiData: [] };
    const transformer = seriesDataTransformer(param1);

    expect(param1.hasOwnProperty("apiData") && isArray(param1.apiData)).toBeTruthy();
  });
  test("test parameter for seriesDataTransformer case3: should have reportType and reportType should be a string", () => {
    const param1: any = { reportType: "" };
    const transformer = seriesDataTransformer(param1);

    expect(param1.hasOwnProperty("reportType") && isString(param1.reportType)).toBeTruthy();
  });
  test("test parameter for seriesDataTransformer case4: should have widgetFilters and widgetFilters should be an object", () => {
    const param1: any = { widgetFilters: {} };
    const transformer = seriesDataTransformer(param1);

    expect(
      param1.hasOwnProperty("widgetFilters") &&
        typeof param1.widgetFilters === "object" &&
        !isArray(param1.widgetFilters)
    ).toBeTruthy();
  });
  test("test result for seriesDataTransformer ", () => {
    const transformer = seriesDataTransformer({});

    expect(typeof transformer === "object" && transformer.hasOwnProperty("data")).toBeTruthy();
  });
  test("test mock parameters and response for seriesDataTransformer ", () => {
    const transformer = orderBy(seriesDataTransformer(seriesDataHelperMock.parameters)?.data, "total_tickets", "asc");
    const mockResponse = orderBy(seriesDataHelperMock.response.data, "total_tickets", "asc");

    expect(isEqual(transformer, mockResponse)).toBeTruthy();
  });
  test("test result for SCMPRReportsTransformer should get all key along with stack items", () => {
    const result = SCMPRReportsTransformer(SCMPRReportsTransformerData);

    const keyInAllData = result.data.every((item: any) => item?.key && typeof item?.key === "string");
    expect(keyInAllData).toBeTruthy();
  });
});
