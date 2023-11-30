import React, { useEffect, useState } from "react";
import { v1 as uuid } from "uuid";
import { useSelector } from "react-redux";
import { get, orderBy } from "lodash";
import { convertEpochToDate } from "../../../utils/dateUtils";
import { AntTag } from "../../../shared-resources/components";
import { getCicdAggsListSelector } from "reduxConfigs/selectors/cicdJobAggsListSelector";
import { restAPILoadingState } from "../../../utils/stateUtil";
import TriageTotals from "./TriageTotals";

interface TriageTotalsContainerProps {
  identifier: string;
}

let columns: any[];
let totalAggs: any[];

export const TriageTotalsContainer: React.FC<TriageTotalsContainerProps> = (props: TriageTotalsContainerProps) => {
  const [loading, setLoading] = useState(true);
  const identifier = props.identifier || "0";
  const cicdAggsListState = useSelector(getCicdAggsListSelector);

  useEffect(() => {
    const { loading } = restAPILoadingState(cicdAggsListState, identifier);
    if (!loading) {
      setLoading(false);
      const totals = get(cicdAggsListState, [identifier, "data", "totals"], {});

      if (Object.keys(totals).length > 0) {
        totalAggs = [totals];
      } else {
        totalAggs = [];
      }

      columns = Object.keys(get(totalAggs, [0], {})).map(item => ({
        key: item,
        title: convertEpochToDate(item, "LL", true),
        dataIndex: item,
        children: ["Success", "Failure", "Aborted"].map((result: string) => ({
          // @ts-ignore
          key: uuid(),
          align: "center",
          width: 90,
          dataIndex: item,
          title: result,
          render: (item: any, record: any, index: number) => {
            if (!item || !item[(result || "").toUpperCase()]) {
              return null;
            }
            const tagColor = (res: string) => {
              switch ((res || "").toUpperCase()) {
                case "SUCCESS":
                  return "green";
                case "FAILURE":
                  return "red";
                case "ABORTED":
                  return "grey";
              }
            };
            return <AntTag color={tagColor(result)}>{item ? item[(result || "").toUpperCase()] || 0 : 0}</AntTag>;
          }
        }))
      }));
      columns = orderBy(columns, ["key"], "desc");
    }
  }, [cicdAggsListState]);

  if (loading || !totalAggs.length) {
    return null;
  }

  return <TriageTotals columns={columns} dataSource={totalAggs} />;
};
