import React from "react";
import { AntButton } from "../../../../../shared-resources/components";

type action = {
  disabled?: boolean;
  type: string;
  id: string;
  title?: string;
  buttonType?: string;
  description?: string;
  onClickEvent: Function;
};

interface VersionTableRowActionsComponentProps {
  actions: action[];
}
const VersionTableRowActionsComponent: React.FC<VersionTableRowActionsComponentProps> = (
  props: VersionTableRowActionsComponentProps
) => {
  const { actions } = props;
  let buttons: JSX.Element | JSX.Element[];

  buttons = actions.map((action: action, i) => {
    switch (action.type) {
      default:
        return (
          <AntButton className="mr-15" type={action.buttonType} onClick={action.onClickEvent}>
            {action.title}
          </AntButton>
        );
    }
  });

  return (
    <>
      <div className="actionsWrapper"> {buttons} </div>
    </>
  );
};

export default VersionTableRowActionsComponent;
