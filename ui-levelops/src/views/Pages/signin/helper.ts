import { DIGIT_WARNING, LENGTH_WARNING, SPECIAL_CASE_WARNING, UPPER_CASE_WARNING } from "constants/error.constants";
import { EMPTY_FIELD_WARNING } from "constants/formWarnings";

export const validatePassword = (value: string) => {
  const regExpUpperCase = RegExp(".*[A-Z].*");
  const regExpNumber = RegExp(/\d/);
  const regExpSpecialChar = RegExp(/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/);
  if (value.length === 0) {
    return EMPTY_FIELD_WARNING;
  } else if (value.length < 8) {
    return LENGTH_WARNING;
  } else if (!regExpUpperCase.test(value)) {
    return UPPER_CASE_WARNING;
  } else if (!regExpNumber.test(value)) {
    return DIGIT_WARNING;
  } else if (!regExpSpecialChar.test(value)) {
    return SPECIAL_CASE_WARNING;
  }
  return "";
};
