import { baseColumnConfig } from "utils/base-table-config";

const renderToPercentage = (item: string) => parseFloat(item).toFixed(2);

export const BullseyeTableConfig = [
  baseColumnConfig("File Name", "name"),
  { ...baseColumnConfig("% of Function Coverage", "functions_percentage_coverage"), render: renderToPercentage },
  { ...baseColumnConfig("% of Branch Coverage", "conditions_percentage_coverage"), render: renderToPercentage },
  { ...baseColumnConfig("% of Decision Coverage", "decisions_percentage_coverage"), render: renderToPercentage }
];
