import * as React from "react";
import { testRender } from "../../shared-resources/components/testing/testing-react.wrapper";
import { mockTestStore } from "../../utils/testUtils";
import { TriageDetail } from "./index";

describe("Triage detail page", () => {
  let store: any;
  let location: any;
  let restapiClear: any;
  let clearPageSettings: any;

  const props = {
    location: { pathname: "" },
    restapiClear: jest.fn(opts => (c: any) => c),
    clearPageSettings: jest.fn(opts => (c: any) => c),
    triageDetailGet: jest.fn(opts => (c: any) => c),
    triageStagesGet: jest.fn(opts => (c: any) => c),
    triageRuleResultsGet: jest.fn(opts => (c: any) => c),
    setPageSettings: jest.fn(opts => (c: any) => c),
    genericGet: jest.fn(opts => (c: any) => c)
  };

  beforeAll(() => {
    store = mockTestStore();
    location = { pathname: "" };
    restapiClear = jest.fn(opts => (c: any) => c);
    clearPageSettings = jest.fn(opts => (c: any) => c);
  });

  test("Stage details should match the snapshot", () => {
    const { asFragment } = testRender(<TriageDetail {...props} />, {
      store
    });

    expect(asFragment()).toMatchSnapshot();
  });
});
