import { ProjectPathProps } from "classes/routeInterface";
import { buildApiUrl, VERSION } from "constants/restUri";
import { BASE_UI_URL } from "helper/envPath.helper";

export const navigateToRoute = (layout: string, route: string): void => {
  window.location.replace(BASE_UI_URL.concat(`${layout}${route}`));
};
export const navigateToSSO = (SSOCompany?: string): void => {
  window.location.href = buildApiUrl(`/${VERSION}/generate_authn?company=${SSOCompany}`);
};
export const projectPathPropsDef: ProjectPathProps = {
  accountId: ':accountId',
  projectIdentifier: ':projectIdentifier',
  orgIdentifier: ':orgIdentifier',
}

export function isIntegrationMapping() {
  const pathname = window.location.pathname;
  return pathname.includes('sei-integration-mapping')
}