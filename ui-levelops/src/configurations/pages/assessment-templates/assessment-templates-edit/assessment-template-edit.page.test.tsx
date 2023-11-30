import * as React from "react";
import { fireEvent } from "@testing-library/react";
import { mockTestStore } from "../../../../utils/testUtils";
import { testRender } from "../../../../shared-resources/components/testing/testing-react.wrapper";
import AT from "./assessment-template-edit.page";

describe("AssessmentTemplateEditAdd", () => {
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });

  test("Assessment template edit page should match the snapshot.", () => {
    const { asFragment } = testRender(<AT />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });

  test("Add section button should add new section.", () => {
    store = {
      ...store,
      location: {
        pathname: "/admin/templates/assessment-templates/create"
      }
    };
    const { asFragment, getByPlaceholderText, getByText } = testRender(<AT />, {
      store
    });

    // // settings template name
    const tempNameField = getByPlaceholderText("New Assessment Template");
    fireEvent.change(tempNameField, { target: { value: "test template" } });

    // closing the setting popup
    const okButton = getByText("Ok");
    fireEvent.click(okButton);

    const btn = getByText("Add Section"); // button when no section is available
    fireEvent.click(btn);

    getByText("Add question to your section"); // button present when we can add new questions

    expect(asFragment()).toMatchSnapshot();
  });

  test("Add question to your section button should add new question", () => {
    store = {
      ...store,
      location: {
        pathname: "/admin/templates/assessment-templates/create"
      }
    };
    const { asFragment, getByPlaceholderText, getByText } = testRender(<AT />, {
      store
    });

    // // settings template name
    const tempNameField = getByPlaceholderText("New Assessment Template");
    fireEvent.change(tempNameField, { target: { value: "test template" } });

    // closing the setting popup
    const okButton = getByText("Ok");
    fireEvent.click(okButton);

    const btn = getByText("Add Section"); // button when no section is available
    fireEvent.click(btn);

    const newBtn = getByText("Add question to your section"); // button present when we can add new questions
    fireEvent.click(newBtn); // opening the question type dropdown

    const checkBoxOption = getByText("Choice - Check Boxes");
    fireEvent.click(checkBoxOption);
    getByText("Edit Question");
    fireEvent.click(getByText("OK"));
    getByText("Question 1");

    const radioOption = getByText("Choice - Radio Buttons");
    fireEvent.click(radioOption);
    fireEvent.click(getByText("OK"));
    getByText("Question 2");

    const multilineOption = getByText("Multi-Line Textbox");
    fireEvent.click(multilineOption);
    fireEvent.click(getByText("OK"));
    getByText("Question 3");

    const fileUploadOption = getByText("File Upload");
    fireEvent.click(fileUploadOption);
    fireEvent.click(getByText("OK"));
    getByText("Question 4");

    const yesNo = getByText("Yes/No");
    fireEvent.click(yesNo);
    fireEvent.click(getByText("OK"));
    getByText("Question 5");

    expect(asFragment()).toMatchSnapshot();
  });
});
