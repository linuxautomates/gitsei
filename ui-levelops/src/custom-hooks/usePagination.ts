import { useEffect, useState } from "react";

const getRange = (page: number, pageSize: number) => {
  const high = pageSize * page;
  const low = high - pageSize;
  return { high, low };
};

export const usePagination = (
  data?: Array<any>,
  pageSize: number = 10
): [number, number, (page: number) => void, Array<any>, number] => {
  const [page, setPage] = useState<number>(1);
  useEffect(() => {
    setPage(1);
  }, [data]);
  const totalCount = data?.length ?? 0;
  const { high, low } = getRange(page, pageSize);
  const currentPageData = data?.slice(low, high) ?? [];
  const totalPages = totalCount / pageSize;
  return [totalCount, page, setPage, currentPageData, totalPages];
};
