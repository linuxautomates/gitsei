import * as React from "react";
import { mockTestStore } from "../../../../utils/testUtils";
import { testRender } from "../../../../shared-resources/components/testing/testing-react.wrapper";
import { KBCreateEdit } from "../index";

describe("KB-Edit/Create Page", () => {
  let testForm: any;
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
    testForm = {
      getFieldError: jest.fn(opts => (c: any) => c),
      isFieldTouched: jest.fn(opts => (c: any) => c),
      getFieldDecorator: jest.fn(opts => (c: any) => c),
      resetFields: jest.fn()
    };
  });

  test("KB create-edit page should match the snapshot.", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: "random"
    };
    const { asFragment } = testRender(<KBCreateEdit location={mockLocation} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });

  describe("Functional Tests", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: null
    };

    test("Should have link as default selection", () => {
      const { getByText } = testRender(<KBCreateEdit location={mockLocation} />, {
        store
      });
      const typeField = getByText("LINK");
      expect(typeField).toBeDefined();
    });

    test("Should have have link value field, if type selected is Link", () => {
      const { getAllByText } = testRender(<KBCreateEdit location={mockLocation} />, {
        store
      });
      const typeFields = getAllByText("LINK");
      expect(typeFields.length).toBe(1);
    });

    test("Should have have name value field", () => {
      const { getAllByText } = testRender(<KBCreateEdit location={mockLocation} />, {
        store
      });
      const typeFields = getAllByText("Name");
      expect(typeFields.length).toBe(1);
    });
  });
});
