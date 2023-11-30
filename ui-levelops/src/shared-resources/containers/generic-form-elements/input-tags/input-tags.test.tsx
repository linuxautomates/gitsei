import * as React from "react";
import { render, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import InputTagsComponent from "./input-tags.component";

describe("InputTags", () => {
  test("On Close click should remove the tag", () => {
    const onChangeMock = jest.fn();
    const props = {
      value: ["Apple", "Mango"],
      onChange: onChangeMock
    };
    const { asFragment, getAllByLabelText } = render(<InputTagsComponent {...props} />);
    expect(asFragment()).toMatchSnapshot();

    const closeBtn = getAllByLabelText("icon: close");
    fireEvent.click(closeBtn[0]);
    expect(onChangeMock).toHaveBeenCalled();
  });

  test("new tag click should show input field", () => {
    const props = {
      value: [],
      onChange: jest.fn()
    };
    const { getByLabelText, getByTestId } = render(<InputTagsComponent {...props} />);
    const addBtn = getByLabelText("icon: plus");
    fireEvent.click(addBtn);
    const addField = getByTestId("tag-input");
    expect(addField).toBeInTheDocument();
  });

  test("input field submit should add new tag", () => {
    const mockOnChange = jest.fn();
    const props = {
      value: [],
      onChange: mockOnChange
    };
    const { getByLabelText, getByTestId } = render(<InputTagsComponent {...props} />);

    const addBtn = getByLabelText("icon: plus");
    fireEvent.click(addBtn);

    const newTag = ["Grapes"];
    const inputField = getByTestId("tag-input");
    fireEvent.change(inputField, { target: { value: newTag } });
    fireEvent.keyDown(inputField, { key: "Enter", keyCode: 13, charCode: 13 });
    expect(mockOnChange).toBeCalledWith(newTag);
  });
});
