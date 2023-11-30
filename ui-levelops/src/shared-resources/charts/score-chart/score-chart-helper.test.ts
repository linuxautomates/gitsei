import * as helperFunctions from "./helper";
import { isNumber } from "lodash";

describe("scoreColor test suite", () => {
  let mock_score = 0;

  test("it should return #BEDD85 if score >= 80", () => {
    mock_score = 80;
    const response = "#BEDD85";
    expect(isNumber(mock_score)).toBeTruthy();
    expect(helperFunctions.scoreColor(mock_score)).toBe(response);
  });

  test("it should return #7491D6 if score >= 60", () => {
    mock_score = 60;
    const response = "#7491D6";
    expect(isNumber(mock_score)).toBeTruthy();
    expect(helperFunctions.scoreColor(mock_score)).toBe(response);
  });

  test("it should return #EFD091 if score >= 30", () => {
    mock_score = 30;
    const response = "#EFD091";
    expect(isNumber(mock_score)).toBeTruthy();
    expect(helperFunctions.scoreColor(mock_score)).toBe(response);
  });

  test("it should return #CA458D if score < 30", () => {
    mock_score = 29;
    const response = "#CA458D";
    expect(isNumber(mock_score)).toBeTruthy();
    expect(helperFunctions.scoreColor(mock_score)).toBe(response);
  });
});
