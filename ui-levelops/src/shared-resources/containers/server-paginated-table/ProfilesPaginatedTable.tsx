import { Divider } from "antd";
import React, { useEffect, useState } from "react";
import { AntAlert, AntInput, AntTable } from "shared-resources/components";
import classNames from "classnames";
import "./ProfilesPaginatedTable.scss";

interface ProfilesPaginatedTableProps {
  dataSource: any;
  columns: any;
  showInfo?: boolean;
  isModalView?: boolean;
}

const ProfilesPaginatedTable = (props: ProfilesPaginatedTableProps) => {
  const { dataSource, showInfo, isModalView } = props;
  const [totalRecords, setTotalRecords] = useState(dataSource.length);
  const [totalPages, setTotalPages] = useState(totalRecords / 10);
  const [currentPageRecords, setCurrentPageRecords] = useState(dataSource.slice(0, 10));
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState<string>("");
  const [filteredData, setFilteredData] = useState<any>(dataSource);

  useEffect(() => setTotalPages(totalRecords / 10), [totalRecords]);

  useEffect(() => {
    setTotalRecords(filteredData.length);
    setPage(1);
  }, [filteredData]);

  useEffect(() => {
    setCurrentPageRecords(filteredData.slice((page - 1) * 10, page * 10));
  }, [page, filteredData]);

  useEffect(() => {
    setFilteredData(dataSource.filter((row: any) => row?.name?.toLowerCase()?.includes(search?.toLowerCase())));
  }, [search, dataSource]);

  const handleSearch = (e: any) => {
    const name = e?.target?.value || "";
    setSearch(name);
  };

  return (
    <div className={classNames("profile-paginated-table", { "px-40": !isModalView })}>
      <Divider className="divider-spacing" />
      <AntInput className="search-box" placeholder="Start typing to filter" value={search} onChange={handleSearch} />
      {showInfo && (
        <AntAlert
          description="This list is based on the profiles that can be associated with the current collection."
          type="info"
          showIcon
        />
      )}
      <AntTable
        dataSource={currentPageRecords}
        hasPagination={true}
        hasCustomPagination={true}
        pageSize={10}
        page={page}
        columns={props.columns}
        totalPages={totalPages}
        totalRecords={totalRecords}
        size="middle"
        rowKey={"id"}
        onPageChange={setPage}
        showPageSizeOptions={false}
      />
    </div>
  );
};

export default ProfilesPaginatedTable;
