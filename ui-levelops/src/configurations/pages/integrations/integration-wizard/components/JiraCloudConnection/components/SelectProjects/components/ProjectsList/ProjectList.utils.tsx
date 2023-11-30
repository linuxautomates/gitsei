import React from "react";
import { CellProps, Renderer } from "react-table";
import { Color, FontVariation, Text } from "@harness/uicore";

export const RenderLastUpdatedAt: Renderer<CellProps<ProjectsListResponse>> = ({ row }): JSX.Element => {
  const rowdata = row.original;
  return (
    <Text color={Color.GREY_700} font={{ variation: FontVariation.BODY2 }}>
      {`Updated ${rowdata.lastUpdatedAt} mins ago`}
    </Text>
  );
};
