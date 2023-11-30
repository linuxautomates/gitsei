import { convertNumArrayToStringArray, divideArrayIntoChunks } from "./arrayUtils";
import { convertEpochToDate, getEndOfDayFromDate, getStartOfDayFromDate } from "./dateUtils";
import moment from "moment";
import { findSumInArray, getTruthyValues, isSanitizedValue, renameKey, sanitizeObject } from "./commonUtils";
import { transformFilters } from "./dashboardFilterUtils";
import { buildLink, removeEmptyIntegrations } from "./integrationUtils";
import { buildQueryParamsFromObject, parseQueryParamsIntoKeys } from "./queryUtils";
import {
  valuetoLabel,
  validateEmail,
  validateURL,
  validateIP,
  toTitleCase,
  stringTransform,
  insertAt,
  truncateAndEllipsis
} from "./stringUtils";
import { getColor, updatedAtColumn, nameColumn, actionsColumn, tagsColumn, tableCell } from "./tableUtils";
import { getLoading, getError, loadingStatus, getData, getStateLoadingStatus } from "./loadingUtils";
import { RestWidget, RestDashboard } from "classes/RestDashboards";
import { generateReport } from "./dashboardPdfUtils";

describe("ArrayUtils.ts Tests", () => {
  test("convertNumArrayToStringArray() should return string array", () => {
    const stringArray = convertNumArrayToStringArray([1, 2, 3, 4, 5]);
    expect(stringArray).toStrictEqual(["1", "2", "3", "4", "5"]);
  });

  test("convertNumArrayToStringArray() data-less test", () => {
    const stringArray = convertNumArrayToStringArray([]);
    expect(stringArray).toStrictEqual([]);
  });

  test("divideArrayIntoChunks() should return chunked array", () => {
    const chunkArray = divideArrayIntoChunks([1, 2, 3, 4, 5, 6, 7, 8], 4);
    expect(chunkArray).toStrictEqual([
      [1, 2, 3, 4],
      [5, 6, 7, 8]
    ]);
  });

  test("divideArrayIntoChunks() data-less test", () => {
    const chunkArray = divideArrayIntoChunks([], 4);
    expect(chunkArray).toStrictEqual([]);
  });
});

describe("dateUtils.ts Tests", () => {
  test("convertEpochToDate() test", () => {
    const epoch = 1611655965;
    const date = convertEpochToDate(epoch);
    expect(date).toBe("26/01/21");

    const date1 = convertEpochToDate(epoch, "DD/MM");
    expect(date1).toBe("26/01");

    const date2 = convertEpochToDate(epoch, "DD/MM/YY", true);
    expect(date2).toBe("26/01/21");

    const date3 = convertEpochToDate(epoch.toString(), "DD/MM/YY", true);
    expect(date3).toBe("26/01/21");
  });

  test("getStartOfDayFromDate() test", () => {
    const epoch = 1611655965;
    const now = moment(epoch);

    const date = getStartOfDayFromDate(now);
    expect(date).toBe(1535400);

    const now1 = moment(epoch);
    const date1 = getStartOfDayFromDate(now1, true);
    expect(date1).toBe(1535400);

    const date2 = getStartOfDayFromDate(null);
    expect(date2).toBe(0);

    const date3 = getStartOfDayFromDate(undefined);
    expect(date3).toBe(0);
  });

  test("getEndOfDayFromDate() test", () => {
    const epoch = 1611655965;
    const now = moment(epoch);

    const date = getEndOfDayFromDate(now);
    expect(date).toBe(1601999);

    const now1 = moment(epoch);
    const date1 = getEndOfDayFromDate(now1, true);
    expect(date1).toBe(1601999);

    const date2 = getEndOfDayFromDate(null);
    expect(date2).toBe(0);

    const date3 = getEndOfDayFromDate(undefined);
    expect(date3).toBe(0);
  });
});

