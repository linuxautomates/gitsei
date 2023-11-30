import React, { useMemo, useState } from "react";
import { Drawer, Intent } from "@blueprintjs/core";
import cx from "classnames";
import { Button, useConfirmationDialog } from "@harness/uicore";
import { useModalHook } from "@harness/use-modal";
import { getDefaultDrawerProps, getParsedDrawerOptions } from "./useDrawerHook.utils";
import type { UseDrawerInterface, UseDrawerPropsInterface } from "./useDrawerHook.types";
import css from "./useDrawerHook.module.scss";

export const useDrawer = ({
  createHeader,
  createDrawerContent,
  drawerOptions,
  className,
  showConfirmationDuringClose = true
}: UseDrawerPropsInterface): UseDrawerInterface => {
  const [drawerContentProps, setDrawerContentProps] = useState({});
  const [drawerHeaderProps, setDrawerHeaderProps] = useState({});

  const { openDialog: showWarning } = useConfirmationDialog({
    intent: Intent.WARNING,
    contentText: "Unsaved changes",
    titleText: "Are you sure?",
    cancelButtonText: "Cancel",
    confirmButtonText: "Confirm",
    onCloseDialog: (isConfirmed: boolean) => isConfirmed && hideModal()
  });

  const header = createHeader ? createHeader(drawerHeaderProps) : undefined;
  const defaultOptions = useMemo(() => getDefaultDrawerProps({ header }), [drawerHeaderProps]);
  const parsedOptions = useMemo(
    () => getParsedDrawerOptions(defaultOptions, drawerOptions),
    [defaultOptions, drawerOptions]
  );

  const [showModal, hideModal] = useModalHook(
    () => (
      <>
        <Drawer {...parsedOptions} onClose={showConfirmationDuringClose ? showWarning : hideModal}>
          <div className={cx(css.formFullheight, className)}>{createDrawerContent(drawerContentProps)}</div>
        </Drawer>
        <Button
          minimal
          style={parsedOptions?.size ? { right: parsedOptions?.size } : {}}
          className={css.almostFullScreenCloseBtn}
          icon="cross"
          withoutBoxShadow
          onClick={hideModal}
        />
      </>
    ),
    [drawerContentProps]
  );

  return {
    showDrawer: data => {
      if (data) {
        setDrawerContentProps(data);
      }
      showModal();
    },
    setDrawerHeaderProps: props => setDrawerHeaderProps(props),
    hideDrawer: hideModal
  };
};
