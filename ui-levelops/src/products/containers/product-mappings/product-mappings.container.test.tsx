import * as productActions from "reduxConfigs/actions/restapi/productActions";
import * as mappingActions from "reduxConfigs/actions/restapi/mappingActions";
import * as integrationActions from "reduxConfigs/actions/restapi/integrationActions";
import { RESTAPI_READ } from "reduxConfigs/actions/actionTypes";
import { mockTestStore } from "../../../utils/testUtils";
import ProductMappingsContainer from "./product-mappings.container";
import { testRender } from "../../../shared-resources/components/testing/testing-react.wrapper";
import React from "react";

const productId = "random-product-id";

describe("Product Mapping Container UI", () => {
  let store: any;
  const mockProps = {
    product_id: productId,
    onUpdate: () => {},
    onCancel: () => {},
    display: true,
    location: {
      pathname: "dummypathName",
      search: "random"
    }
  };
  beforeAll(() => {
    store = mockTestStore();
  });
  test("Page should match the snapshot.", () => {
    const { asFragment } = testRender(<ProductMappingsContainer {...mockProps} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });
});

describe("Product Mapping Container Basic Creation", () => {
  let store: any;
  const mockGetProductResponse: any = {
    type: RESTAPI_READ
  };
  const mockMappingListResponse: any = {
    type: RESTAPI_READ
  };
  const mockIntegrationListResponse: any = {
    type: RESTAPI_READ
  };
  let getProductsSpy: any;
  let mappingListSpy: any;
  let integrationListSpy: any;
  beforeEach(() => {
    store = mockTestStore();
    getProductsSpy = jest.spyOn(productActions, "productsGet").mockReturnValue(mockGetProductResponse);
    mappingListSpy = jest.spyOn(mappingActions, "mappingsList").mockReturnValue(mockMappingListResponse);
    integrationListSpy = jest
      .spyOn(integrationActions, "integrationsList")
      .mockReturnValue(mockIntegrationListResponse);
  });
  test("Should dispatch correct actions when component mounts", () => {
    const mockProps = {
      product_id: productId,
      onUpdate: () => {},
      onCancel: () => {},
      display: true,
      location: {
        pathname: "dummypathName",
        search: "random"
      }
    };
    testRender(<ProductMappingsContainer {...mockProps} />, {
      store
    });
    expect(store.dispatch).toHaveBeenCalledWith(mockGetProductResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockMappingListResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockIntegrationListResponse);

    expect(getProductsSpy).toBeCalledWith(productId);
    expect(mappingListSpy).toBeCalledWith({ filter: { product_id: productId } });
  });
});
