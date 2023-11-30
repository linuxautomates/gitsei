import { useRef } from "react";

export function useDebugTool<T>(value: T, variableName: string) {
  const previous = useRef(value);

  if (previous.current !== value) {
    console.error(`${variableName} changed.`, previous.current, "=>", value);
    previous.current = value;
  }

  return value;
}
