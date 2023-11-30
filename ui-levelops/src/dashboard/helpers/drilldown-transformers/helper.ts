export const filterValueChangeKey = (keyBefore: string, keyAfter: string, filterObject: any) => {
  if (!!keyBefore && filterObject.hasOwnProperty(keyBefore) && !!keyAfter) {
    filterObject = {
      [keyAfter]: filterObject[keyBefore],
      ...filterObject
    };
    delete filterObject[keyBefore];
  }
  return filterObject;
};