describe("commonUtils.ts Tests", () => {
  const sumData = [{ count: 5 }, { count: 1 }, { count: 2 }];

  const objectData = {
    key1: "data",
    key2: "",
    key3: undefined,
    key4: [],
    key5: null
  };

  const emptySanitizeData = {
    key1: "",
    key2: undefined,
    key3: [],
    key4: null
  };

  test("findSumInArray() should return sum", () => {
    const sum = findSumInArray(sumData, "count");
    expect(sum).toBe(8);
  });

  test("findSumInArray() should return 0", () => {
    const sum = findSumInArray([], "count");
    expect(sum).toBe(0);

    const sum1 = findSumInArray([], "");
    expect(sum1).toBe(0);
  });

  test("sanitizeObject() should return data", () => {
    const sanitizedData = sanitizeObject(objectData);
    expect(sanitizedData).toStrictEqual({ key1: "data" });
  });

  test("sanitizeObject() should return empty object", () => {
    const sanitizedData = sanitizeObject(emptySanitizeData);
    expect(sanitizedData).toStrictEqual({});
  });

  test("renameKey() should return renamed object", () => {
    const renamedObject = renameKey(objectData, "key1", "dataKey");
    expect(renamedObject).toStrictEqual({ ...objectData, dataKey: objectData["key1"], key1: undefined });
  });

  test("renameKey() should return same object with undefined values", () => {
    const renamedObject = renameKey(objectData, "dataKey", "dataKey1");
    expect(renamedObject).toStrictEqual({ ...objectData, dataKey: undefined, dataKey1: undefined });
  });

  test("isSanitizedValue() should return true", () => {
    const testValue1 = 5;
    const testValue2 = 0;
    const testValue3 = false;
    const testValue4 = {};

    const isValue1 = isSanitizedValue(testValue1);
    expect(isValue1).toBeTruthy();

    const isValue2 = isSanitizedValue(testValue2);
    expect(isValue2).toBeTruthy();

    const isValue3 = isSanitizedValue(testValue3);
    expect(isValue3).toBeTruthy();

    const isValue4 = isSanitizedValue(testValue4);
    expect(isValue4).toBeTruthy();
  });

  test("isSanitizedValue() should return false", () => {
    const testValue1 = null;
    const testValue2 = "";
    const testValue3 = undefined;

    const isValue1 = isSanitizedValue(testValue1);
    expect(isValue1).toBeFalsy();

    const isValue2 = isSanitizedValue(testValue2);
    expect(isValue2).toBeFalsy();

    const isValue3 = isSanitizedValue(testValue3);
    expect(isValue3).toBeFalsy();
  });

  test("getTruthyValues() should return value", () => {
    const truthValues = getTruthyValues([1, 2, 3, 4, undefined, null]);
    expect(truthValues).toStrictEqual([1, 2, 3, 4]);

    const truthValues1 = getTruthyValues([1, 2, 3]);
    expect(truthValues1).toStrictEqual([1, 2, 3]);

    const truthValues2 = getTruthyValues([]);
    expect(truthValues2).toStrictEqual([]);
  });
});

describe("dashboardFiltersUtils.ts Tests", () => {
  test("transformFilters() should return jira filtered value", () => {
    const filterValue = {
      assignees: [1, 2],
      tags: [1, 2, 3],
      jira_component: [2, 3, 4]
    };

    const zendeskFilters = {
      zendesk_assignees: [1, 2],
      tags: [1, 2, 3]
    };

    const salesforceFilters = {
      salesforce_assignees: [1, 2],
      tags: [1, 2, 3]
    };

    const filterValueOutput = { assignees: [1, 2], tags: [1, 2, 3] };

    const filters = transformFilters(filterValue, "");
    expect(filters).toStrictEqual(filterValueOutput);

    const filters1 = transformFilters(filterValue, "any");
    expect(filters1).toStrictEqual(filterValueOutput);

    const filters2 = transformFilters(filterValue, null);
    expect(filters2).toStrictEqual(filterValueOutput);

    const filters3 = transformFilters(filterValue, "zendesk_");
    expect(filters3).toStrictEqual(filterValueOutput);

    const filters4 = transformFilters(filterValue, "salesforce_");
    expect(filters4).toStrictEqual(filterValueOutput);

    const filters5 = transformFilters(zendeskFilters, "zendesk_");
    expect(filters5).toStrictEqual(filterValueOutput);

    const filters6 = transformFilters(salesforceFilters, "salesforce_");
    expect(filters6).toStrictEqual(filterValueOutput);
  });

  test("transformFilters() should return empty object", () => {
    const filters = transformFilters({}, "");
    expect(filters).toStrictEqual({});
    const filters1 = transformFilters({}, "any");
    expect(filters1).toStrictEqual({});
    const filters3 = transformFilters({}, null);
    expect(filters3).toStrictEqual({});
  });
});

