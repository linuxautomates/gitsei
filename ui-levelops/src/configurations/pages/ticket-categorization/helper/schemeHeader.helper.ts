export const headerActions = (disabled: boolean) => [
  {
    id: "set-default",
    label: "Set Default",
    disabled: disabled,
    hasClicked: false
  },
  {
    id: "edit",
    label: "Edit",
    disabled: false,
    hasClicked: false
  },
  {
    id: "clone",
    label: "Clone",
    disabled: false,
    hasClicked: false
  },
  {
    id: "delete",
    label: "Delete",
    disabled: disabled,
    hasClicked: false
  }
];
