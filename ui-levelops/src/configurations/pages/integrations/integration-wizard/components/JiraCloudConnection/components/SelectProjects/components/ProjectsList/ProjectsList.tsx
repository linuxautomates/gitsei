// import { useDefaultPaginationProps } from "@harness/microfrontends/dist/modules/10-common/hooks/useDefaultPaginationProps";
import { Checkbox, Color, Container, FontVariation, TableV2, Text, Layout } from "@harness/uicore";
import React, { useEffect, useMemo, useState } from "react";
import { CellProps, Column, Renderer } from "react-table";
import { RenderLastUpdatedAt } from "./ProjectList.utils";
import { COMMON_DEFAULT_PAGE_SIZE, data } from "./ProjectsList.constants";
import css from "./ProjectsList.module.scss";

const ProjectsList = (props: ProjectsListProps): JSX.Element => {
  const { selectProjectsCount } = props;
  const [rows, setRows] = useState<ProjectsListResponse[]>(data?.content);
  const {
    total = 0,
    pageSize = COMMON_DEFAULT_PAGE_SIZE,
    pageCount = Math.ceil(total / pageSize),
    pageNumber = 0
  } = data?.pagination || {};

  // Todo - this will be utilised later.
  //   const paginationProps = useDefaultPaginationProps({
  //     itemCount: total,
  //     pageSize: pageSize,
  //     pageCount: pageCount,
  //     pageIndex: pageNumber
  //   });

  const handleSelectAll = (event: React.FormEvent<HTMLInputElement>) => {
    event.stopPropagation();
    const allSelected = rows?.every(row => row.selected);
    const updatedRows = rows?.map(row => ({ ...row, selected: !allSelected }));
    setRows(updatedRows);
  };

  const handleProjectSelect = (event: React.FormEvent<HTMLInputElement>, selectedRowId: string) => {
    const updatedRows = rows?.map(el => {
      if (el.id === selectedRowId) {
        return {
          ...el,
          selected: event.currentTarget.checked
        };
      } else {
        return el;
      }
    });
    setRows(updatedRows);
  };

  const RenderHeaderForProjects = (): JSX.Element => {
    return (
      <Layout.Horizontal>
        <Checkbox onChange={handleSelectAll} />
        <Container flex={{ justifyContent: "center", alignItems: "center" }}>
          <Text color={Color.BLACK} font={{ variation: FontVariation.TABLE_HEADERS }}>
            {`Projects (${selectProjectsCount})`}
          </Text>
        </Container>
      </Layout.Horizontal>
    );
  };

  const RenderProjects: Renderer<CellProps<ProjectsListResponse>> = ({ row }): JSX.Element => {
    const rowdata = row.original;
    const {
      selected,
      projectsInfo: { name, key },
      id
    } = rowdata;
    return (
      <Layout.Horizontal>
        <Checkbox checked={selected} onChange={event => handleProjectSelect(event, id)} />
        <Layout.Vertical>
          <Text>{name}</Text>
          <Layout.Horizontal>
            <Text padding={{ right: "xxlarge" }}>{`Key: ${key}`}</Text>
            <Layout.Horizontal>
              <Text>{`ID: ${id}`}</Text>
            </Layout.Horizontal>
          </Layout.Horizontal>
        </Layout.Vertical>
      </Layout.Horizontal>
    );
  };

  const columns: Column<ProjectsListResponse>[] = useMemo(
    () => [
      {
        accessor: "id",
        width: "60%",
        Header: RenderHeaderForProjects,
        Cell: RenderProjects,
        disableSortBy: true
      },
      {
        accessor: "lastUpdatedAt",
        width: "40%",
        Cell: RenderLastUpdatedAt,
        Header: "LAST UPDATED AT",
        disableSortBy: true
      }
    ],
    [rows]
  );

  if (rows?.length) {
    return (
      <TableV2<ProjectsListResponse>
        className={css.paddingTable}
        data={rows as ProjectsListResponse[]}
        columns={columns}
        // pagination={paginationProps}
      />
    );
  } else {
    return <></>;
  }
};

export default ProjectsList;