describe("integrationUtils.ts Tests", () => {
  test("buildLink() should return link", () => {
    const url = buildLink({}, "", "");
    expect(url).toBe(undefined);

    const url1 = buildLink({}, "https://jira.com", "any");
    expect(url1).toBe(undefined);

    const url2 = buildLink({}, "https://jira.com", "jira");
    expect(url2).toBe(`https://jira.com/browse/${{}}`);

    const url3 = buildLink("xyz", "https://jira.com", "jira");
    expect(url3).toBe("https://jira.com/browse/xyz");

    const url4 = buildLink({}, "https://github.com", "github");
    expect(url4).toBe(`https://github.com/${undefined}/commit/${undefined}`);

    const url5 = buildLink({ repo_cloud_id: 1234, commit_sha: 5678 }, "https://github.com", "github");
    expect(url5).toBe("https://github.com/1234/commit/5678");
  });

  test("removeEmptyIntegrations() should return value", () => {
    const integrationData = {
      id: 12345,
      application: "xyz",
      url: "https://xyz.com",
      username: "user_xyz",
      api_key: "dummy_api_key",
      metadata: {}
    };

    const integration = removeEmptyIntegrations(integrationData);
    expect(integration).toStrictEqual(integrationData);

    const integration1 = removeEmptyIntegrations({ ...integrationData, url: "" });
    const result1 = { ...integrationData };
    delete result1.url;
    expect(integration1).toStrictEqual(result1);

    const integration2 = removeEmptyIntegrations({ ...integrationData, id: "" });
    const result2 = { ...integrationData };
    delete result2.id;
    expect(integration2).toStrictEqual(result2);

    const integration3 = removeEmptyIntegrations({ ...integrationData, application: "" });
    const result3 = { ...integrationData };
    delete result3.application;
    expect(integration3).toStrictEqual(result3);

    const integration4 = removeEmptyIntegrations({});
    expect(integration4).toStrictEqual({});
  });
});

describe("queryUtils.js Tests", () => {
  const queryData = {
    id: 1234,
    assignees: [1, 3, 4],
    tags: []
  };

  const searchString = "?id=1&vanity_id=12k12k1&search=dashboard";

  test("buildQueryParamsFromObject() should return query", () => {
    const query = buildQueryParamsFromObject({});
    expect(query).toBe("");

    const query1 = buildQueryParamsFromObject(queryData);
    expect(query1).toBe("id=1234&assignees=1%2C3%2C4&tags=");
  });

  test("buildQueryParamsFromObject() should remove the empty keys from query", () => {
    const query = buildQueryParamsFromObject({ ...queryData, search: "" });
    expect(query).toBe("id=1234&assignees=1%2C3%2C4&tags=");
  });

  test("parseQueryParamsIntoKeys() should return value", () => {
    const query = parseQueryParamsIntoKeys(searchString, ["id", "search"]);
    expect(query).toStrictEqual({ id: ["1"], search: ["dashboard"] });
  });

  test("parseQueryParamsIntoKeys() should return empty object if now match found", () => {
    const query = parseQueryParamsIntoKeys(searchString, ["name"]);
    expect(query).toStrictEqual({});

    const query1 = parseQueryParamsIntoKeys(searchString, undefined);
    expect(query1).toStrictEqual({});

    const query2 = parseQueryParamsIntoKeys("", undefined);
    expect(query2).toStrictEqual({});

    const query3 = parseQueryParamsIntoKeys("", ["id"]);
    expect(query3).toStrictEqual({});
  });
});

