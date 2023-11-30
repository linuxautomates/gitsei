import * as helperFunctions from "./helper";

describe("getHeaderBreadcrumbTo Test Suite", () => {
  test("it should return correct response when id === 'propels' ", () => {
    const mock_id = "propels";
    const mock_search = "tab=propels";
    const mock_pathroot = "0";
    const mock_pathnames = ["0"];
    const mock_index = 0;
    const mock_location = { search: "/propels" };
    const mock_response = `/${mock_pathroot}/${mock_pathnames.slice(0, mock_index + 1).join("/")}?${mock_search}`;

    expect(
      helperFunctions.getHeaderBreadcrumbTo(mock_id, mock_pathroot, mock_pathnames, mock_index, mock_location)
    ).toBe(mock_response);
  });
  test("it should return correct response when id !== 'propels' ", () => {
    const mock_id = "products";
    const mock_pathroot = "0";
    const mock_pathnames = ["0"];
    const mock_location = { search: "/xyz" };
    const mock_index = 0;
    const mock_response = `/${mock_pathroot}/${mock_pathnames.slice(0, mock_index + 1).join("/")}`;

    expect(
      helperFunctions.getHeaderBreadcrumbTo(mock_id, mock_pathroot, mock_pathnames, mock_index, mock_location)
    ).toBe(mock_response);
  });
});

describe("getHeaderBreadcrumbLabel Test Suite", () => {
  test("it should return correct response when path === 'propels' ", () => {
    const mock_path = "propels";
    const mock_location = { search: "propels" };
    const mock_label = "";
    const mock_response = "My Propels";

    expect(helperFunctions.getHeaderBreadcrumbLabel(mock_path, mock_location, mock_label)).toBe(mock_response);
  });
  test("it should return correct response when path !== 'propels' ", () => {
    const mock_path = "abc";
    const mock_location = { search: "xyz" };
    const mock_label = "LABEL";

    expect(helperFunctions.getHeaderBreadcrumbLabel(mock_path, mock_location, mock_label)).toBe(mock_label);
  });
});
