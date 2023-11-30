import * as React from "react";
import { useEffect, useState } from "react";
import "./breadcrumb-filter.style.scss";
import { getModulePathString } from "./util";
import { AntIcon } from "shared-resources/components";

interface BreadcrumbFiltersProps {
  defaultPath?: string;
  disabled?: boolean;
  onClick?: (value: any) => void;
}

export const BreadCrumbFilters: React.FC<BreadcrumbFiltersProps> = ({ onClick, defaultPath, disabled }) => {
  const [breadCrumbs, setBreadCrumbs] = useState<string[]>(["Root"]);
  const localFilterClicked = (selectedPath: string, pathIndex: number) => {
    if (disabled) {
      return;
    }

    if (pathIndex === breadCrumbs.length - 1) {
      return;
    }
    const modulePath = getModulePathString(breadCrumbs, breadCrumbs.indexOf(selectedPath));
    onClick && onClick(modulePath);
  };

  useEffect(() => {
    if (defaultPath) {
      const pathArray = defaultPath.split("/");
      setBreadCrumbs(["Root", ...pathArray]);
    } else if (defaultPath === "") {
      setBreadCrumbs(["Root"]);
    }
  }, [defaultPath]);

  return (
    <div className="breadcrumbs-container">
      {breadCrumbs.map((bC, index) => {
        const classes = ["path"];
        const homeIconClasses = ["homeIcon"];
        let homeIcon = null;
        if (index === breadCrumbs.length - 1) {
          classes.push("active-path");
          homeIconClasses.push("active");
        }
        if (index === 0) {
          homeIcon = (
            <AntIcon className={homeIconClasses.join(" ")} type="home" onClick={() => localFilterClicked(bC, index)} />
          );
        }
        return (
          <p key={`breadcrumb-${index}`} className="breadcrumbs">
            {homeIcon}
            <span className={classes.join(" ")} onClick={() => localFilterClicked(bC, index)}>
              {bC}
            </span>
            {index < breadCrumbs.length - 1 && <span className="delimiter">/</span>}
          </p>
        );
      })}
    </div>
  );
};
