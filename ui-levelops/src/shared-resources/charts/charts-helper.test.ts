import * as helperFunctions from "./helper";

describe("getInitialFilters Test Suite", () => {
  test("it should return initial filters keys with boolean values' ", () => {
    const mock_data = [
      { key: "Kushagra Saxena", median: 2, min: 0, max: 4, total_tickets: 214, name: "Kushagra Saxena" },
      { key: "Bethany Collins", median: 2, min: 2, max: 2, total_tickets: 1, name: "Bethany Collins" },
      { key: "Harsh Jariwala", median: 1, min: 0, max: 5, total_tickets: 94, name: "Harsh Jariwala" }
    ];

    const mock_ignorekeys = [
      "name",
      "total_tickets",
      "total",
      "id",
      "total_cases",
      "key",
      "count",
      "additional_key",
      "toolTip",
      "stacks",
      "total_tests"
    ];
    const mock_defaultFilterKey = "median";
    const mock_response = { median: true, min: false, max: false };
    expect(helperFunctions.getInitialFilters(mock_data, mock_ignorekeys, mock_defaultFilterKey)).toStrictEqual(
      mock_response
    );
  });
});

describe("getFilteredData Test Suite", () => {
  test("it should return the filtered data' ", () => {
    const mock_data = [
      { key: "Kushagra Saxena", median: 2, min: 0, max: 4, total_tickets: 214, name: "Kushagra Saxena" },
      { key: "Bethany Collins", median: 2, min: 2, max: 2, total_tickets: 1, name: "Bethany Collins" },
      { key: "Harsh Jariwala", median: 1, min: 0, max: 5, total_tickets: 94, name: "Harsh Jariwala" }
    ];
    const mock_filters = { median: true, min: false, max: false };
    const mock_response = [
      { key: "Kushagra Saxena", median: 2, total_tickets: 214, name: "Kushagra Saxena" },
      { key: "Bethany Collins", median: 2, total_tickets: 1, name: "Bethany Collins" },
      { key: "Harsh Jariwala", median: 1, total_tickets: 94, name: "Harsh Jariwala" }
    ];

    expect(helperFunctions.getFilteredData(mock_data, mock_filters)).toStrictEqual(mock_response);
  });
});

describe("onFilterChange Test Suite", () => {
  test("setFilters should be called", () => {
    const setFiltersMock = jest.fn();
    helperFunctions.onFilterChange("new_filter", false, {}, setFiltersMock, "122k3j-12mi2");
    expect(setFiltersMock).toHaveBeenCalled();
  });
});

describe("getTreeMapItemColor Test Suite", () => {
  test("it should return correct value", () => {
    expect(helperFunctions.getTreeMapItemColor(1)).toEqual("#0073CF");
    expect(helperFunctions.getTreeMapItemColor(255)).toEqual("#66D9FF");
    expect(helperFunctions.getTreeMapItemColor(51)).toEqual("#66D9FF");
    expect(helperFunctions.getTreeMapItemColor(0)).toEqual("");
  });
});

describe("getMappedSortValue Test Suite", () => {
  test("it should return correct value", () => {
    expect(helperFunctions.getMappedSortValue("test")).toEqual("test");
    expect(helperFunctions.getMappedSortValue("changes")).toEqual("total_changes");
  });
});

describe("onChartClick Test Suite", () => {
  test("onClick should be called once when reportType is levelops ", () => {
    const mock_data = { activePayload: [{ payload: {} }] };
    const onClickMock = jest.fn();
    const mock_props = { onClick: onClickMock, hasClickEvents: true, reportType: "levelops" };
    const mock_types = 1;
    helperFunctions.onChartClick(mock_data, mock_props, mock_types);
    expect(onClickMock).toHaveBeenCalled();
  });
});

describe("getNumberAbbreviation Test Suite", () => {
  test("it should return correct number abbreviations", () => {
    expect(helperFunctions.getNumberAbbreviation(1)).toEqual("1.00");
    expect(helperFunctions.getNumberAbbreviation(10)).toEqual("10.00");
    expect(helperFunctions.getNumberAbbreviation(100)).toEqual("100.00");
    expect(helperFunctions.getNumberAbbreviation(1000)).toEqual("1.0K");
    expect(helperFunctions.getNumberAbbreviation(1000000)).toEqual("1.0M");
    expect(helperFunctions.getNumberAbbreviation(100000000)).toEqual("100.0M");
  });
});
