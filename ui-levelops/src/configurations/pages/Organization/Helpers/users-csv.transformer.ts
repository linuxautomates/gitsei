import { get } from "lodash";
import { tableCell } from "utils/tableUtils";

export const usersCsvTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders } = data;
  const csvData: string[] = [];
  const allColumns = [...(jsxHeaders || []), ...(columns || [])];

  apiData?.forEach((record: any) => {
    const integrationsDetails: { integrations: any; maxIntegrationCount: number } = getIntegrations(allColumns, record);
    record["integrations"] = integrationsDetails.integrations;
    for (let index = 0; index < integrationsDetails.maxIntegrationCount; index++) {
      const row = createRow(allColumns, record, index);
      csvData.push(row);
    }
  });

  return csvData;
};

const getIntegrations = (allColumns: Array<any>, record: any) => {
  let maxIntegrationCount = 1;
  const integrations: any = {};
  const integrationColumns = allColumns
    .filter((column: { key: string; title: string }) => column.key.includes("integration@"))
    .map((column: { key: string; title: string }) => column.key.replace("integration@", ""));

  integrationColumns.forEach((intCol: string) => {
    integrations[intCol] = (record.integration_user_ids || []).filter(
      (item: { name: string; value: any }) => item.name === intCol
    );
    if (maxIntegrationCount < integrations[intCol].length) maxIntegrationCount = integrations[intCol].length;
  });
  return {
    integrations: integrations,
    maxIntegrationCount
  };
};

const createRow = (allColumns: Array<any>, record: any, integrationIndex: number) =>
  allColumns
    .map((col: any) => {
      const { key, type } = col;
      let result = record[key];

      if (key.includes("integration@")) {
        const updatedKey = key.replace("integration@", "");
        const item = record.integrations[updatedKey]?.[integrationIndex];
        result = item?.user_id || "";
      }

      if (key.includes("additional@")) {
        const updatedKey = key.replace("additional@", "");
        result = get(record, ["additional_fields", updatedKey], undefined);
      }

      if (Array.isArray(result)) {
        if (!result.length) return "";
        return `"${result.join(",")}"`;
      }

      if (typeof result === "string") {
        if (result.includes(",")) {
          return `"${result}"`;
        }
        return result;
      }

      if (key?.includes("created") || key?.includes("updated") || key?.includes("modify") || type === "date") {
        if (col.title.includes("UTC")) {
          return new Date(result).toISOString().replace(/T|Z/g, " ");
        }
        return tableCell("created_at", result);
      }
      return result;
    })
    .join(",");
