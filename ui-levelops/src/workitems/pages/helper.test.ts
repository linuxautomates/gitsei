import { mergeFilters } from "./helper";

describe("helpers.ts Tests", () => {
  test("mergeFilters() test", () => {
    const prevFilter = {
      key1: "value1",
      key2: [1, 2, 3, 4],
      key3: {
        sub_key3: "value 1"
      }
    };
    const newFilter = {
      key1: "newValue1",
      key2: [4, 5, 6],
      key3: {
        sub_key4: "value 2"
      }
    };

    const filter = mergeFilters(prevFilter, newFilter);
    expect(filter).toStrictEqual({
      key1: "newValue1",
      key2: [1, 2, 3, 4, 4, 5, 6],
      key3: { sub_key3: "value 1", sub_key4: "value 2" }
    });

    const filter1 = mergeFilters({}, newFilter);
    expect(filter1).toStrictEqual(newFilter);

    const filter2 = mergeFilters(prevFilter, {});
    expect(filter2).toStrictEqual(prevFilter);
  });
});
