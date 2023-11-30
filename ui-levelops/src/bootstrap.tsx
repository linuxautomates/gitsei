/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { createContext } from "react";
import ReactDOM from "react-dom";
import type { CommonComponents } from "@harness/microfrontends";
import App from "./App/App";
import "./styles/bootstrap.scss";

ReactDOM.render(
  <App
    renderUrl=""
    parentContextObj={{
      appStoreContext: createContext({}) as any,
      licenseStoreProvider: createContext({}) as any,
      permissionsContext: createContext({}) as any
    }}
    scope={{}}
    components={{} as unknown as CommonComponents}
    hooks={{
      useDocumentTitle: () => ({ updateTitle: () => void 0 })
    }}
    matchPath=""
    on401={() => undefined}
    cdServices={{} as any}
    customComponents={{} as any}
    customRoutes={{} as any}
    customUtils={{} as any}
    customHooks={{} as any}
  />,
  document.getElementById("root")
);
