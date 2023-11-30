/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

import React from "react";
import type { IDrawerProps } from "@blueprintjs/core";
import { getDefaultDrawerProps, getParsedDrawerOptions } from "../useDrawerHook.utils";

const header = <>{"header"}</>;

describe("Validate Utils", () => {
  test("should validate getDefaultDrawerProps", () => {
    expect(getDefaultDrawerProps({ header })).toEqual({
      autoFocus: true,
      canEscapeKeyClose: true,
      canOutsideClickClose: true,
      enforceFocus: false,
      hasBackdrop: true,
      isCloseButtonShown: false,
      isOpen: true,
      portalClassName: "health-source-right-drawer",
      position: "right",
      size: "calc(100% - 330px)",
      title: <React.Fragment>header</React.Fragment>,
      usePortal: true
    });
  });

  test("should validate getParsedDrawerOptions", () => {
    const defaultOptions = getDefaultDrawerProps({ header });
    expect(getParsedDrawerOptions(defaultOptions, {} as IDrawerProps)).toEqual(defaultOptions);
    expect(getParsedDrawerOptions(defaultOptions, { size: "400px" } as IDrawerProps)).toEqual({
      ...defaultOptions,
      size: "400px"
    });
  });
});
