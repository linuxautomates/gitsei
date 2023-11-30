import * as React from "react";
import { mockTestStore } from "../../../utils/testUtils";
import { testRender } from "../../../shared-resources/components/testing/testing-react.wrapper";
import { SmartTicketTemplateList } from "../index";
import { RESTAPI_CLEAR, PAGINATION_GET } from "reduxConfigs/actions/actionTypes";
import * as restApiActions from "reduxConfigs/actions/restapi/restapiActions";
import * as paginationActions from "reduxConfigs/actions/paginationActions";
import { fireEvent } from "@testing-library/react";

// UI TESTS
describe("Smart Ticket Template List Page UI", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });

  test("Page should match the snapshot.", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };
    // @ts-ignore
    const { asFragment } = testRender(<SmartTicketTemplateList location={mockLocation} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });
});

// ON MOUNT ACTION DISPATCH TESTS
describe("Smart Ticket Template List Page Basic creation", () => {
  let store: any;
  const mockRestApiClearResponse: any = {
    type: RESTAPI_CLEAR
  };
  const mockPaginationGetResponse: any = {
    type: PAGINATION_GET
  };

  let restApiClearActionSpy: any;
  let paginationGetActionSpy: any;

  beforeEach(() => {
    store = mockTestStore();
    restApiClearActionSpy = jest.spyOn(restApiActions, "restapiClear").mockReturnValue(mockRestApiClearResponse);
    paginationGetActionSpy = jest.spyOn(paginationActions, "paginationGet").mockReturnValue(mockPaginationGetResponse);
  });

  test("Should dispatch correct actions when component mounts", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };
    // @ts-ignore
    testRender(<SmartTicketTemplateList location={mockLocation} />, {
      store
    });
    expect(store.dispatch).toHaveBeenCalledWith(mockRestApiClearResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockPaginationGetResponse);
    expect(store.dispatch).toBeCalledTimes(2);
  });
});

// ON ACTION BUTTON CLICK DISPATCH TESTS
describe("Smart Ticket Template List Page Should dispatch right actions on button clicks", () => {
  let store: any;
  const mockRestApiClearResponse: any = {
    type: RESTAPI_CLEAR
  };
  const mockPaginationGetResponse: any = {
    type: PAGINATION_GET
  };

  let restApiClearActionSpy: any;
  let paginationGetActionSpy: any;

  beforeEach(() => {
    store = mockTestStore();
    restApiClearActionSpy = jest.spyOn(restApiActions, "restapiClear").mockReturnValue(mockRestApiClearResponse);
    paginationGetActionSpy = jest.spyOn(paginationActions, "paginationGet").mockReturnValue(mockPaginationGetResponse);
  });

  test("Smart Ticket Template List should assert dispatch on Next Page click", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };

    const { getByTitle } = testRender(<SmartTicketTemplateList location={mockLocation} />, { store });
    const nextBtn = getByTitle("Next Page");
    fireEvent.click(nextBtn);
    expect(store.dispatch).toHaveBeenCalledWith(mockRestApiClearResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockPaginationGetResponse);
    expect(store.dispatch).toBeCalledTimes(2);
  });

  test("Smart Ticket Template List should assert dispatch on Previous Page click", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };

    const { getByTitle } = testRender(<SmartTicketTemplateList location={mockLocation} />, { store });
    const previousBtn = getByTitle("Next Page");
    fireEvent.click(previousBtn);
    expect(store.dispatch).toHaveBeenCalledWith(mockRestApiClearResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockPaginationGetResponse);
    expect(store.dispatch).toBeCalledTimes(2);
  });
});
