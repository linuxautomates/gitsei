import * as React from "react";
import { fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { testRender } from "../../shared-resources/components/testing/testing-react.wrapper";
import { LoginPage } from "./LoginPage";

describe("LoginPage", () => {
  let session: any = {};

  beforeAll(() => {
    session = {
      sessionLoad: jest.fn(),
      sessionLogin: jest.fn(),
      session_token: "",
      session_error: "",
      session_default_route: "",
      history: []
    };
  });

  test("LoginPage should match the snapshot.", () => {
    const { asFragment } = testRender(<LoginPage {...session} />);

    expect(asFragment()).toMatchSnapshot();
  });

  test("On Login click without filling form should show error.", () => {
    const { getByText, getAllByText, getByTestId } = testRender(<LoginPage {...session} />);

    const loginButton = getByTestId("login");
    // clicking button without filling the form
    fireEvent.click(loginButton);

    // all three errors should be visible
    const generalFieldErrors = getAllByText("This field cannot be blank.");
    const emailFieldError = getByText("You must enter a valid email address.");

    expect(generalFieldErrors).toHaveLength(2);
    expect(emailFieldError).toBeInTheDocument();
    expect(generalFieldErrors[0]).toBeInTheDocument();
    expect(generalFieldErrors[1]).toBeInTheDocument();
  });

  test("Invalid email should show error.", () => {
    const { getByPlaceholderText, getByTestId, getByText } = testRender(<LoginPage {...session} />);

    const companyInput = getByPlaceholderText("Company");
    const emailInput = getByPlaceholderText("Enter Email");
    const passwordInput = getByPlaceholderText("Password");

    fireEvent.change(companyInput, { target: { value: "foo" } });
    fireEvent.change(emailInput, { target: { value: "foolevelops.io" } }); // invalid email
    fireEvent.change(passwordInput, { target: { value: "foolevelops" } });

    fireEvent.click(getByTestId("login"));

    expect(getByText("You must enter a valid email address.")).toBeInTheDocument();
  });

  test("If any one field is empty forget password click should show error.", () => {
    const { getByPlaceholderText, getByText } = testRender(<LoginPage {...session} />);

    const companyInput = getByPlaceholderText("Company");
    const emailInput = getByPlaceholderText("Enter Email");

    fireEvent.change(companyInput, { target: { value: "" } });
    fireEvent.change(emailInput, { target: { value: "" } });

    fireEvent.click(getByText("Forgot Password?"));

    expect(getByText("This field cannot be blank.")).toBeInTheDocument();
    expect(getByText("You must enter a valid email address.")).toBeInTheDocument();
  });

  test("Empty Company field should show error.", () => {
    const { getByPlaceholderText, getByText, getByTestId } = testRender(<LoginPage {...session} />);

    const companyInput = getByPlaceholderText("Company");
    const emailInput = getByPlaceholderText("Enter Email");
    const passwordInput = getByPlaceholderText("Password");

    fireEvent.change(companyInput, { target: { value: "" } });
    fireEvent.change(emailInput, { target: { value: "foo@levelops.io" } });
    fireEvent.change(passwordInput, { target: { value: "foolevelops" } });

    fireEvent.click(getByTestId("login"));

    expect(getByText("This field cannot be blank.")).toBeInTheDocument();
  });

  test("Empty Email field should show error.", () => {
    const { getByPlaceholderText, getByText, getByTestId } = testRender(<LoginPage {...session} />);

    const companyInput = getByPlaceholderText("Company");
    const emailInput = getByPlaceholderText("Enter Email");
    const passwordInput = getByPlaceholderText("Password");

    fireEvent.change(companyInput, { target: { value: "foo" } });
    fireEvent.change(emailInput, { target: { value: "" } });
    fireEvent.change(passwordInput, { target: { value: "foolevelops" } });

    fireEvent.click(getByTestId("login"));

    expect(getByText("You must enter a valid email address.")).toBeInTheDocument();
  });

  test("Empty Password field should show error.", () => {
    const { getByPlaceholderText, getByText, getByTestId } = testRender(<LoginPage {...session} />);

    const companyInput = getByPlaceholderText("Company");
    const emailInput = getByPlaceholderText("Enter Email");
    const passwordInput = getByPlaceholderText("Password");

    fireEvent.change(companyInput, { target: { value: "foo" } });
    fireEvent.change(emailInput, { target: { value: "foo@levelops.io" } });
    fireEvent.change(passwordInput, { target: { value: "" } });

    fireEvent.click(getByTestId("login"));

    expect(getByText("This field cannot be blank.")).toBeInTheDocument();
  });
});
