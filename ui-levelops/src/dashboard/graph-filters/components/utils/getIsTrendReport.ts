export function getIsTrendReport(report_type: string) {
  const result = (report_type || "").includes("trend");
  return result;
}
