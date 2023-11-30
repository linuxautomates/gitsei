import React, { FC, useContext, useMemo } from "react";
import type { ChildAppProps, SEICustomMicroFrontendProps } from "@harness/microfrontends";
import { ModalProvider } from "@harness/use-modal";
import "antd/dist/antd.css";
import "assets/sass/light-bootstrap-dashboard-pro-react.scss?v=1.2.0";
import { AppStoreContext } from "../contexts/AppStoreContext";
import ParentProvider from "../contexts/ParentProvider";
import { LicenseStoreContext } from "../contexts/LicenseStoreContext";
import { TooltipProvider } from "../contexts/TooltipContext";

// import { StringsContextProvider } from "../strings";
// import strings from "strings/strings.en.yaml";
import RouteDestinationsHarness from "../RouteDestinationsHarness";
import RouteDestinationsStandaloneApp from "../RouteDestinationsStandaloneApp";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import useOpenApiClients from "custom-hooks/useOpenAPIClients";
import { removeLastSlash } from "utils/regexUtils";

const ChildApp: FC<SEICustomMicroFrontendProps & ChildAppProps> = props => {
  const {
    parentContextObj,
    scope,
    renderUrl,
    hooks,
    cdServices,
    customComponents,
    customRoutes,
    customUtils,
    components,
    customHooks,
    utils
  } = props;
  const appStoreData = useContext(parentContextObj.appStoreContext);
  const licenseStoreData = useContext(parentContextObj.licenseStoreProvider);
  const { useFeatureFlags } = customHooks;
  const { CDS_NAV_2_0 } = useFeatureFlags?.() || {};

  const isStandaloneApp = scope?.accountId ? false : true;
  window.isStandaloneApp = isStandaloneApp;
  window.baseUrl = removeLastSlash(renderUrl);

  // Redirect from `/#/...` to `/...`
  const { pathname, hash } = window.location;
  if (hash && pathname === "/") {
    const targetUrl = window.location.href.replace("/#/", "/");
    window.location.href = targetUrl;
  }

  useOpenApiClients(() => {}, scope?.accountId || "");
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: false
      },
      mutations: {}
    }
  });
  return (
    <AppStoreContext.Provider value={{ ...appStoreData, baseUrl: renderUrl, scope, isNav2Enabled: !!CDS_NAV_2_0 }}>
      <QueryClientProvider client={queryClient}>
        {/* <StringsContextProvider data={strings}> */}
        <TooltipProvider>
          <LicenseStoreContext.Provider value={licenseStoreData}>
            <ModalProvider>
              <ParentProvider
                hooks={{ ...hooks, ...customHooks }}
                components={{ ...customComponents, ...components }}
                services={cdServices}
                routes={customRoutes}
                utils={{ ...customUtils, ...utils }}>
                {isStandaloneApp ? { ...RouteDestinationsStandaloneApp } : { ...RouteDestinationsHarness }}
              </ParentProvider>
            </ModalProvider>
          </LicenseStoreContext.Provider>
        </TooltipProvider>
        {/* </StringsContextProvider> */}
      </QueryClientProvider>
    </AppStoreContext.Provider>
  );
};

export default ChildApp;
