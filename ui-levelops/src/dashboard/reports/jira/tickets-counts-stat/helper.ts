export const mapFiltersBeforeCallIssueSingleStat = (filter: any) => {
    const finalFilters = {
        ...filter,
        widget: "tickets_counts_stat",
        "across_limit": 2147483647,
    };
    return finalFilters;
};