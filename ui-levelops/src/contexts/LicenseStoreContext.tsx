import { createContext, useContext } from "react";
import type { LicenseStoreContextProps } from "@harness/microfrontends";

export const LicenseStoreContext = createContext<LicenseStoreContextProps>({} as LicenseStoreContextProps);
export const useLicenseStoreContext = () => useContext(LicenseStoreContext);
