import { dateTimeBoundFilterKeys, timeBoundFilterKeys } from "dashboard/graph-filters/components/DateOptionConstants";
import { tableCell } from "utils/tableUtils";
import { checkTimeSecondOrMiliSecond } from "../helper";

export const cicdCsvDrilldownDataTransformer = (data: any) => {

    const { apiData, columns, jsxHeaders, filters } = data;
    let headers = jsxHeaders ?? columns;

    return (apiData || []).map((record: any) => {
        return [...(headers || [])]
            .map((col: any) => {

                if (col.key === "buildId") {
                    return `${record?.job_name || ""}/${record?.job_run_number || ""}`;
                }

                let result = record[col.key];
                if (result === undefined) {
                    return result;
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
                if (col.key === "duration") {
                    const duration = record?.duration || 0;
                    if (duration <= 0) return 0;
                    return (duration / 60).toFixed(2);
                }

                if(dateTimeBoundFilterKeys.includes(col.key)){
                    return tableCell(`time_utc_f2`, result)
                }

                if (
                    timeBoundFilterKeys.includes(col.key) ||
                    col.key?.includes("created") ||
                    col.key?.includes("updated") ||
                    col.key?.includes("modify")
                ) {
                    if (col.key?.includes("workitem_")) {
                        result = checkTimeSecondOrMiliSecond(result);
                    }

                    if (col.title.includes("UTC")) {
                        return new Date(result).toISOString().replace(/T|Z/g, " ");
                    }
                    return tableCell("created_at", result);
                }

                return result;
            })
            .join(",");
    });
};
