import type { IDrawerProps } from "@blueprintjs/core";

export interface UseDrawerInterface {
  showDrawer: (data?: any) => void;
  hideDrawer: () => void;
  setDrawerHeaderProps?: (props: any) => void;
}

export interface UseDrawerPropsInterface {
  drawerOptions?: Partial<IDrawerProps>;
  createHeader?: (data?: any) => JSX.Element;
  createDrawerContent: (props: any) => JSX.Element;
  className?: string;
  showConfirmationDuringClose?: boolean;
}
