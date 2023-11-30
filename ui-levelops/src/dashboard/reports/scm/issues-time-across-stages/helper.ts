export const getTotalKeyHelper = (params: { metric: string }) => {
  const { metric } = params;
  if (metric === "average_time") {
    return "mean";
  }
  return "median";
};
