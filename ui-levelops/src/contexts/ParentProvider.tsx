import React, { createContext, FC, PropsWithChildren, useContext } from "react";
import type { ChildAppProps, SEICustomMicroFrontendProps } from "@harness/microfrontends";

export interface ParentProviderProps {
  hooks: SEICustomMicroFrontendProps["customHooks"] & ChildAppProps["hooks"];
  components: SEICustomMicroFrontendProps["customComponents"] & ChildAppProps["components"];
  services: SEICustomMicroFrontendProps["cdServices"];
  routes: SEICustomMicroFrontendProps["customRoutes"];
  utils: SEICustomMicroFrontendProps["customUtils"] & ChildAppProps["utils"];
}

export const ParentProviderContext = createContext<ParentProviderProps>({} as ParentProviderProps);

const ParentProvider: FC<PropsWithChildren<ParentProviderProps>> = ({
  children,
  hooks,
  components,
  services,
  routes,
  utils
}) => (
  <ParentProviderContext.Provider value={{ hooks, components, services, routes, utils }}>
    {children}
  </ParentProviderContext.Provider>
);

export default ParentProvider;

export const useParentProvider = (): ParentProviderProps => useContext(ParentProviderContext);