describe("stringUtils.js Tests", () => {
  test("valueToLabel() should return value", () => {
    const label = valuetoLabel("label");
    expect(label).toBe("LABEL");

    const label1 = valuetoLabel("label_value");
    expect(label1).toBe("LABEL VALUE");

    const label2 = valuetoLabel("");
    expect(label2).toBe("");
  });

  test("validateEmail() should return true", () => {
    const email = validateEmail("test@gamil.com");
    expect(email).toBeTruthy();

    const email1 = validateEmail("t@ab.xy");
    expect(email1).toBeTruthy();
  });

  test("validateEmail() should return false", () => {
    const email = validateEmail("testing");
    expect(email).toBeFalsy();

    const email1 = validateEmail("");
    expect(email1).toBeFalsy();
  });

  test("validateURL() should return true", () => {
    const url = validateURL("https://levelops.io");
    expect(url).toBeTruthy();

    const url1 = validateURL("https://api.levelops.io");
    expect(url1).toBeTruthy();

    const url2 = validateURL("https://subdomain.main.domain");
    expect(url2).toBeTruthy();

    const url3 = validateURL("levelops.io");
    expect(url3).toBeTruthy();
  });

  test("validateURL() should return false", () => {
    const url = validateURL("");
    expect(url).toBeFalsy();

    const url1 = validateURL(undefined);
    expect(url1).toBeFalsy();

    const url2 = validateURL(null);
    expect(url2).toBeFalsy();
  });

  test("validateIP() should return true", () => {
    const ip = validateIP("192.168.1.1");
    expect(ip).toBeTruthy();

    const ip1 = validateIP("127.0.0.1");
    expect(ip1).toBeTruthy();
  });

  test("validateIP() should return false", () => {
    const ip = validateIP("");
    expect(ip).toBeFalsy();

    const ip1 = validateIP(undefined);
    expect(ip1).toBeFalsy();

    const ip2 = validateIP("192.256.0.1");
    expect(ip2).toBeFalsy();
  });

  test("toTitleCase() should return value", () => {
    const text = toTitleCase("test input");
    expect(text).toBe("Test Input");

    const text1 = toTitleCase("test_input");
    expect(text1).toBe("Test Input");

    const text2 = toTitleCase("test");
    expect(text2).toBe("Test");

    const text3 = toTitleCase("lengthy test input");
    expect(text3).toBe("Lengthy Test Input");

    const text4 = toTitleCase("");
    expect(text4).toBe("");
  });

  test("toTitleCase() should return undefined", () => {
    const text = toTitleCase(null);
    expect(text).toBe(undefined);

    const text1 = toTitleCase(undefined);
    expect(text1).toBe(undefined);
  });

  test("stringTransform() should return value", () => {
    const text = stringTransform("test input");
    expect(text).toBe("Test input");

    const text1 = stringTransform("test_input");
    expect(text1).toBe("TestInput");

    const text2 = stringTransform("test_input", "_", " ");
    expect(text2).toBe("Test Input");

    const text3 = stringTransform("test_input", "", "");
    expect(text3).toBe("TEST_INPUT");

    const text4 = stringTransform("test_input", " ", "");
    expect(text4).toBe("Test_input");

    const text5 = stringTransform("test_input", "_", " ", false);
    expect(text5).toBe("Test Input");

    const text6 = stringTransform("TEST_INPUT", "_", " ");
    expect(text6).toBe("Test Input");

    const text7 = stringTransform("TEST_INPUT", "_", " ", false);
    expect(text7).toBe("TEST INPUT");
  });

  test("stringTransform() should return empty string", () => {
    const text = stringTransform("");
    expect(text).toBe("");

    const text1 = stringTransform(undefined);
    expect(text1).toBe("");

    const text2 = stringTransform(null);
    expect(text2).toBe("");
  });

  test("insertAt() should return value", () => {
    const text = insertAt("leelops", "v", 2);
    expect(text).toBe("levelops");

    const text1 = insertAt("llops", "eve", 1);
    expect(text1).toBe("levelops");

    const text2 = insertAt("levelops", "", 9);
    expect(text2).toBe("levelops");

    const text3 = insertAt("", "levelops", 5);
    expect(text3).toBe("levelops");
  });

  test("insertAt() should return empty string", () => {
    const text = insertAt("", "", 0);
    expect(text).toBe("");

    const text1 = insertAt(null, "hello", 10);
    expect(text1).toBe("");

    const text2 = insertAt(undefined, "", 0);
    expect(text2).toBe("");
  });

  test("truncateAndEllipsis() should return value", () => {
    const text = truncateAndEllipsis("This is a very very long text");
    expect(text).toBe("This is a very ...");

    const text1 = truncateAndEllipsis("This is a text", 4);
    expect(text1).toBe("This...");
  });

  test("truncateAndEllipsis() should return empty string", () => {
    const text = truncateAndEllipsis("");
    expect(text).toBe("");

    const text1 = truncateAndEllipsis(undefined);
    expect(text1).toBe("");

    const text2 = truncateAndEllipsis(null);
    expect(text2).toBe("");
  });
});

