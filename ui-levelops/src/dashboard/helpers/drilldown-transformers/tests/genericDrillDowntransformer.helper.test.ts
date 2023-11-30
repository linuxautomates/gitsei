import { isEqual, isString } from "lodash";
import { genericDrilldownTransformer, githubFilesDrilldownTransformer } from "..";
import {
  genericDrilldownTransformMock,
  genericDrilldownTransformMock2,
  githubFilesDrilldownTransformerMock
} from "./tests.mock";

describe("Generic Drilldown Transformer Helper Tests", () => {
  test("Paramertes for generic Drilldown transformer case1: parameter should have drilldown props", () => {
    const param: any = { drillDownProps: {} };
    genericDrilldownTransformer(param);

    expect(param.hasOwnProperty("drillDownProps")).toBeTruthy();
  });
  test("Paramertes for generic Drilldown transformer case1.1: drilldown props should have x_axis", () => {
    const param: any = { drillDownProps: { x_axis: "" } };
    genericDrilldownTransformer(param);

    expect(
      param.hasOwnProperty("drillDownProps") &&
        param.drillDownProps.hasOwnProperty("x_axis") &&
        isString(param.drillDownProps.x_axis)
    ).toBeTruthy();
  });
  test("Paramertes for generic Drilldown transformer case1.2: drilldown props should have dashboardId", () => {
    const param: any = { drillDownProps: { dashboardId: "" } };
    genericDrilldownTransformer(param);

    expect(
      param.hasOwnProperty("drillDownProps") &&
        param.drillDownProps.hasOwnProperty("dashboardId") &&
        isString(param.drillDownProps.dashboardId)
    ).toBeTruthy();
  });
  test("Paramertes for generic Drilldown transformer case1.3: drilldown props should have widgetId", () => {
    const param: any = { drillDownProps: { widgetId: "" } };
    genericDrilldownTransformer(param);

    expect(
      param.hasOwnProperty("drillDownProps") &&
        param.drillDownProps.hasOwnProperty("widgetId") &&
        isString(param.drillDownProps.widgetId)
    ).toBeTruthy();
  });
  test("Paramertes for generic Drilldown transformer case1.4: drilldown props should have application", () => {
    const param: any = { drillDownProps: { application: "" } };
    genericDrilldownTransformer(param);

    expect(
      param.hasOwnProperty("drillDownProps") &&
        param.drillDownProps.hasOwnProperty("application") &&
        isString(param.drillDownProps.application)
    ).toBeTruthy();
  });
  test("Paramertes for generic Drilldown transformer case2: parameter should have widget data", () => {
    const param: any = { widget: {} };
    genericDrilldownTransformer(param);

    expect(param.hasOwnProperty("widget")).toBeTruthy();
  });
  test("Paramertes for generic Drilldown transformer case3: parameter should have dashboardQuery", () => {
    const param: any = { dashboardQuery: {} };
    genericDrilldownTransformer(param);

    expect(param.hasOwnProperty("dashboardQuery")).toBeTruthy();
  });
  test("test mock parameters and repsonse for genericDrilldownTransformer case1 ", () => {
    const transformer = genericDrilldownTransformer(genericDrilldownTransformMock.parameters);

    expect(isEqual(genericDrilldownTransformMock.response, transformer)).toBeTruthy();
  });
  test("test mock parameters and repsonse for genericDrilldownTransformer case2 ", () => {
    const transformer = genericDrilldownTransformer(genericDrilldownTransformMock2.parameters);

    expect(isEqual(genericDrilldownTransformMock2.response, transformer)).toBeTruthy();
  });
});

describe("github files Drilldown Transformer Helper Tests", () => {
  test("test mock parameters and repsonse for githubFilesDrilldownTransformer", () => {
    const transformer = githubFilesDrilldownTransformer(githubFilesDrilldownTransformerMock.parameters);

    expect(isEqual(githubFilesDrilldownTransformerMock.response, transformer)).toBeTruthy();
  });
});
