import { get } from "lodash";
import React, { useCallback } from "react";
import { newUXColorMapping } from "shared-resources/charts/chart-themes";
import { AntTooltip, TooltipWithTruncatedText } from "shared-resources/components";
import { truncateAndEllipsis } from "utils/stringUtils";
import BurndownLegendComponent from "./BurndownLegendComponent";
import BurndownPopOverContent from "./BurndownPopOverContent";
import "./jiraBurndownCard.styles.scss";
import StackedProgressBar from "./StackedProgressBar";

const JiraBurndownCard: React.FC<{
  data: any;
  dataKeys: string[];
  onChartClick: (data: any) => void;
}> = ({ data, dataKeys, onChartClick }) => {
  const getSprintsRecords = useCallback(() => {
    return get(data, ["sprints"], []);
  }, [data]);

  const getLegendData = useCallback(
    (data: any) => {
      return dataKeys.map(status => {
        return {
          text: data[`total_tickets_${status}`],
          status
        };
      });
    },
    [dataKeys]
  );

  const handleCardClick = useCallback((data: any) => {
    onChartClick(data);
  }, []);

  return (
    <div className="burndown-card">
      <div className="burndown-card-upper">
        <AntTooltip title={data?.summary || data?.name || ""} placement={"topLeft"}>
          <p className="burndown-card-upper-name">{truncateAndEllipsis(data?.summary || data?.name || "", 25)}</p>
        </AntTooltip>
        <TooltipWithTruncatedText
          title={data?.name || ""}
          allowedTextLength={16}
          textClassName={"burndown-card-upper-summary"}
        />
        <StackedProgressBar
          PopoverContent={BurndownPopOverContent}
          popOverContentProps={{
            onClick: (e: any) => handleCardClick(data?.id)
          }}
          mapping={newUXColorMapping}
          records={getSprintsRecords()}
          dataKeys={dataKeys}
          metaData={{
            radiusFactor: 10,
            yHide: true,
            xAxisDatakey: "initial",
            chartProps: {
              barGap: 0,
              margin: { top: 20, right: 5, left: 5, bottom: 50 }
            }
          }}
        />
      </div>
      <div className="burndown-card-lower">
        <div className="burndown-card-lower-text_container">
          <p className="burndown-card-lower-text">Total</p>
        </div>
        <div className="stacked-hr">
          <StackedProgressBar
            PopoverContent={BurndownPopOverContent}
            mapping={newUXColorMapping}
            popOverContentProps={{
              notShowActionButton: true
            }}
            records={[
              {
                completed: data?.total_tickets_completed,
                remaining: data?.total_tickets_remaining,
                new: data?.total_tickets_new
              }
            ]}
            dataKeys={dataKeys}
            metaData={{
              radiusFactor: 10,
              width: 160,
              height: 30,
              orientation: "vertical",
              xAxisType: "number",
              yAxisType: "category",
              xHide: true,
              yHide: true
            }}
          />
        </div>
        <div className="burndown-card-lower-legends">
          <BurndownLegendComponent statusList={getLegendData(data)} mapping={newUXColorMapping} />
        </div>
      </div>
    </div>
  );
};

export default JiraBurndownCard;
