import { WebRoutes } from "routes/WebRoutes";
import { getBreadcumsForListPage } from "./getBreadcumsForListPage";

export const getBreadcumsForSchemePage = (schemeId: string, schemeName: string) => {
  return [
    ...getBreadcumsForListPage(),
    {
      label: schemeName,
      path: WebRoutes.ticket_categorization.scheme.edit(schemeId)
    }
  ];
};