describe("tableUtils.js Tests", () => {
  test("getColor() should return values", () => {
    const color = getColor("tag1");
    expect(color).toBe("red");

    const color1 = getColor("tag2");
    expect(color1).toBe("blue");

    const color2 = getColor("tag3");
    expect(color2).toBe("green");

    const color3 = getColor("tag4");
    expect(color3).toBe("orange");

    const color4 = getColor("tag5");
    expect(color4).toBe("purple");

    const color5 = getColor("tag6");
    expect(color5).toBe("cyan");

    const _color1 = getColor("tag2");
    expect(_color1).toBe("blue");

    const _color5 = getColor("tag6");
    expect(_color5).toBe("cyan");
  });

  test("updatedAtColumn() should return value", () => {
    const epoch = 1611655965;
    const column = updatedAtColumn();
    expect(column.render(epoch)).toBe("1/26/2021");
  });

  test("updatedAtColumn() should return invalid date", () => {
    const column = updatedAtColumn();
    expect(column.render("testing")).toBe("Invalid Date");
  });

  test("nameColumn() should return value", () => {
    const column = nameColumn();
    expect(column.render("Test", { id: "test" }, 0)).toMatchSnapshot();
  });

  test("actionColumn() should return value", () => {
    const column = actionsColumn();
    expect(column).toStrictEqual({
      title: "Actions",
      key: "id",
      dataIndex: "id",
      width: 100,
      align: "center",
      fixed: "right"
    });
  });

  test("tagsColumn() should return value", () => {
    const column = tagsColumn();
    expect(column.render(["tag1", "tag2", "tag3"])).toMatchSnapshot();

    expect(column.render("tag1")).toBe("tag1");

    expect(column.render("")).toBe("");

    expect(column.render(["tag1"])).toMatchSnapshot();
  });

  test("tableCell() should return value", () => {
    const cell = tableCell("default", null);
    expect(cell).toBe("");

    const cell1 = tableCell("default", true);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("default", false);
    expect(cell2).toBe("");

    const cell3 = tableCell("no match header", "any value");
    expect(cell3).toBe("any value");
  });

  test("tableCell() test for libraries header", () => {
    const cell = tableCell("libraries", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("libraries", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("libraries", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("libraries", "singleValue");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("libraries", ["value1", "value2", "value3", "value4"]);
    expect(cell4).toMatchSnapshot();
  });

  test("tableCell() test for frameworks header", () => {
    const cell = tableCell("frameworks", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("frameworks", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("frameworks", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("frameworks", "singleValue");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("frameworks", ["value1", "value2", "value3", "value4"]);
    expect(cell4).toMatchSnapshot();
  });

  test("tableCell() test for files header", () => {
    const cell = tableCell("files", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("files", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("files", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("files", "singleValue");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("files", ["value1", "value2", "value3", "value4"]);
    expect(cell4).toMatchSnapshot();
  });

  test("tableCell() test for link header", () => {
    const cell = tableCell("link", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("link", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("link", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("link", "google.com");
    expect(cell3).toMatchSnapshot();
  });

  test("tableCell() test for file_changes_count header", () => {
    const cell = tableCell("file_changes_count", "", "", {});
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("file_changes_count", "");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("file_changes_count", 9, "google.com", { id: 4, name: "tet" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for configs_count header", () => {
    const cell = tableCell("configs_count", "", "", {});
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("configs_count", "");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("configs_count", 9, "google.com", { id: 4, name: "tet" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for files_ct header", () => {
    const cell = tableCell("files_ct", "", "", {});
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("files_ct", "");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("files_ct", 9, "google.com", { id: 4, name: "tet" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for configs_ct header", () => {
    const cell = tableCell("configs_ct", "", "", {});
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("configs_ct", "");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("configs_ct", 9, "google.com", { id: 4, name: "tet" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for name header", () => {
    const cell = tableCell("name", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("name", null);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("name", "Test", "google.com/names");
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("name", "Test");
    expect(cell3).toMatchSnapshot();
  });

  test("tableCell() test for changes header", () => {
    const cell = tableCell("changes", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("changes", null);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("changes", { id: 101, name: "test", key1: "Value 1", key2: "Value 2" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for security_work header", () => {
    const cell = tableCell("security_work", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("security_work", null);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("security_work", { id: 101, name: "test", key1: "Value 1", key2: "Value 2" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for tickets_by_stage header", () => {
    const cell = tableCell("tickets_by_stage", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("tickets_by_stage", null);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("tickets_by_stage", { id: 101, name: "test", key1: "Value 1", key2: "Value 2" });
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for repos header", () => {
    const cell = tableCell("repos", "");
    expect(cell).toBe("");

    const cell2 = tableCell("repos", null);
    expect(cell2).toBe("");

    const cell3 = tableCell("repos", []);
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("repos", "singleValue");
    expect(cell4).toBe("singleValue");

    const cell21 = tableCell("repos", ["value 1", "value 2", "value 3", "value 4"]);
    expect(cell21).toMatchSnapshot();
  });

  test("tableCell() test for severity header", () => {
    const cell = tableCell("severity", "");
    expect(cell).toBe("");

    const cell1 = tableCell("severity", "high");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("severity", "High");
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("severity", "HIGH");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("severity", "low");
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("severity", "medium");
    expect(cell5).toMatchSnapshot();
  });

  test("tableCell() test for priority header", () => {
    const cell = tableCell("priority", "");
    expect(cell).toBe("");

    const cell1 = tableCell("priority", "high");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("priority", "High");
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("priority", "HIGH");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("priority", "low");
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("priority", "medium");
    expect(cell5).toMatchSnapshot();

    const cell6 = tableCell("priority", "highest");
    expect(cell6).toMatchSnapshot();
  });

  test("tableCell() test for actions/technologies header", () => {
    const cell = tableCell("actions", "");
    expect(cell).toBe("");

    const cell1 = tableCell("actions", "some action");
    expect(cell1).toBe("some action");

    const cell2 = tableCell("actions", null);
    expect(cell2).toBe("");

    const cell3 = tableCell("actions", undefined);
    expect(cell3).toBe(undefined);

    const cell4 = tableCell("technologies", "");
    expect(cell4).toBe("");

    const cell5 = tableCell("technologies", "some technology");
    expect(cell5).toBe("some technology");

    const cell6 = tableCell("technologies", null);
    expect(cell6).toBe("");

    const cell7 = tableCell("technologies", undefined);
    expect(cell7).toBe(undefined);
  });

  test("tableCell() test for due_date header", () => {
    const epoch = 1611655965;
    const cell = tableCell("due_date", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("due_date", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("due_date", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("due_date", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for due_at header", () => {
    const epoch = 1611655965;
    const cell = tableCell("due_at", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("due_at", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("due_at", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("due_at", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for due_date header", () => {
    const epoch = 1611655965;
    const cell = tableCell("release_date", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("release_date", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("release_date", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("release_date", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for created_at header", () => {
    const epoch = 1611655965;
    const cell = tableCell("created_at", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("created_at", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("created_at", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("created_at", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for updated_on header", () => {
    const epoch = 1611655965;
    const cell = tableCell("updated_on", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("updated_on", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("updated_on", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("updated_on", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for last_updated header", () => {
    const epoch = 1611655965;
    const cell = tableCell("last_updated", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("last_updated", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("last_updated", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("last_updated", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for timestamp header", () => {
    const epoch = 1611655965;
    const cell = tableCell("timestamp", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("timestamp", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("timestamp", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("timestamp", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for created_at_epoch header", () => {
    const epoch = 1611655965;
    const cell = tableCell("created_at_epoch", epoch);
    expect(cell).toBe("1/26/2021");

    const cell1 = tableCell("created_at_epoch", "some random string");
    expect(cell1).toBe("Invalid Date");

    const cell2 = tableCell("created_at_epoch", 0);
    expect(cell2).toBe("");

    const cell3 = tableCell("created_at_epoch", undefined);
    expect(cell3).toBe("");
  });

  test("tableCell() test for quiz_progress header", () => {
    const cell = tableCell("quiz_progress", 0);
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("quiz_progress", 15);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("quiz_progress", 45);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("quiz_progress", 85);
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("quiz_progress", 100);
    expect(cell4).toMatchSnapshot();
  });

  test("tableCell() test for progress header", () => {
    const value = { score: 50, SAST: 20, DAST: 30, PEN: 40, Vulnerabilities: 50 };
    const cell = tableCell("progress", value);
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("progress", { ...value, score: 0 });
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("progress", { ...value, SAST: 0 });
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("progress", { ...value, DAST: 0 });
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("progress", { ...value, PEN: 0 });
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("progress", { ...value, Vulnerabilities: 0 });
    expect(cell5).toMatchSnapshot();
  });

  test("tableCell() test for enabled header", () => {
    const cell = tableCell("enabled", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("enabled", 50);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("enabled", "50");
    expect(cell2).toMatchSnapshot();
  });

  test("tableCell() test for integration_type header", () => {
    const cell = tableCell("integration_type", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("integration_type", "some_integration");
    expect(cell1).toMatchSnapshot();

    const cell3 = tableCell("integration_type", null);
    expect(cell3).toBe("");
  });

  test("tableCell() test for integration_types header", () => {
    const cell = tableCell("integration_types", "");
    expect(cell).toBe("");

    const cell1 = tableCell("integration_types", undefined);
    expect(cell1).toBe(undefined);

    const cell2 = tableCell("integration_types", null);
    expect(cell2).toBe("");

    const cell3 = tableCell("integration_types", ["github", "gitlab"]);
    expect(cell3).toMatchSnapshot();
  });

  test("tableCell() test for products header", () => {
    const cell = tableCell("products", "");
    expect(cell).toBe("");

    const cell1 = tableCell("products", undefined);
    expect(cell1).toBe(undefined);

    const cell2 = tableCell("products", null);
    expect(cell2).toBe("");

    const cell3 = tableCell("products", ["product 1", "product 2"]);
    expect(cell3).toMatchSnapshot();
  });

  test("tableCell() test for status header", () => {
    const cell = tableCell("status", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("status", "success");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("status", "OPEN");
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("status", "open");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("status", "ACTIVE");
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("status", "PASSED");
    expect(cell5).toMatchSnapshot();

    const cell6 = tableCell("status", "SUCCESS");
    expect(cell6).toMatchSnapshot();

    const cell7 = tableCell("status", "failure");
    expect(cell7).toMatchSnapshot();

    const cell8 = tableCell("status", "CLOSED");
    expect(cell8).toMatchSnapshot();

    const cell9 = tableCell("status", "closed");
    expect(cell9).toMatchSnapshot();

    const cell10 = tableCell("status", "INACTIVE");
    expect(cell10).toMatchSnapshot();

    const cell11 = tableCell("status", "FAILED");
    expect(cell11).toMatchSnapshot();

    const cell12 = tableCell("status", "FAILURE");
    expect(cell12).toMatchSnapshot();

    const cell13 = tableCell("status", "running");
    expect(cell13).toMatchSnapshot();

    const cell14 = tableCell("status", "IN_PROGRESS");
    expect(cell14).toMatchSnapshot();

    const cell15 = tableCell("status", "some random string");
    expect(cell15).toMatchSnapshot();
  });

  test("tableCell() test for status_badge header", () => {
    const cell = tableCell("status_badge", "success");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("status_badge", "OPEN");
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("status_badge", "ACTIVE");
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("status_badge", "NEW");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("status_badge", "failure");
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("status_badge", "CLOSED");
    expect(cell5).toMatchSnapshot();

    const cell6 = tableCell("status_badge", "INACTIVE");
    expect(cell6).toMatchSnapshot();

    const cell7 = tableCell("status_badge", "running");
    expect(cell7).toMatchSnapshot();

    const cell8 = tableCell("status_badge", "IN_PROGRESS");
    expect(cell8).toMatchSnapshot();

    const cell9 = tableCell("status_badge", "some other string");
    expect(cell9).toMatchSnapshot();
  });

  test("tableCell() test for successful header", () => {
    const cell = tableCell("successful", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("successful", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("successful", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("successful", "any value");
    expect(cell3).toMatchSnapshot();
  });

  test("tableCell() test for user header", () => {
    const cell = tableCell("user", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("user", undefined);
    expect(cell1).toBe(undefined);

    const cell2 = tableCell("user", null);
    expect(cell2).toBe("");

    const cell3 = tableCell("user", "user name");
    expect(cell3).toMatchSnapshot();
  });

  test("tableCell() test for users header", () => {
    const cell = tableCell("users", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("users", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("users", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("users", "some user");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("users", []);
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("users", ["user 1", "user 2"]);
    expect(cell5).toMatchSnapshot();
  });

  test("tableCell() test for time_utc_f1/time_utc_f2", () => {
    const cell = tableCell("time_utc_f1", "1/26/2021");
    expect(cell).toBe(moment("1/26/2021").utc().format("DD/MM/YYYY"));

    const cell1 = tableCell("time_utc_f1", "invalid input");
    expect(cell1).toBe("Invalid date");

    const cell2 = tableCell("time_utc_f2", "1/26/2021");
    expect(cell2).toBe(moment("1/26/2021").utc().format("YYYY-MM-DD HH:mm:ss"));

    const cell3 = tableCell("time_utc_f2", "invalid input");
    expect(cell3).toBe("Invalid date");
  });

  test("tableCell() test for tags_withColor header", () => {
    const cell = tableCell("tags_withColor", "");
    expect(cell).toMatchSnapshot();

    const cell1 = tableCell("tags_withColor", undefined);
    expect(cell1).toMatchSnapshot();

    const cell2 = tableCell("tags_withColor", null);
    expect(cell2).toMatchSnapshot();

    const cell3 = tableCell("tags_withColor", "some user");
    expect(cell3).toMatchSnapshot();

    const cell4 = tableCell("tags_withColor", []);
    expect(cell4).toMatchSnapshot();

    const cell5 = tableCell("tags_withColor", ["tag 1", "tag 2"]);
    expect(cell5).toMatchSnapshot();
  });
});

describe("loadingUtils.js Tests", () => {
  const store = {
    dashboards: {
      list: {
        "0": {
          records: []
        }
      },
      get: {
        "12345": {
          loading: false,
          error: false,
          data: {
            id: "12345",
            name: "test dashboard"
          }
        }
      }
    }
  };

  test("getLoading() test", () => {
    const loading = getLoading("", "dashboards", "get");
    expect(loading).toBe(true);

    const loading1 = getLoading("", "dashboards", "get", "1234");
    expect(loading1).toBe(true);

    const loading2 = getLoading(store, "dashboards", "get");
    expect(loading2).toBe(true);

    const loading3 = getLoading(store, "dashboards", "list");
    expect(loading3).toBe(true);

    const loading4 = getLoading(store, "dashboards", "get", "12345");
    expect(loading4).toBe(false);
  });

  test("getError() test", () => {
    const error = getError("", "dashboards", "get");
    expect(error).toBe(false);

    const error1 = getError("", "dashboards", "get", "1234");
    expect(error1).toBe(false);

    const error2 = getError(store, "dashboards", "get");
    expect(error2).toBe(true);

    const error3 = getError(store, "dashboards", "list");
    expect(error3).toBe(true);

    const error4 = getError(store, "dashboards", "get", "12345");
    expect(error4).toBe(false);
  });

  test("loadingStatus() test", () => {
    const { loading, error } = loadingStatus("", "dashboards", "list");
    expect(loading).toBe(true);
    expect(error).toBe(false);

    const { loading: loading1, error: error1 } = loadingStatus("", "dashboards", "list", "1234");
    expect(loading1).toBe(true);
    expect(error1).toBe(false);

    const { loading: loading2, error: error2 } = loadingStatus(store, "dashboards", "get");
    expect(loading2).toBe(true);
    expect(error2).toBe(true);

    const { loading: loading3, error: error3 } = loadingStatus(store, "dashboards", "list");
    expect(loading3).toBe(true);
    expect(error3).toBe(true);

    const { loading: loading4, error: error4 } = loadingStatus(store, "dashboards", "get", "12345");
    expect(loading4).toBe(false);
    expect(error4).toBe(false);
  });

  test("getData() test", () => {
    const data = getData("", "dashboards", "get");
    expect(data).toStrictEqual({});

    const data1 = getData("", "dashboards", "get", "1234");
    expect(data1).toStrictEqual({});

    const data2 = getData(store, "dashboards", "get");
    expect(data2).toStrictEqual({});

    const data3 = getData(store, "dashboards", "get", "12345");
    expect(data3).toStrictEqual({ id: "12345", name: "test dashboard" });
  });

  test("getStateLoadingStatus() test", () => {
    const { loading, error } = getStateLoadingStatus("", "dashboards", "list");
    expect(loading).toBe(true);
    expect(error).toBe(true);

    const { loading: loading1, error: error1 } = getStateLoadingStatus("", "list", "1234");
    expect(loading1).toBe(true);
    expect(error1).toBe(true);

    const { loading: loading2, error: error2 } = getStateLoadingStatus(store.dashboards, "get");
    expect(loading2).toBe(true);
    expect(error2).toBe(true);

    const { loading: loading3, error: error3 } = getStateLoadingStatus(store.dashboards, "list");
    expect(loading3).toBe(true);
    expect(error3).toBe(true);

    const { loading: loading4, error: error4 } = getStateLoadingStatus(store.dashboards, "get", "12345");
    expect(loading4).toBe(false);
    expect(error4).toBe(false);
  });
});

describe("dashboardPdfUtils.ts", () => {
  const widget = new RestWidget({
    name: "widget 1",
    type: "job_runs_test_report",
    query: { across: "test_suite", job_statuses: ["UNSTABLE"] }
  });
  const dashboard = new RestDashboard({ name: "test dashboard", owner_id: "3", widgets: [widget] });
  const widgetSvg = [
    {
      "92a74d80-53c6-11eb-9f08-d1193fcc86a1":
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAosAQo8CwoUvF8sstU76z+P8BtMecGvZ3oLwAAAAASUVORK5CYII="
    }
  ];
  generateReport(dashboard, [widget], widgetSvg);
});
