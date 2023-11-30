import React from "react";
import { getReportsPage } from "../../../constants/routePaths";
import { AntText } from "../../components";
import { get } from "lodash";
import { Link } from "react-router-dom";

export const levelopsResponseTimeMapping = {
  questionnaire_template_id: "questionnaire_template_ids"
};

export const getLevelopsResponseTimeReport = (x_axis: any, columns: any) => {
  const xunitkey = get(levelopsResponseTimeMapping, [x_axis], x_axis);
  return columns.map((column: any) => {
    if (column.dataIndex === "key") {
      const render = (item: any, record: any, index: any) => {
        let filters: any;
        if (xunitkey === "completed" || xunitkey === "submitted") {
          filters = { [xunitkey]: record.key };
        } else {
          filters = { [xunitkey]: [record.id] };
        }
        const url = `${getReportsPage()}?tab=assessments&filters=${JSON.stringify(filters)}`;
        return (
          <AntText className={"pl-10"}>
            <Link className={"ellipsis"} to={url} target={"_blank"}>
              {item}
            </Link>
          </AntText>
        );
      };
      const modifiedColumn = { ...column, render: render };
      return modifiedColumn;
    }
    return column;
  });
};
