import { CreatedAtUpdateAtOptions } from "dashboard/graph-filters/components/helper";
import { getCreatedAtUpdateAtOptions } from "../getCreatedAtUpdateAtOptions";

describe("getCreatedAtUpdateAtOptions.ts tests", () => {
  const test_cases = [
    {
      it: "input: not a trend report",
      input: {
        time_range_key: "last_week",
        isTrendReport: false
      },
      output: [
        { label: "Last 7 days", value: "last_week" },
        { label: "Last 30 days", value: "last_month" },
        { label: "Last 90 days", value: "last_quarter" },
        { label: "Last 180 days", value: "last_6_months" },
        { label: "Last 365 days", value: "last_year" }
      ]
    },
    {
      it: "input: trend report with less than 90 day keys",
      input: {
        time_range_key: "last_week",
        isTrendReport: true
      },
      output: [
        { label: "Last 7 days", value: "last_week" },
        { label: "Last 30 days", value: "last_month" },
        { label: "Last 90 days", value: "last_quarter" }
      ]
    },
    {
      it: "input: trend report with greater than 90 day keys",
      input: {
        time_range_key: "last_6_months",
        isTrendReport: true
      },
      output: [
        { label: "Last 7 days", value: "last_week" },
        { label: "Last 30 days", value: "last_month" },
        { label: "Last 90 days", value: "last_quarter" },
        { label: "Last 180 days", value: "last_6_months" }
      ]
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      const result = getCreatedAtUpdateAtOptions(
        test_case.input.time_range_key,
        test_case.input.isTrendReport,
        CreatedAtUpdateAtOptions
      );

      expect(result).toStrictEqual(test_case.output);
    });
  });
});
