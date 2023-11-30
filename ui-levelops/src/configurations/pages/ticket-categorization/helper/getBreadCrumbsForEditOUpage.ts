import { ProjectPathProps } from "classes/routeInterface";
import { WebRoutes } from "routes/WebRoutes";

export const getBreadcumsForEditOUPage = (params: ProjectPathProps, workspaceId?: string, ou_category_tab?: string) => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Collections",
      path: WebRoutes.organization_page.root(params, workspaceId, ou_category_tab)
    },
    {
      label: "Edit Collection",
      path: WebRoutes.organization_page.create_org_unit(params)
    }
  ];
};
