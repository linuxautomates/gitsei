import { fireEvent } from "@testing-library/react";
import { RestPropelNode } from "classes/RestPropel";
import * as React from "react";
import { testRender } from "shared-resources/components/testing/testing-react.wrapper";
import { mockTestStore } from "utils/testUtils";
import NodeDryRunModal from "../NodeDryRunModal";
import * as mockNode from "./mock-node.json";

describe("PropelNodeDryRunModal", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });

  test("NodeDryRunModal should match the snapshot", () => {
    const { asFragment } = testRender(<NodeDryRunModal node={{}} visible={true} onCancel={jest.fn()} />, { store });

    expect(asFragment()).toMatchSnapshot();
  });

  test("NodeDryRunModal should render the fields with mock node", () => {
    const restNode = new RestPropelNode(JSON.parse(JSON.stringify(mockNode)));
    const { getByText } = testRender(<NodeDryRunModal node={restNode} visible={true} onCancel={jest.fn()} />, {
      store
    });

    const table = getByText("Table");

    expect(table).toBeInTheDocument();
  });

  test("NodeDryRunModal should show the spinner after clicking on evaluate button", () => {
    const restNode = new RestPropelNode(JSON.parse(JSON.stringify(mockNode)));
    const { getByTestId, getByText } = testRender(
      <NodeDryRunModal node={restNode} visible={true} onCancel={jest.fn()} />,
      { store }
    );

    const evaluateBtn = getByText("Evaluate");
    fireEvent.click(evaluateBtn);

    const spinner = getByTestId("output-loading-spinner");

    expect(spinner).toBeInTheDocument();
  });
});
