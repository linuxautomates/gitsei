export const widgetValidationFunction = (payload: any) => {
  const { query = {} } = payload;
  let isValid = query?.workitem_stages && (query?.workitem_stages || []).length;
  if (isValid) {
    return true;
  } else if (!isValid && (query?.exclude?.workitem_stages || []).length) {
    return true;
  }
  return false;
};
