import { ProjectPathProps } from "classes/routeInterface";
import { WebRoutes } from "../../../../routes/WebRoutes";

export const getBreadcrumbForOrganizationPage = (params: ProjectPathProps) => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Collections",
      path: WebRoutes.organization_page.root(params)
    }
  ];
};
