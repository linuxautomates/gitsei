import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import OrgTree from "../OrgTree";
import { TreeSelect } from "antd";

describe("ORG TREE COMPONENT", () => {
  let props = {
    dataSource: [
      {
        key: "1",
        value: "1",
        title: "OU 1",
        label: "OU 1",
        children: []
      },
      {
        key: "2",
        value: "2",
        title: "OU 2",
        label: "OU 2",
        children: []
      },
      {
        key: "3",
        value: "3",
        title: "OU 3",
        label: "OU 3",
        children: []
      }
    ],
    onCheck: (rows: any) => {},
    notFoundContent: null,
    selectedRowsKeys: [],
    label: "name"
  };
  test("Org tree should be should rendered correctly", async () => {
    const { container } = render(<OrgTree key={"1"} {...props} />);
    expect(screen.getByRole("textbox")).toBeTruthy();
  });

  test("Org tree dropdown should be visible", async () => {
    const { container, debug } = render(<OrgTree key={"1"} {...props} />);
    act(() => {
      fireEvent.click(container.querySelector(".org-tree")!, {});
    });
    const items = await screen.getAllByRole("treeitem");
    expect(items?.length).toEqual(props.dataSource?.length);
  });
});
