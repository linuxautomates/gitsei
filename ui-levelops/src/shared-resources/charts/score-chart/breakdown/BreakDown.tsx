import React, { useCallback, useMemo } from "react";
import { AntText } from "../../../components";
import { toTitleCase, valueToTitle } from "../../../../utils/stringUtils";

import "./BreakDown.scss";

interface BreakdownProps {
  breakdown: any;
  onClick: (breakdown: any) => void;
  total: number;
}

const Breakdown: React.FC<BreakdownProps> = (props: BreakdownProps) => {
  const {
    total,
    breakdown,
    breakdown: { total_tickets, hygiene, id }
  } = props;

  const percentageOfTicket = useMemo(() => {
    return total ? ((total_tickets / total) * 100).toFixed(1) : 0;
  }, [total_tickets, total]);

  const handleClick = (event: any) => {
    event.preventDefault();
    props.onClick(breakdown);
  };

  return (
    <div className="issue-wrapper">
      <span className="issue-container">
        <AntText className="issue-title">{id ? valueToTitle(hygiene) : toTitleCase(hygiene)}</AntText>
      </span>
      <span className="dashed-space" />
      <a href={"#"} className="issue-link" onClick={handleClick}>
        {total_tickets}
        {/*({percentageOfTicket}%)*/}
      </a>
    </div>
  );
};

export default React.memo(Breakdown);
