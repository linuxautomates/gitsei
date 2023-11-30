import { fireEvent } from "@testing-library/react";
import * as React from "react";
import { testRender } from "shared-resources/components/testing/testing-react.wrapper";
import { mockTestStore } from "utils/testUtils";
import ConfigureCompositeWidgetModal from "../configureCompositeWidgetModel";

describe("WidgetConfigureModal", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });

  test("ConfigureModal should match the snapshot", () => {
    const { asFragment } = testRender(
      <ConfigureCompositeWidgetModal
        visible
        widgetData={{}}
        onOk={jest.fn()}
        onCancel={jest.fn()}
        onRemove={jest.fn()}
        setDirty={jest.fn()}
      />,
      {
        store
      }
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test("ConfigureModal Save Btn should be disabled by default ", () => {
    const mockOk = jest.fn();
    const { getByText } = testRender(
      <ConfigureCompositeWidgetModal visible setDirty={jest.fn()} widgetData={{}} onOk={mockOk} onCancel={jest.fn()} />,
      {
        store
      }
    );
    const okBtn = getByText("Save");
    fireEvent.click(okBtn);
    expect(mockOk).toBeCalledTimes(0);
  });

  test("ConfigureModal Single graph type should be selected by default ", () => {
    const { getAllByRole } = testRender(
      <ConfigureCompositeWidgetModal
        visible
        setDirty={jest.fn()}
        widgetData={{}}
        onOk={jest.fn()}
        onCancel={jest.fn()}
      />,
      {
        store
      }
    );
    const checkBoxes = getAllByRole("radio")[0];
    expect(checkBoxes).toHaveAttribute("checked");
  });
});
