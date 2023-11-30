import React from "react";
import * as PropTypes from "prop-types";
import { AntCard, AntRow, AntCol, AntStatistic, AntText, IntegrationIcon } from "shared-resources/components";
import { DonutChart, BarChart } from "shared-resources/charts";
import { Divider } from "antd";

export class PraetorianGraphComponent extends React.PureComponent {
  // eslint-disable-next-line no-useless-constructor
  constructor(props) {
    super(props);
  }

  runReport = (data, title) => {
    return (
      <AntRow type={"flex"} gutter={[10, 10]}>
        <AntCol span={24}>
          <AntText strong>{title.replace(/_/g, " ")}</AntText>
        </AntCol>
        <AntCol span={24}>
          {data.map(item => (
            <AntCol span={6}>
              <AntStatistic title={item.name.toUpperCase()} value={item.value} />
            </AntCol>
          ))}
        </AntCol>
        <AntCol span={24}>
          <DonutChart data={data} pieProps={{ cx: "40%" }} />
        </AntCol>
      </AntRow>
    );
  };

  barChart = (data, props) => {
    return (
      <>
        <div style={{ height: "100%", padding: "0px", margin: "0px" }}>
          <BarChart
            data={data}
            // barProps={
            //     [{ name: "Issues", dataKey: "issues", fill: "#278CE6" }]
            // }
            barProps={props}
            chartProps={{
              barGap: 0,
              margin: { top: 20, right: 0, left: 0, bottom: 80 }
            }}
            stacked={false}
          />
        </div>
      </>
    );
  };

  render() {
    const { report } = this.props;
    const keys = Object.keys(report.results.summary);
    const run1Data = Object.keys(report.results.summary[keys[0]])
      .filter(k => !["total_issues", "total_info"].includes(k))
      .map(key => {
        return {
          name: key.replace("total_", ""),
          value: parseInt(report.results.summary[keys[0]][key])
        };
      });

    const run2Data = Object.keys(report.results.summary[keys[1]])
      .filter(k => !["total_issues", "total_info"].includes(k))
      .map(key => {
        return {
          name: key.replace("total_", ""),
          value: parseInt(report.results.summary[keys[1]][key])
        };
      });

    const barProps = keys.map((key, index) => ({
      name: key,
      dataKey: key
      //fill: lineChartColors[index]
    }));
    let barData = Object.keys(report.results.aggregations[keys[0]].by_category).map(item => {
      return {
        name: item,
        [`${keys[0]}`]: parseInt(report.results.aggregations[keys[0]].by_category[item].total)
      };
    });

    barData.forEach(data => {
      data[`${keys[1]}`] = parseInt(report.results.aggregations[keys[1]].by_category[data.name].total);
    });

    // at least one category has non zero data
    barData = barData.filter(item => item[`${keys[1]}`] + item[`${keys[0]}`] > 0);

    return (
      <AntCard
        title={`Praetorian ( ${this.props.product} )`}
        extra={<IntegrationIcon type="praetorian" style={{ width: "auto", height: "30px" }} />}>
        <AntRow type={"flex"} justify={"space-between"} gutter={[0, 10]}>
          <AntCol span={7}>{this.runReport(run1Data, keys[0])}</AntCol>
          <AntCol span={7}>{this.runReport(run2Data, keys[1])}</AntCol>
          <AntCol span={1}>
            <Divider type="vertical" className="h-100" />
          </AntCol>
          <AntCol span={9}>{this.barChart(barData, barProps)}</AntCol>
        </AntRow>
      </AntCard>
    );
  }
}

PraetorianGraphComponent.propTypes = {
  report: PropTypes.object
};
