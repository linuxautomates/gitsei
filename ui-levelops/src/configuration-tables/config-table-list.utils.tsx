import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
import React from "react";
import { TableRowActions } from "shared-resources/components";

export const BuildActionOptions = (props: {
  id: string;
  onCloneHandler: (id: string) => void;
  onRemoveHandler: (id: string) => void;
}) => {
  const [createAccess, editAccess, deleteAccess] = useConfigScreenPermissions();
  const actions = [
    {
      type: "copy",
      id: props.id,
      description: "Clone",
      onClickEvent: props.onCloneHandler,
      disabled: !createAccess
    },
    {
      type: "delete",
      id: props.id,
      description: "Delete",
      onClickEvent: props.onRemoveHandler,
      disabled: !deleteAccess
    }
  ];
  return <TableRowActions actions={actions} />;
};
