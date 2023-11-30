import React, { useContext } from "react";

import "./WidgetThemes.scss";
import WidgetTheme from "./WidgetTheme";
import { reportThemes, CategoryTheme } from "../../report.constant";
import { DashboardWidgetResolverContext } from "../../../context";
import { ProjectPathProps } from "classes/routeInterface";
import { useParams } from "react-router-dom";

interface WidgetThemesProps {}

const WidgetThemes: React.FC<WidgetThemesProps> = () => {
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const projectParams = useParams<ProjectPathProps>();

  return (
    <>
      <div className="theme-header">Categories</div>
      <div className="widget-themes-container">
        {reportThemes(projectParams, dashboardId).map((theme: CategoryTheme, index: number) => (
          <WidgetTheme key={`theme-${index}`} theme={theme} />
        ))}
      </div>
    </>
  );
};

export default React.memo(WidgetThemes);
