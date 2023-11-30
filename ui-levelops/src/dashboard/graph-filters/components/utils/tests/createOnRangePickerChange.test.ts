import { createOnRangePickerChange } from "../createOnRangePickerChange";
import { MAX_RANGE } from "../getMaxRangeFromReportType";
import moment from "moment";

describe("createOnRangePickerChange.ts tests", () => {
  const START_DATE = moment.unix(1609523489);
  const END_DATE = moment.unix(1611078689);
  const START_DATE_BEYOND_RANGE = moment.unix(1577901089);
  const test_cases = [
    {
      it: "When no dates are passed.",
      input: {
        dates: [],
        value_type: "string"
      },
      output: {
        called: true,
        arg: undefined
      }
    },
    {
      it: "When one date is passed.",
      input: {
        dates: [undefined, END_DATE],
        value_type: "string"
      },
      output: {
        called: true,
        arg: { $gt: undefined, $lt: "1611129599" }
      }
    },
    {
      it: "When two dates is passed.",
      input: {
        dates: [START_DATE, END_DATE],
        value_type: "string"
      },
      output: {
        called: true,
        arg: { $gt: "1609488000", $lt: "1611129599" }
      }
    },
    {
      it: "When two dates is passed beyond range",
      input: {
        dates: [START_DATE_BEYOND_RANGE, END_DATE],
        value_type: "string"
      },
      output: {
        called: false,
        arg: undefined
      }
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      let called = false;
      let arg = undefined;
      const onChange = (mappedRangeValue: any) => {
        arg = mappedRangeValue;
        called = true;
      };

      const onRangePickerChange = createOnRangePickerChange(
        MAX_RANGE,
        onChange,
        test_case.input.value_type as "string" | "number"
      );

      // @ts-ignore
      onRangePickerChange(test_case.input.dates);

      const result = {
        called,
        arg
      };

      expect(result).toStrictEqual(test_case.output);
    });
  });
});
