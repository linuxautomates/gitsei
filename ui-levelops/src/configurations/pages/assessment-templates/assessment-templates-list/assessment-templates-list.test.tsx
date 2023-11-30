import * as React from "react";
import { mockTestStore } from "../../../../utils/testUtils";
import { testRender } from "../../../../shared-resources/components/testing/testing-react.wrapper";
import AssessmentTemplatesListPage from "./assessment-templates-list.page";
// import * as axios from "axios";

// Mock out all top level functions, such as get, put, delete and post:
// jest.mock("axios");

describe("AssessmentTemplateList", () => {
  let store: any;
  let location: any;

  beforeAll(() => {
    store = mockTestStore();

    location = {
      pathname: `/admin/templates/assessment-templates`
    };
  });

  test("Assessment template list should match the snapshot.", () => {
    const { asFragment } = testRender(<AssessmentTemplatesListPage location={location} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });

  // test("AssessmentCopy button should copy the assessment template", () => {
  //    const { asFragment } = testRender(<AssessmentTemplatesListPage location={location}/>, {
  //        store
  //    });
  //
  //     console.log("[] fragment is ", asFragment());
  //
  //    expect(asFragment()).toMatchSnapshot();
  //
  // });
});
