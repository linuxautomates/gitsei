import { isString, isArray, isEqual } from "lodash";
import {
  configureWidgetGroupByTransform,
  configureWidgetSingleStatTransform,
  configWidgetDataTransform
} from "../helper";
import {
  configWidgetDataTransformMock,
  configureWidgetTransformMock2,
  configWidgetTransformMock3,
  configTableStatTransform
} from "./tests.mock";

describe("configWidgetDataTransform tests", () => {
  test("test parameters for configWidgetDataTransform case1: parameter should have xAxis", () => {
    const param: any = { xAxis: "" };
    configWidgetDataTransform(param);

    expect(param.hasOwnProperty("xAxis") && isString(param.xAxis)).toBeTruthy();
  });
  test("test parameters for configWidgetDataTransform case2: parameter should have yAxis", () => {
    const param: any = { yAxis: [] };
    configWidgetDataTransform(param);

    expect(param.hasOwnProperty("yAxis") && isArray(param.yAxis)).toBeTruthy();
  });
  test("test parameters for configWidgetDataTransform case3: parameter should have columns", () => {
    const param: any = { columns: [] };
    configWidgetDataTransform(param);

    expect(param.hasOwnProperty("columns") && isArray(param.columns)).toBeTruthy();
  });
  test("test parameters for configWidgetDataTransform case4: parameter should have rows", () => {
    const param: any = { rows: [] };
    configWidgetDataTransform(param);

    expect(param.hasOwnProperty("rows") && isArray(param.rows)).toBeTruthy();
  });
  test("test parameters for configWidgetDataTransform case5: parameter should have yUnit", () => {
    const param: any = { yUnit: "" };
    configWidgetDataTransform(param);

    expect(param.hasOwnProperty("yUnit") && isString(param.yUnit)).toBeTruthy();
  });
  test("test parameters for configWidgetDataTransform case6: parameter should have filters", () => {
    const param: any = { filters: "" };
    configWidgetDataTransform(param);

    expect(param.hasOwnProperty("filters")).toBeTruthy();
  });
  test("test mock parameters and repsonse for configWidgetDataTransform case1 ", () => {
    const transformer = configWidgetDataTransform(configWidgetDataTransformMock.parameters);

    expect(isEqual(configWidgetDataTransformMock.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for configWidgetDataTransform case2 ", () => {
    const transformer = configWidgetDataTransform(configureWidgetTransformMock2.parameters);

    expect(isEqual(configureWidgetTransformMock2.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for configureWidgetGroupByTransform ", () => {
    const transformer = configureWidgetGroupByTransform(configWidgetTransformMock3.parameters);

    expect(isEqual(configWidgetTransformMock3.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for configureWidgetSingleStatTransform ", () => {
    const transformer = configureWidgetSingleStatTransform(configTableStatTransform.parameters);

    expect(isEqual(configTableStatTransform.response, transformer)).toBeTruthy();
  });
});
