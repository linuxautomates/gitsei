export const resolutionTimeGetGraphFilters = (args: Record<"finalFilters", any>) => {
  let { finalFilters } = args;
  /* 
      LFE-2185 :  We were storing the value for Issue Last Closed By Week (month , quarter) as 
      across = issue_created interval = week /month /quarter 
      checking the case for the same.
      We also have Issue Created By Date where across = issue_created but in that case we have
      interval = day 
     */
  const across = finalFilters?.across;
  const interval = finalFilters?.filter?.interval ?? finalFilters?.interval;
  if (across === "issue_created" && ["week", "quarter", "month"].includes(interval)) {
    finalFilters = { ...finalFilters, across: "issue_closed" };
  }
  return finalFilters;
};
