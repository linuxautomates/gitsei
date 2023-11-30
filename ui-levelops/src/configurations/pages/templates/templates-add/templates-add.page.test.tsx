import * as React from "react";
import { fireEvent } from "@testing-library/react";
import { createMemoryHistory, createLocation } from "history";
import { mockTestStore } from "utils/testUtils";
import { testRender } from "shared-resources/components/testing/testing-react.wrapper";
import { TemplatesAddPage } from "./templates-add.page";

describe("TemplateAddPage", () => {
  let history: any;
  let location: any;
  let match: any;
  let testForm: any;
  let store: any;
  const path = "localhost:3000/#/admin/templates/communication-templates/create";

  beforeAll(() => {
    store = mockTestStore();
    history = createMemoryHistory();
    match = {
      isExact: false,
      path,
      url: path,
      params: {}
    };
    location = createLocation(match.url);
    testForm = {
      getFieldError: jest.fn(opts => (c: any) => c),
      isFieldTouched: jest.fn(opts => (c: any) => c),
      getFieldDecorator: jest.fn(opts => (c: any) => c),
      resetFields: jest.fn()
    };
  });

  test("Template add page should match the snapshot.", () => {
    const { asFragment } = testRender(
      <TemplatesAddPage form={testForm} history={history} location={location} match={match} />,
      {
        store
      }
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
