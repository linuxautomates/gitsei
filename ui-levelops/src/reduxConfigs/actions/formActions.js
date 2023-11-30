import { FORM_CLEAR, FORM_INITIALIZE, FORM_UPDATE_FIELD, FORM_UPDATE_OBJ } from "./formActionTypes";

export const formClear = formName => ({ type: FORM_CLEAR, name: formName });

export const formInitialize = (formName, formData) => ({ type: FORM_INITIALIZE, name: formName, data: formData });

export const formUpdateField = (formName, formField, updateValue) => ({
  type: FORM_UPDATE_FIELD,
  name: formName,
  field: formField,
  value: updateValue
});

export const formUpdateObj = (formName, formObj) => ({ type: FORM_UPDATE_OBJ, name: formName, obj: formObj });
