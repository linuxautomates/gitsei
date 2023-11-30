import React from "react";
import { AntTag } from "../../../../shared-resources/components";

export interface StatusCountCellProps {
  href: string;
  color: string;
  count: number;
}

const StatusCountCellComponent: React.FC<StatusCountCellProps> = (props: StatusCountCellProps) => (
  <AntTag color={props.color}>
    <a href={props.href}>{props.count || 0}</a>
  </AntTag>
);

export default React.memo(StatusCountCellComponent);
