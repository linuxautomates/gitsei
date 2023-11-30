import { Position, IDrawerProps } from "@blueprintjs/core";
import { merge } from "lodash";
import { DRAWER_OFFSET_LEFT } from "./useDrawerHook.constant";

export const getDefaultDrawerProps = ({ header }: { header: JSX.Element | undefined }): IDrawerProps => {
  return {
    usePortal: true,
    autoFocus: true,
    canEscapeKeyClose: true,
    canOutsideClickClose: true,
    enforceFocus: false,
    hasBackdrop: true,
    size: `calc(100% - ${DRAWER_OFFSET_LEFT})`,
    isOpen: true,
    position: Position.RIGHT,
    title: header,
    isCloseButtonShown: false,
    portalClassName: "health-source-right-drawer"
  };
};

export const getParsedDrawerOptions = (defaultOptions: IDrawerProps, options?: Partial<IDrawerProps>) => {
  if (options) {
    return merge(defaultOptions, options);
  } else {
    return defaultOptions;
  }
};
