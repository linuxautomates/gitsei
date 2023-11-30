export const getTagSelectHeightClassName = (
  showLocalPartialFilteredData: boolean,
  localSelectMode: "partial" | "full"
) => {
  let finalHeightClassName = "";

  if (showLocalPartialFilteredData) {
    finalHeightClassName = "tag-paginated-select__popover-content__filter-list-height";
  } else if (localSelectMode === "partial") {
    finalHeightClassName = "tag-paginated-select__popover-content__partial-match-height";
  }

  return finalHeightClassName;
};
