import { buildMissingFieldsQuery, getFilterValue, buildServerPaginatedQuery, setFilterState } from "../helper";

describe("ServerPaginatedTableHelpers", () => {
  test("buildMissingFieldsQuery should return correct value", () => {
    const prevMissingFields = {
      booleanField: true,
      stringField: "test string",
      arrayField: ["a", "b", "c"],
      numericField: 10
    };
    const newFilter = "testFilter";
    const key = "testKey";
    const value = true;

    const returnValue = buildMissingFieldsQuery(prevMissingFields, newFilter, key, value);
    expect(returnValue).toEqual({
      arrayField: ["a", "b", "c"],
      booleanField: true,
      numericField: 10,
      stringField: "test string",
      testFilter: false
    });
  });

  test("getFilterValue should return valid data type", () => {
    const filters = {
      booleanField: true,
      stringField: "test string",
      arrayField: ["a", "b", "c"],
      numericField: 10
    };

    expect(getFilterValue(filters, "stringField", "input")).toEqual("test string");

    expect(getFilterValue(filters, "arrayField", "multiSelect")).toEqual(["a", "b", "c"]);

    expect(getFilterValue(filters, "numericField", "unknown")).toEqual(10);
  });

  test("buildServerPaginatedQuery should return valid object", () => {
    const filters = {
      booleanField: true,
      stringField: "test string",
      status: ["a", "b", "c"],
      numericField: 10
    };

    expect(buildServerPaginatedQuery(filters, "status", "multiSelect", true)).toEqual({
      booleanField: true,
      exclude: { statuses: ["a", "b", "c"] },
      numericField: 10,
      status: ["a", "b", "c"],
      stringField: "test string"
    });
  });

  test("setFilterState should call setValue function", () => {
    const oldValues = [1, 2, 3, "a"];
    const newValues = ["a", "b", "c"];
    const setValue = jest.fn();

    setFilterState(oldValues, newValues, setValue);
    expect(setValue).toBeCalledWith(["a", "b", "c"]);
  });
});
