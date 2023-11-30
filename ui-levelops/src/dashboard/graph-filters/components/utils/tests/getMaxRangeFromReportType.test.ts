import { getMaxRangeFromReportType, MAX_RANGE } from "../getMaxRangeFromReportType";

describe("getMaxRangeFromReportType.ts tests", () => {
  const test_cases = [
    {
      it: "input: not a trend report",
      input: "a_report",
      output: undefined
    },
    {
      it: "input: is a trend report",
      input: "a_trend_report",
      output: MAX_RANGE
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      const result = getMaxRangeFromReportType(test_case.input);
      expect(result).toStrictEqual(test_case.output);
    });
  });
});
