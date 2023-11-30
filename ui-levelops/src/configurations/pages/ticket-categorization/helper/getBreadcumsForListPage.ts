import { WebRoutes } from "routes/WebRoutes";

export const getBreadcumsForListPage = () => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Effort Investment Profiles",
      path: WebRoutes.ticket_categorization.list()
    }
  ];
};
