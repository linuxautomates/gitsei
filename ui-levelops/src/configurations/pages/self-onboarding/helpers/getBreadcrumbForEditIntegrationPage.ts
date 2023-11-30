import { WebRoutes } from "routes/WebRoutes";

export const getBreadcumsForEditIntegrationPage = (integration_id: string, integration?: string) => {
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root()
    },
    {
      label: "Integrations",
      path: `${WebRoutes.integration.list()}?tab=your_integrations`
    },
    {
      label: "Edit Integrations",
      path: WebRoutes.settings.integration_edit(integration_id)
    }
  ];
};
