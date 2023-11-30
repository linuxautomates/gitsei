import { useState, useRef, useCallback } from "react";

export function useStateWithRef<T>(initialValue: T) {
  const [state, _setState] = useState<T>(initialValue);
  const stateRef = useRef<T>(initialValue);

  const setState = useCallback((value: T) => {
    _setState(value);
    stateRef.current = value;
    console.log("[]", value);
  }, []);

  return { state, ref: stateRef.current, setState };
}
