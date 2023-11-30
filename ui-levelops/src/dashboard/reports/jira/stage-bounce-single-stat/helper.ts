export const stageBounceValidationFunc = (payload: any) => {
  const { query = {} } = payload;
  let isValid = query?.stages && (query?.stages || []).length;
  if (isValid) {
    return true;
  } else if (!isValid && (query?.exclude?.stages || []).length) {
    return true;
  }
  return false;
};
