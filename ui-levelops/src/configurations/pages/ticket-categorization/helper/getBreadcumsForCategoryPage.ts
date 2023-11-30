import { WebRoutes } from "routes/WebRoutes";
import { getBreadcumsForSchemePage } from "./getBreadcumsForSchemePage";

export const getBreadcumsForCategoryPage = (
  schemeId: string,
  schemeName: string,
  categoryId: string,
  categoryName: string
) => {
  return [
    ...getBreadcumsForSchemePage(schemeId, schemeName),
    {
      label: categoryName,
      path: WebRoutes.ticket_categorization.scheme.category.details(schemeId, categoryId)
    }
  ];
};
