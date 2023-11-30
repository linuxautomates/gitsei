import React from "react";
import { CATEGORIES_CONFIGURATION_PAGE_DESC } from "../../constants/ticket-categorization.constants";
import "./CategoriesConfigurationPageContent.style.scss";
import CategoriesConfigurationPageContentBody from "./scheme-drilldown-page-content-body/CatagoriesConfigurationPageContentBody";
import CategoriesConfigurationPageContentHeader from "./scheme-drilldown-page-content-header/CategoriesConfigurationPageContentHeader";

export interface CategoriesConfigurationPageContentProps {
  profileId: string;
}
const CategoriesConfigurationPageContent: React.FC<CategoriesConfigurationPageContentProps> = ({ profileId }) => {
  return (
    <div className="scheme-drilldown-page-content">
      <CategoriesConfigurationPageContentHeader profileId={profileId} />
      <p className="categories-page-desc">{CATEGORIES_CONFIGURATION_PAGE_DESC}</p>
      <CategoriesConfigurationPageContentBody profileId={profileId} />
    </div>
  );
};

export default CategoriesConfigurationPageContent;
