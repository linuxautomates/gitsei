import { fireEvent } from "@testing-library/react";
import { DashboardDrillDownPreview } from "dashboard/pages";
import * as React from "react";
import { testRender } from "shared-resources/components/testing/testing-react.wrapper";
import { mockTestStore } from "utils/testUtils";
import { mockDrillDownProps } from "./mockData";

describe("DashboardDrillDown Preview", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });
  test("DrillDownPreview should match the snapshot", () => {
    const { asFragment } = testRender(
      <DashboardDrillDownPreview drillDownProps={mockDrillDownProps as any} onDrilldownClose={jest.fn()} />,
      { store }
    );

    expect(asFragment()).toMatchSnapshot();
  });
  test("Open Report Btn should be in the document", () => {
    const { getByTestId } = testRender(
      <DashboardDrillDownPreview drillDownProps={mockDrillDownProps as any} onDrilldownClose={jest.fn()} />,
      { store }
    );

    const openReportBtn = getByTestId("open-report-btn");

    expect(openReportBtn).toBeInTheDocument();
  });
  test("DrilldownPreview close Btn should be in the document", () => {
    const { getByTestId } = testRender(
      <DashboardDrillDownPreview drillDownProps={mockDrillDownProps as any} onDrilldownClose={jest.fn()} />,
      { store }
    );

    const drilldownCloseBtn = getByTestId("drilldown-close-btn");

    expect(drilldownCloseBtn).toBeInTheDocument();
  });
  test("DrilldownPreview close Btn should be called once", () => {
    const mockFn = jest.fn();
    const { getByTestId } = testRender(
      <DashboardDrillDownPreview drillDownProps={mockDrillDownProps as any} onDrilldownClose={mockFn} />,
      { store }
    );

    const drilldownCloseBtn = getByTestId("drilldown-close-btn");

    fireEvent.click(drilldownCloseBtn);

    expect(mockFn).toBeCalledTimes(1);
  });
});
