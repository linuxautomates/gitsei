import * as React from "react";
import {
  getSupportedFilterURI,
  appendAndUpdateFilters,
  updateLayoutWithNewApplicationFilters,
  checkForCompositeParentIds,
  getApplicationFilters
} from "../helper";
import { isObject } from "lodash";

describe("DashboardGraphFiltersHelper", () => {
  test("getSupportedFilterURI should return string", () => {
    expect(getSupportedFilterURI(undefined)).toBe("");

    expect(
      getSupportedFilterURI({
        uri: "test"
      })
    ).toBe("test");

    expect(
      getSupportedFilterURI([
        {
          uri: "test"
        },
        {
          uri: "uri"
        }
      ])
    ).toBe("test-uri");
  });

  test("appendAndUpdateFilters should return object", () => {
    const prevFilters = {
      a: "test"
    };
    const newFilters = {
      a: "new test",
      b: 123
    };
    expect(isObject(appendAndUpdateFilters(prevFilters, newFilters))).toBeTruthy();
  });

  test("appendAndUpdateFilters should return merged object", () => {
    const prevFilters = {
      a: "test"
    };
    const newFilters = {
      a: "new test",
      b: 123
    };
    expect(appendAndUpdateFilters(prevFilters, newFilters)).toEqual({
      a: "new test",
      b: 123
    });
  });

  test("updateLayoutWithNewApplicationFilters should return valid data", () => {
    const data = [
      {
        id: "test1",
        type: "hops_report_trends",
        metadata: {
          weights: {
            IDLE: 20,
            LATE_ATTACHMENTS: 2,
            NO_ASSIGNEE: 20,
            NO_COMPONENTS: 20,
            NO_DUE_DATE: 30,
            POOR_DESCRIPTION: 8
          },
          widget_type: "graph",
          width: "half",
          hidden: false
        },
        query: {
          across: "trend"
        }
      }
    ];

    const filters = {
      jira_filter_values: {
        issue_types: ["BUG"],
        statuses: ["IN REVIEW"]
      }
    };

    expect(updateLayoutWithNewApplicationFilters(data, filters)).toEqual([
      [
        {
          id: "test1",
          metadata: {
            hidden: false,
            weights: {
              IDLE: 20,
              LATE_ATTACHMENTS: 2,
              NO_ASSIGNEE: 20,
              NO_COMPONENTS: 20,
              NO_DUE_DATE: 30,
              POOR_DESCRIPTION: 8
            },
            widget_type: "graph",
            width: "half"
          },
          query: { across: "trend", issue_types: ["BUG"], statuses: ["IN REVIEW"] },
          type: "hops_report_trends"
        }
      ],
      ["test1"]
    ]);
  });

  test("updateLayoutWithNewApplicationFilters should return valid data", () => {
    const data = [
      {
        id: "test1",
        childrens: ["updatedId1", "updatedId2"]
      },
      {
        id: "test2",
        childrens: ["id1", "id2"]
      }
    ];

    expect(checkForCompositeParentIds(data, ["updatedId1", "updatedId2", "updatedId3"])).toEqual([
      "updatedId1",
      "updatedId1",
      "updatedId2",
      "updatedId2",
      "updatedId3",
      "updatedId3"
    ]);
  });

  test("getApplicationFilters should return object", () => {
    expect(isObject(getApplicationFilters())).toBeTruthy();
  });
});
