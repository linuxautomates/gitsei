import { useSelector } from "react-redux";
import { uniq } from "lodash";

const debugChanges = (left: any, right: any) => {
  if (left === right) {
    return true;
  }

  const oldKeys = Object.keys(left);
  const newKeys = Object.keys(right);

  const allKeys = uniq([...oldKeys, ...newKeys]);

  allKeys.map((prop: string) => {
    const prevValue = left[prop];
    const newValue = right[prop];
    if (prevValue !== newValue) {
      console.error(prop, "Selector changed: Fix It... ", prevValue, newValue);
    }
  });

  return true;
};

export const useDebugSelector = (selector: any, equalityFn?: any) => useSelector(selector, equalityFn || debugChanges);
