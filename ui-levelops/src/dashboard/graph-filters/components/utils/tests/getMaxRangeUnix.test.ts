import { getMaxRangeUnix } from "../getMaxRangeUnix";

describe("getMaxRangeUnix.ts tests", () => {
  const test_cases = [
    {
      it: "input: no time range specified",
      input: undefined,
      output: undefined
    },
    {
      it: "input: is a trend report",
      input: { length: 90, units: "days" },
      output: 7776000
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      const result = getMaxRangeUnix(test_case.input);
      expect(result).toStrictEqual(test_case.output);
    });
  });
});
