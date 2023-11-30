import * as React from "react";
import { fireEvent } from "@testing-library/react";
import { ApikeyCreateContainer } from "./apikey-create.container";
import { mockTestStore } from "../../../utils/testUtils";
import { apikeysCreate } from "reduxConfigs/actions/restapi";
import { RestApikey } from "../../../classes/RestApikey";
import { testRender } from "../../../shared-resources/components/testing/testing-react.wrapper";

describe("APIKeyCreatePage", () => {
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

  test("ApiKey create page should match the snapshot.", () => {
    const { asFragment } = testRender(<ApikeyCreateContainer form={testForm} onOk={jest.fn()} onCancel={jest.fn()} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });

  test("ApiKey assert onCancel should be called on Cancel click", () => {
    const mockOnCancel = jest.fn();
    const { getByText } = testRender(
      <ApikeyCreateContainer form={testForm} onOk={jest.fn()} onCancel={mockOnCancel} />,
      { store }
    );
    const cancelBtn = getByText("Cancel");
    fireEvent.click(cancelBtn);
    expect(mockOnCancel).toBeCalled();
  });

  test("ApiKey create button should disabled by default", () => {
    const mockOk = jest.fn();
    const { getByText } = testRender(<ApikeyCreateContainer form={testForm} onOk={mockOk} onCancel={jest.fn()} />, {
      store
    });
    const okButton = getByText("Create");
    fireEvent.click(okButton);
    expect(mockOk).toBeCalledTimes(0);
  });
});
