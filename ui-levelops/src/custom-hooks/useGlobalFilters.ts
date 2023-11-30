import { useEffect, useRef } from "react";
import { isEqual } from "lodash";

export function useGlobalFilters(globalFilters: any) {
  const filters = useRef(globalFilters);

  useEffect(() => {
    if (!isEqual(filters.current, globalFilters)) {
      filters.current = globalFilters;
    }
  }, [globalFilters]);

  return filters.current;
}
