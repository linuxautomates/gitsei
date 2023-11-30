import { chartProps } from "dashboard/reports/commonReports.constants";
import moment from "moment";

const ackTrendTransform = (data: any) => {
  let result = data;
  const isValid = moment(data).isValid();
  if (isValid) {
    const seconds = data;
    const duration = moment.duration(seconds, "seconds");
    result = duration.format("y[yrs] M[mos] d[d] h[hrs] m[m] s[s]");
  }

  return result;
};

export const pagerdutyAckHoursChartProps = {
  transformFn: ackTrendTransform,
  totalCountTransformFn: ackTrendTransform,
  unit: "Seconds",
  chartProps: chartProps,
  barProps: [
    {
      name: "count",
      dataKey: "count"
    }
  ],
  stacked: true
};
