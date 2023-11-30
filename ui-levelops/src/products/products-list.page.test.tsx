import * as React from "react";
import { mockTestStore } from "../utils/testUtils";
import ProductsListPage from "./products-list.page";
import { testRender } from "../shared-resources/components/testing/testing-react.wrapper";
import { RESTAPI_CLEAR, PAGINATION_GET } from "reduxConfigs/actions/actionTypes";
import { SET_PAGE_SETTINGS } from "reduxConfigs/actions/pagesettings.actions";
import * as restApiActions from "reduxConfigs/actions/restapi/restapiActions";
import * as paginationActions from "reduxConfigs/actions/paginationActions";
import * as pageSettingsActions from "reduxConfigs/actions/pagesettings.actions";
import { fireEvent } from "@testing-library/react";

// UI TESTS
describe("Products List Page UI", () => {
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
    const { asFragment } = testRender(<ProductsListPage location={mockLocation} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });
});

// ON MOUNT ACTION DISPATCH TESTSad
describe("Products List Page Basic creation", () => {
  let store: any;
  const mockRestApiClearResponse: any = {
    type: RESTAPI_CLEAR
  };
  const mockPaginationGetResponse: any = {
    type: PAGINATION_GET
  };
  const mockSetPageSettingsResponse: any = {
    type: SET_PAGE_SETTINGS
  };
  let restApiClearActionSpy: any;
  let paginationGetActionSpy: any;
  let setPageSettingsActionSpy: any;

  beforeEach(() => {
    store = mockTestStore();
    restApiClearActionSpy = jest.spyOn(restApiActions, "restapiClear").mockReturnValue(mockRestApiClearResponse);
    paginationGetActionSpy = jest.spyOn(paginationActions, "paginationGet").mockReturnValue(mockPaginationGetResponse);
    setPageSettingsActionSpy = jest
      .spyOn(pageSettingsActions, "setPageSettings")
      .mockReturnValue(mockSetPageSettingsResponse);
  });

  test("Should dispatch correct actions when component mounts", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };
    testRender(<ProductsListPage location={mockLocation} />, {
      store
    });
    expect(store.dispatch).toHaveBeenCalledWith(mockRestApiClearResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockPaginationGetResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockSetPageSettingsResponse);
    expect(store.dispatch).toBeCalledTimes(3);
  });
});

// ON ACTION BUTTON CLICK DISPATCH TESTS
describe("Products List Page Should dispatch right actions on button clicks", () => {
  let store: any;
  const mockRestApiClearResponse: any = {
    type: RESTAPI_CLEAR
  };
  const mockPaginationGetResponse: any = {
    type: PAGINATION_GET
  };
  const mockSetPageSettingsResponse: any = {
    type: SET_PAGE_SETTINGS
  };
  let restApiClearActionSpy: any;
  let paginationGetActionSpy: any;
  let setPageSettingsActionSpy: any;
  beforeEach(() => {
    store = mockTestStore();
    restApiClearActionSpy = jest.spyOn(restApiActions, "restapiClear").mockReturnValue(mockRestApiClearResponse);
    paginationGetActionSpy = jest.spyOn(paginationActions, "paginationGet").mockReturnValue(mockPaginationGetResponse);
    setPageSettingsActionSpy = jest
      .spyOn(pageSettingsActions, "setPageSettings")
      .mockReturnValue(mockSetPageSettingsResponse);
  });

  test("Products List should assert dispatch on Next Page click", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };
    const { getByTitle } = testRender(<ProductsListPage location={mockLocation} />, { store });
    const nextBtn = getByTitle("Next Page");
    fireEvent.click(nextBtn);
    expect(store.dispatch).toHaveBeenCalledWith(mockRestApiClearResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockPaginationGetResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockSetPageSettingsResponse);
    expect(store.dispatch).toBeCalledTimes(3);
  });

  test("Products List should assert dispatch on Previous Page click", () => {
    const mockLocation = {
      pathname: "dummyPathName",
      search: "random"
    };
    const { getByTitle } = testRender(<ProductsListPage location={mockLocation} />, { store });
    const previousBtn = getByTitle("Previous Page");
    fireEvent.click(previousBtn);
    expect(store.dispatch).toHaveBeenCalledWith(mockRestApiClearResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockPaginationGetResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockSetPageSettingsResponse);
    expect(store.dispatch).toBeCalledTimes(3);
  });
});
