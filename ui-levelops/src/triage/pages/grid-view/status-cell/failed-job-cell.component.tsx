import React, { useCallback } from "react";

import JobCellComponent, { StatusCountCellProps } from "./status-count-cell.component";
import { AntIcon } from "../../../../shared-resources/components";

interface FailedJobCellComponent extends StatusCountCellProps {
  onInfoClick: (record: any, date: any, statusKey?: string) => void;
  record: any;
  date: any;
  statusKey?: string;
}

const FailedJobCellComponent: React.FC<FailedJobCellComponent> = (props: FailedJobCellComponent) => {
  const handleInfoClick = useCallback(() => {
    props.onInfoClick(props.record, props.date, props.statusKey);
  }, [props.record, props.date]);

  return (
    <>
      <JobCellComponent href={props.href} color={props.color} count={props.count} />
      <AntIcon type="info-circle" onClick={handleInfoClick} />
    </>
  );
};

export default React.memo(FailedJobCellComponent);
