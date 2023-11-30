import { fireEvent } from "@testing-library/react";
import * as React from "react";
import { testRender } from "shared-resources/components/testing/testing-react.wrapper";
import { mockTestStore } from "utils/testUtils";
import DashboardGraphFiltersComponent from "../components/DashboardGraphFilters";
import { mockCustomFiltersData, mockFiltersData, mockGithubFiltersData } from "./mockData";

describe("DashboardGraphFilters", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });
  test("Dashboard graph filters component should match the screenshot with data", () => {
    const { asFragment } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );
    expect(asFragment()).toMatchSnapshot();
  });
  test("filters div should be in the document", () => {
    const { getByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const filtersDiv = getByTestId("dashboard-graph-filters-component");

    expect(filtersDiv).toBeInTheDocument();
  });
  test("filters list should have at least 1 filter in the document", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const filterListElement = getAllByTestId("filter-list-element");

    expect(filterListElement[0]).toBeInTheDocument();
  });
  test("custom form item label div should be in the document", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const formItemLabelDiv = getAllByTestId("custom-form-item-label");

    expect(formItemLabelDiv[0]).toBeInTheDocument();
  });
  test("for jira filters , exclude switch should be in the document", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    expect(excludeSwitchDiv[0]).toBeInTheDocument();
  });
  test("for jira filters , exclude switch should be inactive without exclude filter", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    expect(excludeSwitchDiv[0]).not.toHaveClass("ant-switch-checked");
  });
  test("for jira filters , exclude switch should be active with exclude filter", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{ exclude: { statuses: [] } }}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    expect(excludeSwitchDiv[0]).toHaveClass("ant-switch-checked");
  });
  test("for jira filters , exclude switch handler should be called once", () => {
    const mockFn = jest.fn();
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        onExcludeChange={mockFn}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    fireEvent.click(excludeSwitchDiv[0]);

    expect(mockFn).toHaveBeenCalledTimes(1);
  });
  test("for jira filters , partial string filter should be in the document", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterDiv = getAllByTestId("custom-form-item-label-switchWithDropdown");

    expect(partialStringFilterDiv[0]).toBeInTheDocument();
  });
  test("for jira filters , partial string filter checkbox should be inactive by default", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterCheckboxDiv = getAllByTestId("custom-form-item-label-switchWithDropdown-checkbox");

    expect(partialStringFilterCheckboxDiv[0]).not.toHaveAttribute("checked");
  });
  test("for jira filters , partial string filter checkbox should be active with mock value", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{ partial_match: { status: { $begins: "" } } }}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterCheckboxDiv = getAllByTestId("custom-form-item-label-switchWithDropdown-checkbox");

    expect(partialStringFilterCheckboxDiv[0]).toHaveAttribute("checked");
  });
  test("for jira filters , partial string filter checkbox should call its handler once", () => {
    const mockFn = jest.fn();
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={mockFn}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterCheckboxDiv = getAllByTestId("custom-form-item-label-switchWithDropdown-checkbox");

    fireEvent.click(partialStringFilterCheckboxDiv[0]);

    expect(mockFn).toHaveBeenCalledTimes(1);
  });
  test("for jira filters , select dropdown should be there by default", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const filterListElement = getAllByTestId("filter-list-element-select");

    expect(filterListElement[0]).toBeInTheDocument();
  });
  test("for jira filters , input field should be there when partial filter is applied", () => {
    const { getAllByTestId, getByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockFiltersData}
        customData={mockCustomFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{ partial_match: { status: { $begins: "" } } }}
        application="jira"
        reportType="bounce_report"
        applicationUse={false}
      />,
      { store }
    );

    const filterListElementInput = getAllByTestId("filter-list-element-input");

    expect(filterListElementInput[0]).toBeInTheDocument();
  });
  test("for github filters , exclude switch should be in the document", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    expect(excludeSwitchDiv[0]).toBeInTheDocument();
  });
  test("for github filters , exclude switch should be inactive without exclude filter", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    expect(excludeSwitchDiv[0]).not.toHaveClass("ant-switch-checked");
  });
  test("for github filters , exclude switch should be active with exclude filter", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{ exclude: { repo_ids: [] } }}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    expect(excludeSwitchDiv[0]).toHaveClass("ant-switch-checked");
  });
  test("for github filters , exclude switch handler should be called once", () => {
    const mockFn = jest.fn();
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        onExcludeChange={mockFn}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const excludeSwitchDiv = getAllByTestId("custom-form-item-label-withSwitch-switch");

    fireEvent.click(excludeSwitchDiv[0]);

    expect(mockFn).toHaveBeenCalledTimes(1);
  });
  test("for github filters , partial string filter should be in the document", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterDiv = getAllByTestId("custom-form-item-label-switchWithDropdown");

    expect(partialStringFilterDiv[0]).toBeInTheDocument();
  });
  test("for github filters , partial string filter checkbox should be inactive by default", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterCheckboxDiv = getAllByTestId("custom-form-item-label-switchWithDropdown-checkbox");

    expect(partialStringFilterCheckboxDiv[0]).not.toHaveAttribute("checked");
  });
  test("for github filters , partial string filter checkbox should be active with mock value", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{ partial_match: { repo_id: { $begins: "" } } }}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterCheckboxDiv = getAllByTestId("custom-form-item-label-switchWithDropdown-checkbox");

    expect(partialStringFilterCheckboxDiv[0]).toHaveAttribute("checked");
  });
  test("for github filters , partial string filter checkbox should call its handler once", () => {
    const mockFn = jest.fn();
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={mockFn}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const partialStringFilterCheckboxDiv = getAllByTestId("custom-form-item-label-switchWithDropdown-checkbox");

    fireEvent.click(partialStringFilterCheckboxDiv[0]);

    expect(mockFn).toHaveBeenCalledTimes(1);
  });
  test("for github filters , select dropdown should be there by default", () => {
    const { getAllByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{}}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const filterListElement = getAllByTestId("filter-list-element-select");

    expect(filterListElement[0]).toBeInTheDocument();
  });
  test("for github filters , input field should be there when partial filter is applied", () => {
    const { getAllByTestId, getByTestId } = testRender(
      <DashboardGraphFiltersComponent
        data={mockGithubFiltersData}
        onCustomFilterValueChange={jest.fn()}
        onFilterValueChange={jest.fn()}
        onPartialChange={jest.fn()}
        filters={{ partial_match: { repo_id: { $begins: "" } } }}
        application="github"
        reportType="github_prs_report"
        applicationUse={false}
      />,
      { store }
    );

    const filterListElementInput = getAllByTestId("filter-list-element-input");

    expect(filterListElementInput[0]).toBeInTheDocument();
  });
});
