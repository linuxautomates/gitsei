import * as React from "react";
import { mockTestStore } from "utils/testUtils";
import { testRender } from "../../../components/testing/testing-react.wrapper";
import SelectRestAPI from "../select-restapi.helper";

describe("selectRestApiHelper", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });

  test("SelectRestApi should match snapshot", () => {
    const onChangeMock = jest.fn();
    const { asFragment } = testRender(
      <SelectRestAPI
        placeholder="Test"
        mode="single"
        labelInValue
        uri={"test"}
        searchField={"test_field"}
        onChange={onChangeMock}
      />,
      {
        store
      }
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
