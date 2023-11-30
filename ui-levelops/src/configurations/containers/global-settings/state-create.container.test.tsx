import * as React from "react";
import { fireEvent } from "@testing-library/react";
import { mockTestStore } from "../../../utils/testUtils";
import { testRender } from "../../../shared-resources/components/testing/testing-react.wrapper";
import { StateCreateContainer } from "./state-create.container";

describe("GlobalSettingsCreate", () => {
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

  test("create state page should render correctly", () => {
    const { asFragment } = testRender(
      <StateCreateContainer form={testForm} onOk={jest.fn()} onCancel={jest.fn()} />,
      {
        store
      }
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test("State assert onCancel should be called on Cancel click", () => {
    const mockOnCancel = jest.fn();
    const { getByText } = testRender(
      <StateCreateContainer form={testForm} onOk={jest.fn()} onCancel={mockOnCancel} />,
      { store }
    );
    const cancelBtn = getByText("Cancel");
    fireEvent.click(cancelBtn);
    expect(mockOnCancel).toBeCalled();
  });

  test("State create button should disabled by default", () => {
    const mockOk = jest.fn();
    const { getByText } = testRender(
      <StateCreateContainer form={testForm} onOk={mockOk} onCancel={jest.fn()} />,
      {
        store
      }
    );
    const okButton = getByText("Save");
    fireEvent.click(okButton);
    expect(mockOk).toBeCalledTimes(0);
  });
});
