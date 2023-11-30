export const chartLegendSortingComparator = (value1: any, value2: any) => {
    const key1 = value1?.dataKey?.replace(/_/g, " ").trim().toLowerCase();
    const key2 = value2?.dataKey?.replace(/_/g, " ").trim().toLowerCase();
    if (key1 < key2) return -1;
    if (key1 > key2) return 1;
    return 0;
  };
  
  
 export  const getUpdatedFilters = (filters: any, updatedValue: boolean) => {
    const updatedFilters: any = {};
    for (const key in filters) {
      updatedFilters[key] = updatedValue;
    }
    return updatedFilters;
  };