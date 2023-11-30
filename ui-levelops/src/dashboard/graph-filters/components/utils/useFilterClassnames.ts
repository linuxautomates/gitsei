import { useMemo } from "react";
import classnames from "classnames";

interface FilterClassnamesArgs {
  applicationUse?: boolean;
  activePopKey?: boolean;
}

export function useFilterClassnames(args: FilterClassnamesArgs = {}) {
  const { applicationUse, activePopKey } = args;

  const outerClassName = useMemo(() => {
    return classnames("configure-widget-filters", applicationUse && "configure-widget-filters__application-use");
  }, [applicationUse]);

  const innerClassName = useMemo(() => {
    return classnames("configure-widget-filters__half", activePopKey && "overflow-hidden");
  }, [activePopKey]);

  return {
    outerClassName,
    innerClassName
  };
}
