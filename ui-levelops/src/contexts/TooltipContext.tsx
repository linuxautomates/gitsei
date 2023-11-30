/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import React, { createContext, PropsWithChildren, ReactElement, useContext } from "react";
import { TooltipContextProvider } from "@harness/uicore";
import { tooltipDictionary } from "@harness/ng-tooltip";
import type { ParentContext } from "@harness/microfrontends";

export const TooltipProvider = (props: PropsWithChildren<Pick<ParentContext, "tooltipContext">>): ReactElement => {
  const { tooltipContext } = props;

  const tooltipContextValue = tooltipContext || createContext({});

  const tooltipContextData = useContext(tooltipContextValue);

  return (
    <TooltipContextProvider initialTooltipDictionary={tooltipContext ? tooltipContextData : tooltipDictionary}>
      {props.children}
    </TooltipContextProvider>
  );
};
