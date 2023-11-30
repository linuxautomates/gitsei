import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { JiraIssuesReportFiltersConfig } from "../filters.config";
import { filtersConfigMock, tempConfig } from "../tests/__mocks__/filters.config.mock";

jest.mock("../../../../../dashboard/pages/dashboard-tickets/configs/common-table-columns", () => ({
  dateRangeFilterColumn: () => "TEST",
  timeRangeFilterColumn: () => "",
  userColumn: () => "",
  timeDurationColumn: () => "",
  coloredTagsColumn: () => "",
  statusColumn: () => "",
  priorityColumn: () => "",
  timeColumn: () => {},
  convertToReadableTimestamp: () => "",
  convertSecToDay: () => "",
  prTitleColumn: () => "",
  utcTimeColumn: () => "",
  commaSepColumnConfig: () => "",
  cautiousUnixTimeColumn: () => "",
  booleanToStringColumn: () => "",
  addTextToValue: () => ""
}));

jest.mock("../../../../../dashboard/pages/dashboard-tickets/configs/leadTimeTableConfig", () => ({
  dateRangeFilterColumn: () => "TEST",
  timeRangeFilterColumn: () => "",
  userColumn: () => "",
  timeDurationColumn: () => "",
  coloredTagsColumn: () => "",
  statusColumn: () => "",
  priorityColumn: () => "",
  timeColumn: () => {},
  convertToReadableTimestamp: () => "",
  convertSecToDay: () => "",
  prTitleColumn: () => ""
}));

describe(" Issues report filter.config test", () => {
  test("All issues report filter should load correctly", async () => {
    expect(JSON.stringify(JiraIssuesReportFiltersConfig)).toEqual(JSON.stringify(filtersConfigMock));
  });
});
