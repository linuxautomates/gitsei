import moment from "moment";
import { getMappedRangeValue } from "../getMappedRangeValue";

describe("getMappedRangeValue.ts tests", () => {
  const GT = 1582080680;
  const LT = 1613703080;
  const GT_START_OF_DAY = 1582012800;
  const LT_END_OF_DAY = 1613721599;

  const test_cases = [
    {
      it: "input undefined",
      input: {
        dates: undefined,
        value_type: undefined
      },
      output: undefined
    },
    {
      it: "input empty array",
      input: {
        dates: [],
        value_type: undefined
      },
      output: undefined
    },
    {
      it: "input [null]",
      input: {
        dates: [null],
        value_type: undefined
      },
      output: undefined
    },
    {
      it: "input [undefined]",
      input: {
        dates: [undefined],
        value_type: undefined
      },
      output: undefined
    },
    {
      it: "input [undefined, undefined]",
      input: {
        dates: [undefined, undefined],
        value_type: undefined
      },
      output: {
        $gt: undefined,
        $lt: undefined
      }
    },
    {
      it: "input null[]",
      input: {
        dates: [null, null],
        value_type: undefined
      },
      output: {
        $gt: undefined,
        $lt: undefined
      }
    },
    {
      it: "input one moment as number",
      input: {
        dates: [moment.unix(GT), null],
        value_type: "number"
      },
      output: {
        $gt: GT_START_OF_DAY,
        $lt: undefined
      }
    },
    {
      it: "input two moment as number",
      input: {
        dates: [moment.unix(GT), moment.unix(LT)],
        value_type: undefined
      },
      output: {
        $gt: GT_START_OF_DAY,
        $lt: LT_END_OF_DAY
      }
    },
    {
      it: "input one moment as string",
      input: {
        dates: [moment.unix(GT), moment.unix(LT)],
        value_type: "string"
      },
      output: {
        $gt: `${GT_START_OF_DAY}`,
        $lt: `${LT_END_OF_DAY}`
      }
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      const result = getMappedRangeValue(test_case.input.dates, test_case.input.value_type);
      expect(result).toStrictEqual(test_case.output);
    });
  });
});
