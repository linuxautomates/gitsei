import { convertEpochToDate, DateFormats } from "../../utils/dateUtils";

export const pagerdutyServicesTransformer = (data: any) => {
  const { apiData } = data;

  let mappedData;

  switch (data.reportType) {
    case "pagerduty_hotspot_report":
      mappedData = (apiData || []).map((record: { name: any; aggregations: any }) => {
        return {
          name: record.name,
          ...(record.aggregations || []).reduce(
            (acc: { [x: string]: any }, obj: { key: string | number; count: any }) => {
              const curKey = obj.key === "" ? "unknown" : obj.key;
              acc[curKey] = obj.count;
              return acc;
            },
            {}
          )
        };
      });
      break;

    case "pagerduty_release_incidents":
      mappedData = (apiData || []).map((record: any) => {
        const name = `${convertEpochToDate(record.to, DateFormats.DAY, true)} - ${convertEpochToDate(
          record.from,
          DateFormats.DAY,
          true
        )}`;
        return {
          name,
          incidents_count: record.incidents_count,
          alerts_count: record.alerts_count
        };
      });
      break;

    case "pagerduty_ack_trend":
      mappedData = (apiData || []).map((record: { name: any; aggregations: any }) => {
        return {
          name: record.name,
          ...(record.aggregations || []).reduce(
            (acc: { [x: string]: any }, obj: { key: string | number; value: any }) => {
              acc[convertEpochToDate(obj.key, DateFormats.DAY, true)] = obj.value;
              return acc;
            },
            {}
          )
        };
      });
      break;

    case "pagerduty_after_hours":
      mappedData = (apiData || []).map((item: any) => {
        return {
          name: item.name,
          value: item.after_hours_minutes
        };
      });
      break;
  }
  return {
    data: mappedData
  };
  // @ts-ignore
  // const seriesData = (apiData || []).map((record: any) => ({
  //   name: record.name,
  //   color: randomColor({ hue: "red" }),
  //   title: { label: "Service", value: record.name },
  //   subTitle: { label: "Service", value: record.name },
  //   keyCount: 1,
  //   count: (record.aggregations || []).reduce((acc: number, obj: any) => {
  //     acc = acc + (obj.count || 0);
  //     return acc;
  //   }, 0)
  // }));
  // console.log(seriesData);
  //
  //
  // let totalCount = seriesData.reduce((acc: string | number, obj: { count: any }) => {
  //   acc = acc + (obj.count || 0);
  //   return acc;
  // }, 0);
  //
  // console.log(totalCount);
  //
  // const mappedData = seriesData.map((data: any) => ({
  //   ...data,
  //   title: { label: data.name, value: data.count },
  //   subTitle: { label: "Service", value: data.name }
  // }));
  //
  //
  // return {
  //   data: mappedData,
  //   total: totalCount,
  //   dataKey
  // };
};
