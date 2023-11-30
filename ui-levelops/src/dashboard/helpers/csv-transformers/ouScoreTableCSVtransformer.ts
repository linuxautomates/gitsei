import { get } from "lodash";

export const ouScoreTableCSVTransformer = (data: any) => {
  const { apiData, columns } = data;
  const transformedData = apiData.map((record: any) => {
    return columns
      .map((col: any) => {
        switch (col.title) {
          case "name":
            return record?.full_name;
          case "total_score":
            return record?.score ?? 0;
          default:
            const sections = get(record, ["section_responses"], []);
            const section = sections.find((section: { name: string }) => section.name === col.title);
            if (section) {
              return get(section, ["score"], 0);
            }
            return 0;
        }
      })
      .join(",");
  });
  return transformedData;
};
