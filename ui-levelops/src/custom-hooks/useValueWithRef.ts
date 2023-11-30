import { isEqual } from "lodash";
import { useEffect, useRef } from "react";

export function useValueWithRef<T>(value: T): T {
  const valueRef = useRef<T>(value);

  useEffect(() => {
    if (!isEqual(value, valueRef.current)) {
      valueRef.current = value;
    }
  }, [value]);

  return valueRef.current;
}
